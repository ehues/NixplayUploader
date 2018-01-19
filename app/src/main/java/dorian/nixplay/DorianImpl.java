package dorian.nixplay;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dorian.nixplay.results.FetchResult;
import dorian.nixplay.results.LoginResult;
import dorian.nixplay.results.UploadResult;
import dorian.nixplayuploader.services.UploadRequestBody;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class DorianImpl implements Dorian {

    public static final Pattern IMAGE_HASH_PATTERN = Pattern.compile("<ETag>\"([a-z0-9]+)\"</ETag>");

    private final String API = "https://mobile-api.nixplay.com";

    private final OkHttpClient client;
    private String apiToken;

    /**
     * Note that login must be called before the instance is used,
     * since we need to have the apiToken defined.
     */
    public DorianImpl() {
        client = new OkHttpClient.Builder()
                .build();
    }

    LoginResult login(String username, String password) {
        FormBody loginForm = new FormBody.Builder()
                .add("username", username.toLowerCase())
                .add("password", password)
                .build();

        final Request loginReq = new Request.Builder()
                .url(API + "/v1/auth/signin")
                .post(loginForm)
                .build();

        try (Response resp = client.newCall(loginReq).execute()) {
            if (resp.isSuccessful()) {
                try {
                    JSONObject login = new JSONObject(resp.body().string());

                    apiToken = login.getString("token");

                    return LoginResult.success(this);

                } catch (JSONException e) {
                    return LoginResult.failure(e, resp);
                }
            }

            return LoginResult.failure(resp);
        } catch (IOException e) {
            return LoginResult.failure(e, null);
        }
    }

    @Override
    public FetchResult<Playlists> playlists() {

        final Request req = new Request.Builder()
                .url(API + "/v1/playlists")
                .header("Authorization", "Bearer " + this.apiToken)
                .build();

        return fetch(req, new FetchResultCallable<Playlists>() {

            @Override
            public Playlists call(Response resp) throws JSONException, IOException {
                JSONArray playlistJSON = new JSONArray(resp.body().string());

                return parsePlaylists(playlistJSON);
            }
        });
    }

    private interface FetchResultCallable<T> {
        T call(Response resp) throws JSONException, IOException;
    }

    private <T> FetchResult<T> fetch(Request req, FetchResultCallable<T> c) {
        try (Response resp = client.newCall(req).execute()) {
            if (resp.isSuccessful()) {
                try {
                    return FetchResult.success(resp, c.call(resp));

                } catch (JSONException e) {
                    return FetchResult.failure(e, resp);
                }
            }

            return FetchResult.failure(resp);
        } catch (IOException e) {
            return FetchResult.failure(e);
        }
    }

    private Playlists parsePlaylists(JSONArray playlistsJSON) throws JSONException {
        ArrayList<Playlist> toReturn = new ArrayList<>();

        for (int i = 0; i < playlistsJSON.length(); i++) {
            JSONObject playlistJSON = playlistsJSON.getJSONObject(i);

            Playlist playlist = new Playlist(playlistJSON.getInt("id"), playlistJSON.getString("name"));

            toReturn.add(playlist);
        }

        return new Playlists(toReturn);
    }

    private String getUploadToken(Playlists playlists) throws IOException, JSONException {

        JSONArray playlistIds = toIdArray(playlists);

        JSONObject params = new JSONObject();
        params.put("playlistIds", playlistIds);
        params.put("friends", new JSONArray());
        params.put("total", 1);

        final Request loginReq = new Request.Builder()
                .url(API + "/v1/photos/receivers")
                .post(RequestBody.create(MediaType.parse("application/json"), params.toString()))
                .header("Authorization", "Bearer " + this.apiToken)
                .build();

        try (Response resp = client.newCall(loginReq).execute()) {
            if (resp.isSuccessful()) {
                JSONObject lists = new JSONObject(resp.body().string());

                String upToken = lists.getString("token");
                return upToken;
            }

            throw new RuntimeException("No token");
        }
    }

    protected JSONObject getS3Params(String uploadToken, String filename, long size) throws IOException, JSONException, DorianException {
        HttpUrl url = HttpUrl.parse(API + "/v1/photos/S3token").newBuilder()
                .addQueryParameter("uploadToken", uploadToken)
                .addQueryParameter("fileName", filename)
                .addQueryParameter("fileType", "image/jpeg")
                .addQueryParameter("fileSize", Long.toString(size))
                .build();

        final Request paramsReq = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + this.apiToken)
                .build();

        try (Response resp = client.newCall(paramsReq).execute()) {
            if (resp.isSuccessful()) {
                JSONObject wrapper = new JSONObject(resp.body().string());

                return wrapper.getJSONObject("data");
            }

            throw new DorianException("No token", resp);
        }
    }

    private JSONArray toIdArray(Playlists playlists) {
        JSONArray out = new JSONArray();

        for (Playlist playlist : playlists.all()) {
            out.put(playlist.getId());
        }

        return out;
    }

    public UploadResult upload(Playlist playlist, Upload up, UploadConfig cfg) {
        UploadHelper uploadHelper = makeUploadHelper(up, cfg);
        try {
            String uploadToken = getUploadToken(new Playlists(Collections.singletonList(playlist)));
            uploadHelper.updateProgress(UploadProgressCallback.Stage.REQ_UPLOAD, null);

            JSONObject s3Params = getS3Params(uploadToken, up.getFilename(), up.getContentLength());
            uploadHelper.updateProgress(UploadProgressCallback.Stage.S3_PARAMS, null);

            uploadToS3(s3Params, up, uploadHelper);

        } catch (IOException|JSONException e) {
            return UploadResult.failure(e);

        } catch (DorianException e) {
            return UploadResult.failure(e.getResponse());
        }

        return UploadResult.success();
    }

    static class UploadHelper {
        private final UploadProgressCallback progressCallback;

        long reqUploadWork;
        long s3ParmsWork;
        long totalWork;

        public UploadHelper() {
            progressCallback = null;
        }

        public UploadHelper(Upload up, UploadProgressCallback progressCallback) {
            this.progressCallback = progressCallback;

            double bytes = up.getContentLength();
            reqUploadWork = (long)(bytes * .1);
            s3ParmsWork = (long)(bytes * .2);
            totalWork = (long)(bytes * 1.2);
        }

        void updateProgress(UploadProgressCallback.Stage stage, @Nullable Long bytesTransferredOpt) {
            if (progressCallback == null) {
                return;
            }

            switch (stage) {
                case REQ_UPLOAD:
                    progressCallback.setProgress(totalWork, reqUploadWork, stage);
                    break;

                case S3_PARAMS:
                    progressCallback.setProgress(totalWork, s3ParmsWork, stage);
                    break;

                case UPLOAD:
                    progressCallback.setProgress(totalWork, s3ParmsWork + bytesTransferredOpt, stage);
                    break;

                case DONE:
                    progressCallback.setProgress(totalWork, totalWork, UploadProgressCallback.Stage.DONE);
                    break;
            }
        }

        public OkHttpClient makeClientWithProgressCallback(OkHttpClient client) {
            if (progressCallback == null) {
                return client;
            }

            return client.newBuilder()
                    .addNetworkInterceptor(new UploadProgressInterceptor(this))
                    .build();
        }
    }

    private UploadHelper makeUploadHelper(Upload up, @Nullable UploadConfig cfg) {
        if (cfg != null && cfg.getProgressCallback() != null) {
            return new UploadHelper(up, cfg.getProgressCallback());
        }

        return new UploadHelper();
    }

    protected String uploadToS3(JSONObject uploadFields, Upload up, UploadHelper uploadHelper) throws JSONException, IOException {
        Request uploadReq;

        RequestBody requestBody = new UploadRequestBody(MediaType.parse(up.getMimetype()), up);
        MultipartBody payload = new MultipartBody.Builder()
                .addFormDataPart("key", uploadFields.getString("key"))
                .addFormDataPart("acl", uploadFields.getString("acl"))
                .addFormDataPart("content-type", up.getMimetype())
                .addFormDataPart("x-amz-meta-batch-upload-id", uploadFields.getString("userUploadId"))
                .addFormDataPart("success_action_status", "201")
                .addFormDataPart("AWSAccessKeyId", uploadFields.getString("AWSAccessKeyId"))
                .addFormDataPart("Policy", uploadFields.getString("Policy"))
                .addFormDataPart("Signature", uploadFields.getString("Signature"))
                .addFormDataPart("file", up.getFilename(), requestBody)
                .build();

        uploadReq = new Request.Builder()
                .url(uploadFields.getString("s3UploadUrl"))
                .post(payload)
                .build();

        OkHttpClient clientWithProgress = uploadHelper.makeClientWithProgressCallback(client);

        try (Response uploadResponse = clientWithProgress.newCall(uploadReq).execute()) {
            if (uploadResponse.code() == 201) {
                String body = uploadResponse.body().string();

                Matcher hashMatcher = IMAGE_HASH_PATTERN.matcher(body);
                if (hashMatcher.find()) {
                    return hashMatcher.group(1);
                }
            }

            throw new RuntimeException("failed");
        }
    }
}