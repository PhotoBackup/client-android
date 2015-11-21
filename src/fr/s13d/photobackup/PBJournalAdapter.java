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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;


public class PBJournalAdapter extends BaseAdapter implements Filterable, Handler.Callback {
    private static final String LOG_TAG = "PBJournalAdapter";
	private static LayoutInflater inflater;
    private final Context context;
    private final SharedPreferences preferences;
    private Handler messageHandler;
    private ArrayList<PBMedia> items;


	public PBJournalAdapter(final Activity activity) {
        context = activity;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        // Create and start a new thread for the handler
        HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        messageHandler = new Handler(handlerThread.getLooper(), this);
	}


    public void close() {
        messageHandler.getLooper().quit();
    }


    @Override
    public boolean handleMessage(Message message) {
        // done on async thread
        View view = (View)message.obj;
        final ImageView thumbImageView = (ImageView)view.findViewById(R.id.thumbnail);
        final Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                message.arg1, MediaStore.Images.Thumbnails.MINI_KIND, null);

        // back on UI thread to set the bitmap to the view
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                thumbImageView.setImageBitmap(bitmap);
            }
        });

        return true;
    }


    @Override
	public View getView(int position, View view, ViewGroup parent) {
        // create the view if not available
        view = (view == null) ? inflater.inflate(R.layout.list_row, parent, false) : view;

        // fetch media from store list
        //PBMedia media = PBActivity.getMediaStore().getMedias().get(position);
        PBMedia media = items.get(position);
        if (media == null || media.getId() == -1) {
            return view;
        }

        // thumbnail
        messageHandler.obtainMessage(0, media.getId(), 0, view).sendToTarget();

		// filename
		final TextView textView = (TextView)view.findViewById(R.id.filename);
        if (media.getPath() != null) {
            final File file = new File(media.getPath());
            textView.setText(file.getName());
        } else {
            textView.setText("Error on picture data");
        }

		// indicator
        final ImageView imageView = (ImageView)view.findViewById(R.id.state);
        if (media.getState() == PBMedia.PBMediaState.WAITING) {
            imageView.setImageResource(android.R.drawable.presence_away);
        } else if (media.getState() == PBMedia.PBMediaState.SYNCED) {
            imageView.setImageResource(android.R.drawable.presence_online);
        } else if (media.getState() == PBMedia.PBMediaState.ERROR) {
            imageView.setImageResource(android.R.drawable.presence_busy);
        }

		return view;
	}


    @Override
    public Filter getFilter() {
        return new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                items = (ArrayList<PBMedia>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<PBMedia> mediaList = new ArrayList<>();

                // perform search
                Boolean synced = preferences.getBoolean(PBMedia.PBMediaState.SYNCED.name(), true);
                Boolean waiting = preferences.getBoolean(PBMedia.PBMediaState.WAITING.name(), true);
                Boolean error = preferences.getBoolean(PBMedia.PBMediaState.ERROR.name(), true);
                for (PBMedia media : PBActivity.getMediaStore().getMedias()) {
                    if (media.getState() == PBMedia.PBMediaState.SYNCED && synced ||
                            media.getState() == PBMedia.PBMediaState.WAITING && waiting ||
                            media.getState() == PBMedia.PBMediaState.ERROR && error) {
                        mediaList.add(media);
                    }
                }

                results.values = mediaList;
                results.count = mediaList.size();
                return results;
            }
        };
    }

    @Override
    public int getCount() {
        int count = 0;
        try {
            count = items.size();
        } catch (java.lang.NullPointerException e) {
            Log.e(LOG_TAG, "count = " + count);
        }
        return count;
    }

    @Override
    public Object getItem(final int position) {
        try {
            return items.get(position);
        } catch (java.lang.NullPointerException e) {
            return null;
        }
        //return PBActivity.getMediaStore().getMediaAt(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

}
