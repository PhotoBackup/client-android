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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.R;

public class PBServerListPreference extends ListPreference {

    private static final String LOG_TAG = "PBServerListPreference";

    ListPreferenceAdapter listPreferenceAdapter = null;
    Context context;
    private LayoutInflater inflater;
    CharSequence[] entries;
    CharSequence[] entryValues;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;


    public PBServerListPreference(Context theContext, AttributeSet attrs) {
        super(theContext, attrs);
        context = theContext;
        inflater = LayoutInflater.from(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        editor.apply();
    }


    @Override
    protected View onCreateView(ViewGroup parent) {
        LinearLayout layout = new LinearLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
        layout.setLayoutParams(params);
        return super.onCreateView(layout);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        entries = getEntries();
        entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length ) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        listPreferenceAdapter = new ListPreferenceAdapter(context);
        builder.setAdapter(listPreferenceAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.i(LOG_TAG, "clicked");
            }
        });
    }


    private class ListPreferenceAdapter extends BaseAdapter {
        public ListPreferenceAdapter(Context context) {}

        public int getCount() {
            return entries.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View row, ViewGroup parent) {
            if(row == null) {
                row = inflater.inflate(R.layout.server_list_row, parent, false);

                CustomHolder holder = new CustomHolder(row, position);
                row.setTag(holder);
                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.i(LOG_TAG, "clicked on: " + entryValues[position]);

                        Dialog mDialog = getDialog();
                        mDialog.dismiss();
                    }
                });
            }

            return row;
        }

        class CustomHolder {
            private TextView text = null;

            CustomHolder(View row, int position)  {
                text = (TextView)row.findViewById(R.id.servername);
                text.setText(entries[position]);
            }
        }
    }
}