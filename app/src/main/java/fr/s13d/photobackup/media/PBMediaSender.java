/**
 * Copyright (C) 2013-2015 Stéphane Péchard.
 *
 * This file is part of PhotoBackup.
 *
 * PhotoBackup is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PhotoBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.s13d.photobackup.media;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBActivity;
import fr.s13d.photobackup.PBApplication;
import fr.s13d.photobackup.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;
import fr.s13d.photobackup.preferences.PBPreferenceFragment;
import fr.s13d.photobackup.preferences.PBServerPreferenceFragment;


public class PBMediaSender {
    private final static String LOG_TAG = "PBMediaSender";
    private final static String PASSWORD_PARAM = "password";
    private final static String UPFILE_PARAM = "upfile";
    private final static String FILESIZE_PARAM = "filesize";
    private final static String TEST_PATH = "/test";
    private final String serverUrl;
    private final SharedPreferences preferences;
    private final NotificationManager notificationManager;
    private String credentials;
    private Notification.Builder builder;
    private OkHttpClient okClient;
    private static List<PBMediaSenderInterface> interfaces = new ArrayList<>();
    private static int successCount = 0;
    private static int failureCount = 0;
    private final static int timeoutInSeconds = 60;


    public PBMediaSender() {
        this.notificationManager = (NotificationManager) PBApplication.getApp().getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(PBApplication.getApp());
        this.serverUrl = removeFinalSlashes(preferences.getString(PBServerPreferenceFragment.PREF_SERVER_URL, ""));
        buildNotificationBuilder();
    }


    public void addInterface(PBMediaSenderInterface senderInterface) {
        interfaces.add(senderInterface);
    }


    ////////////////
    // Send media //
    ////////////////
    public void send(final PBMedia media, boolean manual) {
        // network
        String wifiOnlyString = preferences.getString(PBPreferenceFragment.PREF_WIFI_ONLY,
                PBApplication.getApp().getResources().getString(R.string.only_wifi_default));
        Boolean wifiOnly = wifiOnlyString.equals(PBApplication.getApp().getResources().getString(R.string.only_wifi));
        ConnectivityManager cm = (ConnectivityManager) PBApplication.getApp().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        Boolean onWifi = info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;

        // recently taken picture
        String uploadRecentOnlyString = preferences.getString(PBPreferenceFragment.PREF_RECENT_UPLOAD_ONLY,
                PBApplication.getApp().getResources().getString(R.string.only_recent_upload_default));
        Boolean uploadRecentOnly = uploadRecentOnlyString.equals(PBApplication.getApp().getResources().getString(R.string.only_recent_upload));
        Boolean recentPicture = (System.currentTimeMillis() / 1000 - media.getDateAdded()) < 600;

        Log.i(LOG_TAG, "Connectivity: onWifi=" + onWifi.toString() + ", wifiOnly=" + wifiOnly.toString() + ", recentPicture=" + recentPicture.toString());
        // test to send or not
        if (manual || (!wifiOnly || onWifi) && (!uploadRecentOnly || recentPicture)) {
            sendMedia(media);
        }
    }


    private void sendMedia(final PBMedia media) {
        this.builder.setContentText(PBApplication.getApp().getResources().getString(R.string.notif_start_text))
                .setLargeIcon(MediaStore.Images.Thumbnails.getThumbnail(PBApplication.getApp().getContentResolver(),
                        media.getId(), MediaStore.Images.Thumbnails.MICRO_KIND, null));
        notificationManager.notify(0, this.builder.build());

        final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
        final File upfile = new File(media.getPath());
        final RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(PASSWORD_PARAM, preferences.getString(PBServerPreferenceFragment.PREF_SERVER_PASS_HASH, ""))
                .addFormDataPart(FILESIZE_PARAM, String.valueOf(upfile.length()))
                .addFormDataPart(UPFILE_PARAM, upfile.getName(), RequestBody.create(MEDIA_TYPE_JPG, upfile))
                .build();
        final Request request = makePostRequest(requestBody);
        getOkClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(LOG_TAG, "Get response with code " + response.code());
                if (response.code() == 200 || response.code() == 409) {
                    sendDidSucceed(media);
                } else {
                    sendDidFail(media, new Throwable(response.message()));
                }
                response.body().close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(LOG_TAG, e.toString());
                sendDidFail(media, e);
            }
        });
    }


    ///////////////
    // Send test //
    ///////////////
    public void test() {
        final RequestBody requestBody = new FormBody.Builder()
                .add(PASSWORD_PARAM, preferences.getString(PBServerPreferenceFragment.PREF_SERVER_PASS_HASH, ""))
                .build();
        final Request request = makePostRequest(requestBody, TEST_PATH);
        Log.i(LOG_TAG, "Initiating test call to " + request.url());

        getOkClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    testDidSucceed();
                } else {
                    testDidFail(response.message());
                }
                response.body().close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                testDidFail(e.getLocalizedMessage());
            }
        });
    }


    private Request makePostRequest(RequestBody requestBody) {
        return makePostRequest(requestBody, "");
    }


    private Request makePostRequest(RequestBody requestBody, String pathSuffix) {
        final Request.Builder requestBuilder = new Request.Builder()
                .url(serverUrl + pathSuffix)
                .header("User-Agent", PBApplication.PB_USER_AGENT)
                .post(requestBody);
        if (credentials != null) {
            requestBuilder.header("Authorization", credentials);
        }
        return requestBuilder.build();
    }


    /////////////////////
    // Private methods //
    /////////////////////
    private void createAuthCredentials() {
        // add HTTP Basic Auth to the client
        final String login = preferences.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_LOGIN, "");
        final String pass = preferences.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_PASS, "");
        if(preferences.getBoolean(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_SWITCH, false) &&
                !preferences.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_LOGIN, "").isEmpty() &&
                !preferences.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_PASS, "").isEmpty()) {
            this.credentials = Credentials.basic(login, pass);
        } else {
            this.credentials = null;
        }
    }


    private void buildNotificationBuilder() {
        this.builder = new Notification.Builder(PBApplication.getApp());
        this.builder.setSmallIcon(R.drawable.ic_backup_white_48dp)
                .setContentTitle(PBApplication.getApp().getResources().getString(R.string.app_name));

        // add content intent to reopen the activity
        Intent intent = new Intent(PBApplication.getApp(), PBActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(PBApplication.getApp(), 0, intent, 0);
        this.builder.setContentIntent(resultPendingIntent);
    }


    private void sendDidSucceed(final PBMedia media) {
        this.builder.setSmallIcon(R.drawable.ic_done_white_48dp);
        media.setState(PBMedia.PBMediaState.SYNCED);
        media.setErrorMessage("");
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onSendSuccess();
        }
        incrementSuccessCount();
        updateNotificationText();
    }


    private void sendDidFail(final PBMedia media, final Throwable e) {
        this.builder.setSmallIcon(R.drawable.ic_error_outline_white_48dp);
        media.setState(PBMedia.PBMediaState.ERROR);
        media.setErrorMessage(e.getLocalizedMessage());
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onSendFailure();
        }
        Log.w(LOG_TAG, e.toString());
        incrementFailureCount();
        updateNotificationText();
    }


    private void testDidSucceed() {
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onMessage(PBApplication.getApp().getResources().getString(R.string.toast_configuration_ok));
            senderInterface.onTestSuccess();
        }
    }


    private void testDidFail(final String failMessage) {
        final String message = PBApplication.getApp().getResources().getString(R.string.toast_configuration_ko) + " - (" + failMessage + ")";
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onMessage(message);
            senderInterface.onTestFailure();
        }
    }


    private void updateNotificationText() {
        final String successContent = PBApplication.getApp().getResources().getQuantityString(R.plurals.notif_success, successCount, successCount);
        final String failureContent = PBApplication.getApp().getResources().getQuantityString(R.plurals.notif_failure, failureCount, failureCount);

        String contentText = successContent + " ; " + failureContent;
        if (successCount != 0 && failureCount == 0) {
            contentText = successContent;
        }
        if (successCount == 0 && failureCount != 0) {
            contentText = failureContent;
        }
        Bitmap icon = BitmapFactory.decodeResource(PBApplication.getApp().getResources(), R.mipmap.ic_launcher);
        this.builder.setLargeIcon(icon).setContentText(contentText);

        notificationManager.notify(0, this.builder.build());
    }


    //////////////////
    // Lazy loaders //
    //////////////////
    private OkHttpClient getOkClient() {
        if (okClient == null) {
            okClient = new OkHttpClient.Builder()
                    .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                    .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                    .build();
            createAuthCredentials();
            buildNotificationBuilder();
        }
        return okClient;
    }


    ///////////
    // Utils //
    ///////////
    static public String removeFinalSlashes(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        int count = 0;
        int length = s.length();
        while (s.charAt(length - 1 - count) == '/') {
            count++;
        }

        return s.substring(0, s.length() - count);
    }


    private static void incrementSuccessCount() {
        successCount++;
    }


    private static void incrementFailureCount() {
        failureCount++;
    }
}
