package dorian.nixplayuploader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import dorian.nixplay.Playlist;

public class PlaylistSpinnerAdapter extends ArrayAdapter<Playlist> {
    public PlaylistSpinnerAdapter(@NonNull Context context, int resource, @NonNull Spinner spinner) {
        super(context, resource, new ArrayList<Playlist>());
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.spinner_item_with_padding, parent, false);
        }
        else {
            view = convertView;
        }

        TextView text = view.findViewById(android.R.id.text1);
        text.setText(getItem(position).getName());

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public void setPlaylists(Playlist[] playlists) {
        addAll(playlists);

        notifyDataSetChanged();
    }
}
