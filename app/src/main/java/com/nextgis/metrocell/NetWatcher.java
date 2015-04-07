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

package com.nextgis.metrocell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;

import com.nextgis.metrocell.util.Constants;

public class NetWatcher extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent i) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();

            if (info != null && info.isConnected()) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String mails = preferences.getString(Constants.PREF_APP_SAVED_MAILS, "");
                preferences.edit().remove(Constants.PREF_APP_SAVED_MAILS).commit();

                for (String data : mails.split(";")) {
                    Reporter reporter = new Reporter(context);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        reporter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
                    else
                        reporter.execute(data);
                }
            }
        } catch (Exception ignored) {
        }
    }
}