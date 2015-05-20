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
/*   file  :  FBBirthdayImportService.java                                          */
/* Labels  :                                                                        */
/*=======================================================================================================*/
/* Modifications   (month/day/year)                                                 */
/*=======================================================================================================*/
/* date      | author                    |FeatureID    |modification                */
/*==============|===================================|================|====================================*/
/* 09/06/12  | Neo Skunkworks-Kevin cHEN | CR-267788   |                           */
/*=======================================================================================================*/
/* Problems Report(PR/CR)                                                           */
/*=======================================================================================================*/
/* date    | author       | PR #                       |                            */
/*============|==================|===================================|===================================*/
/*            |                  |                                   |                                   */
/*=======================================================================================================*/

package com.jrdcom.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendarcommon2.EventRecurrence;
import com.facebook.android.Facebook;

public class FBBirthdayImportService extends Service {
    public static final String TAG = "FBBirthdayImportService";

    // No used, could use to specify request permission
    // private String[] permissions = new String[] {
    // "xxx", "xxx"
    // };

    private static final String PREF_KEY_REMAIN_RETRY = "preference_remain_retry";

    private ImportProgressListener mProgressListener;

    private NotificationManager mNotificationManager;

    private int mProgressMax = -1;

    private int mCurrentProgress = -1;

    private String mCurrentFriend = "";

    private ArrayList<FBBirthday> mFBBirthdayList;

    private Facebook mFacebook;

    private BirthdayImportTask mBirthdayImportTask;

    private Account mAccount;

    private long mCalendarId = -1;

    private static boolean mInImporting = false;

    private Object mLock = new Object();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "receive action : " + action);
            if (action.equals(Constants.FACEBOOK_SESSION_ACTION)) {
                if (mFacebook == null) {
                    mFacebook = new Facebook(SystemProperties.get("ro.config.fb.appid",
                            Constants.APP_ID));
                }
                mFacebook.setAccessToken(intent.getStringExtra(Constants.FACEBOOK_SESSION_TOKEN));
                mFacebook.setAccessExpires(intent.getLongExtra(Constants.FACEBOOK_SESSION_EXPIRES,
                        0));
            }

            synchronized (mLock) {
                try {
                    mLock.notifyAll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mFacebook = new Facebook(SystemProperties.get("ro.config.fb.appid", Constants.APP_ID));
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.FACEBOOK_SESSION_ACTION);
        registerReceiver(mReceiver, filter);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ImportServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isImporting()) {
            return super.onStartCommand(intent, flags, startId);
        }

        // TODO which calendar to save the events
        String accountName = null;
        String accountType = null;
        if (intent != null) {
            accountName = intent.getStringExtra("account_name");
            accountType = intent.getStringExtra("account_type");
        }
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            final Cursor cursor = this.getContentResolver().query(Calendars.CONTENT_URI,
                    new String[] {
                            Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE
                    }, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    accountName = cursor.getString(0);
                    accountType = cursor.getString(1);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            Log.d(TAG, "No calendar created");
            return super.onStartCommand(intent, flags, startId);
        }
        mAccount = new Account(accountName, accountType);
        mCalendarId = getCalendarIdByAccount(mAccount);

        // mAccount = Utils.getFacebookAccount(this);
        //
        // if (mAccount == null) {
        // Log.d(TAG, "No facebook account login");
        // return super.onStartCommand(intent, flags, startId);
        // }
        // mCalendarId = getFBCalendarId(mAccount);

        if (mCalendarId <= 0) {
            Log.d(TAG, "No facebook calendar created");
            return super.onStartCommand(intent, flags, startId);
        }

        if (mBirthdayImportTask == null && Utils.IS_FACEBOOK_DEVICE) {
            mBirthdayImportTask = new BirthdayImportTask(this);
            mBirthdayImportTask.execute();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mBirthdayImportTask != null) {
            mBirthdayImportTask.interruptImportProgress();
        }
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public static void cancelImporting() {
        mInImporting = false;
    }

    public void registerProgressListener(ImportProgressListener listener) {
        mProgressListener = listener;
    }

    public void unRegisterProgressListener() {
        mProgressListener = null;
    }

    public int getProgressMax() {
        return mProgressMax;
    }

    public int getCurrentProgress() {
        return mCurrentProgress;
    }

    public String getCurrentFriend() {
        return mCurrentFriend;
    }

    public static boolean isImporting() {
        return mInImporting;
    }

    private void initialFacebookToken(boolean refreshToken) {
        synchronized (mLock) {
            Intent requestToken = new Intent();
            requestToken.setAction(Constants.FACEBOOK_SESSION_REQUEST_ACTION);
            if (refreshToken) {
                requestToken.putExtra(Constants.FACEBOOK_REQUEST_EXCEPTION, "OAuthException");
            }
            sendBroadcast(requestToken);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void showProgressNotification() {
        String msg = this.getResources().getString(R.string.importing_msg);
        Notification notification = new Notification(R.drawable.facebook_notify, msg,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(
                FBBirthdayImportReporter.ACTION_IMPORT_PROGRESS), 0);
        notification
                .setLatestEventInfo(this, getText(R.string.import_birthday), msg, contentIntent);
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(R.string.import_birthday, notification);
    }

    @SuppressWarnings("deprecation")
    private void showResultNotification(int totalImported) {
        String msg;
        Intent intent = new Intent(FBBirthdayImportReporter.ACTION_IMPORT_RESULT);
        msg = this.getResources().getString(R.string.import_result_msg, totalImported);
        intent.putExtra(FBBirthdayImportReporter.EXTRA_TOTAL_IMPORTED, totalImported);
        Notification notification = new Notification(R.drawable.facebook_notify, msg,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification
                .setLatestEventInfo(this, getText(R.string.import_birthday), msg, contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(R.string.import_birthday, notification);
    }

    @SuppressWarnings("deprecation")
    private void showFinishNotification(int totalImported) {
        String msg;
        Intent intent = new Intent(FBBirthdayImportReporter.ACTION_IMPORT_RESULT);
        msg = this.getResources().getString(R.string.import_result_msg, totalImported);
        intent.putExtra(FBBirthdayImportReporter.EXTRA_TOTAL_IMPORTED, totalImported);
        Notification notification = new Notification(R.drawable.facebook_notify, msg,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification.setLatestEventInfo(this, getText(R.string.import_birthday_finish), "",
                contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(R.string.import_birthday, notification);
    }

    @SuppressWarnings("deprecation")
    private void showErrorNotification() {
        String msg;
        Intent intent = new Intent(FBBirthdayImportReporter.ACTION_IMPORT_RESULT);
        msg = this.getResources().getString(R.string.import_error_msg);
        intent.putExtra(FBBirthdayImportReporter.EXTRA_REQUEST_ERROR, true);
        Notification notification = new Notification(R.drawable.facebook_notify, msg,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification
                .setLatestEventInfo(this, getText(R.string.import_birthday), msg, contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(R.string.import_birthday, notification);
    }

    private void clearNotification() {
        mNotificationManager.cancel(R.string.import_birthday);
    }

    void cancelImportForOutInvoke() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(R.string.import_birthday);
        }
    }

    private long getCalendarIdByAccount(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append(Calendars.ACCOUNT_NAME + "=");
        DatabaseUtils.appendEscapedSQLString(sb, mAccount.name);
        sb.append(" AND " + Calendars.ACCOUNT_TYPE + "=");
        DatabaseUtils.appendEscapedSQLString(sb, mAccount.type);
        final Cursor cursor = this.getContentResolver().query(Calendars.CONTENT_URI, new String[] {
            Calendars._ID,
        }, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * get facebook calendar id
     * 
     * @param account
     * @return
     */
    @SuppressWarnings("unused")
    private long getFBCalendarId(Account account, boolean allowCreate) {
        long calendarId = -1;
        calendarId = getFBCalendarIdByAccount(account);
        if (calendarId <= 0 && allowCreate) {
            calendarId = createFBCalendarByAccount(account);
        }
        return calendarId;
    }

    /**
     * get facebook calendar id
     * 
     * @param account
     * @return
     */
    private long getFBCalendarIdByAccount(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append(Calendars.ACCOUNT_NAME + "=");
        DatabaseUtils.appendEscapedSQLString(sb, mAccount.name);
        sb.append(" AND " + Calendars.OWNER_ACCOUNT + "=");
        DatabaseUtils.appendEscapedSQLString(sb, Utils.CALENDARS_ACCOUNT_TYPE_FACEBOOK);
        final Cursor cursor = this.getContentResolver().query(Calendars.CONTENT_URI, new String[] {
            Calendars._ID
        }, sb.toString(), null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * create facebook calendar
     * 
     * @param account
     * @return
     */
    @SuppressWarnings("finally")
    private long createFBCalendarByAccount(Account account) {

        long calendarId = -1;

        ContentValues values = new ContentValues();

        values.put(Calendars.ACCOUNT_NAME, account.name);
        values.put(Calendars.ACCOUNT_TYPE, Constants.CALENDAR_TYPE_LOCAL);
        values.put(Calendars.DIRTY, Constants.CALENDAR_DIRTY_FB);
        values.put(Calendars.CALENDAR_DISPLAY_NAME, account.name);
        values.put(Calendars.CALENDAR_COLOR, Constants.CALENDAR_COLOR_FB);
        values.put(Calendars.CALENDAR_ACCESS_LEVEL, Constants.CALENDAR_ACCESS_LEVEL_FB);
        values.put(Calendars.SYNC_EVENTS, Constants.CALENDAR_SYNC_EVENTS_FB);
        values.put(Calendars.OWNER_ACCOUNT, Utils.CALENDARS_ACCOUNT_TYPE_FACEBOOK);

        try {
            Uri uri = this.getContentResolver().insert(Calendars.CONTENT_URI, values);
            calendarId = (int) ContentUris.parseId(uri);
        } catch (Exception e) {
            Log.w(TAG, "create facebook calendar happen exception", e);
        } finally {
            return calendarId;
        }

    }

    private static boolean containAuthExcetion(String json) {
        try {
            if (json == null) {
                return false;
            }

            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has("error_code")) {
                Log.d(TAG, "error occur : " + json);
                return true;
            } else if (jsonObj.has("error")) {
                JSONObject errorObj = jsonObj.getJSONObject("error");
                if (errorObj.getString("type").equals("OAuthException")) {
                    Log.d(TAG, "OAuthException occur");
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int loadFBFriendsBirthday() {
        String resonse = null;
        mFBBirthdayList = new ArrayList<FBBirthday>();
        try {
            Bundle params = new Bundle();
            params.putString("method", "fql.query");
            params.putString(
                    "query",
                    "SELECT uid, name, birthday_date FROM user WHERE uid = me() OR uid IN (SELECT uid2 FROM friend WHERE uid1 = me())");
            resonse = mFacebook.request(null, params, "GET");
            if (containAuthExcetion(resonse)) {
                return Constants.REQUEST_AUTH_FAIL;
            }
            final JSONArray dataArray = new JSONArray(resonse);
            int l = (dataArray != null ? dataArray.length() : 0);
            for (int i = 0; i < l; i++) {
                JSONObject jsonObj = dataArray.getJSONObject(i);
                FBBirthday birthday = new FBBirthday();
                if (jsonObj.has("uid") && jsonObj.has("birthday_date") && jsonObj.has("name")) {
                    birthday.userId = jsonObj.getString("uid");
                    birthday.date = jsonObj.getString("birthday_date");
                    birthday.userName = jsonObj.getString("name");
                } else {
                    continue;
                }
                mFBBirthdayList.add(birthday);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "error occur while load facebook friends birthday list, response : "
                    + resonse);
            mFBBirthdayList = null;
            return Constants.REQUEST_FAIL;
        }
        return Constants.REQUEST_SUCCESS;
    }

    private Time getTimeFromBirthdayString(String birthday) {
        if (null == birthday) {
            return null;
        }

        String[] tokens = birthday.split("/");
        if (tokens.length < 2) {
            return null;
        }

        Time time = new Time();

        time.month = Integer.valueOf(tokens[0]) - 1;
        time.monthDay = Integer.valueOf(tokens[1]);

        // PR-312668-Neo skunkworks-david.zhang-001 begin
        if (time.month == 1 && time.monthDay == 29) {
            time.year = Constants.BIRTHDAY_DEFAULT_YEAR;
        }
        // PR-312668-Neo skunkworks-david.zhang-001 end

        /*
         * if (tokens.length >= 3 && Integer.valueOf(tokens[2]) > 1900) {
         * time.year = Integer.valueOf(tokens[2]); }
         */
        return time;
    }

    private boolean insertBirthReminder(Context context, FBBirthday birthday) {
        if (birthday == null || context == null) {
            return false;
        }
        // delete old
        context.getContentResolver().delete(Events.CONTENT_URI, Events.FBFRIEND_ID + "=?",
                new String[] {
                    birthday.userId,
                });
        Time startTime = null;
        // Time endTime = null;
        try {
            startTime = getTimeFromBirthdayString(birthday.date);
        } catch (Exception ex) {
            return false;
        }
        if (startTime == null) {
            return false;
        }

        // PR-312443-Neo skunkworks-david.zhang-001 begin
        // String timezone = Time.getCurrentTimezone();
        String timezone = Time.TIMEZONE_UTC;
        // PR-312443-Neo skunkworks-david.zhang-001 end
        startTime.hour = 0;
        startTime.minute = 0;
        startTime.second = 0;
        startTime.timezone = timezone;
        // endTime = new Time(startTime);
        // endTime.monthDay++;

        long startMillis = startTime.normalize(true);
        // long endMillis = endTime.normalize(true);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentValues values = new ContentValues();

        values.clear();

        String eventDescription = context.getString(R.string.birthday_reminder_description,
                birthday.userName);

        values.put(Events.CALENDAR_ID, mCalendarId);
        values.put(Events.EVENT_TIMEZONE, timezone);
        values.put(Events.TITLE, eventDescription);
        values.put(Events.ALL_DAY, 1);
        values.put(Events.STATUS, 0);
        values.put(Events.DTSTART, startMillis);
        // values.put(Events.DTEND, endMillis);
        values.put(Events.DESCRIPTION, eventDescription);

        values.put(Events.HAS_ALARM, 1);
        values.put(Events.HAS_ATTENDEE_DATA, 1);
        values.put(Events.FBFRIEND_ID, birthday.userId);
        values.put(Events.FBFRIEND_NAME, birthday.userName);

        EventRecurrence mEventRecurrence = new EventRecurrence();
        int mFirstDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        mEventRecurrence.wkst = EventRecurrence.calendarDay2Day(mFirstDayOfWeek);
        mEventRecurrence.freq = EventRecurrence.YEARLY;
        String mRrule = mEventRecurrence.toString();
        /*
         * long days = (endMillis - startMillis + DateUtils.DAY_IN_MILLIS - 1) /
         * DateUtils.DAY_IN_MILLIS;
         */
        String duration = "P" + 1 + "D";
        values.put(Events.RRULE, mRrule);
        values.put(Events.DURATION, duration);

        int eventIdIndex = ops.size();
        Builder b = ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values);
        ops.add(b.build());

        b = ContentProviderOperation.newDelete(Reminders.CONTENT_URI);
        b.withSelection(Reminders.EVENT_ID + "=?", new String[1]);
        b.withSelectionBackReference(0, eventIdIndex);
        ops.add(b.build());

        values.clear();
        values.put(Reminders.MINUTES, Constants.ADVANCE_IN_MINUTES);
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        b = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(values);
        b.withValueBackReference(Reminders.EVENT_ID, eventIdIndex);
        ops.add(b.build());

        values.clear();
        values.put(Attendees.ATTENDEE_EMAIL, mAccount.name);
        values.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ORGANIZER);
        values.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_NONE);
        values.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_ACCEPTED);

        b = ContentProviderOperation.newInsert(Attendees.CONTENT_URI).withValues(values);
        b.withValueBackReference(Reminders.EVENT_ID, eventIdIndex);
        ops.add(b.build());

        try {
            getContentResolver().applyBatch(android.provider.CalendarContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            Log.w(TAG, "Ignoring unexpected remote exception", e);
        } catch (OperationApplicationException e) {
            Log.w(TAG, "Ignoring unexpected exception", e);
        }
        return true;
    }

    private void removeDeletedFirendsBirthday(Context context, ArrayList<String> importedFriendsId) {
        String selection = Events.FBFRIEND_ID + " IS NOT NULL AND " + Events.DELETED + "=0";
        Cursor cursor = context.getContentResolver().query(Events.CONTENT_URI, new String[] {
            Events.FBFRIEND_ID,
        }, selection, null, null);
        List<String> fbBirthdayList = new ArrayList<String>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    fbBirthdayList.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        fbBirthdayList.removeAll(importedFriendsId);
        for (String id : fbBirthdayList) {
            context.getContentResolver().delete(Events.CONTENT_URI, Events.FBFRIEND_ID + "=?",
                    new String[] {
                        id
                    });
        }
    }

    private static long getRetryPeriods(int retryTimes) {
        switch (retryTimes) {
            case 0:
                return 1;
            case 1:
                return 5;
            case 2:
                return 10;
            default:
                return 0;
        }
    }

    public static void checkMissedRetry(Context context) {
        String pref = Utils.getSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(-1));
        int remainRetry = Integer.valueOf(pref);
        if (remainRetry == 0) {
            Utils.setSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(-1));
            return;
        } else if (remainRetry == -1) {
            return;
        }

        startRetry(context);
    }

    private static void startRetry(Context context) {
        String pref = Utils.getSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(-1));
        int remainRetry = Integer.valueOf(pref);
        if (remainRetry == -1) {
            Utils.setSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(3));
            startRetry(context);
            return;
        } else if (remainRetry == 0) {
            Utils.setSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(-1));
            return;
        } else {
            Utils.setSharedPreference(context, PREF_KEY_REMAIN_RETRY,
                    String.valueOf(remainRetry - 1));
        }

        long retryPeriod = getRetryPeriods(3 - remainRetry) * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        AlarmManager alarmManager = null;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FBAuthorizeActivity.class);
        intent.putExtra(FBAuthorizeActivity.EXTRA_RETRY_SESSION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = null;
        pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarmManager.cancel(pending);
        }
        pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime + retryPeriod, pending);
    }

    public static void cancelRetry(Context context) {
        Utils.setSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(-1));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, FBAuthorizeActivity.class);
        intent.putExtra(FBAuthorizeActivity.EXTRA_RETRY_SESSION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarmManager.cancel(pending);
        }
    }

    public static boolean isInRetrySession(Context context) {
        String pref = Utils.getSharedPreference(context, PREF_KEY_REMAIN_RETRY, String.valueOf(-1));
        int remainRetry = Integer.valueOf(pref);
        return remainRetry != -1;
    }

    class BirthdayImportTask extends AsyncTask<Void, Integer, Integer> {
        private static final int RESULT_ERROR = -1;

        private static final int RESULT_CANCEL = -2;

        private Context mContext;

        public BirthdayImportTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            if (!isInRetrySession(mContext)) {
                Toast.makeText(mContext, R.string.toast_start_import, Toast.LENGTH_LONG).show();
            }
            showProgressNotification();
            FBBirthdayImportReceiver.updateRegularImport(mContext);
            mInImporting = true;
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int imported = 0;
            boolean authFail = false;
            int authRetry = 1;
            do {
                initialFacebookToken(authFail);
                authFail = false;

                if (!mInImporting) {
                    return RESULT_CANCEL;
                }

                if (mFacebook == null || !mFacebook.isSessionValid()) {
                    Log.d(TAG, "Token is expired");
                    return RESULT_ERROR;
                }
                Log.d(TAG, "Facebook access token init end");

                int errCode;

                errCode = loadFBFriendsBirthday();

                if (!mInImporting) {
                    return RESULT_CANCEL;
                }

                if (errCode == Constants.REQUEST_AUTH_FAIL) {
                    authFail = true;
                    continue;
                } else if (errCode == Constants.REQUEST_FAIL) {
                    return RESULT_ERROR;
                }

                if (mFBBirthdayList == null) {
                    return RESULT_ERROR;
                }

                ArrayList<String> importedFriendsId = new ArrayList<String>();
                int l = mFBBirthdayList.size();
                for (int i = 0; i < l; i++) {
                    if (!mInImporting) {
                        return RESULT_CANCEL;
                    }

                    publishProgress(i, l);

                    FBBirthday birthday = mFBBirthdayList.get(i);
                    if (!TextUtils.isEmpty(birthday.date)) {
                        if (insertBirthReminder(mContext, birthday)) {
                            importedFriendsId.add(birthday.userId);
                            imported++;
                        }
                    } else {
                        Log.d(TAG, "birthday of " + birthday.userName + " : " + birthday.userId
                                + " is null");
                    }
                }
                // maybe some friends is removed, or birthday is removed.
                removeDeletedFirendsBirthday(mContext, importedFriendsId);
            } while (authFail && authRetry-- > 0);

            if (authFail) {
                return RESULT_ERROR;
            } else {
                return imported;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mCurrentProgress = values[0] + 1;
            mProgressMax = values[1];
            mCurrentFriend = mFBBirthdayList.get(values[0]).userName;

            if (mProgressListener != null) {
                mProgressListener.onProgressUpdate(mCurrentFriend, mCurrentProgress, mProgressMax);
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == RESULT_ERROR) {
                if (mProgressListener != null) {
                    mProgressListener.onErrorEnd(-1);
                }

                if (!isInRetrySession(mContext)) {
                    showErrorNotification();
                    Toast.makeText(mContext, R.string.toast_import_fail, Toast.LENGTH_LONG).show();
                } else {
                    clearNotification();
                }

                startRetry(mContext);
            } else if (result == RESULT_CANCEL) {
                if (mProgressListener != null) {
                    mProgressListener.onCancelEnd();
                }
                clearNotification();
                if (isInRetrySession(mContext)) {
                    cancelRetry(mContext);
                }
            } else {
                if (mProgressListener != null) {
                    mProgressListener.onSuccessEnd(result);
                }

                // showResultNotification(result);
                showFinishNotification(result);
                Toast.makeText(mContext,
                        mContext.getResources().getString(R.string.import_result_msg, result),
                        Toast.LENGTH_LONG).show();

                if (isInRetrySession(mContext)) {
                    cancelRetry(mContext);
                }
            }
            mBirthdayImportTask = null;
            mInImporting = false;
            stopSelf();
            super.onPostExecute(result);
        }

        public void interruptImportProgress() {
            mInImporting = false;
            synchronized (mLock) {
                try {
                    mLock.notifyAll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class FBBirthday {
        public String userId;

        public String userName;

        public String date;
    }

    public class ImportServiceBinder extends Binder {
        public FBBirthdayImportService getService() {
            return FBBirthdayImportService.this;
        }
    }

    public interface ImportProgressListener {
        public void onProgressUpdate(String curItem, int curStep, int progressMax);

        public void onErrorEnd(int errCode);

        public void onSuccessEnd(int totalImported);

        public void onCancelEnd();
    }
}
