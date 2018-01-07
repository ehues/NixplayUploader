package dorian.nixplayuploader.settings;

import java.io.Serializable;

/**
 * Records the settings during a share so they can be set as defaults next time 'round.
 */
public class ShareDefaults implements Serializable {

    public static final int NO_PLAYLIST = -1;
    /**
     * The id of the playlist. May be {@link ShareDefaults#NO_PLAYLIST} indicating there was no playlist id.
     */
    private int playlistId = NO_PLAYLIST;

    public ShareDefaults setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
        return this;
    }

    public int getPlaylistId() {
        return playlistId;
    }
}
