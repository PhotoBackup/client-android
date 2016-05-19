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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;

public class PBMediaStore {

    private static final String LOG_TAG = "PBMediaStore";
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static Context context;
    private static List<PBMedia> mediaList;
    private static SyncMediaStoreTask syncTask;
    private static SharedPreferences picturesPreferences;
    private static SharedPreferences.Editor picturesPreferencesEditor;
    public static final String PhotoBackupPicturesSharedPreferences = "PhotoBackupPicturesSharedPreferences";
    private static PBMediaStoreQueries queries;
    private List<PBMediaStoreInterface> interfaces = new ArrayList<>();


    public PBMediaStore(Context theContext) {
        context = theContext;
        mediaList = new ArrayList<>();
        picturesPreferences = context.getSharedPreferences(PBApplication.PB_PICTURES_SHARED_PREFS, Context.MODE_PRIVATE);
        picturesPreferencesEditor = picturesPreferences.edit();
        picturesPreferencesEditor.apply();
        queries = new PBMediaStoreQueries(context);
        syncTask=new SyncMediaStoreTask();
    }


    private static void setMediaListToNull(){
        mediaList = null;
    }
    private static void setPicturesPreferencesToNull(){
        picturesPreferences = null;
    }
    private static void setPicturesPreferencesEditorToNull(){
        picturesPreferencesEditor = null;
    }

    public PBMedia getLastMediaInStore() {
        int id = queries.getLastMediaIdInStore();
        return getMedia(id);
    }

    public PBMediaStoreQueries getQueries() {
        return queries;
    }

    public void addInterface(PBMediaStoreInterface storeInterface) {
        interfaces.add(storeInterface);
    }


    public void close() {
        if (syncTask != null) {
            syncTask.cancel(true);
        }

        setMediaListToNull();
        setPicturesPreferencesToNull();
        setPicturesPreferencesEditorToNull();
    }


    public PBMedia getMedia(int id) {
        PBMedia media = null;
        if (id == 0) {
            return null;
        }
        Cursor cursor = queries.getMediaById(id);
        if (cursor == null) {
            Log.e(LOG_TAG, "Photo not returned. Probably filtered by Bucket or deleted");
            return null;
        }
        Integer idCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
        queries.isSelectedBucket(cursor.getString(idCol));


        media = new PBMedia(context, cursor);
        try {
            String stateString = picturesPreferences.getString(String.valueOf(media.getId()), PBMedia.PBMediaState.WAITING.name());
            media.setState(PBMedia.PBMediaState.valueOf(stateString));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Explosion!!");
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }
        return media;
    }


    public List<PBMedia> getMedias() {
        return mediaList;
    }




    /////////////////////////////////
    // Synchronize the media store //
    /////////////////////////////////
    public void sync() {
        if (syncTask != null) {
            syncTask.cancel(true);
        }
        setSyncTask();
        Log.i(LOG_TAG, "Start SyncMediaStoreTask");
    }


    private static  SyncMediaStoreTask getSyncMediaStoreTask(){
        return syncTask;
    }


    private static void setSyncTask(){
        syncTask=getSyncMediaStoreTask();
        syncTask.execute();
    }


    private class SyncMediaStoreTask extends AsyncTask<Void, Void, Void> {

        /////////////////////////////////
        // What makes you an AsyncTask //
        /////////////////////////////////
        protected Void doInBackground(Void... voids) {

            // Get all known pictures in PB
            Map<String, ?> mediasMap = context.getSharedPreferences(PBApplication.PB_PICTURES_SHARED_PREFS,
                    Context.MODE_PRIVATE).getAll();
            Set<String> inCursor = new HashSet<>();

            // Get all pictures on device
            Cursor cursor = queries.getAllMedia();

            // loop through them to sync
            PBMedia media;
            String stateString;
            PBMedia.PBMediaState state;
            mediaList.clear();
            while (cursor != null && cursor.moveToNext()) {
                if(syncTask.isCancelled()) {
                    Log.i(LOG_TAG, "SyncMediaStoreTask cancelled");
                    return null;
                }
                // build media
                media = new PBMedia(context, cursor);
                stateString = (String)mediasMap.get(Integer.toString(media.getId()));
                state = (stateString != null) ?
                        PBMedia.PBMediaState.valueOf(stateString) : PBMedia.PBMediaState.WAITING;
                media.setState(state);
                mediaList.add(media); // populate list
                inCursor.add(Integer.toString(media.getId()));
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

            // purge pictures in preferences that were removed from device
            Set<String> inCursorCopy = new HashSet<>(inCursor);
            Set<String> inMap = new HashSet<>(mediasMap.keySet());
            inMap.removeAll(inCursor);
            inCursor.removeAll(inCursorCopy);
            inMap.addAll(inCursor);

            for (String key : inMap) {
                Log.d(LOG_TAG, "Remove media " + key + " from preference");
                picturesPreferencesEditor.remove(key).apply();
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            for(PBMediaStoreInterface storeInterface : interfaces) {
                storeInterface.onSyncMediaStoreTaskPostExecute();
            }
            Log.i(LOG_TAG, "Stop SyncMediaStoreTask");
        }
    }

}
