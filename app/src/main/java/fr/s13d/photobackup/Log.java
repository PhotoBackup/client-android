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


/**
 * Own logger class to avoid leaking logs in release mode.
 */
public final class Log {

    private Log() throws InstantiationException {
        throw new InstantiationException("This class is not meant to be instantiated!");
    }


    /**
     * Log a debug message
     * @param tag tag of the logged message
     * @param message message logged
     */
    public static void d(final String tag, final String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, message);
        }
    }


    /**
     * Log a debug exception
     * @param tag tag of the logged exception
     * @param e exception logged
     */
    public static void d(final String tag, final Exception e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, e.toString());
        }
    }


    /**
     * Log an error message
     * @param tag tag of the logged message
     * @param message message logged
     */
    public static void e(final String tag, final String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, message);
        }
    }


    /**
     * Log an error exception
     * @param tag tag of the logged exception
     * @param e exception logged
     */
    public static void e(final String tag, final Exception e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, e.toString());
        }
    }


    /**
     * Log an info message
     * @param tag tag of the logged message
     * @param message message logged
     */
    public static void i(final String tag, final String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, message);
        }
    }


    /**
     * Log an info exception
     * @param tag tag of the logged exception
     * @param e exception logged
     */
    public static void i(final String tag, final Exception e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, e.toString());
        }
    }


    /**
     * Log a verbose message
     * @param tag tag of the logged message
     * @param message message logged
     */
    public static void v(final String tag, final String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(tag, message);
        }
    }


    /**
     * Log a verbose exception
     * @param tag tag of the logged exception
     * @param e exception logged
     */
    public static void v(final String tag, final Exception e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(tag, e.toString());
        }
    }


    /**
     * Log a warning message
     * @param tag tag of the logged message
     * @param message message logged
     */
    public static void w(final String tag, final String message) {
        if (BuildConfig.DEBUG) {
        	android.util.Log.w(tag, message);
        }
    }


    /**
     * Log a warning exception
     * @param tag tag of the logged exception
     * @param e exception logged
     */
    public static void w(final String tag, final Exception e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w(tag, e.toString());
        }
    }

}
