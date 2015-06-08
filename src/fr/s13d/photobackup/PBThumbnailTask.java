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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.lang.ref.WeakReference;


class PBThumbnailTask extends AsyncTask<Integer, Void, Bitmap> {
	private final WeakReference<ImageView> imageViewReference;
    private Context context;


	public PBThumbnailTask(Context theContext, ImageView imageView) {
		imageViewReference = new WeakReference<>(imageView);
        context = theContext;
	}


	@Override
	protected Bitmap doInBackground(Integer... params) {
        return MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                params[0], MediaStore.Images.Thumbnails.MINI_KIND, null);
	}


	@Override
	protected void onPostExecute(final Bitmap bitmap) {
		if (!isCancelled()) {
            final ImageView imageView = imageViewReference.get();
            if (bitmap != null && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
		}
	}

}
