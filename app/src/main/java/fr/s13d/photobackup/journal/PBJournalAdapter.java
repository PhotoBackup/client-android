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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Thumbnails;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import fr.s13d.photobackup.Log;
import fr.s13d.photobackup.PBMedia;
import fr.s13d.photobackup.R;


public class PBJournalAdapter extends ArrayAdapter<PBMedia> implements Filterable, Handler.Callback {
    private static final String LOG_TAG = "PBJournalAdapter";
	private static LayoutInflater inflater;
    private final Context context;
    private final SharedPreferences preferences;
    private WeakReference<Handler> handlerWeakReference;
    private Filter filter;
    private List<PBMedia> medias;
    private List<PBMedia> filteredMedias;


	public PBJournalAdapter(final Activity activity, final int textViewResourceId, final List<PBMedia> medias) {
        super(activity, textViewResourceId, medias);

        this.context = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.medias = medias;
        this.filteredMedias = new ArrayList<>(medias.size());

        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Create and start a new thread for the handler
        final HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        handlerWeakReference = new WeakReference<>(new Handler(handlerThread.getLooper(), this));
	}


    public void close() {
        if (handlerWeakReference != null) {
            final Handler handler = handlerWeakReference.get();
            handler.getLooper().quit();
        }
    }


    //////////////////////
    // Handler.Callback //
    //////////////////////
    @Override
    public boolean handleMessage(Message message) {
        // done on async thread
        final View view = (View)message.obj;
        final ImageView thumbImageView = (ImageView)view.findViewById(R.id.thumbnail);
        final Bitmap bitmap = Thumbnails.getThumbnail(context.getContentResolver(),
                message.what, Thumbnails.MICRO_KIND, null);

        // back on UI thread to set the bitmap to the view
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                thumbImageView.setImageBitmap(bitmap);
            }
        });

        return true;
    }


    /////////////
    // Adapter //
    /////////////
    @Override
	public View getView(int position, View view, ViewGroup parent) {
        // create the view if not available
        view = (view == null) ? inflater.inflate(R.layout.list_row, parent, false) : view;

        final PBMedia media = filteredMedias.get(position);
        if (media == null || media.getId() == -1) {
            return view;
        }

        // create thumbnail
        if (handlerWeakReference != null) {
            final Handler handler = handlerWeakReference.get();
            handler.obtainMessage(media.getId(), view).sendToTarget();
        }

        // filename
		final TextView textView = (TextView)view.findViewById(R.id.filename);
        if (media.getPath() != null) {
            final File file = new File(media.getPath());
            textView.setText(file.getName());
        } else {
            textView.setText(R.string.journal_error);
        }

        // indicator
        final TextView errorTextView = (TextView)view.findViewById(R.id.errorHint);
        final ImageView imageView = (ImageView)view.findViewById(R.id.state);
        if (media.getState() == PBMedia.PBMediaState.WAITING) {
            errorTextView.setVisibility(View.GONE);
            imageView.setImageResource(android.R.drawable.presence_away);
        } else if (media.getState() == PBMedia.PBMediaState.SYNCED) {
            errorTextView.setVisibility(View.GONE);
            imageView.setImageResource(android.R.drawable.presence_online);
        } else if (media.getState() == PBMedia.PBMediaState.ERROR) {
            errorTextView.setText(media.getErrorMessage());
            imageView.setImageResource(android.R.drawable.presence_busy);
        }

		return view;
	}


    @Override
    public int getCount() {
        int count = 0;
        try {
            count = filteredMedias.size();
        } catch (java.lang.NullPointerException e) {
            Log.w(LOG_TAG, "count = " + count);
            e.printStackTrace();
        }
        return count;
    }


    @Override
    public PBMedia getItem(final int position) {
        PBMedia item = null;
        try {
            item = filteredMedias.get(position);
        } catch (java.lang.NullPointerException e) {
            e.printStackTrace();
        }
        return item;
    }


    @Override
    public long getItemId(final int position) {
        return position;
    }


    ////////////////
    // Filterable //
    ////////////////
    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    filteredMedias = (List<PBMedia>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    final FilterResults results = new FilterResults();
                    final List<PBMedia> mediaList = getMediaList();
                    results.values = mediaList;
                    results.count = mediaList.size();
                    return results;
                }
            };
        }
        return filter;
    }


    /////////////////////
    // Private methods //
    /////////////////////
    private List<PBMedia> getMediaList() {

        //List<PBMedia> mediaList = new ArrayList<>(medias.size());
        filteredMedias.clear();
        final Boolean synced = preferences.getBoolean(PBMedia.PBMediaState.SYNCED.name(), true);
        final Boolean waiting = preferences.getBoolean(PBMedia.PBMediaState.WAITING.name(), true);
        final Boolean error = preferences.getBoolean(PBMedia.PBMediaState.ERROR.name(), true);

        for (int i = 0; i < medias.size(); i++) {
            PBMedia media = medias.get(i);
            if (media.getState() == PBMedia.PBMediaState.SYNCED && synced ||
                    media.getState() == PBMedia.PBMediaState.WAITING && waiting ||
                    media.getState() == PBMedia.PBMediaState.ERROR && error) {
                filteredMedias.add(media);
            }
        }
        return filteredMedias;
    }


    // Getter //
    public List<PBMedia> getFilteredMedias() {
        return filteredMedias;
    }
}
