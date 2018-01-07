package dorian.nixplay;

import dorian.nixplay.results.LoginResult;

/**
 * Creates a logged-in dorian instance for accessing Nixplay.
 */
public class DorianBuilder {

    public LoginResult build(String username, String password) {
        DorianImpl dorian = new DorianImpl();

        return dorian.login(username, password);
    }
}
