package dorian.nixplay;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Optional fields to set when uploading.
 */
public class UploadConfig {

    @Nullable
    private UploadProgressCallback progressCallback;

    @NonNull
    public UploadConfig setProgressCallback(UploadProgressCallback cb) {
        this.progressCallback = cb;
        return this;
    }

    @Nullable
    public UploadProgressCallback getProgressCallback() {
        return progressCallback;
    }
}
