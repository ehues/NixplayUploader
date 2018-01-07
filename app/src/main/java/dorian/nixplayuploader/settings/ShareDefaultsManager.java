package dorian.nixplayuploader.settings;

import android.content.Context;

import java.util.Optional;

public class ShareDefaultsManager extends  SerializationStorage {
    private static final String DEFAULTS_FILENAME = "share_defaults.serialized";

    public static void saveDefaults(Context context, ShareDefaults defaults) {
        SerializationStorage.save(context, DEFAULTS_FILENAME, defaults);
    }

    public static Optional<ShareDefaults> loadDefaults(Context context) {
        return SerializationStorage.load(context, DEFAULTS_FILENAME, ShareDefaults.class);
    }
}
