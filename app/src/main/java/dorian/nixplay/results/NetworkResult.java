package dorian.nixplay.results;

import android.support.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;

import okhttp3.Response;

/**
 * Result of a network operation. If everything went well, {@link #succeeded()} will return
 * true.
 * If {@link #succeeded()} returns false, then you can check {@link #getException()} for
 * transport failures, or {@link #getResponse()} for protocol failures.
 */
public class NetworkResult {
    protected final boolean succeeded;

    @Nullable
    protected final Exception exceptionOpt;

    @Nullable
    protected final Response responseOpt;

    NetworkResult(boolean succeeded, @Nullable Exception ex, @Nullable Response response) {
        this.exceptionOpt = ex;
        this.responseOpt = response;
        this.succeeded = succeeded;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public boolean failed() {
        return !succeeded();
    }

    /**
     * Return true if network issues caused the failure.
     */
    public boolean failedDueToNetworkIssue() {
        return failed() && exceptionOpt != null && exceptionOpt instanceof IOException;
    }

    /**
     * Return true if we got an unexpected response from the Nix infrastructure.
     */
    public boolean failedDueToCommunicationConfusion() {
        return failed()
                && (
                        exceptionOpt instanceof JSONException
                        || responseOpt != null
        );
    }

    @Nullable
    public Response getResponse() {
        return responseOpt;
    }

    @Nullable
    public Exception getException() {
        return exceptionOpt;
    }
}
