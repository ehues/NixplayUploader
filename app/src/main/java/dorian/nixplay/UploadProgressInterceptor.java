package dorian.nixplay;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;

import static okio.Okio.buffer;

/**
 * Tracks progress of an okhttp3 upload.
 */
public class UploadProgressInterceptor implements Interceptor {
    private static final String TAG = UploadProgressInterceptor.class.getSimpleName();

    private final DorianImpl.UploadHelper toCall;

    public UploadProgressInterceptor(DorianImpl.UploadHelper uploadHelper) {
        this.toCall = uploadHelper;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String method = originalRequest.method();
        RequestBody wrappedBody = new ProgressRequestBody(originalRequest.body());

        Request wrappedRequest = chain.request().newBuilder()
                .method(method, wrappedBody)
                .build();

        return chain.proceed(wrappedRequest);
    }

    private class ProgressRequestBody extends RequestBody {

        private final RequestBody wrapped;

        ProgressRequestBody(RequestBody wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public MediaType contentType() {
            return wrapped.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return wrapped.contentLength();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            BufferedSink progressSink = buffer(new ForwardingSink(sink) {
                long transferred = 0;
                long nextUpdateAllowedAfter = 0;

                @Override
                public void write(Buffer source, long byteCount) throws IOException {
                    super.write(source, byteCount);

                    transferred += byteCount;

                    long now = System.currentTimeMillis();
                    if (nextUpdateAllowedAfter < now) {
                        toCall.updateProgress(UploadProgressCallback.Stage.UPLOAD, transferred);
                        nextUpdateAllowedAfter = now + 500;
                        Log.i(TAG, "write: wrote");
                    }
                }

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        toCall.updateProgress(UploadProgressCallback.Stage.DONE, null);
                    }
                }
            });

            wrapped.writeTo(progressSink);

            progressSink.flush(); // https://github.com/square/okhttp/issues/1587
        }
    }
}
