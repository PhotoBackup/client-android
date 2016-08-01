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
package fr.s13d.photobackup.journal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ToggleButton;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBActivity;
import fr.s13d.photobackup.PBMedia;
import fr.s13d.photobackup.PBMediaSender;
import fr.s13d.photobackup.R;
import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;


public class PBJournalActivity extends ListActivity implements PBMediaSenderInterface {

    private PBJournalAdapter adapter;
    private PBMediaSender mediaSender;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI (with binding)
        DataBindingUtil.setContentView(this, R.layout.activity_journal);

        // layout
        setContentView(R.layout.activity_journal);

        // preferences
        initPreferences();

        // on click listener
        ListView list = (ListView) findViewById(android.R.id.list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setOnItemClick(position, PBJournalActivity.this);
            }
        });

        // adapter
        adapter = new PBJournalAdapter(this, 0, PBActivity.getMediaStore().getMedias());
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
        ToggleButton btn = (ToggleButton) findViewById(R.id.savedToggleButton);
        btn.setChecked(preferences.getBoolean(PBMedia.PBMediaState.SYNCED.name(), true));
        btn = (ToggleButton) findViewById(R.id.waitingToggleButton);
        btn.setChecked(preferences.getBoolean(PBMedia.PBMediaState.WAITING.name(), true));
        btn = (ToggleButton) findViewById(R.id.errorToggleButton);
        btn.setChecked(preferences.getBoolean(PBMedia.PBMediaState.ERROR.name(), true));
    }


    private PBMediaSender getMediaSender() {
        if (mediaSender == null) {
            mediaSender = new PBMediaSender(this);
            mediaSender.addInterface(this);
        }
        return mediaSender;
    }


    private void setOnItemClick(int position, final Activity self) {
        try {
            final PBMedia media = adapter.getFilteredMedias().get(position);
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
            e.printStackTrace();
        }
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


    ////////////////////////////
    // PBMediaSenderInterface //
    ////////////////////////////
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
    public void onTestSuccess() {}


    @Override
    public void onTestFailure() {}
}
