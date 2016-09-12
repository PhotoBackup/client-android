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
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import fr.s13d.photobackup.BuildConfig;
import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBActivity;
import fr.s13d.photobackup.PBApplication;
import fr.s13d.photobackup.PBConstants;
import fr.s13d.photobackup.media.PBMediaSender;
import fr.s13d.photobackup.PBService;
import fr.s13d.photobackup.R;
import fr.s13d.photobackup.about.PBAboutActivity;
import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;
import fr.s13d.photobackup.interfaces.PBMediaStoreInterface;
import fr.s13d.photobackup.journal.PBJournalActivity;


public class PBPreferenceFragment extends PreferenceFragment
                                implements SharedPreferences.OnSharedPreferenceChangeListener,
                                           PBMediaStoreInterface, PBMediaSenderInterface {

    private static final String LOG_TAG = "PBPreferenceFragment";
    private PBService currentService;
    private final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PBApplication.getApp());
    private ArrayMap<String, String> bucketNames;
    public static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;
    private int permissionOrigin;


    // binding
    private boolean isBoundToService = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            PBService.Binder b = (PBService.Binder) binder;
            currentService = b.getService();
            updatePreferences(); // update journal serverKeys number
            Log.i(LOG_TAG, "Connected to service");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "Disconnected to service");
        }
    };


    //////////////
    // Override //
    //////////////
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        migratePreferences();

        // create preferences
        addPreferencesFromResource(R.xml.preferences);

        // add click listeners to launch activities on click
        // Intents were historically defined into preferences.xml but as build variants cannot
        // inject applicationId as intent packageName properly, it was done this way now...
        setOnClickListener(PBConstants.PREF_ABOUT, PBAboutActivity.class);
        setOnClickListener(PBConstants.PREF_UPLOAD_JOURNAL, PBJournalActivity.class);

        final PreferenceScreen mediasPreferenceScreen = (PreferenceScreen) findPreference(PBConstants.PREF_MEDIAS_TO_BACKUP);
        mediasPreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                checkPermissions(PBConstants.PERM_ORIGIN_MEDIAS);
                return true;
            }
        });
        setHasOptionsMenu(true);
    }


    private void setOnClickListener(final String preferenceKey, final Class cls) {
        final Preference uploadJournalPref = findPreference(preferenceKey);
        uploadJournalPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(PBPreferenceFragment.this.getActivity(), cls);
                startActivity(intent);
                return true;
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        initPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);
        PBApplication.getMediaStore().addInterface(this);

        if (isPhotoBackupServiceRunning()) {
            Intent intent = new Intent(this.getActivity(), PBService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            isBoundToService = true;
        }

        updatePreferences();
        try {
            fillBuckets();
        } catch (SecurityException e) {
            Log.d(LOG_TAG, "Permission denied...");
        }

    }


    @Override
    public void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        PBApplication.getMediaStore().removeInterface(this);

        if (isPhotoBackupServiceRunning() && isBoundToService) {
            getActivity().unbindService(serviceConnection);
            isBoundToService = false;
        }
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {

        Log.i(LOG_TAG, "onSharedPreferenceChanged: " + key);
        if (key.equals(PBConstants.PREF_SERVICE_RUNNING)) {
            startOrStopService(sharedPreferences);

        } else if (key.equals(PBConstants.PREF_WIFI_ONLY)) {
            setSummaries();

        } else if (key.equals(PBConstants.PREF_RECENT_UPLOAD_ONLY)) {
            setSummaries();

        } else if (key.equals(PBConstants.PREF_PICTURE_FOLDER_LIST)) {
            Log.w(LOG_TAG, "PREF_PICTURE_FOLDER_LIST");
            setSummaries();
            PBApplication.getMediaStore().sync();
        } else if (sharedPreferences == null) {
            Log.e(LOG_TAG, "Error: preferences == null");
        }

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.menu_upload_all).setVisible(isPhotoBackupServiceRunning());
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_upload_all) {
            currentService.sendNextMedia();
            Toast.makeText(getActivity(), R.string.toast_upload_all, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /////////////////////
    // private methods //
    /////////////////////
    private void migratePreferences() {
        Map<String, ?> allPrefs = preferences.getAll();
        if (allPrefs.containsKey(PBConstants.PREF_WIFI_ONLY)) {
            Object obj = allPrefs.get(PBConstants.PREF_WIFI_ONLY);
            if (obj instanceof Boolean) {
                Log.i(LOG_TAG, "Migrating PREF_WIFI_ONLY for v0.7.0");
                Boolean bool = preferences.getBoolean(PBConstants.PREF_WIFI_ONLY, false);
                String wifiOnlyString = bool ? getString(R.string.only_wifi) : getString(R.string.not_only_wifi);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PBConstants.PREF_WIFI_ONLY, wifiOnlyString).apply();
                Log.i(LOG_TAG, "Migration done!");
            }
        }

    }


    private void initPreferences() {
        ((PBActivity) getActivity()).setActionBar();

        // switch on if service is running
        final SwitchPreference switchPreference = (SwitchPreference) findPreference(PBConstants.PREF_SERVICE_RUNNING);
        switchPreference.setChecked(isPhotoBackupServiceRunning());

        // initiate preferences summaries
        setSummaries();
    }


    private void setSummaries() {
        final String wifiOnly = preferences.getString(PBConstants.PREF_WIFI_ONLY,
                getResources().getString(R.string.only_wifi_default)); // default
        final ListPreference wifiPreference = (ListPreference) findPreference(PBConstants.PREF_WIFI_ONLY);
        wifiPreference.setSummary(wifiOnly);

        final String recentUploadOnly = preferences.getString(PBConstants.PREF_RECENT_UPLOAD_ONLY,
                getResources().getString(R.string.only_recent_upload_default)); // default
        final ListPreference recentUploadPreference = (ListPreference) findPreference(PBConstants.PREF_RECENT_UPLOAD_ONLY);
        recentUploadPreference.setSummary(recentUploadOnly);

        final String serverUrl = preferences.getString(PBServerPreferenceFragment.PREF_SERVER_URL, null);
        if (serverUrl != null) {
            final String serverName = preferences.getString(PBConstants.PREF_SERVER, null);
            if (serverName != null) {
                final PBServerListPreference serverPreference = (PBServerListPreference) findPreference(PBConstants.PREF_SERVER);
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

        String bucketSummary = "";
        final Set<String> selectedBuckets = preferences.getStringSet(PBConstants.PREF_PICTURE_FOLDER_LIST, null);
        if (selectedBuckets != null && bucketNames != null) {
            ArrayList<String> selectedBucketNames = new ArrayList<>();
            for (String entry : selectedBuckets) {
                String oneName = bucketNames.get(entry);
                if (oneName != null) {
                    oneName = oneName.substring(0, oneName.lastIndexOf('(') - 1);
                    selectedBucketNames.add(oneName);
                }
            }
            bucketSummary = TextUtils.join(", ", selectedBucketNames);
        }
        final MultiSelectListPreference bucketListPreference = (MultiSelectListPreference) findPreference(PBConstants.PREF_PICTURE_FOLDER_LIST);
        bucketListPreference.setSummary(bucketSummary);
    }


    private void startOrStopService(final SharedPreferences preferences) {
        boolean userDidStart = preferences.getBoolean(PBConstants.PREF_SERVICE_RUNNING, false);
        Log.i(LOG_TAG, "PREF_SERVICE_RUNNING = " + userDidStart);

        if (userDidStart) {
            if (validatePreferences()) {
                checkPermissions(PBConstants.PERM_ORIGIN_SERVICE);
            } else {
                final SwitchPreference switchPreference = (SwitchPreference) findPreference(PBConstants.PREF_SERVICE_RUNNING);
                switchPreference.setChecked(false);
            }
        } else if (isPhotoBackupServiceRunning() && currentService != null) {
            Log.i(LOG_TAG, "stop PhotoBackup service");
            getActivity().unbindService(serviceConnection);
            getActivity().invalidateOptionsMenu();
            isBoundToService = false;
            currentService.stopSelf();
            currentService = null;
            updatePreferences();
        }
    }


    private void checkPermissions(final int permOrigin) {
        permissionOrigin = permOrigin;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_READ_EXTERNAL_STORAGE);
            } else {
                Log.i(LOG_TAG, "Permission READ_EXTERNAL_STORAGE already given, cool!");
                didGrantPermission(); // next step
            }
        } else { // for older Android versions, continue without asking permission
            didGrantPermission();
        }
    }


    public void didGrantPermission() {
        if (permissionOrigin == PBConstants.PERM_ORIGIN_SERVICE) {
            testMediaSender();
        } else if (permissionOrigin == PBConstants.PERM_ORIGIN_MEDIAS) {
            fillBuckets();
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
                boolean sameClass = PBService.class.getName().equals(service.service.getClassName());
                boolean samePackage = BuildConfig.APPLICATION_ID.equals(service.service.getPackageName());
                if (sameClass && samePackage) {
                    Log.i(LOG_TAG, "Service is running");
                    return true;
                }
            }
        }
        Log.i(LOG_TAG, "Service or activity is NOT running");
        return false;
    }


    private void updatePreferences() {
        try {
            final Preference uploadJournalPref = findPreference(PBConstants.PREF_UPLOAD_JOURNAL);
            // service is running
            if (isPhotoBackupServiceRunning() && currentService != null) {
                int nbMedia = PBApplication.getMediaStore().getMediaList().size();
                uploadJournalPref.setTitle(this.getResources().getString(R.string.journal_title) + " (" + nbMedia + ")");
                uploadJournalPref.setEnabled(nbMedia > 0);
            // service is not running
            } else {
                uploadJournalPref.setTitle(getResources().getString(R.string.journal_noaccess));
                uploadJournalPref.setEnabled(false);
            }
        } catch (IllegalStateException e) {
            Log.w(LOG_TAG, e.toString());
        }
    }


    private void fillBuckets() {
        this.bucketNames = PBApplication.getMediaStore().getBucketData();

        final CharSequence[] bucketIds = this.bucketNames.values().toArray(new CharSequence[this.bucketNames.size()]);
        final CharSequence[] bucketNames = this.bucketNames.keySet().toArray(new CharSequence[this.bucketNames.size()]);

        final MultiSelectListPreference bucketListPreference = (MultiSelectListPreference) findPreference("PREF_PICTURE_FOLDER_LIST");
        bucketListPreference.setEntries(bucketIds);
        bucketListPreference.setEnabled(true);
        bucketListPreference.setEntryValues(bucketNames);

        setSummaries();
    }


    ////////////////////
    // public methods //
    ////////////////////
    private void testMediaSender() {
        Log.i(LOG_TAG, "start PhotoBackup service");
        PBMediaSender mediaSender = new PBMediaSender();
        mediaSender.addInterface(this);
        mediaSender.test();
    }


    ////////////////////////////////////
    // PBMediaStoreListener callbacks //
    ////////////////////////////////////
    public void onSyncMediaStoreTaskPostExecute() {
        updatePreferences();
    }


    ///////////////////////////////////
    // PBMediaSenderEvents callbacks //
    ///////////////////////////////////
    public void onMessage(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PBApplication.getApp(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void onTestSuccess() {
        final Intent serviceIntent = new Intent(getActivity(), PBService.class);
        getActivity().startService(serviceIntent);
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        isBoundToService = true;
        getActivity().invalidateOptionsMenu();
    }


    public void onTestFailure() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final SwitchPreference switchPreference = (SwitchPreference) findPreference(PBConstants.PREF_SERVICE_RUNNING);
                switchPreference.setChecked(false);
            }
        });
    }


    public void onSendSuccess() {}
    public void onSendFailure()  {}
}
