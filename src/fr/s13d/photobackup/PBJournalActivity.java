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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class PBJournalActivity extends ListActivity {

    private PBMediaSender mediaSender;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);


        // on click listener
        final Activity self = this;
        mediaSender = new PBMediaSender(this);
        final ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    final PBMedia media = PBActivity.getMediaStore().getMedias().get(position);

                    final AlertDialog.Builder builder = new AlertDialog.Builder(self);
                    builder.setMessage("You can backup this picture now!").setTitle("Manual backup");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mediaSender.send(media);
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                } catch(NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        // adapter
        final PBJournalAdapter adapter = new PBJournalAdapter(this);
        setListAdapter(adapter);
    }

}
