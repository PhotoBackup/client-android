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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBApplication;


public class PBSyncMediaStoreTask extends AsyncTask<Void, Void, Void> {

    private static final String LOG_TAG = "PBSyncMediaStoreTask";
    private final SharedPreferences picturesPreferences;


    /////////////////
    // Constructor //
    /////////////////
    public PBSyncMediaStoreTask() {
        this.picturesPreferences = PBApplication.getApp().getSharedPreferences(PBApplication.PB_PICTURES_SHARED_PREFS, Context.MODE_PRIVATE);
    }


    /////////////////////////////////
    // What makes you an AsyncTask //
    /////////////////////////////////
    protected Void doInBackground(Void... voids) {

        // Get all known pictures in PB
        final Map<String, ?> mediasMap = picturesPreferences.getAll();
        final Set<String> inCursor = new HashSet<>();

        // Get all pictures on device
        final Cursor cursor = PBApplication.getMediaStore().getAllMediasCursor();

        // loop through them to sync
        PBMedia media;
        String stateString;
        PBMedia.PBMediaState state;
        PBApplication.getMediaStore().getMediaList().clear();
        while (cursor != null && cursor.moveToNext()) {
            if(isCancelled()) {
                Log.i(LOG_TAG, "PBSyncMediaStoreTask cancelled");
                return null;
            }
            // create new media
            media = new PBMedia(cursor);
            stateString = (String)mediasMap.get(Integer.toString(media.getId()));
            state = (stateString != null) ?
                    PBMedia.PBMediaState.valueOf(stateString) : PBMedia.PBMediaState.WAITING;
            media.setState(state);
            PBApplication.getMediaStore().getMediaList().add(media); // populate list
            inCursor.add(Integer.toString(media.getId()));
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // purge pictures in preferences that were removed from device
        final Set<String> inCursorCopy = new HashSet<>(inCursor);
        final Set<String> inMap = new HashSet<>(mediasMap.keySet());
        inMap.removeAll(inCursor);
        inCursor.removeAll(inCursorCopy);
        inMap.addAll(inCursor);

        for (String key : inMap) {
            Log.d(LOG_TAG, "Remove media " + key + " from preference");
            picturesPreferences.edit().remove(key).apply();
        }

        return null;
    }

    protected void onPostExecute(Void result) {
        PBApplication.getMediaStore().onPostSync();
    }
}