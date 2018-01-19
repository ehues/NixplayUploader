package dorian.nixplayuploader.settings;


import android.content.Context;
import android.support.annotation.Nullable;

import java.util.Optional;

public class CredentialsManager {

    private static final String CREDENTIALS_FILENAME = "credentials.serialized";

    public static void saveActivityLog(Context context, Credentials credentials) {
        SerializationStorage.save(context, CREDENTIALS_FILENAME, credentials);
    }

    @Nullable
    public static Credentials loadActivityLog(Context context) {
        return SerializationStorage.load(context, CREDENTIALS_FILENAME, Credentials.class);
    }

    public static void clear(Context context) {
        context.deleteFile(CREDENTIALS_FILENAME);
    }
}
