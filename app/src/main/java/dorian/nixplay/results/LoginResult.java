package dorian.nixplay.results;

import android.support.annotation.Nullable;

import dorian.nixplay.Dorian;
import okhttp3.Response;

public class LoginResult extends NetworkResult {

    @Nullable
    private final Dorian dorianOpt;

    LoginResult(boolean succeeded, @Nullable Exception ex, @Nullable Response response, @Nullable Dorian dorian) {
        super(succeeded, ex, response);
        this.dorianOpt = dorian;
    }

    /**
     * @return An optional containing the logged in dorianOpt (null if the login failed).
     */
    @Nullable
    public Dorian loggedInDorian() {
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

    /**
     * @return true if Nix could be contacted, but the login failed because the username/password
     *      were incorrect
     */
    public boolean failedDueToIncorrectUsernameAndPassword() {
        if (responseOpt == null) {
            // We didn't receive a response, meaning we couldn't sent auth tokens, etc.
            return false;
        }


        return responseOpt.code() == 401;
    }
}
