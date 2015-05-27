/*
 * Project:  Metrocell
 * Purpose:  Locating in metro by cell towers
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.metrocell.util;

public interface Constants {
    String TAG = "metrocell";
    String CSV_SEPARATOR = ";";
    int UNDEFINED = -1;

    String PREF_APP_VERSION = "app_version";
    String PREF_APP_FIRST_RUN = "is_first_run";
    String PREF_APP_SAVED_MAILS = "saved_mails";

    String PREF_APP_USE_INVALID_LAC_CID = "use_invalid_lac_cid";
    String PREF_APP_SAVE_LOGCAT = "save_logcat";
}
