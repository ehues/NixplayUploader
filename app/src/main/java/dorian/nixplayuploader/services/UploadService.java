package dorian.nixplayuploader.services;

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import dorian.nixplay.Dorian;
import dorian.nixplay.DorianBuilder;
import dorian.nixplay.Playlist;
import dorian.nixplay.Upload;
import dorian.nixplay.UploadConfig;
import dorian.nixplay.UploadProgressCallback;
import dorian.nixplay.results.LoginResult;
import dorian.nixplay.results.NetworkResult;
import dorian.nixplay.results.UploadResult;
import dorian.nixplayuploader.R;
import dorian.nixplayuploader.settings.Credentials;
import dorian.nixplayuploader.settings.CredentialsManager;
import okhttp3.Response;

public class UploadService extends IntentService {

    private final static String TAG = UploadService.class.getSimpleName();

    private static final String EXTRA_DESTINATION_PLAYLIST = "playlist";
    private static final String EXTRA_INITIAL_FILENAME = "filename";
    private static final String EXTRA_FILE_IN_CACHE = "file_uri";

    public static void sendIntent(@NonNull Context source, @NonNull String initialFilename, @NonNull String nameInCacheDir, @NonNull Playlist selectedItem) {
        Intent intent = new Intent(source, UploadService.class);

        intent.putExtra(EXTRA_INITIAL_FILENAME, initialFilename);
        intent.putExtra(EXTRA_FILE_IN_CACHE, nameInCacheDir);

        intent.putExtra(EXTRA_DESTINATION_PLAYLIST, selectedItem);

        source.startService(intent);
    }

    /**
     * needed by android
     */
    @SuppressWarnings("unused")
    public UploadService() {
        this(UploadService.class.getSimpleName());
    }

    public UploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final int NOTIFICATION_ID = 11;

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_cloud_upload_white_raster)
                .setContentTitle(getString(R.string.notification_upload_title))
                .setContentText(getString(R.string.notification_upload_content));



        Notification notification = notificationBuilder
                .build();

        startForeground(NOTIFICATION_ID, notification);

        Playlist playlist = (Playlist) intent.getSerializableExtra(EXTRA_DESTINATION_PLAYLIST);

        final String initialFilename = intent.getStringExtra(EXTRA_INITIAL_FILENAME);
        final String fileInCache = intent.getStringExtra(EXTRA_FILE_IN_CACHE);

        final File imageToShare = new File(getCacheDir(), fileInCache);

        try {
            Credentials credsOpt = CredentialsManager.loadActivityLog(this);
            if (credsOpt == null) {
                // Shouldn't happen, since our UI activity ensures we have valid credentials
                Log.i(TAG, "onHandleIntent: No credentials");
                toast("No credentials (!?");
                return;
            }

            Upload up = makeUpload(initialFilename, imageToShare);

            Dorian d2;
            {
                LoginResult loginResult = new DorianBuilder().build(credsOpt.username, credsOpt.password);
                if (loginResult.failed()) {
                    sendFailureReason(loginResult);
                    return;
                }

                d2 = loginResult.loggedInDorian();
            }

            final NotificationManagerCompat notMan = NotificationManagerCompat.from(this);

            UploadConfig cfg = new UploadConfig().setProgressCallback(new UploadProgressCallback() {
                @Override
                public void setProgress(long total, long current, Stage stage) {
                    if (stage == Stage.DONE) {
                        notificationBuilder.setProgress(0, 0, false);
                    }
                    else {
                        double fraction = ((double) current) / ((double) total);
                        int MAX = 10000;
                        int c = (int) (fraction * MAX);

                        notificationBuilder.setProgress(MAX, c, false);
                    }

                    notMan.notify(NOTIFICATION_ID, notificationBuilder.build());
                }
            });

            UploadResult uploadResult = d2.upload(playlist, up, cfg);
            if (uploadResult.failed()) {
                sendFailureReason(uploadResult);
                return;
            }
        } finally {
            // Don't leave shared images lying around in the cache.
            imageToShare.delete();
        }

        Log.i(TAG, "onHandleIntent: completed successfully");

        toast(getString(R.string.toast_upload_succeeded));
    }

    private void sendFailureReason(NetworkResult loginResult) {
        if (loginResult.failedDueToNetworkIssue()) {
            toast(getString(R.string.toast_upload_failed_network));
            return;
        }

        if (loginResult.failedDueToCommunicationConfusion()) {
            toast(getString(R.string.toast_upload_failed_nix));

            Response resp = loginResult.getResponse();
            Log.e(TAG, "sendFailureReason: " + resp.code() + ' ' + resp.message());
            try {
                Log.e(TAG, "sendFailureReason: " + resp.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        toast(getString(R.string.toast_login_failed));
        return;
    }

    @NonNull
    private Upload makeUpload(String initialFilename, final File file) {
        String mimeType = "image/jpeg";

        final long size = file.length();

        return new Upload(initialFilename, mimeType) {
            @Override
            public long getContentLength() {
                return size;
            }

            @Override
            public InputStream getBytes() throws FileNotFoundException {
                return new FileInputStream(file);
            }
        };
    }

    private void toast(final String text) {
        new android.os.Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UploadService.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }
}
