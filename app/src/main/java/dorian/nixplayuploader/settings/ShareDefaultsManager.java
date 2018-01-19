package dorian.nixplayuploader.settings;

import android.content.Context;

public class ShareDefaultsManager extends  SerializationStorage {
    private static final String DEFAULTS_FILENAME = "share_defaults.serialized";

    public static void saveDefaults(Context context, ShareDefaults defaults) {
        SerializationStorage.save(context, DEFAULTS_FILENAME, defaults);
    }

    public static ShareDefaults loadDefaults(Context context) {
        return SerializationStorage.load(context, DEFAULTS_FILENAME, ShareDefaults.class);
    }
}
