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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextgis.metrocell.util.Constants;
import com.nextgis.metrocell.util.ConstantsSecured;
import com.nextgis.metrocell.util.Mail;

public class Reporter extends AsyncTask<String, Void, Boolean> {
    private Context mContext;
    private String mData;

    public Reporter(Context context) {
        mContext = context;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        mData = params[0];

        if (TextUtils.isEmpty(mData))
            return true;

        Mail mail = new Mail(ConstantsSecured.EMAIL_FROM, ConstantsSecured.EMAIL_PASS);
//        mail.setTo(new String[]{ConstantsSecured.EMAIL_TO});
        mail.setTo(ConstantsSecured.EMAIL_TO);
        mail.setFrom(ConstantsSecured.EMAIL_FROM);
        mail.setSubject(String.format("Not found"));
        mail.setBody(mData);

        try {
            return mail.send();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (!result) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            String savedMails = preferences.getString(Constants.PREF_APP_SAVED_MAILS, "");
            savedMails += mData + ";";
            preferences.edit().putString(Constants.PREF_APP_SAVED_MAILS, savedMails).commit();
        }
    }
}
