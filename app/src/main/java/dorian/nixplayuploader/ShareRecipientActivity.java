package dorian.nixplayuploader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.model.AspectRatio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

import dorian.nixplay.Dorian;
import dorian.nixplay.DorianBuilder;
import dorian.nixplay.Playlist;
import dorian.nixplay.Playlists;
import dorian.nixplay.results.FetchResult;
import dorian.nixplay.results.LoginResult;
import dorian.nixplayuploader.services.UploadService;
import dorian.nixplayuploader.settings.Credentials;
import dorian.nixplayuploader.settings.CredentialsManager;
import dorian.nixplayuploader.settings.ShareDefaults;
import dorian.nixplayuploader.settings.ShareDefaultsManager;
import dorian.nixplayuploader.util.ImageResizer;
import dorian.nixplayuploader.util.StreamSource;

/**
 * Receives share requests and kicks off handling.
 */
public class ShareRecipientActivity extends AppCompatActivity {

    public static int RESULT_CREDENTIALS_UPDATED = 999;

    private static final String TAG = ShareRecipientActivity.class.getSimpleName();
    private ProgressBar progress;
    private ImageView primaryImage;
    private Spinner playlistSpinner;
    private ShareDefaults defaults;
    private Button editButton;

    /**
     * Size of the source image.
     */
    private Optional<Rect> originalImageSizeOpt = Optional.empty();

    /**
     * File where we've cached the shared image. It's in the original form (ie, unmodified).
     */
    private File originalImage;

    /**
     * File containing a modified version of the image. Will be null until the user attempts
     * an edit.
     */
    private Optional<File> workingImageOpt = Optional.empty();
    private Button uploadButton;
    private Button revertButton;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!CredentialsManager.loadActivityLog(this).isPresent()) {
            Intent startCredsIntent = new Intent("update", null, this, InitializationActivity.class);
            startActivityForResult(startCredsIntent, RESULT_CREDENTIALS_UPDATED);
        }

        setContentView(R.layout.activity_share_recipient);

        final ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);

        if (!intentReader.isShareIntent()) {
            Log.e(TAG, "onCreate: Not a share intent");
        }

        Log.i(TAG, "onCreate: Got share intent from (calling application label)" + intentReader.getCallingApplicationLabel());
        Log.i(TAG, "onCreate: Got share intent from (activity)" + intentReader.getCallingActivity());

        for (int i = 0; i < intentReader.getStreamCount(); i++) {
            Log.i(TAG, "onCreate: stream " + i + ": " + intentReader.getStream(i));
        }

        defaults = loadDefaults();

        progress = findViewById(R.id.load_progress);
        primaryImage = findViewById(R.id.image);

        final View container = findViewById(R.id.ic);

        playlistSpinner = findViewById(R.id.playlist_spinner);

        revertButton = findViewById(R.id.undo_button);
        revertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                revertChanges();
            }
        });

        editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEdit();
            }
        });

        uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                doUpload();
                                            }
                                        }
        );

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    originalImage = dumpToCache(intentReader.getStream());

                    return true;
                } catch (IOException e) {
                    Toast.makeText(ShareRecipientActivity.this, "Unable to copy file for sharing", Toast.LENGTH_LONG).show();
                    finish();
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                editButton.setEnabled(aBoolean);
                uploadButton.setEnabled(aBoolean);
            }
        }.execute();

        progress.setMax(10);
        progress.setProgress(2, true);

        final View imageContainer = findViewById(R.id.image_container);

        // Set the image
        ViewTreeObserver vto = primaryImage.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @SuppressLint("StaticFieldLeak")
            public boolean onPreDraw() {

                primaryImage.getViewTreeObserver().removeOnPreDrawListener(this);

                final int width = imageContainer.getMeasuredWidth();
                final int height = imageContainer.getMeasuredHeight();

                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected void onPreExecute() {
                        progress.setVisibility(View.VISIBLE);
                        container.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        StreamSource source = new StreamSource() {
                            @Override
                            public InputStream openStream() throws IOException {
                                return getContentResolver().openInputStream(intentReader.getStream());
                            }
                        };


                        try {
                            return decodeSampledBitmapFromResource(source, width, height, progress);
                        } catch (IOException e) {
                            Log.e(TAG, "onCreate: ", e);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        primaryImage.setImageBitmap(bitmap);

                        container.setVisibility(View.VISIBLE);
                        container.setAlpha(0F);
                        container.animate()
                                .alpha(1F);

                        // Add a short delay so the user can see the progress bar full
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                progress.animate()
                                        .alpha(0F)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                progress.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }, 100);
                    }
                    }.execute();

                return true;
            }
        });


        // Find the playlists
        initializePlaylistsAsync();
    }

    private void revertChanges() {
        StreamSource source = new StreamSource() {
            @Override
            public InputStream openStream() throws IOException {
                return new FileInputStream(originalImage);
            }
        };

        try {
            Bitmap bitmap = decodeSampledBitmapFromResource(source,
                    primaryImage.getWidth(), primaryImage.getHeight(), progress
            );

            primaryImage.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.e(TAG, "revertChanges: failed to set primary image", e);
            return;
        }

        workingImageOpt.ifPresent(new Consumer<File>() {
                @Override
                public void accept(File file) {
                    file.delete();
                }
        });
        workingImageOpt = Optional.empty();

        revertButton.setEnabled(false);
    }

    private void initializePlaylistsAsync() {
        new AsyncTask<Void, Void, Playlist[]>() {
            @Override
            protected void onPreExecute() {
                Optional<Credentials> credsOpt = CredentialsManager.loadActivityLog(ShareRecipientActivity.this);
                if (!credsOpt.isPresent()) {
                    Log.i(TAG, "onHandleIntent: No credentials");
                    Toast.makeText(ShareRecipientActivity.this, "Need credentials", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected Playlist[] doInBackground(Void... voids) {
                Optional<Credentials> credsOpt = CredentialsManager.loadActivityLog(ShareRecipientActivity.this);
                if (!credsOpt.isPresent()) {
                    return null;
                }

                LoginResult loginResult = new DorianBuilder().build(credsOpt.get().username, credsOpt.get().password);
                if (loginResult.failed()) {
                    return null;
                }

                Dorian dorian = loginResult.loggedInDorian().get();

                FetchResult<Playlists> playlistsFetchResult = dorian.playlists();
                if (playlistsFetchResult.failed()) {
                    return null;
                }

                ArrayList<Playlist> playlists = new ArrayList<>(playlistsFetchResult.getValue().get().all());

                Collections.sort(playlists, new Comparator<Playlist>() {
                    @Override
                    public int compare(Playlist o1, Playlist o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                return playlists.toArray(new Playlist[playlists.size()]);
            }

            @Override
            protected void onPostExecute(Playlist[] playlists) {
                if (playlists == null) {
                    // We didn't get the playlists for some reason. Escape.
                    return;
                }

                ArrayAdapter<Playlist> adapter = new PlaylistSpinnerAdapter(ShareRecipientActivity.this, android.R.layout.simple_spinner_item, playlists);
                playlistSpinner.setAdapter(adapter);

                if (defaults.getPlaylistId() != ShareDefaults.NO_PLAYLIST) {
                    for (int i = 0; i < playlists.length; i++) {
                        if (defaults.getPlaylistId() == playlists[i].getId()) {
                            playlistSpinner.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }.execute();
    }

    private void startEdit() {
        workingImageOpt.ifPresent(new Consumer<File>() {
            @Override
            public void accept(File file) {
                file.delete();
            }
        });

        File destination = null;
        try {
            destination = File.createTempFile("from-share", "modified", getCacheDir());
            destination.delete();
        } catch (IOException e) {
            Log.e(TAG, "startEdit: ", e);
        }

        workingImageOpt = Optional.of(destination);

        final UCrop uCropBuilder = UCrop.of(Uri.fromFile(originalImage), Uri.fromFile(destination));

        final ArrayList<AspectRatio> ratios = new ArrayList<>();
        ratios.add(new AspectRatio("Landscape", 4, 3));
        ratios.add(new AspectRatio("Portrait", 3, 4));

        originalImageSizeOpt.ifPresent(new Consumer<Rect>() {
            @Override
            public void accept(Rect rect) {
                ratios.add(0, new AspectRatio("Original", rect.width(), rect.height()));
            }
        });

        UCrop.Options opts = new UCrop.Options();

        opts.setAspectRatioOptions(0, ratios.toArray(new AspectRatio[ratios.size()]));

        // Enabling freestyle crop disables the other gestures
//        opts.setFreeStyleCropEnabled(true);

        uCropBuilder.withOptions(opts);

        uCropBuilder.start(this);
    }

    private File dumpToCache(Uri uri) throws IOException {
        long size;
        try (Cursor returnCursor =
                     getContentResolver().query(uri, null, null, null, null)) {

            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            returnCursor.moveToFirst();

            size = returnCursor.getLong(sizeIndex);
        }


        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            File cachedVersion = File.createTempFile("from-share", "orig", getCacheDir());

            try (ReadableByteChannel sourceChannel = Channels.newChannel(inputStream);
                 FileChannel destChannel = new FileOutputStream(cachedVersion).getChannel()
            ) {
                long transferred = destChannel.transferFrom(sourceChannel, 0, size);
                Log.i(TAG, "dumpToCache: transferred " + transferred + '/' + size);
            }

            return cachedVersion;
        }
    }

    @NonNull
    private ShareDefaults loadDefaults() {
        Optional<ShareDefaults> defaultsOpt = ShareDefaultsManager.loadDefaults(this);

        if (defaultsOpt.isPresent()) {
            return defaultsOpt.get();
        }

        ShareDefaults defaults = new ShareDefaults();
        return defaults;
    }

    private void doUpload() {
        Playlist playlist = (Playlist) playlistSpinner.getSelectedItem();
        if (playlist == null) {
            Toast.makeText(this, "Select a playlist", Toast.LENGTH_LONG).show();
            return;
        }

        ShareDefaults defaults = new ShareDefaults()
                .setPlaylistId(playlist == null ? ShareDefaults.NO_PLAYLIST : playlist.getId());

        ShareDefaultsManager.saveDefaults(this, defaults);

        String filenameInCacheDir;
        if (workingImageOpt.isPresent()) {
            filenameInCacheDir = workingImageOpt.get().getName();
        }
        else {
            filenameInCacheDir = originalImage.getName();
        }

        UploadService.sendIntent(this,"zzz.jpeg", filenameInCacheDir, playlist);

        workingImageOpt.ifPresent(new Consumer<File>() {
            @Override
            public void accept(File file) {
                originalImage.delete();
            }
        });

        finish();
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;


        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromResource(StreamSource inputSource,
                                                  int reqWidth, int reqHeight, ProgressBar progress) throws IOException {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        long before = System.currentTimeMillis();
        try (InputStream inputStream = inputSource.openStream()) {
            BitmapFactory.decodeStream(inputStream, null, options);
            Log.i(TAG, "first decode: " + (System.currentTimeMillis() - before));
            progress.setProgress(3, true);
        }

        // Calculate inSampleSize
        originalImageSizeOpt = Optional.of(new Rect(0, 0, options.outWidth, options.outHeight));
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        before = System.currentTimeMillis();
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream inputStream = inputSource.openStream()) {
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            Log.i(TAG, "resize: " + (System.currentTimeMillis() - before));
            progress.setProgress(6, true);
        }

        before = System.currentTimeMillis();
        Bitmap toReturn = ImageResizer.rotateImageIfRequired(bitmap, inputSource.openStream());
        Log.i(TAG, "rotate: " + (System.currentTimeMillis() - before));
        progress.setProgress(10, true);

        return toReturn;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {

            StreamSource source = new StreamSource() {
                @Override
                public InputStream openStream() throws IOException {
                    return new FileInputStream(workingImageOpt.get());
                }
            };

            try {
                Bitmap bitmap = decodeSampledBitmapFromResource(source,
                        primaryImage.getWidth(), primaryImage.getHeight(), progress
                );

                primaryImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "onActivityResult: failed to set primary image", e);
                return;
            }

            revertButton.setEnabled(true);

        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        }
        else if (requestCode == RESULT_CREDENTIALS_UPDATED && resultCode == RESULT_CREDENTIALS_UPDATED) {
            initializePlaylistsAsync();
        }
    }
}