package dorian.nixplay;

import java.util.List;

/**
 * Created by User on 12/29/2017.
 */

public class Playlists {

    private final List<Playlist> playlists;

    public Playlists(List<Playlist> playlists) {
        this.playlists = playlists;
    }

    public int size() {
        return playlists.size();
    }

    public List<Playlist> all() {
        return playlists;
    }
}
