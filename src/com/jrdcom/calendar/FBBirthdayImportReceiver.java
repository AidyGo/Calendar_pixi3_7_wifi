/*********************************************************************************************************/
/*                                                                   Date : 09/2012 */
/*                                   PRESENTATION                                   */
/*                     Copyright (c) 2011 JRD Communications, Inc.                  */
/*********************************************************************************************************/
/*                                                                                                       */
/*           This material is company confidential, cannot be reproduced in any     */
/*           form without the written permission of JRD Communications, Inc.        */
/*                                                                                                       */
/*=======================================================================================================*/
/*   Author :  Neo Skunkworks-Kevin CHEN                                            */
/*   Role :    Calendar                                                             */
/*   Reference documents :                                                          */
/*=======================================================================================================*/
/* Comments:                                                                        */
/*   file  :  FBBirthdayImportReceiver.java                                         */
/* Labels  :                                                                        */
/*=======================================================================================================*/
/* Modifications   (month/day/year)                                                 */
/*=======================================================================================================*/
/* date      | author                    |FeatureID    |modification                */
/*==============|===================================|================|====================================*/
/* 09/06/12  | Neo Skunkworks-Kevin CHEN | CR-267788   |                            */
/*=======================================================================================================*/
/* Problems Report(PR/CR)                                                           */
/*=======================================================================================================*/
/* date    | author       | PR #                       |                            */
/*============|==================|===================================|===================================*/
/*            |                  |                                   |                                   */
/*=======================================================================================================*/

package com.jrdcom.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.provider.CalendarContract.Events;
import android.util.Log;

import com.android.calendar.GeneralPreferences;
import com.android.calendar.Utils;

public class FBBirthdayImportReceiver extends BroadcastReceiver {
    private static final String TAG = "FBBirthdayImportReceiver";

    private static final String PREF_KEY_LAST_IMPORT = "last_import";

    private static final long WEEK_IN_MILLIS = 7 * 24 * 60 * 60 * 1000;

    private static final long MINUTE_IN_MILLIS = 60 * 1000;

    private Context mContext;

    private static boolean mBeFirstConnect = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        Log.d(TAG, "Receive :" + action);
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (!mBeFirstConnect) {
                return;
            }
            mBeFirstConnect = false;
            FBBirthdayImportService.checkMissedRetry(context);
            startRegluarImport(context);
        } else if (action.equals("jrdcom.intent.action.FACEBOOK_LOGIN")) {
            startDelayImportRequest(context);
        } else if (action.equals("jrdcom.intent.action.FACEBOOK_LOGOUT")) {
            // cancel retry current importing
            FBBirthdayImportService.cancelImporting();
            // cancel retry session
            FBBirthdayImportService.cancelRetry(context);
            // cancel regular sync session
            startRegluarImport(context);
            new Thread() {
                public void run() {
                    // delete facebook friend birthday
                    mContext.getContentResolver().delete(Events.CONTENT_URI,
                            Events.FBFRIEND_ID + " IS NOT NULL", null);
                    // delete facebook calendar
                    // mContext.getContentResolver().delete(Calendars.CONTENT_URI,
                    // Calendars.ACCOUNT_TYPE + "=?", new String[] {
                    // Utils.CALENDARS_ACCOUNT_TYPE_FACEBOOK
                    // });
                };
            }.start();
        }
    }

    private void startDelayImportRequest(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mContext, FBAuthorizeActivity.class);
        intent.putExtra(FBAuthorizeActivity.EXTRA_IGNORE_WHILE_RUN, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarmManager.cancel(pending);
        }
        pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1 * MINUTE_IN_MILLIS, pending);
    }

    public static void startRegluarImport(Context context) {
        String pref = Utils.getSharedPreference(context, PREF_KEY_LAST_IMPORT, "0");
        long lastImportTime = Long.valueOf(pref);

        long regularPeriod = lastImportTime + WEEK_IN_MILLIS;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FBAuthorizeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarmManager.cancel(pending);
        }

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context
                .getApplicationContext());
        boolean syncBirthday = prefs.getBoolean(GeneralPreferences.KEY_SYNC_BIRTHDAY, false);
        if (!syncBirthday || !Utils.isFacebookLogin(context)) {
            return;
        }
        pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, regularPeriod, pending);
    }

    public static void updateRegularImport(Context context) {
        long currentTime = System.currentTimeMillis();
        Utils.setSharedPreference(context, PREF_KEY_LAST_IMPORT, String.valueOf(currentTime));
        startRegluarImport(context);
    }
}
