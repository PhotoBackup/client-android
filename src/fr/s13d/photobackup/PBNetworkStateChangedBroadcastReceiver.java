/**
 * Copyright (C) 2013-2016 Stéphane Péchard.
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class PBNetworkStateChangedBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "PBNetworkStateChangedBroadcastReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent != null && intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.i(LOG_TAG, "NETWORK_STATE_CHANGED_ACTION");
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                Log.i(LOG_TAG, "isConnected()");
            }
        }
    }
}
