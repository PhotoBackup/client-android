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
package fr.s13d.photobackup.about;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;
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


    public void clickOnOpenSourceLicenses(final View view) throws Exception {
        final Notices notices = new Notices();
        notices.addNotice(new Notice(
                "Android v7 Support Libraries",
                "https://developer.android.com/topic/libraries/support-library/features.html#v7",
                "Copyright (C) 2012 The Android Open Source Project",
                new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice(
                "LicensesDialog",
                "http://psdev.de",
                "Copyright 2013 Philip Schiffer <admin@psdev.de>",
                new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice(
                "OkHttp",
                "http://square.github.io/okhttp/",
                "Copyright 2016 Square, Inc.",
                new ApacheSoftwareLicense20()));

        new LicensesDialog.Builder(this)
                .setNotices(notices)
                .build()
                .show();
    }
}
