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

package fr.s13d.photobackup.settings;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBMediaSender;
import fr.s13d.photobackup.PBService;
import fr.s13d.photobackup.R;
import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;


public class PBSettingsFragment extends PreferenceFragment
                                implements SharedPreferences.OnSharedPreferenceChangeListener,
                                           PBMediaStoreInterface, PBMediaSenderInterface {

    private static final String LOG_TAG = "PBSettingsFragment";
    private static PBSettingsFragment self;
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
            updateUploadJournalPreference(); // update journal entries number
            Log.i(LOG_TAG, "Connected to service");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "Disconnected to service");
        }
    };

    // receiver
    private final BroadcastReceiver stopServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(PBService.STOP_SERVICE)) {
                // stop the service
                final SwitchPreference switchPreference = (SwitchPreference) findPreference(PREF_SERVICE_RUNNING);
                switchPreference.setChecked(false);
            }
        }
    };


    // should correspond to what is in preferences.xml
    public static final String PREF_SERVICE_RUNNING = "PREF_SERVICE_RUNNING";
    public static final String PREF_SERVER_URL = "PREF_SERVER_URL";
    private static final String PREF_SERVER_PASS = "PREF_SERVER_PASS";
    public static final String PREF_SERVER_PASS_HASH = "PREF_SERVER_PASS_HASH";
    public static final String PREF_WIFI_ONLY = "PREF_WIFI_ONLY";
    public static final String PREF_RECENT_UPLOAD_ONLY = "PREF_RECENT_UPLOAD_ONLY";


    //////////////////
    // Constructors //
    //////////////////
    public PBSettingsFragment() {
        self = this;
    }


    //////////////
    // Override //
    //////////////
    @Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
    }


    @Override
    public void onResume() {
        super.onResume();

        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            preferencesEditor = preferences.edit();
            preferencesEditor.apply();
        }
        migratePreferences();

        initPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (isPhotoBackupServiceRunning()) {
            Intent intent = new Intent(this.getActivity(), PBService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            isBoundToService = true;
        }

        // Register a receiver for stop service message
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(stopServiceBroadcastReceiver,
                new IntentFilter(PBService.STOP_SERVICE));

        updateServerPasswordPreference();
        updateUploadJournalPreference();
    }


    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(stopServiceBroadcastReceiver);
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

        } else if (key.equals(PREF_SERVER_URL)) {
            updateServerUrlPreference();

        } else if (key.equals(PREF_SERVER_PASS)) {
            final String pass = sharedPreferences.getString(PREF_SERVER_PASS, "");
            if (!pass.isEmpty()) {
                createAndSetServerPass(sharedPreferences);
                updateServerPasswordPreference();
            }

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

        // switch on if service is running
        final SwitchPreference switchPreference = (SwitchPreference) findPreference(PREF_SERVICE_RUNNING);
        switchPreference.setChecked(isPhotoBackupServiceRunning());

        // show set server url
        updateServerUrlPreference();

        // initiate preferences summaries
        setSummaries();
    }


    private void setSummaries() {
        String wifiOnly = preferences.getString(PREF_WIFI_ONLY,
                getResources().getString(R.string.only_wifi_default)); // default
        final ListPreference wifiPreference = (ListPreference) findPreference(PREF_WIFI_ONLY);
        wifiPreference.setSummary(wifiOnly);

        String recentUploadOnly = preferences.getString(PREF_RECENT_UPLOAD_ONLY,
                getResources().getString(R.string.only_recent_upload_default)); // default
        final ListPreference recentUploadPreference = (ListPreference) findPreference(PREF_RECENT_UPLOAD_ONLY);
        recentUploadPreference.setSummary(recentUploadOnly);
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
        String serverUrl = preferences.getString(PREF_SERVER_URL, "");
        if (!URLUtil.isValidUrl(serverUrl) || serverUrl.isEmpty()) {
            Toast.makeText(getActivity(), R.string.toast_urisyntaxexception, Toast.LENGTH_LONG).show();
            return false;
        }

        String serverPassHash = preferences.getString(PREF_SERVER_PASS_HASH, "");
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


    private void createAndSetServerPass(final SharedPreferences sharedPreferences) {
        // store only the hash of the password in the preferences
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "ERROR: " + e.getMessage());
            return;
        }

        final String pass = sharedPreferences.getString(PREF_SERVER_PASS, null);
        if (pass == null) {
            return;
        }

        // compute the hash
        md.update(pass.getBytes());
        byte[] mb = md.digest();
        String hash = "";
        for (byte temp : mb) {
            String s = Integer.toHexString(temp);
            while (s.length() < 2) {
                s = "0" + s;
            }
            s = s.substring(s.length() - 2);
            hash += s;
        }

        // set the hash in the preferences
        preferencesEditor.putString(PREF_SERVER_PASS_HASH, hash);
        preferencesEditor.apply();

        // Remove the real password from the preferences, for security.
        preferencesEditor.putString(PREF_SERVER_PASS, "").apply();
    }


    private void updateServerPasswordPreference() {
        /*final String serverPassHash = preferences.getString(PREF_SERVER_PASS_HASH, "");
        final EditTextPreference serverPassTextPreference = (EditTextPreference) findPreference(PREF_SERVER_PASS);
        if (serverPassHash.isEmpty()) {
            serverPassTextPreference.setSummary(getResources().getString(R.string.server_password_summary));
        } else {
            serverPassTextPreference.setSummary(getResources().getString(R.string.server_password_summary_set));
        }*/
    }


    private void updateServerUrlPreference() {
//        final EditTextPreference textPreference = (EditTextPreference) findPreference(PREF_SERVER_URL);
//        textPreference.setSummary(preferences.getString(PREF_SERVER_URL, this.getResources().getString(R.string.server_url_summary)));
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
            e.printStackTrace();
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
        final SwitchPreference switchPreference = (SwitchPreference) findPreference(PREF_SERVICE_RUNNING);
        switchPreference.setChecked(false);
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
