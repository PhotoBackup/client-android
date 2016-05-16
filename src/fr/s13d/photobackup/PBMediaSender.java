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
package fr.s13d.photobackup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;

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
    private static final String LOG_TAG = "PBMediaSender";
    private final static String PASSWORD_PARAM = "password";
    private final static String UPFILE_PARAM = "upfile";
    private final static String FILESIZE_PARAM = "filesize";
    private final static String TEST_PATH = "/test";
    private final Context context;
    private final String serverUrl;
    private final SharedPreferences prefs;
    private final NotificationManager notificationManager;
    private String credentials;
    private Notification.Builder builder;
    private OkHttpClient okClient;
    private static List<PBMediaSenderInterface> interfaces = new ArrayList<>();
    private static int successCount = 0;
    private static int failureCount = 0;


    public PBMediaSender(final Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.serverUrl = removeFinalSlashes(prefs.getString(PBServerPreferenceFragment.PREF_SERVER_URL, ""));
    }


    public void addInterface(PBMediaSenderInterface senderInterface) {
        interfaces.add(senderInterface);
    }


    ////////////////
    // Send media //
    ////////////////
    public void send(final PBMedia media, boolean manual) {
        // network
        String wifiOnlyString = prefs.getString(PBPreferenceFragment.PREF_WIFI_ONLY,
                context.getResources().getString(R.string.only_wifi_default));
        Boolean wifiOnly = wifiOnlyString.equals(context.getResources().getString(R.string.only_wifi));
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        Boolean onWifi = info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;

        // recently taken picture
        String uploadRecentOnlyString = prefs.getString(PBPreferenceFragment.PREF_RECENT_UPLOAD_ONLY,
                context.getResources().getString(R.string.only_recent_upload_default));
        Boolean uploadRecentOnly = uploadRecentOnlyString.equals(context.getResources().getString(R.string.only_recent_upload));
        Boolean recentPicture = (System.currentTimeMillis() / 1000 - media.getDateAdded()) < 600;

        // test to send or not
        if (manual || (!wifiOnly || onWifi) && (!uploadRecentOnly || recentPicture)) {
            sendMedia(media);
        }
    }


    private void sendMedia(final PBMedia media) {
        builder.setContentText(context.getResources().getString(R.string.notif_start_text))
                .setLargeIcon(MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                        media.getId(), MediaStore.Images.Thumbnails.MINI_KIND, null));
        notificationManager.notify(0, builder.build());

        final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
        final File upfile = new File(media.getPath());
        final RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(PASSWORD_PARAM, prefs.getString(PBServerPreferenceFragment.PREF_SERVER_PASS_HASH, ""))
                .addFormDataPart(FILESIZE_PARAM, String.valueOf(upfile.length()))
                .addFormDataPart(UPFILE_PARAM, upfile.getName(), RequestBody.create(MEDIA_TYPE_JPG, upfile))
                .build();
        final Request request = makePostRequest(requestBody);
        getOkClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    Log.i(LOG_TAG, "Get response with code " + response.code());
                    if (response.isSuccessful() || response.code() == 409) {
                        sendDidSucceed(media);
                    } else {
                        sendDidFail(media, new Throwable(response.message()));
                    }
                } finally {
                    response.body().close();
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
        final Toast toast = Toast.makeText(context,
                context.getResources().getString(R.string.toast_test_configuration),
                Toast.LENGTH_SHORT);
        toast.show();

        final RequestBody requestBody = new FormBody.Builder()
                .add(PASSWORD_PARAM, prefs.getString(PBServerPreferenceFragment.PREF_SERVER_PASS_HASH, ""))
                .build();
        final Request request = makePostRequest(requestBody, TEST_PATH);
        Log.i(LOG_TAG, "Initiating test call to " + request.url());

        getOkClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        testDidSucceed(toast);
                    } else {
                        testDidFail(toast, response.message());
                    }
                } finally {
                    response.body().close();
                }
                response.body().close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                testDidFail(toast, e.getLocalizedMessage());
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
        final String login = prefs.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_LOGIN, "");
        final String pass = prefs.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_PASS, "");
        if(prefs.getBoolean(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_SWITCH, false) &&
                !prefs.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_LOGIN, "").isEmpty() &&
                !prefs.getString(PBServerPreferenceFragment.PREF_SERVER_HTTPAUTH_PASS, "").isEmpty()) {
            this.credentials = Credentials.basic(login, pass);
        } else {
            this.credentials = null;
        }
    }


    private void buildNotificationBuilder() {
        this.builder = new Notification.Builder(context);
        this.builder.setSmallIcon(R.drawable.ic_backup_white_48dp)
                .setContentTitle(context.getResources().getString(R.string.app_name));

        // add content intent to reopen the activity
        Intent intent = new Intent(context, PBActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        this.builder.setContentIntent(resultPendingIntent);

        // add button action to stop the service
        Intent stopIntent = new Intent(context, PBService.class);
        stopIntent.setAction(PBApplication.PB_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, 0);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.builder.addAction(android.R.drawable.ic_delete,
                    context.getResources().getString(R.string.stop_service), stopPendingIntent);
        } else {
            Notification.Action.Builder actionBuilder = new Notification.Action.Builder(android.R.drawable.ic_delete,
                    context.getResources().getString(R.string.stop_service), stopPendingIntent);
            this.builder.addAction(actionBuilder.build());
        }
    }


    private void sendDidSucceed(final PBMedia media) {
        builder.setSmallIcon(R.drawable.ic_done_white_48dp);
        media.setState(PBMedia.PBMediaState.SYNCED);
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onSendSuccess();
        }
        incrementSuccessCount();
        updateNotificationText();
    }


    private void sendDidFail(final PBMedia media, final Throwable e) {
        builder.setSmallIcon(R.drawable.ic_error_outline_white_48dp);
        media.setState(PBMedia.PBMediaState.ERROR);
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onSendFailure();
        }
        if (e != null) {
            Log.w(LOG_TAG, e.toString());

        }
        incrementFailureCount();
        updateNotificationText();
    }


    private void testDidSucceed(final Toast toast) {
        showToast(toast, context.getResources().getString(R.string.toast_configuration_ok), Toast.LENGTH_SHORT);
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onTestSuccess();
        }
    }


    private void testDidFail(final Toast toast, final String message) {
        final String toastMessage = context.getResources().getString(R.string.toast_configuration_ko) + " - (" + message + ")";
        showToast(toast, toastMessage, Toast.LENGTH_LONG);
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onTestFailure();
        }
    }


    private void updateNotificationText() {
        String successContent = context.getResources().getQuantityString(R.plurals.notif_success, successCount, successCount);
        String failureContent = context.getResources().getQuantityString(R.plurals.notif_failure, failureCount, failureCount);

        if (successCount != 0 && failureCount != 0) {
            builder.setContentText(successContent + " ; " + failureContent);
        } else {
            if (successCount != 0) {
                builder.setContentText(successContent);
            }
            if (failureCount != 0) {
                builder.setContentText(failureContent);
            }
        }

        notificationManager.notify(0, builder.build());
    }


    // show a text in a given toast on the UI thread
    private void showToast(final Toast toast, final String text, final int duration) {
        ((PBActivity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast.setText(text);
                toast.setDuration(duration);
                toast.show();
            }
        });
   }


    //////////////////
    // Lazy loaders //
    //////////////////
    private OkHttpClient getOkClient() {
        if (okClient == null) {
            okClient = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
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
