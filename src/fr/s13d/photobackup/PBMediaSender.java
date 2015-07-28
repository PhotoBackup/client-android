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
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;


public class PBMediaSender {

    private final static String LOG_TAG = "PBMediaSender";
    private final static String PASSWORD_PARAM = "password";
    private final static String UPFILE_PARAM = "upfile";
    private final static String FILESIZE_PARAM = "filesize";
    private final static String TEST_PATH = "/test";
    private final Context context;
    private final String serverUrl;
    private final SharedPreferences prefs;
    private final NotificationManager notificationManager;
    private final Notification.Builder builder;
    private static AsyncHttpClient client = new AsyncHttpClient();
    private final RequestParams params = new RequestParams();
    private static List<PBMediaSenderInterface> interfaces = new ArrayList<>();
    private static int successCount = 0;
    private static int failureCount = 0;


    PBMediaSender(final Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.builder = new Notification.Builder(context);
        this.builder.setSmallIcon(R.drawable.ic_backup_white_48dp)
                    .setContentTitle(context.getResources().getString(R.string.app_name));

        // add action to reopen the activity
        Intent intent = new Intent(context, PBActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        this.builder.setContentIntent(resultPendingIntent);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        serverUrl = removeFinalSlashes(prefs.getString(PBSettingsFragment.PREF_SERVER_URL, ""));
        params.put(PASSWORD_PARAM, prefs.getString(PBSettingsFragment.PREF_SERVER_PASS_HASH, ""));

    }


    public void addInterface(PBMediaSenderInterface senderInterface) {
        interfaces.add(senderInterface);
    }


    ////////////////
    // Send media //
    ////////////////
    public void send(final PBMedia media) {
        // user preference
        Boolean wifiOnly = prefs.getBoolean(PBSettingsFragment.PREF_WIFI_ONLY,
                                            PBSettingsFragment.DEFAULT_WIFI_ONLY);

        // test current network
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (wifiOnly) {
            if (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) {
                sendMedia(media); // send if on wifi and user ask for wifi only
            }
        } else {
            sendMedia(media); // always send if user do not care about wifi only
        }

    }


    private void sendMedia(final PBMedia media) {
        builder.setContentText(context.getResources().getString(R.string.notif_start_text))
                .setLargeIcon(MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                        media.getId(), MediaStore.Images.Thumbnails.MINI_KIND, null));
        notificationManager.notify(0, builder.build());

        try { // Add media file as request parameter
            File upfile = new File(media.getPath());
            params.put(UPFILE_PARAM, upfile);
            params.put(FILESIZE_PARAM, upfile.length());
        }
        catch(FileNotFoundException e) {
            sendDidFail(media, e);
        }

        // Send media
        client.post(serverUrl, params, new AsyncHttpResponseHandler(Looper.getMainLooper()) {

            @Override // called when response HTTP status is "200 OK"
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                sendDidSucceed(media);
            }

            @Override // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                sendDidFail(media, e);
            }

            @Override // called before request is started
            public void onStart() {
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
            }

        });
    }


    ///////////////
    // Send test //
    ///////////////
    public void test() {
        Toast.makeText(context, "Testing server", Toast.LENGTH_SHORT).show();
        client.post(serverUrl + TEST_PATH, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                for (PBMediaSenderInterface senderInterface : interfaces) {
                    senderInterface.onTestSuccess();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                for (PBMediaSenderInterface senderInterface : interfaces) {
                    senderInterface.onTestFailure();
                }
            }

        });
    }


    /////////////////////
    // Private methods //
    /////////////////////
    private void sendDidSucceed(final PBMedia media) {
        builder.setSmallIcon(R.drawable.ic_done_white_48dp);
        media.setState(PBMedia.PBMediaState.SYNCED);
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onSendSuccess();
        }
        successCount++;
        updateNotificationText();
    }


    private void sendDidFail(final PBMedia media, final Throwable e) {
        builder.setSmallIcon(R.drawable.ic_error_outline_white_48dp);
        media.setState(PBMedia.PBMediaState.ERROR);
        for (PBMediaSenderInterface senderInterface : interfaces) {
            senderInterface.onSendFailure();
        }
        e.printStackTrace();
        failureCount++;
        updateNotificationText();
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

}
