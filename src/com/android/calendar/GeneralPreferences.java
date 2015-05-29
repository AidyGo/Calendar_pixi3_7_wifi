/* **********************************************************************************************/
/*                                                                              Date : 12/2013 */
/*                            PRESENTATION                                                     */
/*              Copyright (c) 2013 JRD Communications, Inc.                                    */
/* **********************************************************************************************/
/*                                                                                             */
/*    This material is company confidential, cannot be reproduced in any                       */
/*    form without the written permission of JRD Communications, Inc.                          */
/*                                                                                             */
/*=============================================================================================*/
/*   Author :  Wu Shenglong                                                                    */
/*   Role :    Calendar                                                                        */
/*   Reference documents :                                                                     */
/*=============================================================================================*/
/* Comments :                                                                                  */
/*     file    :                                                                               */
/*     Labels  :                                                                               */
/*=============================================================================================*/
/* Modifications   (day/month/year)                                                            */
/*=============================================================================================*/
/* date    | author       |FeatureID                |modification                              */
/*=========|==============|=========================|==========================================*/
/*         |              |                         |                                          */
/*=============================================================================================*/
/* Problems Report(PR/CR)                                                                      */
/*=============================================================================================*/
/* date    | author    | PR #                  |                                               */
/*===========|===========|=======================|===============================================*/
/*12/24/13 | Shenglog Wu |RR-556516-Shenglong-Wu-001 | Add SDMID for first day of week         */
/*=============================================================================================*/
/*12/24/13   |Yuansheng.Zhao   |FR556557-Yuansheng-Zhao-001 |The week number is displayed according to this setting.*/
/*================================================================================================*/
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarCache;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.Toast;
import com.jrdcom.calendar.FBBirthdayImportReceiver;//added by Fujuan.Lin for RR565704
import android.os.SystemProperties;
import com.android.calendar.alerts.AlertReceiver;
import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;
import com.android.timezonepicker.TimeZonePickerDialog.OnTimeZoneSetListener;
import com.android.timezonepicker.TimeZonePickerUtils;
import com.mediatek.calendar.MTKToast;

public class GeneralPreferences extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener, OnTimeZoneSetListener {
    // The name of the shared preferences file. This name must be maintained for historical
    // reasons, as it's what PreferenceManager assigned the first time the file was created.
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    static final String SHARED_PREFS_NAME_NO_BACKUP = "com.android.calendar_preferences_no_backup";

    private static final String FRAG_TAG_TIME_ZONE_PICKER = "TimeZonePicker";

    // Preference keys
    public static final String KEY_HIDE_DECLINED = "preferences_hide_declined";
    public static final String KEY_WEEK_START_DAY = "preferences_week_start_day";
    public static final String KEY_SHOW_WEEK_NUM = "preferences_show_week_num";
    public static final String KEY_DAYS_PER_WEEK = "preferences_days_per_week";
    public static final String KEY_SKIP_SETUP = "preferences_skip_setup";

    public static final String KEY_CLEAR_SEARCH_HISTORY = "preferences_clear_search_history";

    public static final String KEY_ALERTS_CATEGORY = "preferences_alerts_category";
    public static final String KEY_ALERTS = "preferences_alerts";
    public static final String KEY_ALERTS_VIBRATE = "preferences_alerts_vibrate";
    // /M:Add a new key to save vibrate setting for calendar event reminder.
    public static final String KEY_VIBRATE_FOR_EVENT_REMINDER = "preferences_vibrate_forEventReminder";
    public static final String KEY_ALERTS_RINGTONE = "preferences_alerts_ringtone";
    public static final String KEY_ALERTS_POPUP = "preferences_alerts_popup";

    public static final String KEY_SHOW_CONTROLS = "preferences_show_controls";

    public static final String KEY_DEFAULT_REMINDER = "preferences_default_reminder";
    public static final int NO_REMINDER = -1;
    public static final String NO_REMINDER_STRING = "-1";
    public static final int REMINDER_DEFAULT_TIME = 10; // in minutes

    public static final String KEY_DEFAULT_CELL_HEIGHT = "preferences_default_cell_height";
    public static final String KEY_VERSION = "preferences_version";

    /** Key to SharePreference for default view (CalendarController.ViewType) */
    public static final String KEY_START_VIEW = "preferred_startView";
    /**
     * Key to SharePreference for default detail view (CalendarController.ViewType) Typically used
     * by widget
     */
    public static final String KEY_DETAILED_VIEW = "preferred_detailedView";
    public static final String KEY_DEFAULT_CALENDAR = "preference_defaultCalendar";

    // These must be in sync with the array preferences_week_start_day_values
    public static final String WEEK_START_DEFAULT = "-1";
    public static final String WEEK_START_SATURDAY = "7";
    public static final String WEEK_START_SUNDAY = "1";
    public static final String WEEK_START_MONDAY = "2";

    // These keys are kept to enable migrating users from previous versions
    private static final String KEY_ALERTS_TYPE = "preferences_alerts_type";
    private static final String ALERT_TYPE_ALERTS = "0";
    private static final String ALERT_TYPE_STATUS_BAR = "1";
    private static final String ALERT_TYPE_OFF = "2";
    static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
    static final String KEY_HOME_TZ = "preferences_home_tz";

    // Default preference values
    public static final int DEFAULT_START_VIEW = CalendarController.ViewType.MONTH;// FR593012-Wentao-Wan-001
    public static final int DEFAULT_DETAILED_VIEW = CalendarController.ViewType.MONTH;// modified
                                                                                      // for
                                                                                      // FR602091
                                                                                      // Porting
                                                                                      // Wave3
                                                                                      // Calendar to
                                                                                      // soul45 by
                                                                                      // yubin.yi.hz
                                                                                      // at
                                                                                      // 2014-02-13
    public static final boolean DEFAULT_SHOW_WEEK_NUM = false;
    public static final String CHANGE_WEEK_NUMBER = "com.android.broadcast.CHANGE_WEEK_NUMBER";// FR593012-Wentao-Wan-001
    // This should match the XML file.
    public static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";
    // added by Fujuan.Lin for RR565704 begin
    // facebook sync preference items
    public static final String KEY_SYNC_BIRTHDAY = "preferences_sync_birthday";
    public static final String KEY_SYNC_TITLE = "preferences_sync_title_key";
    PreferenceCategory mPreferencesSyncTitleKey;
    // added by Fujuan.Lin for RR565704 end

    CheckBoxPreference mAlert;
    CheckBoxPreference mVibrate;
    RingtonePreference mRingtone;
    CheckBoxPreference mPopup;
    CheckBoxPreference mUseHomeTZ;
    CheckBoxPreference mHideDeclined;
    Preference mHomeTZ;
    TimeZonePickerUtils mTzPickerUtils;
    ListPreference mWeekStart;
    ListPreference mDefaultReminder;

    private String mTimeZoneId;

    /** Return a properly configured SharedPreferences instance */
    public static SharedPreferences getSharedPreferences(Context context) {
        // FR593012-Wentao-Wan-001 begin
        // return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_WORLD_READABLE);
        // FR593012-Wentao-Wan-001 end
    }

    /** Set the default shared preferences in the proper context */
    public static void setDefaultValues(Context context) {
        // FR593012-Wentao-Wan-001 begin
        /*
         * PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
         * R.xml.general_preferences, false);
         */
        PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_WORLD_READABLE,
                R.xml.general_preferences, false);
        // FR593012-Wentao-Wan-001 end
    }

    // FR556557-Yuansheng-Zhao-001 begin
    static final String KEY_FIRST_WEEK_OPTION = "preferences_first_week_option";
    static final String FIRST_WEEK_FIRST_DAY = "0";
    static final String FIRST_WEEK_FIRST_FOUR_DAYS = "1";
    static final String FIRST_WEEK_FIRST_FULL_WEEK = "2";
    ListPreference mFirstWeekOption;
    CheckBoxPreference mWeekNumberEnable;

    int defFirstWeekOption = 0;
    /* If week setting is changed, the widget need to update */
    private boolean isWeekSettingChanged = false; // PR507896-tingzhu.guo-001 add

    public static String defFirstWeekOptionValue[] = {
            FIRST_WEEK_FIRST_DAY, FIRST_WEEK_FIRST_FOUR_DAYS,
            FIRST_WEEK_FIRST_FULL_WEEK
    };

    static String getDefaultFirstWeek(Context context)
    {
        int defFirstWeek = context.getResources().getInteger(
                R.integer.def_calendar_first_week_of_year);
        if (defFirstWeek < 0 || defFirstWeek > 2)
            return defFirstWeekOptionValue[0];

        return defFirstWeekOptionValue[defFirstWeek];
    }

    // FR556557-Yuansheng-Zhao-001 end

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Activity activity = getActivity();

        // Make sure to always use the same preferences file regardless of the package name
        // we're running under
        final PreferenceManager preferenceManager = getPreferenceManager();
        final SharedPreferences sharedPreferences = getSharedPreferences(activity);
        preferenceManager.setSharedPreferencesName(SHARED_PREFS_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_preferences);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mAlert = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS);
        mVibrate = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS_VIBRATE);
        Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            PreferenceCategory mAlertGroup = (PreferenceCategory) preferenceScreen
                    .findPreference(KEY_ALERTS_CATEGORY);
            mAlertGroup.removePreference(mVibrate);
        }

        mRingtone = (RingtonePreference) preferenceScreen.findPreference(KEY_ALERTS_RINGTONE);
        // mRingtone.setmShowMoreRingtong(true);
        String ringToneUri = Utils.getRingTonePreference(activity);
        // / M: Set default ringtone when old ringtone has been deleted from storage. @{
        if (!RingtoneManager.isRingtoneExist(activity, Uri.parse(ringToneUri))) {
            ringToneUri = DEFAULT_RINGTONE;
            Utils.setRingTonePreference(activity, DEFAULT_RINGTONE);
        }
        // / @}

        // Set the ringToneUri to the backup-able shared pref only so that
        // the Ringtone dialog will open up with the correct value.
        final Editor editor = preferenceScreen.getEditor();
        editor.putString(GeneralPreferences.KEY_ALERTS_RINGTONE, ringToneUri).apply();

        String ringtoneDisplayString = getRingtoneTitleFromUri(activity, ringToneUri);
        mRingtone.setSummary(ringtoneDisplayString == null ? "" : ringtoneDisplayString);

        mPopup = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS_POPUP);
        mUseHomeTZ = (CheckBoxPreference) preferenceScreen.findPreference(KEY_HOME_TZ_ENABLED);
        mHideDeclined = (CheckBoxPreference) preferenceScreen.findPreference(KEY_HIDE_DECLINED);
        mWeekStart = (ListPreference) preferenceScreen.findPreference(KEY_WEEK_START_DAY);
        mDefaultReminder = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_REMINDER);
        mHomeTZ = preferenceScreen.findPreference(KEY_HOME_TZ);

        // RR-556516-Shenglong-Wu-001 begin
        mWeekStart.setDefaultValue(getDefaultFirstDayOfWeek(activity));
        if (mWeekStart.getValue() == null) {
            mWeekStart.setValue(getDefaultFirstDayOfWeek(activity));
        }
        // RR-556516-Shenglong-Wu-001 end

        // added by Fujuan.Lin for RR565704 begin
        // get the facebook sync title preference category
        mPreferencesSyncTitleKey = (PreferenceCategory) preferenceScreen
                .findPreference(KEY_SYNC_TITLE);
        // if it's not a facebook device ,then remove the sync title preference category
        if (!Utils.IS_FACEBOOK_DEVICE) {
            preferenceScreen.removePreference(mPreferencesSyncTitleKey);
        }
        // added by Fujuan.Lin for RR565704 end

        mWeekStart.setSummary(mWeekStart.getEntry());
        mDefaultReminder.setSummary(mDefaultReminder.getEntry());

        // This triggers an asynchronous call to the provider to refresh the data in shared pref
        mTimeZoneId = Utils.getTimeZone(activity, null);

        // FR556557-Yuansheng-Zhao-001 begin
        mFirstWeekOption = (ListPreference) preferenceScreen.findPreference(KEY_FIRST_WEEK_OPTION);
        mWeekNumberEnable = (CheckBoxPreference) preferenceScreen.findPreference(KEY_SHOW_WEEK_NUM);
        mFirstWeekOption.setDefaultValue(getDefaultFirstWeek(activity));
        if (mFirstWeekOption.getValue() == null)
            mFirstWeekOption.setValue(getDefaultFirstWeek(activity));
        mFirstWeekOption.setSummary(mFirstWeekOption.getEntry());
        // FR556557-Yuansheng-Zhao-001 end

        SharedPreferences prefs = CalendarUtils.getSharedPreferences(activity,
                Utils.SHARED_PREFS_NAME);

        // Utils.getTimeZone will return the currentTimeZone instead of the one
        // in the shared_pref if home time zone is disabled. So if home tz is
        // off, we will explicitly read it.
        if (!prefs.getBoolean(KEY_HOME_TZ_ENABLED, false)) {
            mTimeZoneId = prefs.getString(KEY_HOME_TZ, Time.getCurrentTimezone());
        }

        mHomeTZ.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showTimezoneDialog();
                return true;
            }
        });

        if (mTzPickerUtils == null) {
            mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }
        CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(getActivity(), mTimeZoneId,
                System.currentTimeMillis(), false);
        mHomeTZ.setSummary(timezoneName != null ? timezoneName : mTimeZoneId);

        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) activity.getFragmentManager()
                .findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);
        if (tzpd != null) {
            tzpd.setOnTimeZoneSetListener(this);
        }

        migrateOldPreferences(sharedPreferences);

        updateChildPreferences();
    }

    private void showTimezoneDialog() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Bundle b = new Bundle();
        b.putLong(TimeZonePickerDialog.BUNDLE_START_TIME_MILLIS, System.currentTimeMillis());
        b.putString(TimeZonePickerDialog.BUNDLE_TIME_ZONE, Utils.getTimeZone(activity, null));

        FragmentManager fm = getActivity().getFragmentManager();
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm
                .findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);
        if (tzpd != null) {
            tzpd.dismiss();
        }
        tzpd = new TimeZonePickerDialog();
        tzpd.setArguments(b);
        tzpd.setOnTimeZoneSetListener(this);
        tzpd.show(fm, FRAG_TAG_TIME_ZONE_PICKER);
    }

    @Override
    public void onStart() {
        // FR556557-Yuansheng-Zhao-001 begin
        if (mWeekNumberEnable.isChecked()) {
            mFirstWeekOption.setEnabled(true);
        } else {
            mFirstWeekOption.setEnabled(false);
        }
        // FR556557-Yuansheng-Zhao-001 end
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        setPreferenceListeners(this);
    }

    // FR593012-Wentao-Wan-001 begin
    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        if (isWeekSettingChanged) {
            /* Widget need to update */
            Activity act = getActivity();
            Intent intent = new Intent(CHANGE_WEEK_NUMBER);
            // Modified by guangqian.peng for PR711052
            intent.putExtra("week_start_day", Utils.getFirstDayOfWeek(getActivity()));
            intent.putExtra("show_week_number", Utils.getShowWeekNumber(getActivity()));
            intent.putExtra("first_week_option", Utils.getFirstWeekOption(getActivity()));
            act.sendBroadcast(intent);
            isWeekSettingChanged = false;
        }
        super.onPause();
    }

    // FR593012-Wentao-Wan-001 end

    /**
     * Sets up all the preference change listeners to use the specified listener.
     */
    private void setPreferenceListeners(OnPreferenceChangeListener listener) {
        mUseHomeTZ.setOnPreferenceChangeListener(listener);
        mHomeTZ.setOnPreferenceChangeListener(listener);
        mWeekStart.setOnPreferenceChangeListener(listener);
        mFirstWeekOption.setOnPreferenceChangeListener(listener); // FR556557-Yuansheng-Zhao-001
        mDefaultReminder.setOnPreferenceChangeListener(listener);
        mRingtone.setOnPreferenceChangeListener(listener);
        mHideDeclined.setOnPreferenceChangeListener(listener);
        mVibrate.setOnPreferenceChangeListener(listener);
    }

    @Override
    public void onStop() {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        setPreferenceListeners(null);
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity a = getActivity();
        if (key.equals(KEY_ALERTS)) {
            updateChildPreferences();
            // /M: check whether provider is available first.
            boolean canUseProvider = Utils.canUseProviderByUri(a.getContentResolver(),
                    CalendarContract.CalendarAlerts.CONTENT_URI);
            if (canUseProvider) {
                if (a != null) {
                    Intent intent = new Intent();
                    intent.setClass(a, AlertReceiver.class);
                    if (mAlert.isChecked()) {
                        intent.setAction(AlertReceiver.ACTION_DISMISS_OLD_REMINDERS);
                    } else {
                        intent.setAction(AlertReceiver.EVENT_REMINDER_APP_ACTION);
                    }
                    a.sendBroadcast(intent);
                }
            } else {
                Toast.makeText(getActivity(), R.string.operation_failed, Toast.LENGTH_LONG).show();
            }
        }
        if (a != null) {
            BackupManager.dataChanged(a.getPackageName());
        }
        // FR556557-Yuansheng-Zhao-001 begin
        if (key.equals(KEY_SHOW_WEEK_NUM) || key.equals(KEY_FIRST_WEEK_OPTION)
                || key.equals(KEY_WEEK_START_DAY)) {
            if (mWeekNumberEnable.isChecked()) {
                mFirstWeekOption.setEnabled(true);
            } else {
                mFirstWeekOption.setEnabled(false);
            }
            isWeekSettingChanged = true; // PR507896-tingzhu.guo-001 add
        }
        // FR556557-Yuansheng-Zhao-001 end

        // added by Fujuan.Lin for RR565704 begin
        // import facebook data,and show friends' birthday info.
        if (Utils.IS_FACEBOOK_DEVICE && key.equals(KEY_SYNC_BIRTHDAY)) {
            FBBirthdayImportReceiver.startRegluarImport(getActivity());
        }
        // added by Fujuan.Lin for RR565704 end
    }

    /**
     * Handles time zone preference changes
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String tz;
        final Activity activity = getActivity();
        if (preference == mUseHomeTZ) {
            if ((Boolean) newValue) {
                tz = mTimeZoneId;
            } else {
                tz = CalendarCache.TIMEZONE_TYPE_AUTO;
            }
            Utils.setTimeZone(activity, tz);
            return true;
        } else if (preference == mHideDeclined) {
            mHideDeclined.setChecked((Boolean) newValue);
            Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(activity));
            intent.setDataAndType(CalendarContract.CONTENT_URI, Utils.APPWIDGET_DATA_TYPE);
            activity.sendBroadcast(intent);
            return true;
        } else if (preference == mWeekStart) {
            mWeekStart.setValue((String) newValue);
            mWeekStart.setSummary(mWeekStart.getEntry());
            // FR556557-Yuansheng-Zhao-001 begin
        } else if (preference == mFirstWeekOption) {
            mFirstWeekOption.setValue((String) newValue);
            mFirstWeekOption.setSummary(mFirstWeekOption.getEntry());
            // FR556557-Yuansheng-Zhao-001 end
        } else if (preference == mDefaultReminder) {
            mDefaultReminder.setValue((String) newValue);
            mDefaultReminder.setSummary(mDefaultReminder.getEntry());
        } else if (preference == mRingtone) {
            if (newValue instanceof String) {
                Utils.setRingTonePreference(activity, (String) newValue);
                String ringtone = getRingtoneTitleFromUri(activity, (String) newValue);
                mRingtone.setSummary(ringtone == null ? "" : ringtone);
            }
            return true;
        } else if (preference == mVibrate) {
            mVibrate.setChecked((Boolean) newValue);
            return true;
        } else {
            return true;
        }
        return false;
    }

    public String getRingtoneTitleFromUri(Context context, String uri) {
        if (TextUtils.isEmpty(uri)) {
            return null;
        }

        Ringtone ring = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri));
        if (ring != null) {
            return ring.getTitle(context);
        }
        return null;
    }

    /**
     * If necessary, upgrades previous versions of preferences to the current set of keys and
     * values.
     * 
     * @param prefs the preferences to upgrade
     */
    private void migrateOldPreferences(SharedPreferences prefs) {
        // If needed, migrate vibration setting from a previous version

        mVibrate.setChecked(Utils.getDefaultVibrate(getActivity(), prefs));

        // If needed, migrate the old alerts type setting
        if (!prefs.contains(KEY_ALERTS) && prefs.contains(KEY_ALERTS_TYPE)) {
            String type = prefs.getString(KEY_ALERTS_TYPE, ALERT_TYPE_STATUS_BAR);
            if (type.equals(ALERT_TYPE_OFF)) {
                mAlert.setChecked(false);
                mPopup.setChecked(false);
                mPopup.setEnabled(false);
            } else if (type.equals(ALERT_TYPE_STATUS_BAR)) {
                mAlert.setChecked(true);
                mPopup.setChecked(false);
                mPopup.setEnabled(true);
            } else if (type.equals(ALERT_TYPE_ALERTS)) {
                mAlert.setChecked(true);
                mPopup.setChecked(true);
                mPopup.setEnabled(true);
            }
            // clear out the old setting
            prefs.edit().remove(KEY_ALERTS_TYPE).commit();
        }
    }

    /**
     * Keeps the dependent settings in sync with the parent preference, so for example, when
     * notifications are turned off, we disable the preferences for configuring the exact
     * notification behavior.
     */
    private void updateChildPreferences() {
        if (mAlert.isChecked()) {
            mVibrate.setEnabled(true);
            mRingtone.setEnabled(true);
            mPopup.setEnabled(true);
        } else {
            mVibrate.setEnabled(false);
            mRingtone.setEnabled(false);
            mPopup.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (KEY_CLEAR_SEARCH_HISTORY.equals(key)) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    Utils.getSearchAuthority(getActivity()),
                    CalendarRecentSuggestionsProvider.MODE);
            suggestions.clearHistory();
            // /M:here use MTKToast to avoid show very long time when click many times
            MTKToast.toast(getActivity(), R.string.search_history_cleared);
            // /@}
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        if (mTzPickerUtils == null) {
            mTzPickerUtils = new TimeZonePickerUtils(getActivity());
        }

        final CharSequence timezoneName = mTzPickerUtils.getGmtDisplayName(
                getActivity(), tzi.mTzId, System.currentTimeMillis(), false);
        mHomeTZ.setSummary(timezoneName);
        Utils.setTimeZone(getActivity(), tzi.mTzId);
    }

    // RR-556516-Shenglong-Wu-001 begin
    static String getDefaultFirstDayOfWeek(Context context) {
        // Get the value customized in CLID for first day of week.
        // PR-832904-shiyong.luo-20141110 begin
        // int defFirstDayOfWeek
        // =context.getResources().getInteger(R.integer.def_calendar_first_day_of_week);
        int defFirstDayOfWeek = 2;
        // PR-832904-shiyong.luo-20141110 end
        if (defFirstDayOfWeek == 0) {
            return GeneralPreferences.WEEK_START_DEFAULT;
        } else if (defFirstDayOfWeek == 1) {
            return GeneralPreferences.WEEK_START_SATURDAY;
        } else if (defFirstDayOfWeek == 2) {
            return GeneralPreferences.WEEK_START_SUNDAY;
        } else if (defFirstDayOfWeek == 3) {
            return GeneralPreferences.WEEK_START_MONDAY;
        }
        return GeneralPreferences.WEEK_START_DEFAULT;
    }
    // RR-556516-Shenglong-Wu-001 end
}
