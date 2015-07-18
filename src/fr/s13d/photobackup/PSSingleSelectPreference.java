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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;


public class PSSingleSelectPreference extends DialogPreference {

    private final Context context;
    private final PSSingleSelectPreference self;
    private final CharSequence[] choices;
    private final SharedPreferences preferences;
    private Boolean wifiOnly;


    public PSSingleSelectPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        self = this;
        context = ctx;
        choices = new CharSequence[] {
                context.getResources().getString(R.string.only_wifi),
                context.getResources().getString(R.string.not_only_wifi)
        };

        // set original state summary
        preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        wifiOnly = preferences.getBoolean(PBSettingsFragment.PREF_WIFI_ONLY,
                PBSettingsFragment.DEFAULT_WIFI_ONLY);
        setSummary(choices[wifiOnly ? 0 : 1]);
    }


    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {

        builder.setSingleChoiceItems(choices, wifiOnly ? 0 : 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // update preferences
                        wifiOnly = (which == 0);
                        final SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(PBSettingsFragment.PREF_WIFI_ONLY, wifiOnly).apply();

                        // update view
                        self.setSummary(choices[which]);
                        self.getDialog().dismiss();
                    }
                });

        builder.setNegativeButton(context.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        builder.setPositiveButton("", null);

    }
}
