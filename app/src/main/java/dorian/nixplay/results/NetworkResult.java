package dorian.nixplay.results;

import android.support.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;
import java.util.Optional;

import okhttp3.Response;

/**
 * Result of a network operation. If everything went well, {@link #succeeded()} will return
 * true.
 * If {@link #succeeded()} returns false, then you can check {@link #getException()} for
 * transport failures, or {@link #getResponse()} for protocol failures.
 */
public class NetworkResult {
    protected final boolean succeeded;
    protected final Optional<Exception> exception;
    protected final Optional<Response> response;

    NetworkResult(boolean succeeded, @Nullable Exception ex, @Nullable Response response) {
        this.exception = Optional.ofNullable(ex);
        this.response = Optional.ofNullable(response);
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
        return failed() && getException().isPresent() && getException().get() instanceof IOException;
    }

    /**
     * Return true if we got an unexpected response from the Nix infrastructure.
     */
    public boolean failedDueToCommunicationConfusion() {
        return failed() && (
                (getException().isPresent() && getException().get() instanceof JSONException)
                || (getResponse().isPresent())
        );
    }

    public Optional<Response> getResponse() {
        return response;
    }

    public Optional<Exception> getException() {
        return exception;
    }
}
