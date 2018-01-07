package dorian.nixplayuploader.services;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import dorian.nixplay.Upload;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by User on 12/26/2017.
 */

public class UploadRequestBody extends RequestBody {

    private final MediaType mediaType;
    private final Upload up;

    public UploadRequestBody(MediaType mediaType, Upload up) {
        this.mediaType = mediaType;
        this.up = up;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return up.getContentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try (InputStream out = up.getBytes()) {
            Source source = Okio.source(out);
            sink.writeAll(source);
        }
    }
}
