package com.vaadin.testing.unified.spi;

/** Adapter access to an Upload, reached by the core with {@code element.unwrap(UploadAccess.class)}. */
public interface UploadAccess {

    /** Uploads a file as a user picking it would, and waits until the upload has finished. */
    void upload(String fileName, String mimeType, byte[] content);
}
