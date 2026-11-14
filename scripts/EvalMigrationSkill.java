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
 * Exit code 0 when the run is acceptable against the baseline (better when the skill changed, not worse
 * otherwise), 1 when it is not, 2 on bad arguments.
 */
public class EvalMigrationSkill {

    static final String MARKER = "TODO unified:";
    static final String KEPT = "Escape Hatch kept:";

    /** Metric name, whether a higher value is better. Ordered as reported. */
    static final Map<String, Boolean> METRICS = new LinkedHashMap<>();
    static {
        METRICS.put("markersBefore", null);
        METRICS.put("resolved", true);
        METRICS.put("kept", null);
        METRICS.put("untouched", false);
        METRICS.put("escapeHatches", false);
        METRICS.put("singleToolTests", false);
        METRICS.put("compiles", true);
        METRICS.put("browserless.passed", true);
        METRICS.put("browserless.failed", false);
        METRICS.put("playwright.passed", true);
        METRICS.put("playwright.failed", false);
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
        Map<String, Map<String, Long>> perFile = new LinkedHashMap<>();
        Map<String, Long> run = new LinkedHashMap<>();
        for (String metric : METRICS.keySet()) {
            run.put(metric, 0L);
        }
        for (String file : fixtures) {
            String old = Files.readString(before.resolve(file));
            Path afterFile = after.resolve(file);
            String now = Files.exists(afterFile) ? Files.readString(afterFile) : "";
            Map<String, Long> m = new LinkedHashMap<>();
            m.put("markersBefore", count(old, MARKER));
            m.put("untouched", count(now, MARKER));
            m.put("kept", count(now, KEPT));
            m.put("resolved", Math.max(0, m.get("markersBefore") - m.get("untouched") - m.get("kept")));
            m.put("escapeHatches", count(now, "TestBenchEscapeHatch.of(") + count(now, "BrowserlessEscapeHatch.of("));
            // tests (or classes) still tied to the Testing Tool of their Escape Hatch or Browserless-only pattern
            m.put("singleToolTests", count(now, "@RequiresTestingTool(TestingTool.SELENIUM)")
                    + count(now, "@RequiresTestingTool(TestingTool.BROWSERLESS)"));
            perFile.put(file, m);
            m.forEach((k, v) -> run.merge(k, v, Long::sum));
        }
        run.put("compiles", Files.exists(reports.resolve("compiled")) ? 1L : 0L);
        for (String tool : List.of("browserless", "playwright")) {
            long[] counts = surefire(reports.resolve(tool));
            run.put(tool + ".passed", counts[0]);
            run.put(tool + ".failed", counts[1]);
        }

        String skillHash = sha256(Files.readString(Path.of(a.get("skill"))));
        Path baselineFile = Path.of(a.get("baseline"));
        Map<String, String> baseline = Files.exists(baselineFile) ? readJson(Files.readString(baselineFile)) : Map.of();
        boolean skillChanged = !skillHash.equals(baseline.get("skillSha256"));

        // verdict: better (no metric worse, one better), same, or worse
        List<String> worse = new ArrayList<>();
        List<String> better = new ArrayList<>();
        for (var e : METRICS.entrySet()) {
            if (e.getValue() == null || !baseline.containsKey(e.getKey())) {
                continue;
            }
            long was = Long.parseLong(baseline.get(e.getKey()));
            long is = run.get(e.getKey());
            if (is != was) {
                ((is > was) == e.getValue() ? better : worse).add(e.getKey());
            }
        }
        String verdict = baseline.isEmpty() ? "no baseline yet" : !worse.isEmpty() ? "worse" : better.isEmpty() ? "same" : "better";
        boolean acceptable = baseline.isEmpty() || worse.isEmpty() && (!skillChanged || !better.isEmpty());

        Map<String, String> result = new LinkedHashMap<>();
        result.put("skillSha256", skillHash);
        run.forEach((k, v) -> result.put(k, String.valueOf(v)));
        Path out = Path.of(a.get("out"));
        Files.writeString(out.resolveSibling("result.json"), toJson(result));
        String diff = a.containsKey("diff") && Files.exists(Path.of(a.get("diff"))) ? Files.readString(Path.of(a.get("diff"))) : "";
        String report = format.equals("md")
                ? markdown(run, baseline, perFile, verdict, skillChanged, acceptable, diff)
                : html(run, baseline, perFile, verdict, skillChanged, acceptable, diff);
        Files.writeString(out, report);
        if (a.containsKey("accept") && acceptable) {
            Files.writeString(baselineFile, toJson(result));
            System.out.println("Baseline updated: " + baselineFile);
        }
        System.out.println("Verdict: " + verdict + (skillChanged ? " (skill changed: must be better)" : "")
                + (acceptable ? ", acceptable" : ", NOT acceptable") + ". Report: " + out);
        System.exit(acceptable ? 0 : 1);
    }

    static long count(String text, String needle) {
        long n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    /** Passed and failed (failures + errors) test cases in the Surefire XML reports of a directory. */
    static long[] surefire(Path dir) throws IOException {
        long passed = 0;
        long failed = 0;
        if (!Files.isDirectory(dir)) {
            return new long[] {0, 0};
        }
        Pattern suite = Pattern.compile("<testsuite [^>]*>");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().startsWith("TEST-")).toList()) {
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

    /** A flat JSON object of string and number values. */
    static Map<String, String> readJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|[-0-9]+)").matcher(json);
        while (m.find()) {
            map.put(m.group(1), m.group(3) != null ? m.group(3) : m.group(2));
        }
        return map;
    }

    static String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (var e : map.entrySet()) {
            boolean number = e.getValue().matches("-?\\d+");
            sb.append("  \"").append(e.getKey()).append("\": ")
                    .append(number ? e.getValue() : "\"" + e.getValue() + "\"")
                    .append(++i < map.size() ? ",\n" : "\n");
        }
        return sb.append("}\n").toString();
    }

    static String change(String metric, Map<String, Long> run, Map<String, String> baseline) {
        Boolean higherIsBetter = METRICS.get(metric);
        if (higherIsBetter == null || !baseline.containsKey(metric)) {
            return "";
        }
        long was = Long.parseLong(baseline.get(metric));
        long is = run.get(metric);
        return is == was ? "=" : (is > was) == higherIsBetter ? "better" : "worse";
    }

    static String markdown(Map<String, Long> run, Map<String, String> baseline, Map<String, Map<String, Long>> perFile,
            String verdict, boolean skillChanged, boolean acceptable, String diff) {
        StringBuilder sb = new StringBuilder("# Migration skill evaluation\n\n");
        sb.append("**Verdict:** ").append(verdict).append(skillChanged ? " (the skill changed: it must be better)" : "")
                .append(acceptable ? ". Acceptable." : ". **Not acceptable.**").append("\n\n");
        sb.append("| Metric | Baseline | This run | |\n|---|---|---|---|\n");
        for (String metric : METRICS.keySet()) {
            sb.append("| ").append(metric).append(" | ").append(baseline.getOrDefault(metric, ""))
                    .append(" | ").append(run.get(metric)).append(" | ").append(change(metric, run, baseline)).append(" |\n");
        }
        sb.append("\n## Per fixture\n\n| File | Markers before | Resolved | Kept | Untouched | Escape Hatches | Single-tool tests |\n|---|---|---|---|---|---|---|\n");
        perFile.forEach((file, m) -> sb.append("| ").append(file).append(" | ").append(m.get("markersBefore"))
                .append(" | ").append(m.get("resolved")).append(" | ").append(m.get("kept")).append(" | ")
                .append(m.get("untouched")).append(" | ").append(m.get("escapeHatches")).append(" | ")
                .append(m.get("singleToolTests")).append(" |\n"));
        if (!diff.isBlank()) {
            sb.append("\n## Changes\n\n```diff\n").append(diff).append("\n```\n");
        }
        return sb.toString();
    }

    static String html(Map<String, Long> run, Map<String, String> baseline, Map<String, Map<String, Long>> perFile,
            String verdict, boolean skillChanged, boolean acceptable, String diff) {
        StringBuilder sb = new StringBuilder("""
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Migration skill evaluation</title>
                <style>
                  body { font-family: system-ui, sans-serif; margin: 2rem auto; max-width: 70rem; padding: 0 1rem; color: #1b1f24; }
                  table { border-collapse: collapse; margin: 1rem 0; }
                  th, td { border: 1px solid #d0d7de; padding: .35rem .7rem; text-align: left; }
                  th { background: #f6f8fa; }
                  .better { color: #1a7f37; font-weight: 600; } .worse { color: #cf222e; font-weight: 600; }
                  .verdict { font-size: 1.2rem; padding: .6rem 1rem; border-radius: 6px; display: inline-block; }
                  .ok { background: #dafbe1; } .ko { background: #ffebe9; }
                  pre { background: #f6f8fa; padding: 1rem; overflow-x: auto; font-size: .85rem; }
                  .add { color: #1a7f37; } .del { color: #cf222e; } .hunk { color: #8250df; }
                </style></head><body>
                <h1>Migration skill evaluation</h1>
                """);
        sb.append("<p class=\"verdict ").append(acceptable ? "ok" : "ko").append("\">Verdict: <strong>").append(verdict)
                .append("</strong>").append(skillChanged ? " (the skill changed: it must be better)" : "")
                .append(acceptable ? " · acceptable" : " · not acceptable").append("</p>\n");
        sb.append("<table><tr><th>Metric</th><th>Baseline</th><th>This run</th><th></th></tr>\n");
        for (String metric : METRICS.keySet()) {
            String c = change(metric, run, baseline);
            sb.append("<tr><td>").append(metric).append("</td><td>").append(baseline.getOrDefault(metric, ""))
                    .append("</td><td>").append(run.get(metric)).append("</td><td class=\"").append(c).append("\">")
                    .append(c).append("</td></tr>\n");
        }
        sb.append("</table>\n<h2>Per fixture</h2>\n<table><tr><th>File</th><th>Markers before</th><th>Resolved</th>"
                + "<th>Kept</th><th>Untouched</th><th>Escape Hatches</th><th>Single-tool tests</th></tr>\n");
        perFile.forEach((file, m) -> sb.append("<tr><td>").append(escape(file)).append("</td><td>")
                .append(m.get("markersBefore")).append("</td><td>").append(m.get("resolved")).append("</td><td>")
                .append(m.get("kept")).append("</td><td>").append(m.get("untouched")).append("</td><td>")
                .append(m.get("escapeHatches")).append("</td><td>").append(m.get("singleToolTests")).append("</td></tr>\n"));
        sb.append("</table>\n");
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

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
