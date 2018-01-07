package dorian.nixplayuploader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import dorian.nixplay.Playlist;

public class PlaylistSpinnerAdapter extends ArrayAdapter<Playlist> {
    private final Playlist[] playlists;

    public PlaylistSpinnerAdapter(@NonNull Context context, int resource, @NonNull Playlist[] objects) {
        super(context, resource, objects);
        this.playlists = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView label;
        if (convertView == null) {
            label = new TextView(getContext());
        }
        else {
            label = (TextView) convertView;
        }

        label.setText(playlists[position].getName());

        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
