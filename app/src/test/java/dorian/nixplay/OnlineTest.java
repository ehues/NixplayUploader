package dorian.nixplay;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dorian.nixplay.results.LoginResult;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Runs tests directly against the nixplay cloud service. Account credentials must be provided
 * through the {@value OnlineTest#ENV_NIX_PASSWORD} and {@value OnlineTest#ENV_NIX_USERNAME}.
 * The account must contain a playlist named "test".
 */
public class OnlineTest {

    public static final String ENV_NIX_USERNAME = "NIX_USERNAME";
    public static final String ENV_NIX_PASSWORD = "NIX_PASSWORD";

    private static final String USERNAME = System.getenv(ENV_NIX_USERNAME);
    public static final String PASSWORD = System.getenv(ENV_NIX_PASSWORD);

    @Before
    public void ensureUsernameAndPasswordPresent() {
        assertNotNull("Nix username must be set with the " + ENV_NIX_USERNAME + " environment variable", USERNAME);
        assertNotNull("Nix password must be set with the " + ENV_NIX_PASSWORD + " environment variable", PASSWORD);
    }

    @Test
    public void testLogin() throws Exception {
        DorianImpl d2 = new DorianImpl();
        LoginResult login = d2.login(USERNAME, PASSWORD);

        assertTrue(login.succeeded());
    }

    @Test
    public void testLoginFailure() throws Exception {
        DorianImpl d2 = new DorianImpl();
        LoginResult login = d2.login("zzz" + USERNAME, "123");

        assertFalse(login.succeeded());
        assertTrue(login.failedDueToIncorrectUsernameAndPassword());
    }

    @Test
    public void testUpload() throws Exception {
        LoginResult r = new DorianBuilder().build(USERNAME, PASSWORD);

        assertTrue(r.succeeded());

        Dorian dorian = r.loggedInDorian();

        Playlists playlists = dorian.playlists().getValue();
        assertTrue(playlists.size() > 0);

        Playlist testPlaylist = null;
        for (Playlist candidate : playlists.all()) {
            if ("test".equals(candidate.getName())) {
                testPlaylist = candidate;
                break;
            }
        }

        assertNotNull("Create a playlist named \"test\"", testPlaylist);

        final byte[] bytes = load(getClass().getClassLoader().getResourceAsStream("blank.jpg"));

        Upload up = new Upload("blank.jpg", "image/jpeg") {
            @Override
            public long getContentLength() {
                return bytes.length;
            }

            @Override
            public InputStream getBytes() throws IOException {
                return new ByteArrayInputStream(bytes);
            }
        };

        dorian.upload(testPlaylist, up, null);
    }

    private byte[] load(InputStream resourceAsStream) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        int read;
        byte[] data = new byte[1024];

        while (-1 != (read = resourceAsStream.read(data, 0, data.length))) {
            buf.write(data, 0, read);
        }

        buf.flush();

        return buf.toByteArray();
    }
}
