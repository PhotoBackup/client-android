/**
 * Copyright (C) 2013-2016 Stéphane Péchard.
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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;
import fr.s13d.photobackup.media.PBMedia;
import fr.s13d.photobackup.media.PBMediaSender;


/**
 * Main background service doing the whole job of listening
 * to new media being saved and initiating media uploads.
 */
public class PBService extends Service implements PBMediaStoreInterface, PBMediaSenderInterface {

	private static final String LOG_TAG = "PBService";
    private static MediaContentObserver imagesContentObserver;
    private static MediaContentObserver videosContentObserver;
    private PBMediaSender mediaSender;
    private Binder binder;


    //////////////
    // Override //
    //////////////
    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Binder();
        setImagesContentObserver(new MediaContentObserver());
        this.getApplicationContext().getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, imagesContentObserver);
        setVideosContentObserver(new MediaContentObserver());
        this.getApplicationContext().getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false, videosContentObserver);
        PBApplication.getMediaStore().addInterface(this);

        Log.i(LOG_TAG, "PhotoBackup service is created");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getApplicationContext().getContentResolver().unregisterContentObserver(imagesContentObserver);
        this.getApplicationContext().getContentResolver().unregisterContentObserver(videosContentObserver);
        setImagesContentObserver(null);
        setVideosContentObserver(null);
        PBApplication.getMediaStore().removeInterface(this);
        PBApplication.nullifyMediaStore();

        Log.i(LOG_TAG, "PhotoBackup service has stopped");
    }


    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(LOG_TAG, "PhotoBackup service has started");
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * Binder class
     */
    public class Binder extends android.os.Binder {
        public PBService getService() {
            return PBService.this;
        }
    }


    /////////////
    // Methods //
    /////////////
    /**
     * Sends next media in store.
     */
    public void sendNextMedia() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PBApplication.getApp());
        final boolean isRunning = preferences.getBoolean(PBConstants.PREF_SERVICE_RUNNING, false);
        if (isRunning) {
            for (PBMedia media : PBApplication.getMediaStore().getMediaList()) {
                if (media.getState() != PBMedia.PBMediaState.SYNCED) {
                    sendMedia(media, false);
                    break;
                }
            }
        }
    }


    /**
     * Initiates a media media upload.
     * @param media media to be sent
     * @param manual indicates if upload is manually or automatically (through sendNextMedia()) initiated.
     */
    public void sendMedia(PBMedia media, boolean manual) {
        getMediaSender().send(media, manual);
    }


    ////////////////////////////////////
    // PBMediaStoreListener callbacks //
    ////////////////////////////////////
    /**
     * Sends next media on media store synchronisation.
     */
    public void onSyncMediaStoreTaskPostExecute() {
        sendNextMedia();
    }


    //////////////////////////////////////
    // PBMediaSenderInterface callbacks //
    //////////////////////////////////////
    /**
     * Does nothing on receiving a message.
     */
    public void onMessage(final String message) {
        // Do nothing
    }


    /**
     * Send next media on sending with success.
     */
    public void onSendSuccess() {
        sendNextMedia();
    }


    /**
     * Does nothing on sending with failure.
     */
    public void onSendFailure() {
        // Do nothing
    }


    /**
     * Does nothing on test success.
     */
    public void onTestSuccess() {
        // Do nothing
    }


    /**
     * Does nothing on test failure.
     */
    public void onTestFailure() {
        // Do nothing
    }


    /////////////////////////////////////////////////////////////
    // ContentObserver to react on the creation of a new media //
    /////////////////////////////////////////////////////////////
    private class MediaContentObserver extends ContentObserver {

        private MediaContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            Log.i(LOG_TAG, "MediaContentObserver:onChange()");

            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PBService.this);
            final boolean backupVideos = sp.getBoolean(PBConstants.PREF_MEDIA_BACKUP_VIDEO, false);
            if ("content://media/external/images/media".equals(uri.toString()) ||
                    (backupVideos && "content://media/external/video/media".equals(uri.toString()))) {

                try {
                    final PBMedia media = PBApplication.getMediaStore().createMediaForLatestInStore(backupVideos);
                    if (media != null) {
                        media.setState(PBMedia.PBMediaState.WAITING);
                        getMediaSender().send(media, false);
                    }
                }
                catch (Exception e) {
                    Log.e(LOG_TAG, "Upload failed :-(");
                    Log.w(LOG_TAG, e);
                }
            }
        }
    }


    /////////////////////
    // getters/setters //
    /////////////////////
    private PBMediaSender getMediaSender() {
        if (mediaSender == null) {
            mediaSender = new PBMediaSender();
            mediaSender.addInterface(this);
        }
        return mediaSender;
    }


    private static void setVideosContentObserver(MediaContentObserver contentObserver) {
        videosContentObserver = contentObserver;
    }


    private  static void setImagesContentObserver(MediaContentObserver contentObserver) {
        imagesContentObserver = contentObserver;
    }


}
