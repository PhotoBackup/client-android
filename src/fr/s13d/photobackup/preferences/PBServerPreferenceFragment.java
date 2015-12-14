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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.R;

public class PBServerPreferenceFragment extends PreferenceFragment {

    private static final String LOG_TAG = "PBServerPreferenceFragment";
    private String serverName;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;

    public static final String PREF_SERVER_NAME = "PREF_SERVER_NAME";

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle!= null && bundle.containsKey(PREF_SERVER_NAME)) {
            serverName = bundle.getString(PREF_SERVER_NAME);
            configurePreference();
        }
        return view;
    }


    /////////////////////
    // Private methods //
    /////////////////////
    private void configurePreference() {
        getActivity().setTitle(serverName + " server settings");

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

}
