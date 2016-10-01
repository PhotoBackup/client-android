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
import android.widget.Toast;
import android.widget.ToggleButton;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBApplication;
import fr.s13d.photobackup.media.PBMedia;
import fr.s13d.photobackup.media.PBMediaSender;
import fr.s13d.photobackup.R;
import fr.s13d.photobackup.databinding.ActivityJournalBinding;
import fr.s13d.photobackup.interfaces.PBMediaSenderInterface;


public class PBJournalActivity extends ListActivity implements PBMediaSenderInterface {

    private PBJournalAdapter adapter;
    private PBMediaSender mediaSender;
    private SharedPreferences preferences;
    private ActivityJournalBinding binding;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI (with binding)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_journal);

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
        adapter = new PBJournalAdapter(this, PBApplication.getMediaStore().getMediaList());
        setListAdapter(adapter);
        adapter.getFilter().filter(null); // to init the view
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.close();
    }



    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    /////////////////////
    // private methods //
    /////////////////////
    private void initPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
        }

        // set stored values
        binding.savedToggleButton.setChecked(preferences.getBoolean(PBMedia.PBMediaState.SYNCED.name(), true));
        binding.waitingToggleButton.setChecked(preferences.getBoolean(PBMedia.PBMediaState.WAITING.name(), true));
        binding.errorToggleButton.setChecked(preferences.getBoolean(PBMedia.PBMediaState.ERROR.name(), true));
    }


    private PBMediaSender getMediaSender() {
        if (mediaSender == null) {
            mediaSender = new PBMediaSender();
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
        final SharedPreferences.Editor preferencesEditor = preferences.edit();
        preferencesEditor.putBoolean(PBMedia.PBMediaState.SYNCED.name(), btn.isChecked()).apply();
        adapter.getFilter().filter(null);
    }


    public void clickOnWaiting(View v) {
        Log.i("PBJournalActivity", "clickOnWaiting");
        ToggleButton btn = (ToggleButton)v;
        final SharedPreferences.Editor preferencesEditor = preferences.edit();
        preferencesEditor.putBoolean(PBMedia.PBMediaState.WAITING.name(), btn.isChecked()).apply();
        adapter.getFilter().filter(null);
    }


    public void clickOnError(View v) {
        Log.i("PBJournalActivity", "clickOnError");
        ToggleButton btn = (ToggleButton)v;
        final SharedPreferences.Editor preferencesEditor = preferences.edit();
        preferencesEditor.putBoolean(PBMedia.PBMediaState.ERROR.name(), btn.isChecked()).apply();
        adapter.getFilter().filter(null);
    }


    ////////////////////////////
    // PBMediaSenderInterface //
    ////////////////////////////
    public void onMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PBApplication.getApp(), message, Toast.LENGTH_SHORT).show();
            }
        });
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
    public void onSendFailure() { onSendSuccess(); }


    @Override
    public void onTestSuccess() {}


    @Override
    public void onTestFailure() {}
}
