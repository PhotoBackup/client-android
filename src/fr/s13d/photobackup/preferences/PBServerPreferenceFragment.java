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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.R;

public class PBServerPreferenceFragment extends PreferenceFragment {

    private static final String LOG_TAG = "PBServerPreferenceFragment";
    private String serverName;
    public static final String SERVER_NAME = "SERVER_NAME";

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
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle!= null && bundle.containsKey(SERVER_NAME)) {
            serverName = bundle.getString(SERVER_NAME);
            getActivity().setTitle(serverName + " server settings");

            // access configuration in servers_params.xml file
            final int arrayId = getActivity().getResources().getIdentifier(serverName, "array", getActivity().getPackageName());
            final String[] serverParams = getActivity().getResources().getStringArray(arrayId);

            // 2. remove/hide unused preferences depending on xml list
            PreferenceScreen screen = (PreferenceScreen) findPreference("PBServerPreferenceFragment");
            PreferenceCategory cat = (PreferenceCategory) findPreference("basicauth");
            screen.removePreference(cat);
            for (String param : serverParams) {
                Log.i(LOG_TAG, "param: " + param);
            }

        }
        return view;
    }

}
