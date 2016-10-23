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

import android.app.Application;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.Set;

import fr.s13d.photobackup.media.PBMediaStore;


public class PBApplication extends Application {

    private final static String LOG_TAG = "PBApplication";
    private static PBApplication app;
    private static PBMediaStore mediaStore;


    ////////////////
    // Life-cycle //
    ////////////////
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "Initializing app");
        app = this;

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Default SharedPreferences:");
            Map<String, ?> allPrefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
            Set<String> set = allPrefs.keySet();
            for(String s : set) {
                Log.d(LOG_TAG, s + "<" + allPrefs.get(s).getClass().getSimpleName() +"> =  "
                        + allPrefs.get(s).toString());
            }
        }
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        trimMemory();
    }


    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        trimMemory();
    }


    private void trimMemory() {
        Log.d(LOG_TAG, "trimMemory");
        if (mediaStore != null) {
            mediaStore.close();
            mediaStore = null;
        }
    }


    ///////////////
    // Constants //
    ///////////////
    public final static String PB_USER_AGENT = "PhotoBackup Android Client v" + BuildConfig.VERSION_NAME;
    public static final String PB_MEDIAS_SHARED_PREFS = "PB_MEDIAS_SHARED_PREFS";


    /////////////////////
    // Getters/setters //
    /////////////////////
    public static PBApplication getApp() { return app; }


    public static PBMediaStore getMediaStore() {
        if (mediaStore == null) {
            mediaStore = new PBMediaStore();
            mediaStore.sync();
        }
        return mediaStore;
    }

    public static void setMediaStore(PBMediaStore store) {
        mediaStore = store;
    }
}
