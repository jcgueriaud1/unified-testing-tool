package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.upload.Upload;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** An {@link Upload}. Uploading waits until the server has received the file. */
@UnifiedComponent(Upload.class)
public interface UnifiedUpload extends UnifiedWrapper, Findable<UnifiedUpload.UploadQuery> {

    interface UploadQuery extends Query<UnifiedUpload, UploadQuery> {}

    /** Uploads content as a file with this name and MIME type, as a user picking it would. */
    default void upload(String fileName, String mimeType, byte[] content) {
        Wrappers.lazyElementOf(this).act("upload " + fileName + " to",
                e -> e.unwrap(UploadAccess.class).upload(fileName, mimeType, content));
    }

    /** Uploads a file from disk; the MIME type comes from its name. */
    default void upload(Path file) {
        try {
            String type = Files.probeContentType(file);
            upload(file.getFileName().toString(), type == null ? "application/octet-stream" : type,
                    Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
