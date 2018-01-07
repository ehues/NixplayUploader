package dorian.nixplay.results;

import android.support.annotation.Nullable;

import java.util.Optional;

import dorian.nixplay.Dorian;
import okhttp3.Response;

public class LoginResult extends NetworkResult {

    private final Optional<Dorian> dorianOpt;

    LoginResult(boolean succeeded, @Nullable Exception ex, @Nullable Response response, @Nullable Dorian dorian) {
        super(succeeded, ex, response);
        this.dorianOpt = Optional.ofNullable(dorian);
    }

    /**
     * @return An optional containing the logged in dorianOpt (empty if the login failed).
     */
    public Optional<Dorian> loggedInDorian() {
        return dorianOpt;
    }

    public static LoginResult success(Dorian dorian) {
        return new LoginResult(true, null, null, dorian);
    }

    public static LoginResult failure(Response resp) {
        return new LoginResult(false, null, resp, null);
    }

    public static LoginResult failure(Exception e, Response resp) {
        return new LoginResult(false, e, resp, null);
    }

    public static LoginResult loginFailure(Response r) {
        return new LoginResult(false, null, r, null);
    }

    /**
     * @return true if Nix could be contacted, but the login failed because the username/password
     *      were incorrect
     */
    public boolean loginFailed() {
        if (response.isPresent()) {
            return response.get().code() == 401;
        }

        return false;
    }
}
