package dorian.nixplay;

import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulate a file to be uploaded.
 */
public abstract class Upload {

    private final String filename;
    private final String mimetype;


    public Upload(String filename, String mimetype) {
        this.filename = filename;
        this.mimetype = mimetype;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimetype() {
        return mimetype;
    }

    public abstract long getContentLength();

    public abstract InputStream getBytes() throws IOException;
}
