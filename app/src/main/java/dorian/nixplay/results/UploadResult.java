package dorian.nixplay.results;

import android.support.annotation.Nullable;

import okhttp3.Response;

public class UploadResult extends NetworkResult {
    public UploadResult(boolean succeeded, @Nullable Exception ex, @Nullable Response response) {
        super(succeeded, ex, response);
    }

    public static UploadResult failure(Response tokenResp) {
        return new UploadResult(false, null, tokenResp);
    }

    public static UploadResult failure(Exception e) {
        return new UploadResult(false, e, null);
    }

    public static UploadResult success() {
        return new UploadResult(true, null, null);
    }
}
