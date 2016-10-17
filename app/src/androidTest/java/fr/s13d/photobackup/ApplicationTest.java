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

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationTest {

    @Rule
    public ActivityTestRule<PBActivity> mActivityTestRule = new ActivityTestRule<>(PBActivity.class);

    @Test
    public void startMainActivity_shouldStartWithBasicContent() {
        // this is the PhotoBackup activity, it contains its name
        onView(withText(R.string.app_name)).check(matches(isDisplayed()));

        // Service is not running at start
        onView(withText(R.string.service_title)).check(matches(isDisplayed()));
        onView(withText(R.string.service_state_not_running)).check(matches(isDisplayed()));

        onView(withText(R.string.server_pref_title)).check(matches(isDisplayed()));
    }


    @Test
    public void clickOnAboutPreference_shouldOpenAboutActivity() {

        try {
            onView(withText(R.string.app_name)).check(matches(isDisplayed()));

            // Service is not running at start
            onView(withText(R.string.service_state_not_running)).check(matches(isDisplayed()));

            onView(withText(R.string.about_title)).check(matches(isDisplayed()));

            // open About activity
            onView(withText(R.string.about_title)).perform(click());

            // check that About activity is opened
            String aboutText = InstrumentationRegistry.getTargetContext().getString(R.string.about_text);
            onView(withId(R.id.aboutTextTextView)).check(matches(withText(aboutText)));
        } catch (NoMatchingViewException ex) {
            //openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            //onView(withText(R.string.s_ab_filter_by)).perform(click());
            Log.e(ApplicationTest.class.getName(), ex.toString());
        }
    }
}
