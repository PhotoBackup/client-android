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

package fr.s13d.photobackup.preferences;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Map;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBActivity;
import fr.s13d.photobackup.PBMediaSender;
import fr.s13d.photobackup.PBService;
import fr.s13d.photobackup.R;
import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;


public class PBPreferenceFragment extends PreferenceFragment
                                implements SharedPreferences.OnSharedPreferenceChangeListener,
                                           PBMediaStoreInterface, PBMediaSenderInterface {

    private static final String LOG_TAG = "PBPreferenceFragment";
    private static PBPreferenceFragment self;
    private PBService currentService;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;
    private Preference uploadJournalPref;
    public static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;


    // binding
    private boolean isBoundToService = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            PBService.Binder b = (PBService.Binder) binder;
            currentService = b.getService();
            currentService.getMediaStore().addInterface(self);
            updateUploadJournalPreference(); // update journal serverKeys number
            Log.i(LOG_TAG, "Connected to service");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "Disconnected to service");
        }
    };


    // should correspond to what is in preferences.xml
    public static final String PREF_SERVICE_RUNNING = "PREF_SERVICE_RUNNING";
    public static final String PREF_SERVER = "PREF_SERVER";
    public static final String PREF_WIFI_ONLY = "PREF_WIFI_ONLY";
    public static final String PREF_RECENT_UPLOAD_ONLY = "PREF_RECENT_UPLOAD_ONLY";


    //////////////////
    // Constructors //
    //////////////////
    public PBPreferenceFragment() {
        self = this;
    }


    //////////////
    // Override //
    //////////////
    @Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            preferencesEditor = preferences.edit();
            preferencesEditor.apply();
        }
        migratePreferences();

        addPreferencesFromResource(R.xml.preferences);
    }


    @Override
    public void onResume() {
        super.onResume();

        initPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (isPhotoBackupServiceRunning()) {
            Intent intent = new Intent(this.getActivity(), PBService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            isBoundToService = true;
        }

        updateUploadJournalPreference();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        if (isPhotoBackupServiceRunning() && isBoundToService) {
            getActivity().unbindService(serviceConnection);
            isBoundToService = false;
        }
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {

        Log.i(LOG_TAG, "onSharedPreferenceChanged: " + key);
        if (key.equals(PREF_SERVICE_RUNNING)) {
            startOrStopService(sharedPreferences);

        } else if (key.equals(PREF_WIFI_ONLY)) {
            setSummaries();

        } else if (key.equals(PREF_RECENT_UPLOAD_ONLY)) {
            setSummaries();

        } else if (sharedPreferences == null) {
            Log.e(LOG_TAG, "Error: preferences == null");
        }

    }


    /////////////////////
    // private methods //
    /////////////////////
    private void migratePreferences() {
        Map<String, ?> allPrefs = preferences.getAll();
        if (allPrefs.containsKey(PREF_WIFI_ONLY)) {
            Object obj = allPrefs.get(PREF_WIFI_ONLY);
            if (obj instanceof Boolean) {
                Log.i(LOG_TAG, "Migrating PREF_WIFI_ONLY for v0.7.0");
                Boolean bool = preferences.getBoolean(PREF_WIFI_ONLY, false);
                String wifiOnlyString = bool ? getString(R.string.only_wifi) : getString(R.string.not_only_wifi);
                preferencesEditor.putString(PREF_WIFI_ONLY, wifiOnlyString).apply();
                Log.i(LOG_TAG, "Migration done!");
            }
        }

    }


    private void initPreferences() {
        // init
        uploadJournalPref = findPreference("uploadJournalPref");
        ((PBActivity)getActivity()).setActionBar();

        // switch on if service is running
        final SwitchPreference switchPreference = (SwitchPreference) findPreference(PREF_SERVICE_RUNNING);
        switchPreference.setChecked(isPhotoBackupServiceRunning());

        // initiate preferences summaries
        setSummaries();
    }


    private void setSummaries() {
        final String wifiOnly = preferences.getString(PREF_WIFI_ONLY,
                getResources().getString(R.string.only_wifi_default)); // default
        final ListPreference wifiPreference = (ListPreference) findPreference(PREF_WIFI_ONLY);
        wifiPreference.setSummary(wifiOnly);

        final String recentUploadOnly = preferences.getString(PREF_RECENT_UPLOAD_ONLY,
                getResources().getString(R.string.only_recent_upload_default)); // default
        final ListPreference recentUploadPreference = (ListPreference) findPreference(PREF_RECENT_UPLOAD_ONLY);
        recentUploadPreference.setSummary(recentUploadOnly);

        final String serverUrl = preferences.getString(PBServerPreferenceFragment.PREF_SERVER_URL, null);
        if (serverUrl != null) {
            final String serverName = preferences.getString(PREF_SERVER, null);
            if (serverName != null) {
                final PBServerListPreference serverPreference = (PBServerListPreference) findPreference(PREF_SERVER);
                serverPreference.setSummary(serverName + " @ " + serverUrl);

                // bonus: left icon of the server
                final int serverNamesId = getResources().getIdentifier("pref_server_names", "array", getActivity().getPackageName());
                final String[] serverNames = getResources().getStringArray(serverNamesId);
                final int serverPosition = Arrays.asList(serverNames).indexOf(serverName);
                final int serverIconsId = getResources().getIdentifier("pref_server_icons", "array", getActivity().getPackageName());
                final String[] serverIcons = getResources().getStringArray(serverIconsId);
                final String serverIcon = serverIcons[serverPosition];
                final String[] parts = serverIcon.split("\\.");
                final String drawableName = parts[parts.length - 1];
                final int id = getResources().getIdentifier(drawableName, "drawable", getActivity().getPackageName());
                if (id != 0) {
                    serverPreference.setIcon(id);
                }
            }
        }
    }


    private void startOrStopService(final SharedPreferences preferences) {
        boolean userDidStart = preferences.getBoolean(PREF_SERVICE_RUNNING, false);
        Log.i(LOG_TAG, "PREF_SERVICE_RUNNING = " + userDidStart);

        if (userDidStart) {
            if (validatePreferences()) {
                Log.i(LOG_TAG, "start PhotoBackup service");
                checkPermissions();
            } else {
                final SwitchPreference switchPreference = (SwitchPreference) findPreference(PREF_SERVICE_RUNNING);
                switchPreference.setChecked(false);
            }
        } else if (isPhotoBackupServiceRunning() && currentService != null) {
            Log.i(LOG_TAG, "stop PhotoBackup service");
            getActivity().unbindService(serviceConnection);
            isBoundToService = false;
            currentService.stopSelf();
            currentService = null;
            updateUploadJournalPreference();
        }
    }


    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_READ_EXTERNAL_STORAGE);
            } else {
                Log.i(LOG_TAG, "Permission READ_EXTERNAL_STORAGE already given, cool!");
                testMediaSender(); // next step
            }
        } else { // for older Android versions, continue without asking permission
            testMediaSender();
        }
    }

    private boolean validatePreferences() {
        String serverUrl = preferences.getString(PBServerPreferenceFragment.PREF_SERVER_URL, "");
        if (!URLUtil.isValidUrl(serverUrl) || serverUrl.isEmpty()) {
            Toast.makeText(getActivity(), R.string.toast_urisyntaxexception, Toast.LENGTH_LONG).show();
            return false;
        }

        String serverPassHash = preferences.getString(PBServerPreferenceFragment.PREF_SERVER_PASS_HASH, "");
        if (serverPassHash.isEmpty()) {
            Toast.makeText(getActivity(), R.string.toast_serverpassempty, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }


    // Returns the current state of the PhotoBackup Service
    // See http://stackoverflow.com/a/5921190/417006
    private boolean isPhotoBackupServiceRunning() {
        final Activity activity = getActivity();
        if (activity != null) {
            final ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (PBService.class.getName().equals(service.service.getClassName())) {
                    Log.i(LOG_TAG, "Service is running");
                    return true;
                }
            }
        }
        Log.i(LOG_TAG, "Service or activity is NOT running");
        return false;
    }


    private void updateUploadJournalPreference() {
        try {
            if (isPhotoBackupServiceRunning() && currentService != null) {
                uploadJournalPref.setTitle(this.getResources().getString(R.string.journal_title) +
                        " (" + currentService.getMediaSize() + ")");
                uploadJournalPref.setEnabled(currentService.getMediaSize() > 0);
            } else {
                uploadJournalPref.setTitle(this.getResources().getString(R.string.journal_noaccess));
                uploadJournalPref.setEnabled(false);
            }
        } catch (IllegalStateException e) {
            Log.w(LOG_TAG, e.toString());
        }
    }


    ////////////////////
    // public methods //
    ////////////////////
    public void testMediaSender() {
        PBMediaSender mediaSender = new PBMediaSender(getActivity());
        mediaSender.addInterface(this);
        mediaSender.test();
    }


    ////////////////////////////////////
    // PBMediaStoreListener callbacks //
    ////////////////////////////////////
    public void onSyncMediaStoreTaskPostExecute() {
        updateUploadJournalPreference();
    }


    ///////////////////////////////////
    // PBMediaSenderEvents callbacks //
    ///////////////////////////////////
    public void onTestSuccess() {
        final Intent serviceIntent = new Intent(getActivity(), PBService.class);
        getActivity().startService(serviceIntent);
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        isBoundToService = true;
    }


    public void onTestFailure() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final SwitchPreference switchPreference = (SwitchPreference) findPreference(PREF_SERVICE_RUNNING);
                switchPreference.setChecked(false);
            }
        });
    }


    public void onSendSuccess() {}
    public void onSendFailure()  {}


    /////////////
    // getters //
    /////////////
    public PBService getService() {
        return currentService;
    }
}
