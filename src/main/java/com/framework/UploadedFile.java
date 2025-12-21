package main.java.com.framework;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Représente un fichier uploadé, côté framework, sans exposer de types Jakarta.
 */
public class UploadedFile {

    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final byte[] content;

    public UploadedFile(String originalFilename, String contentType, long size, byte[] content) {
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.content = (content != null) ? content : new byte[0];
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }
}
