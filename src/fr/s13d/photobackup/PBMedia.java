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

import java.io.Serializable;

public class PBMedia implements Serializable {
    final private int id;
    final private String path;
    private PBMediaState state;
    Context context;
    public enum PBMediaState { WAITING, SYNCED, ERROR }


    //////////////////
    // Constructors //
    //////////////////
    public PBMedia(Context context, Cursor mediaCursor) {
        this.id = mediaCursor.getInt(mediaCursor.getColumnIndexOrThrow("_id"));
        this.path = mediaCursor.getString(mediaCursor.getColumnIndexOrThrow("_data"));

        // Find state from the shared preferences
        SharedPreferences preferences = context.getSharedPreferences(PBMediaStore.PhotoBackupPicturesSharedPreferences, Context.MODE_PRIVATE);
        String stateString = preferences.getString(String.valueOf(this.id), PBMedia.PBMediaState.WAITING.name());
        this.state = PBMedia.PBMediaState.valueOf(stateString);
        this.context = context;
    }


    ////////////
    // Methods//
    ////////////

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return "PBMedia: " + this.path;
    }


    /////////////////////////////////////////
    // Getters/Setters are the Java fun... //
    /////////////////////////////////////////
    public int getId() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

    public PBMediaState getState() {
        return this.state;
    }

    public void setState(PBMediaState mediaState) {
        if (this.state != mediaState) {
            this.state = mediaState;
            Log.i("PBMedia", "Setting state " + mediaState.toString() + " to " + this.getPath());

            SharedPreferences preferences = context.getSharedPreferences(PBMediaStore.PhotoBackupPicturesSharedPreferences, Context.MODE_PRIVATE);
            preferences.edit().putString(String.valueOf(this.getId()), mediaState.name()).apply();
        }
    }

}
