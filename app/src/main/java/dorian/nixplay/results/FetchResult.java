package dorian.nixplay.results;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Optional;

import okhttp3.Response;

/**
 * Result of fetching something from across the network. On {@link #succeeded()},
 * you'll be able to get a value from {@link #getValue()} (it won't be null in that case).
 */
public class FetchResult<T> extends NetworkResult {

    private final Optional<T> value;

    public static <T> FetchResult<T> success(@NonNull Response response, @NonNull T value) {
        return new FetchResult<>(true, response, null, value);
    }

    public static <T> FetchResult<T> failure(@NonNull Response response) {
        return new FetchResult<>(false, response, null, null);
    }

    public static <T> FetchResult<T> failure(@NonNull Exception e, @NonNull Response response) {
        return new FetchResult<>(false, response, e, null);
    }

    public static <T> FetchResult<T> failure(@NonNull Exception ex) {
        return new FetchResult<>(false, null, ex, null);
    }

    public FetchResult(boolean succeeded, @Nullable Response response, @Nullable Exception ex, @Nullable T value) {
        super(succeeded, ex, response);

        this.value = Optional.ofNullable(value);
    }

    public Optional<T> getValue() {
        return value;
    }
}
