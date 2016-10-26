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
package fr.s13d.photobackup.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
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
import fr.s13d.photobackup.PBConstants;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class PBMediaStore {

    private static final String LOG_TAG = "PBMediaStore";
    private static List<PBMedia> mediaList;
    private static PBSyncMediaStoreTask syncTask;
    private static final SharedPreferences picturesPreferences = PBApplication.getApp()
            .getSharedPreferences(PBApplication.PB_MEDIAS_SHARED_PREFS, Context.MODE_PRIVATE);
    private final List<PBMediaStoreInterface> interfaces = new ArrayList<>();
    private static final Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;


    ////////////////
    // Life-cycle //
    ////////////////
    public void addInterface(PBMediaStoreInterface storeInterface) {
        interfaces.add(storeInterface);
    }


    public void removeInterface(PBMediaStoreInterface storeInterface) {
        interfaces.remove(storeInterface);
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
    public PBMedia createMediaForLatestInStore(boolean backupVideos) {
        final ContentResolver cr = PBApplication.getApp().getContentResolver();
        final Cursor cursor = cr.query(backupVideos ? videosUri : imagesUri, null, null, null, "date_added DESC");
        if (cursor == null || !cursor.moveToFirst()) {
            Log.d(LOG_TAG, "Media cursor is null or empty.");
            return null;
        }

        final int bucketId = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
        if (!backupVideos && !isBucketSelected(cursor.getString(bucketId))) {
            Log.d(LOG_TAG, "Media not in selected buckets.");
            return null;
        }

        final PBMedia media = new PBMedia(cursor);
        try {
            String stateString = picturesPreferences.getString(String.valueOf(media.getId()), PBMedia.PBMediaState.WAITING.name());
            media.setState(PBMedia.PBMediaState.valueOf(stateString));
        } catch (Exception e) {
            Log.e(LOG_TAG, e);
        }
        closeCursor(cursor);

        return media;
    }


    Cursor getAllMediasCursor() {
        String where = null;
        final SharedPreferences prefs = getDefaultSharedPreferences(PBApplication.getApp());
        final Set<String> bucketIds = prefs.getStringSet(PBConstants.PREF_PICTURE_FOLDER_LIST, null);
        if (bucketIds != null && !bucketIds.isEmpty()) {
            final String bucketString = TextUtils.join(", ", bucketIds);
            where = "bucket_id in (" + bucketString + ")";
        }

        final boolean backupVideos = prefs.getBoolean(PBConstants.PREF_MEDIA_BACKUP_VIDEO, false);
        final String[] projection = new String[] { "_id", "_data", "date_added" };
        final ContentResolver cr = PBApplication.getApp().getContentResolver();
        final Cursor[] cursors = new Cursor[backupVideos ? 2 : 1];
        cursors[0] = cr.query(imagesUri, projection, where, null, "date_added DESC");
        if (backupVideos) {
            cursors[1] = cr.query(videosUri, projection, where, null, "date_added DESC");
        }
        if (cursors[0] == null) {
            Log.d(LOG_TAG, "Media cursor is null.");
            return null;
        }

        return new MergeCursor(cursors);
    }


    private boolean isBucketSelected(final String requestedBucketId) {
        Log.d(LOG_TAG, "Checking if bucket " + requestedBucketId + " is selected by user.");
        final SharedPreferences prefs = getDefaultSharedPreferences(PBApplication.getApp());
        final Set<String> bucketSet = prefs.getStringSet(PBConstants.PREF_PICTURE_FOLDER_LIST, null);
        return bucketSet != null && bucketSet.contains(requestedBucketId);
    }


    public ArrayMap<String, String> getBucketData(final ArrayMap<String, String> bucketNamesList, final String buckedId, final String buckedName) {

        // We want to group the images by bucket names. We abuse the
        // "WHERE" parameter to insert a "GROUP BY" clause into the SQL statement.
        // The template for "WHERE" parameter is like:
        //    SELECT ... FROM ... WHERE (%s)
        // and we make it look like:
        //    SELECT ... FROM ... WHERE (1) GROUP BY (2)
        // The "(1)" means true. The "(2)" means the second columns specified in projection.
        // Note that because there is a ")" in the template, we use "(2" to match it.
        final String[] projection = { buckedId, buckedName, "count(*) as media_count" };
        final String groupBy = "1) GROUP BY (2";
        final Cursor cursor = PBApplication.getApp().getContentResolver().query(imagesUri, projection, groupBy, null, "media_count desc");

        if (cursor != null && cursor.moveToFirst()) {
            String name;
            String id;
            String count;
            final int bucketIdColumn = cursor.getColumnIndex(buckedId);
            final int bucketNameColumn = cursor.getColumnIndex(buckedName);
            final int bucketCountColumn = cursor.getColumnIndex("media_count");
            do {
                id = cursor.getString(bucketIdColumn);
                name = cursor.getString(bucketNameColumn);
                count = cursor.getString(bucketCountColumn);
                bucketNamesList.put(id, name + " (" + count + ")");
            } while (cursor.moveToNext());
        }
        closeCursor(cursor);

        Log.d(LOG_TAG, bucketNamesList.toString());

        return bucketNamesList;
    }


    public ArrayMap<String, String> getBucketData() {
        final ArrayMap<String, String> bucketNames = new ArrayMap<>();
        getBucketData(bucketNames, MediaStore.Images.ImageColumns.BUCKET_ID, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PBApplication.getApp());
        final boolean backupVideos = sp.getBoolean(PBConstants.PREF_MEDIA_BACKUP_VIDEO, false);
        if (backupVideos) {
            getBucketData(bucketNames, MediaStore.Video.VideoColumns.BUCKET_ID, MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME);
        }
        return bucketNames;
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

        setSyncTask(new PBSyncMediaStoreTask());
        syncTask.execute();
        Log.i(LOG_TAG, "Start SyncMediaStoreTask");
    }


    void onPostSync() {
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
            setMediaList(new ArrayList<PBMedia>());
        }
        return mediaList;
    }


    ////////////////////
    // Static setters //
    ////////////////////
    private static void setMediaListToNull(){ mediaList = null; }
    private static void setSyncTask(PBSyncMediaStoreTask syncTask) { PBMediaStore.syncTask = syncTask; }
    private static void setMediaList(List<PBMedia> mediaList) { PBMediaStore.mediaList = mediaList; }
}
