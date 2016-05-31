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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.R;


public class PBServerPreferenceFragment extends PreferenceFragment
                implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = "PBServerPreferenceFragment";
    private String serverName;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;

    public static final String PREF_SERVER_NAME = "PREF_SERVER_NAME";
    public static final String PREF_SERVER_URL = "PREF_SERVER_URL";
    private static final String PREF_SERVER_PASS = "PREF_SERVER_PASS";
    public static final String PREF_SERVER_PASS_HASH = "PREF_SERVER_PASS_HASH";
    public static final String PREF_SERVER_HTTPAUTH_SWITCH = "PREF_SERVER_HTTPAUTH_SWITCH";
    public static final String PREF_SERVER_HTTPAUTH_LOGIN = "PREF_SERVER_HTTPAUTH_LOGIN";
    public static final String PREF_SERVER_HTTPAUTH_PASS = "PREF_SERVER_HTTPAUTH_PASS";


    //////////////////
    // Constructors //
    //////////////////
    public PBServerPreferenceFragment() {}


    //////////////
    // Override //
    //////////////
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.server_preferences);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferencesEditor = preferences.edit();
        preferencesEditor.apply();
    }


    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);
        setSummaries();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle!= null && bundle.containsKey(PREF_SERVER_NAME)) {
            serverName = bundle.getString(PREF_SERVER_NAME);
            configurePreference();
        }
        return view;
    }


    @Override
    public void onPause() {
        super.onPause();
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Log.i(LOG_TAG, "onSharedPreferenceChanged: " + key);

        setSummaries();
        if (key.equals(PREF_SERVER_PASS)) {
            final String pass = sharedPreferences.getString(PREF_SERVER_PASS, "");
            if (!pass.isEmpty()) {
                createAndSetServerPass(sharedPreferences);
            }

        } else if (sharedPreferences == null) {
            Log.e(LOG_TAG, "Error: preferences == null");
        }
    }


    /////////////////////
    // Private methods //
    /////////////////////
    private void configurePreference() {
        // title and back button of the action bar
        getActivity().setTitle(serverName + " server settings");
        if (getActivity().getActionBar() != null) {
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        }

            // save server name into the preferences
        preferencesEditor.putString(PREF_SERVER_NAME, serverName);

        // access configuration in servers_params.xml file
        final int arrayId = getActivity().getResources().getIdentifier(serverName, "array", getActivity().getPackageName());
        final String[] serverPrefsToRemove = getActivity().getResources().getStringArray(arrayId);

        // remove unused preferences given in xml list
        PreferenceScreen screen = (PreferenceScreen) findPreference(LOG_TAG);
        for (String param : serverPrefsToRemove) {
            Log.i(LOG_TAG, "Remove preference named: " + param);
            Preference pref = findPreference(param);
            screen.removePreference(pref);
        }
    }


    private void setSummaries() {
        final EditTextPreference urlPreference = (EditTextPreference) findPreference(PREF_SERVER_URL);
        urlPreference.setSummary(preferences.getString(PREF_SERVER_URL, this.getResources().getString(R.string.server_url_summary)));

        final String serverPassHash = preferences.getString(PREF_SERVER_PASS_HASH, "");
        final EditTextPreference serverPassTextPreference = (EditTextPreference) findPreference(PREF_SERVER_PASS);
        if (serverPassHash.isEmpty()) {
            serverPassTextPreference.setSummary(getResources().getString(R.string.server_password_summary));
        } else {
            serverPassTextPreference.setSummary(getResources().getString(R.string.server_password_summary_set));
        }

        final EditTextPreference httpLoginPreference = (EditTextPreference) findPreference(PREF_SERVER_HTTPAUTH_LOGIN);
        httpLoginPreference.setSummary(preferences.getString(PREF_SERVER_HTTPAUTH_LOGIN, ""));

        String httpPass = preferences.getString(PREF_SERVER_HTTPAUTH_PASS,"");
        if (!httpPass.isEmpty()) {
            final EditTextPreference httpPassPreference = (EditTextPreference) findPreference(PREF_SERVER_HTTPAUTH_PASS);
            httpPassPreference.setSummary(getResources().getString(R.string.server_password_summary_set));
        }
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
        preferencesEditor.putString(PREF_SERVER_PASS_HASH, hash).apply();
        // remove the real password from the preferences, for security
        preferencesEditor.putString(PREF_SERVER_PASS, "").apply();
    }

}
