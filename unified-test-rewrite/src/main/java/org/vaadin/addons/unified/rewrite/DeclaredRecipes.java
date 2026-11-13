package org.vaadin.addons.unified.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import org.openrewrite.config.YamlResourceLoader;

/** The declarative recipes in this module's {@code META-INF/rewrite} files, for the composite recipes to run. */
final class DeclaredRecipes {

    private DeclaredRecipes() {
    }

    static YamlResourceLoader load(String resource) {
        URL url = DeclaredRecipes.class.getResource(resource);
        try (InputStream in = url.openStream()) {
            return new YamlResourceLoader(in, url.toURI(), new Properties(), DeclaredRecipes.class.getClassLoader());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
