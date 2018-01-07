package dorian.nixplay;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import dorian.nixplay.results.FetchResult;
import dorian.nixplay.results.UploadResult;

/**
 * Created by User on 12/21/2017.
 */

public interface Dorian {
    FetchResult<Playlists> playlists();

    UploadResult upload(@NonNull Playlist playlist, @NonNull Upload up, @Nullable UploadConfig cfg);
}
