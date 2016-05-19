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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ToggleButton;

import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;


public class PBJournalActivity extends ListActivity implements PBMediaSenderInterface {

    private static final String LOG_TAG = "PBJournalActivity";
    private PBJournalAdapter adapter;
    private PBMediaSender mediaSender;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // layout
        setContentView(R.layout.activity_journal);

        // preferences
        initPreferences();

        // on click listener
        final Activity self = this;
        final ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    final PBMedia media = PBActivity.getMediaStore().getMedias().get(position);

                    final AlertDialog.Builder builder = new AlertDialog.Builder(self);
                    builder.setMessage(self.getResources().getString(R.string.manual_backup_message))
                            .setTitle(self.getResources().getString(R.string.manual_backup_title));
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getMediaSender().send(media, true);
                        }
                    });
                    builder.setNegativeButton(self.getString(R.string.cancel), null);
                    builder.create().show();
                } catch(NullPointerException e) {
                    Log.w(LOG_TAG, e.toString());
                }
            }
        });

        // adapter
        adapter = new PBJournalAdapter(this);
        setListAdapter(adapter);
        adapter.getFilter().filter(null); // to init the view
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.close();
    }

    /////////////////////
    // private methods //
    /////////////////////
    private void initPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferencesEditor = preferences.edit();
            preferencesEditor.apply();
        }

        // set stored values
        ToggleButton savedButton = (ToggleButton)this.findViewById(R.id.savedToggleButton);
        savedButton.setChecked(preferences.getBoolean(PBMedia.PBMediaState.SYNCED.name(), true));
        ToggleButton waitingButton = (ToggleButton)this.findViewById(R.id.waitingToggleButton);
        waitingButton.setChecked(preferences.getBoolean(PBMedia.PBMediaState.WAITING.name(), true));
        ToggleButton errorButton = (ToggleButton)this.findViewById(R.id.errorToggleButton);
        errorButton.setChecked(preferences.getBoolean(PBMedia.PBMediaState.ERROR.name(), true));
    }


    private PBMediaSender getMediaSender() {
        if (mediaSender == null) {
            mediaSender = new PBMediaSender(this);
            mediaSender.addInterface(this);
        }
        return mediaSender;
    }


    //////////////////
    // buttons call //
    //////////////////
    public void clickOnSaved(View v) {
        Log.i("PBJournalActivity", "clickOnSaved");
        ToggleButton btn = (ToggleButton)v;
        preferencesEditor.putBoolean(PBMedia.PBMediaState.SYNCED.name(), btn.isChecked()).apply();
        adapter.getFilter().filter(null);
    }

    public void clickOnWaiting(View v) {
        Log.i("PBJournalActivity", "clickOnWaiting");
        ToggleButton btn = (ToggleButton)v;
        preferencesEditor.putBoolean(PBMedia.PBMediaState.WAITING.name(), btn.isChecked()).apply();
        adapter.getFilter().filter(null);
    }

    public void clickOnError(View v) {
        Log.i("PBJournalActivity", "clickOnError");
        ToggleButton btn = (ToggleButton)v;
        preferencesEditor.putBoolean(PBMedia.PBMediaState.ERROR.name(), btn.isChecked()).apply();
        adapter.getFilter().filter(null);
    }

    @Override
    public void onSendSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("PBJournalActivity", "Trying to refresh view");
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onSendFailure() {
        onSendSuccess();
    }

    @Override
    public void onTestSuccess() {
    }

    @Override
    public void onTestFailure() {
    }
}
