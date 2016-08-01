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
package fr.s13d.photobackup.about;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import fr.s13d.photobackup.BuildConfig;
import fr.s13d.photobackup.R;
import fr.s13d.photobackup.databinding.ActivityAboutBinding;


public class PBAboutActivity extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI (with binding)
        ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);
        String versionString = BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ')';
        binding.versionTextView.setText(getString(R.string.app_version, versionString));
    }

}
