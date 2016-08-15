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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBApplication;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;
import fr.s13d.photobackup.preferences.PBPreferenceFragment;


public class PBMediaStore {

    private static final String LOG_TAG = "PBMediaStore";
    private static List<PBMedia> mediaList;
    private static PBSyncMediaStoreTask syncTask;
    private final SharedPreferences picturesPreferences;
    private final List<PBMediaStoreInterface> interfaces = new ArrayList<>();
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;


    ////////////////
    // Life-cycle //
    ////////////////
    public PBMediaStore() {
        picturesPreferences = PBApplication.getApp().getSharedPreferences(PBApplication.PB_PICTURES_SHARED_PREFS, Context.MODE_PRIVATE);
    }


    public void addInterface(PBMediaStoreInterface storeInterface) {
        interfaces.add(storeInterface);
    }


    public void close() {
        if (syncTask != null) {
            syncTask.cancel(true);
        }
        setMediaListToNull();
    }


    /////////////
    // Queries //
    /////////////
    public PBMedia createMediaForLatestInStore() {
        final ContentResolver cr = PBApplication.getApp().getContentResolver();
        final Cursor cursor = cr.query(uri, null, null, null, "date_added DESC");
        if (cursor == null || !cursor.moveToFirst()) {
            Log.d(LOG_TAG, "Media cursor is null or empty.");
            return null;
        }

        final int bucketId = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
        if (isBucketSelected(cursor.getString(bucketId))) {
            Log.d(LOG_TAG, "Media not in selected buckets.");
            return null;
        }

        final PBMedia media = new PBMedia(cursor);
        try {
            String stateString = picturesPreferences.getString(String.valueOf(media.getId()), PBMedia.PBMediaState.WAITING.name());
            media.setState(PBMedia.PBMediaState.valueOf(stateString));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Explosion!!");
        }
        closeCursor(cursor);

        return media;
    }


    public Cursor getAllMediasCursor() {
        String WHERE = null;
        final Set<String> bucketIds = picturesPreferences.getStringSet(PBPreferenceFragment.PREF_PICTURE_FOLDER_LIST, null);
        if (bucketIds != null && bucketIds.size() > 0) {
            String bucket_ids = TextUtils.join(", ", bucketIds);
            WHERE = "bucket_id in (" + bucket_ids + ")";
        }

        final String[] PROJECTION = new String[] { "_id", "_data", "date_added" };
        final ContentResolver cr = PBApplication.getApp().getContentResolver();
        final Cursor cursor = cr.query(uri, PROJECTION, WHERE, null, "date_added DESC");
        if (cursor == null) {
            Log.d(LOG_TAG, "Media cursor is null.");
            return null;
        }

        return cursor;
    }


    public boolean isBucketSelected(final String requestedBucketId) {
        Log.d(LOG_TAG, "Checking if bucket " + requestedBucketId + " is selected by user.");
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PBApplication.getApp());
        final Set<String> bucketSet = preferences.getStringSet(PBPreferenceFragment.PREF_PICTURE_FOLDER_LIST, null);
        return (bucketSet != null && bucketSet.contains(requestedBucketId));
    }


    public ArrayMap<String, String> getBucketData() {

        // We want to group the images by bucket names. We abuse the
        // "WHERE" parameter to insert a "GROUP BY" clause into the SQL statement.
        // The template for "WHERE" parameter is like:
        //    SELECT ... FROM ... WHERE (%s)
        // and we make it look like:
        //    SELECT ... FROM ... WHERE (1) GROUP BY (2)
        // The "(1)" means true. The "(2)" means the second columns specified in projection.
        // Note that because there is a ")" in the template, we use "(2" to match it.
        final String[] PROJECTION = {
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                "count(*) as photo_count"
        };
        final String GROUP_BY = "1) GROUP BY (2";
        final Cursor cursor = PBApplication.getApp().getContentResolver().query(uri, PROJECTION, GROUP_BY, null, "photo_count desc");
        final ArrayMap<String, String> bucketNamesList = new ArrayMap<>();

        if (cursor != null && cursor.moveToFirst()) {
            String name;
            String id;
            String count;
            final int bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            final int bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
            final int bucketCountColumn = cursor.getColumnIndex("photo_count");
            do {
                name = cursor.getString(bucketNameColumn);
                id = cursor.getString(bucketIdColumn);
                count = cursor.getString(bucketCountColumn);
                bucketNamesList.put(id, name + " (" + count + ")");
            } while (cursor.moveToNext());
        }
        closeCursor(cursor);

        Log.d(LOG_TAG, bucketNamesList.toString());

        return bucketNamesList;
    }


    private void closeCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }


    /////////////////////////////////
    // Synchronize the media store //
    /////////////////////////////////
    public void sync() {
        if (syncTask != null) {
            syncTask.cancel(true);
        }

        syncTask = new PBSyncMediaStoreTask(this);
        syncTask.execute();
        Log.i(LOG_TAG, "Start SyncMediaStoreTask");
    }


    public void onPostSync() {
        for(PBMediaStoreInterface storeInterface : interfaces) {
            storeInterface.onSyncMediaStoreTaskPostExecute();
        }
        Log.i(LOG_TAG, "Stop PBSyncMediaStoreTask");
    }


    //////////////////
    // Lazy loaders //
    //////////////////
    public List<PBMedia> getMediaList() {
        if (mediaList == null) {
            mediaList = new ArrayList<>();
        }
        return mediaList;
    }


    ////////////////////
    // Static setters //
    ////////////////////
    private static void setMediaListToNull(){ mediaList = null; }

}
