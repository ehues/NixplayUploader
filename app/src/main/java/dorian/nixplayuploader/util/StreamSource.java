package dorian.nixplayuploader.util;

import java.io.IOException;
import java.io.InputStream;

public interface StreamSource {

    InputStream openStream() throws IOException;
}
