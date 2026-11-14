package org.vaadin.addons.unified.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainTextParser;

/**
 * Writes the {@code resolve-migration-markers} agent skill into the migrated project, at
 * {@code .claude/skills/resolve-migration-markers/SKILL.md}, unless that file is already there. The skill ships in
 * this jar, so it matches the Migration Markers this version of the recipe writes.
 */
public class AddMigrationMarkersSkill extends ScanningRecipe<AtomicBoolean> {

    static final Path PATH = Path.of(".claude/skills/resolve-migration-markers/SKILL.md");
    private static final String RESOURCE = "/META-INF/unified/skills/resolve-migration-markers/SKILL.md";

    @Override
    public String getDisplayName() {
        return "Add the agent skill that resolves Migration Markers";
    }

    @Override
    public String getDescription() {
        return "Writes `.claude/skills/resolve-migration-markers/SKILL.md`, a Claude Code skill that works through the "
                + "Migration Markers the Migration Recipes leave.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean exists) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile source && source.getSourcePath().equals(PATH)) {
                    exists.set(true);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean exists, ExecutionContext ctx) {
        if (exists.get()) {
            return List.of();
        }
        return PlainTextParser.builder().build().parse(skill())
                .map(source -> (SourceFile) source.withSourcePath(PATH))
                .toList();
    }

    static String skill() {
        try (InputStream in = AddMigrationMarkersSkill.class.getResourceAsStream(RESOURCE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
