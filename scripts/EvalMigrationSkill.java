import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scores one run of the resolve-migration-markers skill and compares it with the committed baseline.
 * Run by scripts/eval-migration-skill.sh; see its usage. Single-file program: java scripts/EvalMigrationSkill.java ...
 *
 * <pre>
 * java EvalMigrationSkill.java --before DIR --after DIR --reports DIR --skill FILE --baseline FILE
 *                              --format html|md --out FILE [--diff FILE] [--accept]
 * </pre>
 *
 * The run is compared with the baseline fixture by fixture, over the fixtures both have: a fixture the recipes no
 * longer leave markers in has left the evaluation ("no longer needs the skill"), a new one is reported and compared
 * against nothing. Marker metrics are compared as rates of the fixture's markers, so a fixture whose recipe output
 * changed is still compared like with like.
 *
 * Exit code 0 when the run is acceptable against the baseline (better when the skill changed, not worse
 * otherwise), 1 when it is not, 2 on bad arguments.
 */
public class EvalMigrationSkill {

    static final String MARKER = "TODO unified:";
    static final String KEPT = "Escape Hatch kept:";
    static final List<String> TOOLS = List.of("browserless", "playwright");

    /** The counts scored per fixture, ordered as reported. */
    static final List<String> COUNTS = List.of("markersBefore", "resolved", "kept", "untouched", "escapeHatches",
            "singleToolTests", "browserless.passed", "browserless.failed", "playwright.passed", "playwright.failed");

    /**
     * What a fixture is compared on: a count, or a count over a denominator (a rate), and whether higher is better.
     * Kept markers are neither: keeping an Escape Hatch with a reason is a valid outcome.
     */
    record Measure(String name, String count, String over, boolean higherIsBetter) {

        /** 1 better, -1 worse, 0 same. */
        int compare(Map<String, Long> was, Map<String, Long> is) {
            long wasCount = was.getOrDefault(count, 0L);
            long isCount = is.getOrDefault(count, 0L);
            int c;
            if (over == null) {
                c = Long.compare(isCount, wasCount);
            } else {
                long wasOver = was.getOrDefault(over, 0L);
                long isOver = is.getOrDefault(over, 0L);
                if (wasOver == 0 || isOver == 0) {
                    return 0;
                }
                // isCount / isOver against wasCount / wasOver, without rounding
                c = Long.compare(isCount * wasOver, wasCount * isOver);
            }
            return higherIsBetter ? c : -c;
        }
    }

    static final List<Measure> MEASURES = List.of(
            new Measure("resolved rate", "resolved", "markersBefore", true),
            new Measure("untouched rate", "untouched", "markersBefore", false),
            new Measure("escapeHatches", "escapeHatches", null, false),
            new Measure("singleToolTests", "singleToolTests", null, false),
            new Measure("browserless.passed", "browserless.passed", null, true),
            new Measure("browserless.failed", "browserless.failed", null, false),
            new Measure("playwright.passed", "playwright.passed", null, true),
            new Measure("playwright.failed", "playwright.failed", null, false));

    /** A fixture's place in the comparison. */
    enum Status {
        COMPARED("compared"), NEW("new: compared against nothing"), GONE("no longer needs the skill");

        final String label;

        Status(String label) {
            this.label = label;
        }
    }

    /** One fixture's row: its scores in the baseline and in this run (either may be null), and how it compares. */
    record Row(String file, Status status, Map<String, Long> was, Map<String, Long> is, List<String> better,
            List<String> worse) {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> a = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--accept")) {
                a.put("accept", "true");
            } else if (args[i].startsWith("--") && i + 1 < args.length) {
                a.put(args[i].substring(2), args[++i]);
            }
        }
        for (String required : List.of("before", "after", "reports", "skill", "baseline", "out")) {
            if (!a.containsKey(required)) {
                System.err.println("missing --" + required);
                System.exit(2);
            }
        }
        Path before = Path.of(a.get("before"));
        Path after = Path.of(a.get("after"));
        Path reports = Path.of(a.get("reports"));
        String format = a.getOrDefault("format", "html");

        // the fixtures: the files that had markers before the run
        List<String> fixtures = new ArrayList<>();
        try (Stream<Path> files = Files.list(before)) {
            files.filter(f -> f.toString().endsWith(".java")).sorted().forEach(f -> fixtures.add(f.getFileName().toString()));
        }
        Map<String, Map<String, Long>> run = new LinkedHashMap<>();
        for (String file : fixtures) {
            String old = Files.readString(before.resolve(file));
            Path afterFile = after.resolve(file);
            String now = Files.exists(afterFile) ? Files.readString(afterFile) : "";
            Map<String, Long> m = new LinkedHashMap<>();
            long markersBefore = count(old, MARKER);
            long kept = count(now, KEPT);
            long untouched = count(now, MARKER);
            m.put("markersBefore", markersBefore);
            m.put("resolved", Math.max(0, markersBefore - untouched - kept));
            m.put("kept", kept);
            m.put("untouched", untouched);
            m.put("escapeHatches", count(now, "TestBenchEscapeHatch.of(") + count(now, "BrowserlessEscapeHatch.of("));
            // tests (or classes) still tied to the Testing Tool of their Escape Hatch or Browserless-only pattern
            m.put("singleToolTests", count(now, "@RequiresTestingTool(TestingTool.SELENIUM)")
                    + count(now, "@RequiresTestingTool(TestingTool.BROWSERLESS)"));
            String testClass = file.substring(0, file.length() - ".java".length());
            for (String tool : TOOLS) {
                long[] counts = surefire(reports.resolve(tool), testClass);
                m.put(tool + ".passed", counts[0]);
                m.put(tool + ".failed", counts[1]);
            }
            run.put(file, m);
        }
        long compiles = Files.exists(reports.resolve("compiled")) ? 1L : 0L;

        String skillHash = sha256(Files.readString(Path.of(a.get("skill"))));
        Path baselineFile = Path.of(a.get("baseline"));
        Map<String, Object> baseline = Files.exists(baselineFile) ? new Json(Files.readString(baselineFile)).object() : Map.of();
        boolean skillChanged = !skillHash.equals(baseline.get("skillSha256"));
        Map<String, Map<String, Long>> was = fixtures(baseline);

        // per fixture, over the fixtures in both
        List<Row> rows = new ArrayList<>();
        TreeSet<String> all = new TreeSet<>(run.keySet());
        if (was != null) {
            all.addAll(was.keySet());
        }
        for (String file : all) {
            Map<String, Long> w = was == null ? null : was.get(file);
            Map<String, Long> i = run.get(file);
            List<String> better = new ArrayList<>();
            List<String> worse = new ArrayList<>();
            if (w != null && i != null) {
                for (Measure measure : MEASURES) {
                    int c = measure.compare(w, i);
                    if (c > 0) {
                        better.add(measure.name());
                    } else if (c < 0) {
                        worse.add(measure.name());
                    }
                }
            }
            rows.add(new Row(file, w == null ? Status.NEW : i == null ? Status.GONE : Status.COMPARED, w, i, better, worse));
        }
        List<String> worse = new ArrayList<>();
        List<String> better = new ArrayList<>();
        for (Row row : rows) {
            row.worse().forEach(m -> worse.add(row.file() + ": " + m));
            row.better().forEach(m -> better.add(row.file() + ": " + m));
        }
        if (was != null && baseline.get("compiles") instanceof Long c && c != compiles) {
            (compiles > c ? better : worse).add("compiles");
        }

        String verdict = was == null ? "no baseline in this format yet" : !worse.isEmpty() ? "worse" : better.isEmpty() ? "same" : "better";
        boolean acceptable = was == null || worse.isEmpty() && (!skillChanged || !better.isEmpty());

        Map<String, Long> totals = totals(run.values());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skillSha256", skillHash);
        result.put("compiles", compiles);
        result.put("totals", totals);
        result.put("fixtures", run);
        Path out = Path.of(a.get("out"));
        Files.writeString(out.resolveSibling("result.json"), Json.write(result, "") + "\n");
        String diff = a.containsKey("diff") && Files.exists(Path.of(a.get("diff"))) ? Files.readString(Path.of(a.get("diff"))) : "";
        Report report = new Report(verdict, skillChanged, acceptable, compiles, baseline.get("compiles"), totals,
                was == null ? null : totals(compared(rows, true)), totals(compared(rows, false)), rows, better, worse, diff);
        Files.writeString(out, format.equals("md") ? report.markdown() : report.html());
        if (a.containsKey("accept") && acceptable) {
            Files.writeString(baselineFile, Json.write(result, "") + "\n");
            System.out.println("Baseline updated: " + baselineFile);
        }
        System.out.println("Verdict: " + verdict + (skillChanged ? " (skill changed: must be better)" : "")
                + (acceptable ? ", acceptable" : ", NOT acceptable") + ". Report: " + out);
        System.exit(acceptable ? 0 : 1);
    }

    /** The baseline's per-fixture scores, or null for a missing baseline or one with totals only (the old format). */
    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Long>> fixtures(Map<String, Object> baseline) {
        if (!(baseline.get("fixtures") instanceof Map<?, ?> fixtures)) {
            return null;
        }
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        fixtures.forEach((file, scores) -> result.put((String) file, (Map<String, Long>) scores));
        return result;
    }

    /** The scores of the compared fixtures, in the baseline ({@code was}) or in this run. */
    static List<Map<String, Long>> compared(List<Row> rows, boolean was) {
        return rows.stream().filter(r -> r.status() == Status.COMPARED).map(r -> was ? r.was() : r.is()).toList();
    }

    static Map<String, Long> totals(Iterable<Map<String, Long>> scores) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (String count : COUNTS) {
            totals.put(count, 0L);
        }
        for (Map<String, Long> m : scores) {
            m.forEach((k, v) -> totals.merge(k, v, Long::sum));
        }
        return totals;
    }

    static long count(String text, String needle) {
        long n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    /**
     * Passed and failed (failures + errors) test cases of one test class (and its nested classes) in the Surefire XML
     * reports of a directory.
     */
    static long[] surefire(Path dir, String testClass) throws IOException {
        long passed = 0;
        long failed = 0;
        if (!Files.isDirectory(dir)) {
            return new long[] {0, 0};
        }
        Pattern name = Pattern.compile("TEST-(.+\\.)?" + Pattern.quote(testClass) + "(\\$.+)?\\.xml");
        Pattern suite = Pattern.compile("<testsuite [^>]*>");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> name.matcher(p.getFileName().toString()).matches()).toList()) {
                Matcher m = suite.matcher(Files.readString(f));
                if (m.find()) {
                    String tag = m.group();
                    long tests = attr(tag, "tests");
                    long fail = attr(tag, "failures") + attr(tag, "errors");
                    passed += tests - fail - attr(tag, "skipped");
                    failed += fail;
                }
            }
        }
        return new long[] {passed, failed};
    }

    static long attr(String tag, String name) {
        Matcher m = Pattern.compile(" " + name + "=\"(\\d+)\"").matcher(tag);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** "3/4 (75%)", or the count alone over nothing. */
    static String rate(long count, long over) {
        return over == 0 ? count + "/0" : count + "/" + over + " (" + Math.round(100.0 * count / over) + "%)";
    }

    /** JSON of objects, strings and whole numbers: what the baseline holds. */
    static final class Json {
        private final String s;
        private int i;

        Json(String s) {
            this.s = s;
        }

        Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            if (peek() == '}') {
                i++;
                return map;
            }
            do {
                String key = string();
                expect(':');
                map.put(key, value());
            } while (next() == ',');
            i--;
            expect('}');
            return map;
        }

        private Object value() {
            char c = peek();
            if (c == '{') {
                return object();
            }
            if (c == '"') {
                return string();
            }
            int start = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '-')) {
                i++;
            }
            return Long.parseLong(s.substring(start, i));
        }

        private String string() {
            expect('"');
            int start = i;
            while (s.charAt(i) != '"') {
                i++;
            }
            return s.substring(start, i++);
        }

        private char peek() {
            while (Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            return s.charAt(i);
        }

        private char next() {
            char c = peek();
            i++;
            return c;
        }

        private void expect(char c) {
            if (next() != c) {
                throw new IllegalArgumentException("expected " + c + " at " + (i - 1));
            }
        }

        static String write(Object value, String indent) {
            if (value instanceof Map<?, ?> map) {
                StringBuilder sb = new StringBuilder("{\n");
                int n = 0;
                for (var e : map.entrySet()) {
                    sb.append(indent).append("  \"").append(e.getKey()).append("\": ")
                            .append(write(e.getValue(), indent + "  ")).append(++n < map.size() ? ",\n" : "\n");
                }
                return sb.append(indent).append("}").toString();
            }
            return value instanceof String text ? "\"" + text + "\"" : String.valueOf(value);
        }
    }

    /**
     * The report: the verdict, the totals over the compared fixtures with their rates, the per-fixture comparison and
     * the agent's changes. {@code wasCompared} is null without a baseline in this format.
     */
    record Report(String verdict, boolean skillChanged, boolean acceptable, long compiles, Object baselineCompiles,
            Map<String, Long> all, Map<String, Long> wasCompared, Map<String, Long> isCompared, List<Row> rows,
            List<String> better, List<String> worse, String diff) {

        /** The summary rows: name, baseline, this run (compared fixtures), this run (all fixtures). */
        List<List<String>> summary() {
            List<List<String>> lines = new ArrayList<>();
            lines.add(List.of("Fixtures", wasCompared == null ? "" : String.valueOf(count(Status.COMPARED) + count(Status.GONE)),
                    String.valueOf(count(Status.COMPARED)), String.valueOf(count(Status.COMPARED) + count(Status.NEW))));
            lines.add(List.of("Compiles", baselineCompiles == null ? "" : yes(baselineCompiles), yes(compiles), yes(compiles)));
            lines.add(rateLine("Markers resolved", "resolved", "markersBefore"));
            lines.add(rateLine("Markers kept", "kept", "markersBefore"));
            lines.add(rateLine("Markers untouched", "untouched", "markersBefore"));
            lines.add(countLine("Escape Hatches", "escapeHatches"));
            lines.add(countLine("Single-tool tests", "singleToolTests"));
            for (String tool : TOOLS) {
                lines.add(testLine("Tests passed on " + tool, tool));
            }
            return lines;
        }

        private long count(Status status) {
            return rows.stream().filter(r -> r.status() == status).count();
        }

        private static String yes(Object value) {
            return Long.valueOf(1).equals(value) ? "yes" : "no";
        }

        private List<String> rateLine(String name, String count, String over) {
            return List.of(name, wasCompared == null ? "" : rate(wasCompared.get(count), wasCompared.get(over)),
                    rate(isCompared.get(count), isCompared.get(over)), rate(all.get(count), all.get(over)));
        }

        private List<String> countLine(String name, String count) {
            return List.of(name, wasCompared == null ? "" : String.valueOf(wasCompared.get(count)),
                    String.valueOf(isCompared.get(count)), String.valueOf(all.get(count)));
        }

        private List<String> testLine(String name, String tool) {
            return List.of(name, wasCompared == null ? "" : tests(wasCompared, tool), tests(isCompared, tool), tests(all, tool));
        }

        private static String tests(Map<String, Long> m, String tool) {
            long passed = m.get(tool + ".passed");
            return rate(passed, passed + m.get(tool + ".failed"));
        }

        static final List<String> SUMMARY_HEAD = List.of("", "Baseline", "This run, compared fixtures", "This run, all fixtures");
        static final List<String> FIXTURE_HEAD = List.of("File", "Status", "Resolved", "Kept", "Untouched", "Escape Hatches",
                "Single-tool tests", "Browserless passed", "Playwright passed", "Compared with the baseline");

        /** A per-fixture row, its cells as text; the last is the comparison. */
        static List<String> fixtureCells(Row row) {
            Map<String, Long> m = row.is() != null ? row.is() : row.was();
            String prefix = row.is() == null ? "baseline: " : "";
            String comparison = row.status() != Status.COMPARED ? ""
                    : row.better().isEmpty() && row.worse().isEmpty() ? "same"
                    : String.join(", ", Stream.concat(row.better().stream().map(b -> "better " + b),
                            row.worse().stream().map(w -> "worse " + w)).toList());
            return List.of(row.file(), row.status().label, prefix + rate(m.get("resolved"), m.get("markersBefore")),
                    String.valueOf(m.get("kept")), String.valueOf(m.get("untouched")), String.valueOf(m.get("escapeHatches")),
                    String.valueOf(m.get("singleToolTests")), tests(m, "browserless"), tests(m, "playwright"), comparison);
        }

        String heading() {
            return "Verdict: " + verdict + (skillChanged ? " (the skill changed: it must be better)" : "")
                    + (acceptable ? ". Acceptable." : ". Not acceptable.");
        }

        String markdown() {
            StringBuilder sb = new StringBuilder("# Migration skill evaluation\n\n");
            sb.append("**").append(heading()).append("**\n\n");
            appendChanges(sb, "Better", better);
            appendChanges(sb, "Worse", worse);
            sb.append("The run is compared with the baseline over the fixtures both have; marker metrics as rates of each "
                    + "fixture's markers.\n\n");
            table(sb, SUMMARY_HEAD, summary());
            sb.append("\n## Per fixture\n\n");
            table(sb, FIXTURE_HEAD, rows.stream().map(Report::fixtureCells).toList());
            if (!diff.isBlank()) {
                sb.append("\n## Changes\n\n```diff\n").append(diff).append("\n```\n");
            }
            return sb.toString();
        }

        private static void appendChanges(StringBuilder sb, String title, List<String> changes) {
            if (!changes.isEmpty()) {
                sb.append(title).append(": ").append(String.join("; ", changes)).append("\n\n");
            }
        }

        private static void table(StringBuilder sb, List<String> head, List<List<String>> lines) {
            sb.append("| ").append(String.join(" | ", head)).append(" |\n|")
                    .append("---|".repeat(head.size())).append("\n");
            for (List<String> line : lines) {
                sb.append("| ").append(String.join(" | ", line)).append(" |\n");
            }
        }

        String html() {
            StringBuilder sb = new StringBuilder("""
                    <!doctype html>
                    <html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Migration skill evaluation</title>
                    <style>
                      body { font-family: system-ui, sans-serif; margin: 2rem auto; max-width: 80rem; padding: 0 1rem; color: #1b1f24; }
                      table { border-collapse: collapse; margin: 1rem 0; }
                      th, td { border: 1px solid #d0d7de; padding: .35rem .7rem; text-align: left; }
                      th { background: #f6f8fa; }
                      .better { color: #1a7f37; font-weight: 600; } .worse { color: #cf222e; font-weight: 600; }
                      .gone, .new { color: #57606a; font-style: italic; }
                      .verdict { font-size: 1.2rem; padding: .6rem 1rem; border-radius: 6px; display: inline-block; }
                      .ok { background: #dafbe1; } .ko { background: #ffebe9; }
                      pre { background: #f6f8fa; padding: 1rem; overflow-x: auto; font-size: .85rem; }
                      .add { color: #1a7f37; } .del { color: #cf222e; } .hunk { color: #8250df; }
                    </style></head><body>
                    <h1>Migration skill evaluation</h1>
                    """);
            sb.append("<p class=\"verdict ").append(acceptable ? "ok" : "ko").append("\">").append(escape(heading())).append("</p>\n");
            if (!better.isEmpty()) {
                sb.append("<p class=\"better\">Better: ").append(escape(String.join("; ", better))).append("</p>\n");
            }
            if (!worse.isEmpty()) {
                sb.append("<p class=\"worse\">Worse: ").append(escape(String.join("; ", worse))).append("</p>\n");
            }
            sb.append("<p>The run is compared with the baseline over the fixtures both have; marker metrics as rates of "
                    + "each fixture's markers.</p>\n");
            htmlTable(sb, SUMMARY_HEAD, summary(), null);
            sb.append("<h2>Per fixture</h2>\n");
            htmlTable(sb, FIXTURE_HEAD, rows.stream().map(Report::fixtureCells).toList(), rows);
            if (!diff.isBlank()) {
                sb.append("<h2>Changes</h2>\n<pre>");
                for (String line : diff.split("\n", -1)) {
                    String css = line.startsWith("+") && !line.startsWith("+++") ? "add"
                            : line.startsWith("-") && !line.startsWith("---") ? "del" : line.startsWith("@@") ? "hunk" : "";
                    sb.append(css.isEmpty() ? escape(line) : "<span class=\"" + css + "\">" + escape(line) + "</span>").append('\n');
                }
                sb.append("</pre>\n");
            }
            return sb.append("</body></html>\n").toString();
        }

        private static void htmlTable(StringBuilder sb, List<String> head, List<List<String>> lines, List<Row> rows) {
            sb.append("<table><tr>");
            head.forEach(h -> sb.append("<th>").append(escape(h)).append("</th>"));
            sb.append("</tr>\n");
            for (int n = 0; n < lines.size(); n++) {
                Row row = rows == null ? null : rows.get(n);
                String css = row == null ? "" : row.status() == Status.GONE ? "gone" : row.status() == Status.NEW ? "new"
                        : !row.worse().isEmpty() ? "worse" : !row.better().isEmpty() ? "better" : "";
                sb.append(css.isEmpty() ? "<tr>" : "<tr class=\"" + css + "\">");
                lines.get(n).forEach(cell -> sb.append("<td>").append(escape(cell)).append("</td>"));
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
        }
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
