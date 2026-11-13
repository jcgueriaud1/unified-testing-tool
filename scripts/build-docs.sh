#!/usr/bin/env bash
# Builds the documentation site (MkDocs Material) into target/site.
# Stages README.md, GLOSSARY.md and docs/ into target/docs-src so that
# the repository's relative links keep working on the site.
set -euo pipefail
cd "$(dirname "$0")/.."

repo_url="https://github.com/jcgueriaud1/unified-testing-tool/blob/main"
src=target/docs-src

rm -rf "$src"
mkdir -p "$src"
cp -r docs/spec docs/adr docs/guide "$src"/
cp docs/comparison.md "$src"/comparison.md
cp GLOSSARY.md "$src"/GLOSSARY.md
cp README.md "$src"/index.md

# README.md sits at the site root: docs/x -> x
sed -i -E 's#\]\(docs/#](#g' "$src"/index.md
# Pages under spec/, adr/ and guide/: ../../README.md -> ../index.md, ../../GLOSSARY.md -> ../GLOSSARY.md,
# other ../../ links (source code) -> GitHub
find "$src"/spec "$src"/adr "$src"/guide -name '*.md' -exec sed -i -E \
  -e 's#\]\(\.\./\.\./README\.md#](../index.md#g' \
  -e 's#\]\(\.\./\.\./GLOSSARY\.md#](../GLOSSARY.md#g' \
  -e "s#\]\(\.\./\.\./#](${repo_url}/#g" \
  -e 's#\]\(\.\./adr\)#](../adr/index.md)#g' {} +
sed -i -E 's#\]\(adr\)#](adr/index.md)#g' "$src"/index.md

# Index page for the ADRs
{
  echo "# Architecture decision records"
  echo
  for f in docs/adr/[0-9]*.md; do
    title=$(grep -m1 '^# ' "$f" | sed 's/^# //')
    echo "- [$(basename "$f" .md | cut -c1-4): $title]($(basename "$f"))"
  done
} > "$src"/adr/index.md

mkdocs build --strict "$@"
