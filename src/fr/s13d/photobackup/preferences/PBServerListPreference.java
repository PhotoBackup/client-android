/**
 * Copyright (C) 2013-2015 Stéphane Péchard.
 * <p/>
 * This file is part of PhotoBackup.
 * <p/>
 * PhotoBackup is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * PhotoBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.s13d.photobackup.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.R;

public class PBServerListPreference extends ListPreference {

    private static final String LOG_TAG = "PBServerListPreference";

    ListPreferenceAdapter listPreferenceAdapter = null;
    Context context;
    private LayoutInflater inflater;
    CharSequence[] servers;
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
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        servers = getEntryValues();

        if (servers == null) {
            throw new IllegalStateException("ListPreference requires an entryValues array!");
        }

        listPreferenceAdapter = new ListPreferenceAdapter();
        builder.setAdapter(listPreferenceAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.i(LOG_TAG, "clicked");
            }
        });
    }


    class ListPreferenceAdapter extends BaseAdapter {
        public ListPreferenceAdapter() {}

        public int getCount() {
            return servers.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View row, ViewGroup parent) {
            if (row == null) {
                row = inflater.inflate(R.layout.server_list_row, parent, false);
                RowHolder holder = new RowHolder(position, row);

                row.setTag(holder);
                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.i(LOG_TAG, "clicked on: " + servers[position]);
                        clickedAtPosition(position);

                        Dialog mDialog = getDialog();
                        mDialog.dismiss();
                    }
                });
            }

            return row;
        }


        private void clickedAtPosition(int position) {
            // build new preference fragment from server position in list
            Bundle fragmentArguments = new Bundle(1);
            fragmentArguments.putString(PBServerPreferenceFragment.SERVER_NAME,
                    servers[position].toString());
            PBServerPreferenceFragment fragment = new PBServerPreferenceFragment();
            fragment.setArguments(fragmentArguments);

            // put it on back stack
            final Activity activity = (Activity) context;
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(android.R.id.content, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }


        class RowHolder {
            private TextView text = null;

            RowHolder(int position, View row) {
                text = (TextView) row.findViewById(R.id.servername);
                text.setText(servers[position]);
                setRowIcon(position, row);
            }

            private void setRowIcon(final int position, final View row) {
                final int arrayId = context.getResources().getIdentifier("pref_server_icons", "array", context.getPackageName());
                final String[] stringArray = context.getResources().getStringArray(arrayId);
                final String drawablePath = stringArray[position];
                final String[] parts = drawablePath.split("\\.");
                final String drawableName = parts[parts.length - 1];
                final int drawableId = context.getResources().getIdentifier(drawableName,
                        "drawable", context.getPackageName());
                if (drawableId != 0) {
                    final ImageView imageView = (ImageView)row.findViewById(R.id.thumbnail);
                    imageView.setImageResource(drawableId);
                }
            }

        }
    }
}