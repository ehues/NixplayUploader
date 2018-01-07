package dorian.nixplay;

/**
 * Provides progress when sharing a picture with Nix.
 */
public interface UploadProgressCallback {

    enum Stage {
        REQ_UPLOAD, S3_PARAMS, UPLOAD, DONE
    }

    /**
     * @param total The total number of work units we need to complete.
     * @param current The current number of work units completed.
     * @param stage An internal tag of the stage that we're on.
     */
    void setProgress(long total, long current, Stage stage);
}
