#!/usr/bin/env bash
# Evaluates the resolve-migration-markers agent skill (#119): runs it with Claude Code on the demo's migrated
# fixtures in a scratch worktree, runs their tests on Browserless and Playwright, scores the result and compares it
# with the committed baseline. Needs Claude Code (`claude`) logged in, Java 21 and Maven.
#
#   scripts/eval-migration-skill.sh [--format html|md] [--accept] [--keep] [--playwright-executable PATH]
#
#   --format   report format (default html)
#   --accept   when the run is acceptable, write it as the new baseline
#   --keep     keep the scratch worktree
#
# The run is compared with the baseline fixture by fixture, over the fixtures both have (marker metrics as rates of
# each fixture's markers); a fixture the recipes no longer leave markers in drops out, a new one is only reported.
# A run is acceptable when no compared metric is worse than the baseline, and, when SKILL.md changed since the
# baseline, at least one is better. Exit code 0 when acceptable, 1 when not.
set -euo pipefail
cd "$(dirname "$0")/.."
root=$(pwd)

format=html
accept=""
keep=""
playwright_executable=""
while [ $# -gt 0 ]; do
  case "$1" in
    --format) format="$2"; shift 2 ;;
    --accept) accept="--accept"; shift ;;
    --keep) keep=1; shift ;;
    --playwright-executable) playwright_executable="$2"; shift 2 ;;
    *) echo "unknown option $1" >&2; exit 2 ;;
  esac
done

skill="$root/unified-test-rewrite/src/main/resources/META-INF/unified/skills/resolve-migration-markers/SKILL.md"
baseline="$root/unified-test-rewrite/skill-eval/baseline.json"
tests_dir=unified-test-demo/src/test/java/demo
out="$root/target/skill-eval/$(date +%Y%m%d-%H%M%S)"
work="$out/worktree"
mkdir -p "$out/before" "$out/browserless" "$out/playwright"

git worktree add --detach "$work" HEAD >/dev/null
cleanup() { [ -n "$keep" ] || git worktree remove --force "$work" >/dev/null 2>&1 || true; }
trap cleanup EXIT

# the fixtures: the migrated demo tests that still carry Migration Markers
fixtures=$(grep -l "TODO unified:" "$work/$tests_dir"/*.java || true)
for f in $fixtures; do cp "$f" "$out/before/"; done
echo "Fixtures: $(ls "$out/before" | tr '\n' ' ')"

# the skill, where Claude Code finds it; the repository's own .claude settings stay as they are
mkdir -p "$work/.claude/skills/resolve-migration-markers"
cp "$skill" "$work/.claude/skills/resolve-migration-markers/SKILL.md"

echo "Building the worktree..."
(cd "$work" && mvn -B -q -ntp install -DskipTests) >"$out/build.log" 2>&1

echo "Running the skill with Claude Code (this takes a while)..."
(cd "$work" && claude -p "/resolve-migration-markers" \
    --permission-mode acceptEdits \
    --allowedTools "Read" "Edit" "Write" "Glob" "Grep" "Bash(mvn:*)" "Bash(grep:*)" "Bash(ls:*)") \
    >"$out/agent.log" 2>&1 || echo "Claude Code exited with an error, see $out/agent.log"

# the fixtures' test classes (helpers and page objects have no tests of their own)
classes=$(for f in "$out"/before/*.java; do n=$(basename "$f" .java); grep -q "@Test" "$work/$tests_dir/$n.java" 2>/dev/null && echo "$n"; done | paste -sd, -)
if (cd "$work" && mvn -B -q -ntp test-compile -pl unified-test-demo) >"$out/compile.log" 2>&1; then
  touch "$out/compiled"
  for tool in browserless playwright; do
    extra=()
    if [ "$tool" = playwright ] && [ -n "$playwright_executable" ]; then
      extra=("-Dvaadin.testing.playwright.executable=$playwright_executable")
    fi
    echo "Running the fixtures on $tool..."
    (cd "$work" && mvn -B -q -ntp test -pl unified-test-demo -Dtest="$classes" -Dsurefire.failIfNoSpecifiedTests=false \
        -Dvaadin.testing.tool=$tool "${extra[@]}") >"$out/$tool.log" 2>&1 || true
    cp "$work"/unified-test-demo/target/surefire-reports/TEST-*.xml "$out/$tool/" 2>/dev/null || true
  done
fi

git -C "$work" diff -- "$tests_dir" >"$out/changes.diff" || true
# files the agent added (a Wrapper, say); none is fine
{ git -C "$work" status --porcelain -- "$tests_dir" | grep '^??' || true; } | awk '{print $2}' | while read -r f; do
  git -C "$work" diff --no-index /dev/null "$f" >>"$out/changes.diff" || true
done

report="$out/report.$format"
java "$root/scripts/EvalMigrationSkill.java" --before "$out/before" --after "$work/$tests_dir" --reports "$out" \
  --skill "$skill" --baseline "$baseline" --format "$format" --out "$report" --diff "$out/changes.diff" $accept
