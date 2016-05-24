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

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;

import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;


public class PBService extends Service implements PBMediaStoreInterface, PBMediaSenderInterface {

	private static final String LOG_TAG = "PBService";
    private static MediaContentObserver newMediaContentObserver;
    private PBMediaStore mediaStore;
    private PBMediaSender mediaSender;
    private Binder binder;


    //////////////
    // Override //
    //////////////
    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Binder();
        newMediaContentObserver = new MediaContentObserver();
        mediaStore = new PBMediaStore(this);
        mediaStore.addInterface(this);
        mediaSender = new PBMediaSender(this);
        mediaSender.addInterface(this);
        this.getApplicationContext().getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, newMediaContentObserver);

        Log.i(LOG_TAG, "PhotoBackup service is created");
        mediaStore.sync();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getApplicationContext().getContentResolver().unregisterContentObserver(newMediaContentObserver);
        setNewMediaContentObserverToNull();
        mediaStore.close();
        mediaStore = null;

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


    public class Binder extends android.os.Binder {
        public PBService getService() {
            return PBService.this;
        }
    }


    /////////////
    // Methods //
    /////////////
    public void sendNextMedia() {
        if (mediaStore != null) {
            for (PBMedia media : mediaStore.getMedias()) {
                if (media.getState() != PBMedia.PBMediaState.SYNCED) {
                    mediaSender.send(media, false);
                    break;
                }
            }
        }
    }


    ////////////////////////////////////
    // PBMediaStoreListener callbacks //
    ////////////////////////////////////
    public void onSyncMediaStoreTaskPostExecute() {
        sendNextMedia();
    }


    //////////////////////////////////////
    // PBMediaSenderInterface callbacks //
    //////////////////////////////////////
    public void onSendSuccess() {
        sendNextMedia();
    }
    public void onSendFailure() {}
    public void onTestSuccess() {}
    public void onTestFailure() {}


    /////////////////////////////////////////////////////////////
    // ContentObserver to react on the creation of a new media //
    /////////////////////////////////////////////////////////////
    private class MediaContentObserver extends ContentObserver {

        public MediaContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            Log.i(LOG_TAG, "MediaContentObserver:onChange()");

            if (uri.toString().equals("content://media/external/images/media")) {

                try {
                    final PBMedia media = mediaStore.getLastMediaInStore();
                    media.setState(PBMedia.PBMediaState.WAITING);
                    mediaSender.send(media, false);
                    //mediaStore.sync();
                }
                catch (Exception e) {
                    Log.e(LOG_TAG, "Upload failed :-(");
                    Log.w(LOG_TAG, e.toString());
                }
            }
        }
    }


    /////////////
    // getters //
    /////////////
    public PBMediaStore getMediaStore() {
        return mediaStore;
    }


    public int getMediaSize() {
        return mediaStore.getMedias().size();
    }


    private static void setNewMediaContentObserverToNull(){
        newMediaContentObserver = null;
    }

}
