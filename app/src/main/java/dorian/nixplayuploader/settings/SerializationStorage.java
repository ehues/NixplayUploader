package dorian.nixplayuploader.settings;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;

/**
 * Shared implementation of serializing/deserializing an object.
 */
abstract class SerializationStorage {
    private static final String TAG = SerializationStorage.class.getSimpleName();

    public static <T extends Serializable> void save(Context context, String filename, T toSave) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(toSave);
            os.close();
        } catch (IOException e) {
            Log.e(TAG, "saveRegistry on " + filename + ": ", e);
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

    }

    public static <T extends Serializable> Optional<T> load(Context context, String filename, Class<T> type) {
        FileInputStream fis = null;
        ObjectInputStream is = null;
        try {
            fis = context.openFileInput(filename);
            is = new ObjectInputStream(fis);
            return Optional.of((T) is.readObject());

        } catch (Exception e) {
            Log.e(TAG, "loadRegistry: ", e);
            return Optional.empty();

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
