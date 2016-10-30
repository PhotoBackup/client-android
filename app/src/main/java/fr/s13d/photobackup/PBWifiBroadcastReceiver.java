/**
 * Copyright (C) 2013-2016 Stéphane Péchard.
 * <p>
 * This file is part of PhotoBackup.
 * <p>
 * PhotoBackup is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * PhotoBackup is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package fr.s13d.photobackup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.util.List;

import fr.s13d.photobackup.media.PBMedia;


/**
 * Receives wifi broadcast intents and starts media upload if required.
 */
public class PBWifiBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "PBWifiBroadcastReceiver";
    private static long lastFiredOn = 0;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        // Receiver should only live a short time, but the onReceive is called multiple times
        // Only accept reception every 10 minutes
        final long now = System.currentTimeMillis() / 1000;
        if (now - getLastFiredOn() < 600) {
            return;
        }

        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String wifiOnlyString = preferences.getString(PBConstants.PREF_WIFI_ONLY, context.getResources().getString(R.string.only_wifi_default));
        final Boolean wifiOnly = wifiOnlyString.equals(context.getResources().getString(R.string.only_wifi));

        Log.i(LOG_TAG, "New intent: action=" + intent.getAction() + ", type=" + intent.getType());
        if (wifiManager.isWifiEnabled() && wifiOnly) {
            handleWifi(context, now);
        }
    }


    private void handleWifi(final Context context, final long now) {
        Log.i(LOG_TAG, "Wifi comes back, checking Service");
        final IBinder binder = peekService(context, new Intent(context, PBService.class));
        if (binder == null) {
            return;
        }
        final PBService service = ((PBService.Binder) binder).getService();
        if (service == null) {
            return;
        }
        setLastFiredOn(now);

        final List<PBMedia> medias = PBApplication.getMediaStore().getMediaList();
        Log.i(LOG_TAG, "media count = " + medias.size());

        for (final PBMedia media : medias) {
            if (media.getAge() < 3600 * 24 * 7 && media.getState() != PBMedia.PBMediaState.SYNCED) {
                Log.i(LOG_TAG, "Notify to send " + media.getPath());
                service.sendMedia(media, true);
            }
        }
    }


    /////////////////////
    // getters/setters //
    /////////////////////
    private static long getLastFiredOn() {
        return lastFiredOn;
    }

    private static void setLastFiredOn(long lastFiredOn) {
        PBWifiBroadcastReceiver.lastFiredOn = lastFiredOn;
    }
}
