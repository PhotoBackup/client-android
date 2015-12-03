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
import android.graphics.Color;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.R;

public class PBServerListPreference extends ListPreference {

    private static final String LOG_TAG = "PBServerListPreference";

    CustomListPreferenceAdapter customListPreferenceAdapter = null;
    Context mContext;
    private LayoutInflater mInflater;
    CharSequence[] entries;
    CharSequence[] entryValues;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;


    public PBServerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = prefs.edit();
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        entries = getEntries();
        entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length ) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        customListPreferenceAdapter = new CustomListPreferenceAdapter(mContext);
        builder.setAdapter(customListPreferenceAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.i(LOG_TAG, "clicked");
            }
        });
    }


    private class CustomListPreferenceAdapter extends BaseAdapter {
        public CustomListPreferenceAdapter(Context context) {

        }

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
                row = mInflater.inflate(R.layout.list_row, parent, false);
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
                text = (TextView)row.findViewById(R.id.filename);
                text.setText(entries[position]);
            }
        }
    }
}