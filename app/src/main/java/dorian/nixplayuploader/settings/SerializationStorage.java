package dorian.nixplayuploader.settings;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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

    @Nullable
    public static <T extends Serializable> T load(Context context, String filename, Class<T> type) {
        FileInputStream fis = null;
        ObjectInputStream is = null;
        try {
            fis = context.openFileInput(filename);
            is = new ObjectInputStream(fis);
            return (T) is.readObject();

        } catch (Exception e) {
            Log.e(TAG, "loadRegistry: ", e);
            return null;

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
