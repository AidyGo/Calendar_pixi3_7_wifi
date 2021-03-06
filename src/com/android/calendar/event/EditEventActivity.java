/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.calendar.event;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

import android.app.ActionBar;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.text.format.Time;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.os.SystemProperties;

import com.android.calendar.AbstractCalendarActivity;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarEventModel.ReminderEntry;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.mediatek.calendar.LogUtil;

import java.util.ArrayList;

public class EditEventActivity extends AbstractCalendarActivity {
    private static final String TAG = "EditEventActivity";

    private static final boolean DEBUG = false;

    private static final String BUNDLE_KEY_EVENT_ID = "key_event_id";

    public static final String EXTRA_EVENT_COLOR = "event_color";

    public static final String EXTRA_EVENT_REMINDERS = "reminders";

    private static boolean mIsMultipane;

    private EditEventFragment mEditFragment;

    private ArrayList<ReminderEntry> mReminders;

    private int mEventColor;

    private boolean mEventColorInitialized;

    private EventInfo mEventInfo;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.simple_frame_layout);
        IntentFilter intentFilterHidekeybroad = new IntentFilter("HIDE_KEYBROAD");
        registerReceiver(mHidekeybroadReceiver, intentFilterHidekeybroad);      //huanglin 20140104 for PR576713
        mEventInfo = getEventInfoFromIntent(icicle);
        mReminders = getReminderEntriesFromIntent();
        mEventColorInitialized = getIntent().hasExtra(EXTRA_EVENT_COLOR);
        mEventColor = getIntent().getIntExtra(EXTRA_EVENT_COLOR, -1);


        mEditFragment = (EditEventFragment) getFragmentManager().findFragmentById(R.id.main_frame);

        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);

        if (mIsMultipane) {
            getActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);
            getActionBar().setTitle(
                    mEventInfo.id == -1 ? R.string.event_create : R.string.event_edit);
        }
        else {
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME|
                    ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        if (mEditFragment == null) {
            Intent intent = null;
            if (mEventInfo.id == -1) {
                intent = getIntent();
            }

            mEditFragment = new EditEventFragment(mEventInfo, mReminders, mEventColorInitialized,
                    mEventColor, false, intent);

            mEditFragment.mShowModifyDialogOnLaunch = getIntent().getBooleanExtra(
                    CalendarController.EVENT_EDIT_ON_LAUNCH, false);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.main_frame, mEditFragment);
            ft.show(mEditFragment);
            ft.commit();
        }

        ///M: keep DatePickerDialog and TimePickerDialog when rotate device @{
        if (icicle != null) {
            mDateTimeIdentifier = icicle.getInt(DATE_TIME_IDENTIFIER);
            LogUtil.d(DATE_TIME_TAG, "onCreate(), mDateTimeIdentifier: " + mDateTimeIdentifier);
        }
        ///@}
    }

    @SuppressWarnings("unchecked")
    private ArrayList<ReminderEntry> getReminderEntriesFromIntent() {
        Intent intent = getIntent();
        return (ArrayList<ReminderEntry>) intent.getSerializableExtra(EXTRA_EVENT_REMINDERS);
    }

    private EventInfo getEventInfoFromIntent(Bundle icicle) {
        EventInfo info = new EventInfo();
        long eventId = -1;
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            try {
                eventId = Long.parseLong(data.getLastPathSegment());
            } catch (NumberFormatException e) {
                if (DEBUG) {
                    Log.d(TAG, "Create new event");
                }
            }
        } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_EVENT_ID)) {
            eventId = icicle.getLong(BUNDLE_KEY_EVENT_ID);
        }

        boolean allDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);

        long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
        if (end != -1) {
            info.endTime = new Time();
            if (allDay) {
                info.endTime.timezone = Time.TIMEZONE_UTC;
            }
            info.endTime.set(end);
        }
        if (begin != -1) {
            info.startTime = new Time();
            if (allDay) {
                info.startTime.timezone = Time.TIMEZONE_UTC;
            }
            info.startTime.set(begin);
        }
        info.id = eventId;
        info.eventTitle = intent.getStringExtra(Events.TITLE);
        info.calendarId = intent.getLongExtra(Events.CALENDAR_ID, -1);

        if (allDay) {
            info.extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
        } else {
            info.extraLong = 0;
        }
        return info;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Utils.returnToCalendarHome(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mHidekeybroadReceiver);   //huanglin 20140104 for PR576713
        ///M: To remove its CalendarController instance if exists @{
        CalendarController.removeInstance(this);
        ///@}
    }

    ///M: keep DatePickerDialog and TimePickerDialog when rotate device @{
    private static final String DATE_TIME_TAG = TAG + "::date_time_debug_tag";

    // id indicate which dialog should be shown among notations
    private int mDateTimeIdentifier = EditEventView.ID_INVALID;
    static final String DATE_TIME_IDENTIFIER = "date_time_identifier";

    void setDateTimeViewId(int id) {
        LogUtil.d(DATE_TIME_TAG, "setDateTimeViewId(), id: " + id);

        mDateTimeIdentifier = id;
    }

    public OnDateSetListener getDateTimeOnDateSetListener() {
        LogUtil.d(DATE_TIME_TAG, "getDateTimeOnDateSetListener()");

        View v = mEditFragment.getEditEventView().getDateTimeView(
                mDateTimeIdentifier);

        if (v != null) {
            return mEditFragment.getEditEventView().getOnDateSetListener(v);
        }

        return null;
    }

    public OnTimeSetListener getDateTimeOnTimeSetListener() {
        LogUtil.d(DATE_TIME_TAG, "getDateTimeOnTimeSetListener()");

        View v = mEditFragment.getEditEventView().getDateTimeView(
                mDateTimeIdentifier);

        if (v != null) {
            return mEditFragment.getEditEventView()
                    .getDateTimeOnTimeSetListener(v);
        }

        return null;
    }

    public boolean isAnyDialogShown() {
        LogUtil.d(DATE_TIME_TAG, "isAnyDialogShown()");

        return mEditFragment.getEditEventView().isAnyDialogShown();
    }

    public void setDialogShown() {
        LogUtil.d(DATE_TIME_TAG, "setDialogShown()");

        mEditFragment.getEditEventView().setDialogShown();
    }

    public OnDismissListener getDateTimeOnDismissListener() {
        LogUtil.d(DATE_TIME_TAG, "getDateTimeOnDismissListener()");

        return mEditFragment.getEditEventView().getDateTimeOnDismissListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(DATE_TIME_IDENTIFIER, mDateTimeIdentifier);
        LogUtil.d(DATE_TIME_TAG, "onSaveInstanceState(), mDateTimeIdentifier: "
                + mDateTimeIdentifier);
    }

    private BroadcastReceiver mHidekeybroadReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if ("HIDE_KEYBROAD".equals(intent.getAction())) {
               InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
               boolean isOpen=imm.isActive();
               if (isOpen)
               {
                   imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
               }
			}
		}
	};
    ///@}
}
