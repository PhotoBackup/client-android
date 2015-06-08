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
import android.os.Bundle;

public class PBActivity extends Activity {

    private static final PBSettingsFragment settingsFragment = new PBSettingsFragment();


    //////////////
    // Override //
    //////////////
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
	}


    /////////////
    // getters //
    /////////////
    public static PBMediaStore getMediaStore() {
        try {
            return settingsFragment.getService().getMediaStore();
        }
        catch (Exception e) {
            return null;
        }
    }
}
