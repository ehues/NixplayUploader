package dorian.nixplay;

import java.io.Serializable;

public class Playlist implements Serializable {
    final int id;
    final String name;


    public Playlist(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
