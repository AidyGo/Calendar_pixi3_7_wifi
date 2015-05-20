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

import android.accounts.Account;//added by Fujuan.Lin for RR565704
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.calendar.CalendarEventModel;
import com.android.calendar.CalendarEventModel.Attendee;
import com.android.calendar.CalendarEventModel.ReminderEntry;
import com.android.calendar.EmailAddressAdapter;
import com.android.calendar.EventInfoFragment;
import com.android.calendar.EventRecurrenceFormatter;
import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import com.android.calendar.RecipientAdapter;
import com.android.calendar.Utils;
import com.android.calendar.event.EditEventHelper.EditDoneRunnable;
import com.android.calendar.recurrencepicker.RecurrencePickerDialog;
import com.android.calendarcommon2.EventRecurrence;
import com.android.common.Rfc822InputFilter;
import com.android.common.Rfc822Validator;
import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener;
import com.android.ex.chips.AccountSpecifier;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.ChipsUtil;
import com.android.ex.chips.RecipientEditTextView;
import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerDialog;
import com.android.timezonepicker.TimeZonePickerUtils;

//import com.mediatek.calendar.EditEventTimePickerDialogFragment;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKToast;
import com.mediatek.calendar.edittext.IEditTextExt;
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.IEditEventView;
import com.mediatek.calendar.extension.IEditEventViewExt;
import com.mediatek.calendar.lunar.date.LunarDatePickerDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;
import android.os.SystemProperties;

public class EditEventView implements View.OnClickListener, DialogInterface.OnCancelListener,
        DialogInterface.OnClickListener, OnItemSelectedListener,
        RecurrencePickerDialog.OnRecurrenceSetListener,
        TimeZonePickerDialog.OnTimeZoneSetListener, IEditEventView {

    private static final String TAG = "EditEventView";
    private static final String GOOGLE_SECONDARY_CALENDAR = "calendar.google.com";
    private static final String PERIOD_SPACE = ". ";

    private static final String FRAG_TAG_DATE_PICKER = "datePickerDialogFragment";
    private static final String FRAG_TAG_TIME_PICKER = "timePickerDialogFragment";
    private static final String FRAG_TAG_TIME_ZONE_PICKER = "timeZonePickerDialogFragment";
    private static final String FRAG_TAG_RECUR_PICKER = "recurrencePickerDialogFragment";

    ArrayList<View> mEditOnlyList = new ArrayList<View>();
    ArrayList<View> mEditViewList = new ArrayList<View>();
    ArrayList<View> mViewOnlyList = new ArrayList<View>();
    TextView mLoadingMessage;
    ScrollView mScrollView;
    Button mStartDateButton;
    Button mEndDateButton;
    Button mStartTimeButton;
    Button mEndTimeButton;
    Button mTimezoneButton;
    View mColorPickerNewEvent;
    View mColorPickerExistingEvent;
    OnClickListener mChangeColorOnClickListener;
    View mTimezoneRow;
    TextView mStartTimeHome;
    TextView mStartDateHome;
    TextView mEndTimeHome;
    TextView mEndDateHome;
    CheckBox mAllDayCheckBox;
    Spinner mCalendarsSpinner;
    Button mRruleButton;
    Spinner mAvailabilitySpinner;
    Spinner mAccessLevelSpinner;
    RadioGroup mResponseRadioGroup;
    TextView mTitleTextView;
    AutoCompleteTextView mLocationTextView;
    EventLocationAdapter mLocationAdapter;
    TextView mDescriptionTextView;
    TextView mWhenView;
    TextView mTimezoneTextView;
    TextView mTimezoneLabel;
    LinearLayout mRemindersContainer;
    MultiAutoCompleteTextView mAttendeesList;
    View mCalendarSelectorGroup;
    View mCalendarSelectorWrapper;
    View mCalendarStaticGroup;
    View mLocationGroup;
    View mDescriptionGroup;
    View mRemindersGroup;
    View mResponseGroup;
    View mOrganizerGroup;
    View mAttendeesGroup;
    View mStartHomeGroup;
    View mEndHomeGroup;

    private int[] mOriginalPadding = new int[4];

    public boolean mIsMultipane;
    private ProgressDialog mLoadingCalendarsDialog;
    private AlertDialog mNoCalendarsDialog;
    private AlertDialog mAddDefaultCalendarsDialog;
    private DialogFragment mTimezoneDialog;
    private Activity mActivity;
    private EditDoneRunnable mDone;
    private View mView;
    private CalendarEventModel mModel;
    private Cursor mCalendarsCursor;
    private AccountSpecifier mAddressAdapter;
    private Rfc822Validator mEmailValidator;

    public boolean mTimeSelectedWasStartTime;
    public boolean mDateSelectedWasStartDate;
    private TimePickerDialog mStartTimePickerDialog;
    private TimePickerDialog mEndTimePickerDialog;
    /** M: change from DatePickerDialog to LunarDatePickerDialog */
    private LunarDatePickerDialog mDatePickerDialog;

    /**
     * Contents of the "minutes" spinner.  This has default values from the XML file, augmented
     * with any additional values that were already associated with the event.
     */
    private ArrayList<Integer> mReminderMinuteValues;
    private ArrayList<String> mReminderMinuteLabels;

    /**
     * Contents of the "methods" spinner.  The "values" list specifies the method constant
     * (e.g. {@link Reminders#METHOD_ALERT}) associated with the labels.  Any methods that
     * aren't allowed by the Calendar will be removed.
     */
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMethodLabels;

    /**
     * Contents of the "availability" spinner. The "values" list specifies the
     * type constant (e.g. {@link Events#AVAILABILITY_BUSY}) associated with the
     * labels. Any types that aren't allowed by the Calendar will be removed.
     */
    private ArrayList<Integer> mAvailabilityValues;
    private ArrayList<String> mAvailabilityLabels;
    private ArrayList<String> mOriginalAvailabilityLabels;
    private ArrayAdapter<String> mAvailabilityAdapter;
    private boolean mAvailabilityExplicitlySet;
    private boolean mAllDayChangingAvailability;
    private int mAvailabilityCurrentlySelected;

    private int mDefaultReminderMinutes;

    private boolean mSaveAfterQueryComplete = false;

    private TimeZonePickerUtils mTzPickerUtils;
    private Time mStartTime;
    private Time mEndTime;
    /**
     * M: the backup of the mStartTime
     */
    private Time mPreStartTime;
    private String mTimezone;
    private boolean mAllDay = false;
    private int mModification = EditEventHelper.MODIFY_UNINITIALIZED;

    private EventRecurrence mEventRecurrence = new EventRecurrence();

    private ArrayList<LinearLayout> mReminderItems = new ArrayList<LinearLayout>(0);
    private ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();
    private String mRrule;

    private static StringBuilder sStringBuilder = new StringBuilder(50);
    private static Formatter sFormatter = new Formatter(sStringBuilder, Locale.getDefault());

    private DialogManager mDialogManager = new DialogManager();

    public static final String PC_SYNC_ACCOUNT_DISPLAY_NAME = "PC Sync";
    /* This class is used to update the time buttons. */
    private class TimeListener implements OnTimeSetListener,
            android.app.TimePickerDialog.OnTimeSetListener {
        private View mView;

        public TimeListener(View view) {
            mView = view;
        }
        
        private void onTimeSetImpl(int hourOfDay, int minute) {
            // Cache the member variables locally to avoid inner class overhead.
            Time startTime = mStartTime;
            Time endTime = mEndTime;

            // Cache the start and end millis so that we limit the number
            // of calls to normalize() and toMillis(), which are fairly
            // expensive.
            long startMillis;
            long endMillis;
            if (mView == mStartTimeButton) {
                // The start time was changed.
                int hourDuration = endTime.hour - startTime.hour;
                int minuteDuration = endTime.minute - startTime.minute;

                startTime.hour = hourOfDay;
                startTime.minute = minute;
                startMillis = startTime.normalize(true);

                // Also update the end time to keep the duration constant.
                endTime.hour = hourOfDay + hourDuration;
                endTime.minute = minute + minuteDuration;

                // Update tz in case the start time switched from/to DLS
                populateTimezone(startMillis);
            } else {
                // The end time was changed.
                startMillis = startTime.toMillis(true);
                endTime.hour = hourOfDay;
                endTime.minute = minute;

                // Move to the start time if the end time is before the start
                // time.
                if (endTime.before(startTime)) {
                    endTime.monthDay = startTime.monthDay + 1;
                }
                // Call populateTimezone if we support end time zone as well
            }

            endMillis = endTime.normalize(true);

            setDate(mEndDateButton, endMillis);
            setTime(mStartTimeButton, startMillis);
            setTime(mEndTimeButton, endMillis);
            updateHomeTime();
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            onTimeSetImpl(hourOfDay, minute);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            onTimeSetImpl(hourOfDay, minute);
        }
    }

    private class TimeClickListener implements View.OnClickListener {
        private Time mTime;

        public TimeClickListener(Time time) {
            mTime = time;
        }

        @Override
        public void onClick(View v) {
            TimePickerDialog dialog;
            if (v == mStartTimeButton) {
                mTimeSelectedWasStartTime = true;
                if (mStartTimePickerDialog == null) {
                    mStartTimePickerDialog = TimePickerDialog.newInstance(new TimeListener(v),
                            mTime.hour, mTime.minute, DateFormat.is24HourFormat(mActivity));
                } else {
                    mStartTimePickerDialog.setStartTime(mTime.hour, mTime.minute);
                }
                dialog = mStartTimePickerDialog;
            } else {
                mTimeSelectedWasStartTime = false;
                if (mEndTimePickerDialog == null) {
                    mEndTimePickerDialog = TimePickerDialog.newInstance(new TimeListener(v),
                            mTime.hour, mTime.minute, DateFormat.is24HourFormat(mActivity));
                } else {
                    mEndTimePickerDialog.setStartTime(mTime.hour, mTime.minute);
                }
                dialog = mEndTimePickerDialog;

            }

            final FragmentManager fm = mActivity.getFragmentManager();
            fm.executePendingTransactions();

            if (dialog != null && !dialog.isAdded()) {
                dialog.show(fm, FRAG_TAG_TIME_PICKER);
            }
        }
    }

    private class DateListener implements OnDateSetListener,
            android.app.DatePickerDialog.OnDateSetListener {
        View mView;

        public DateListener(View view) {
            mView = view;
        }
        
        private void onDateSetImpl(int year, int month, int monthDay) {

            Log.d(TAG, "onDateSet: " + year +  " " + month +  " " + monthDay);
            // Cache the member variables locally to avoid inner class overhead.
            Time startTime = mStartTime;
            Time endTime = mEndTime;
            /*
             * M: back the old start time.
             */
            mPreStartTime = new Time(mStartTime);
            mPreStartTime.normalize(false);

            // Cache the start and end millis so that we limit the number
            // of calls to normalize() and toMillis(), which are fairly
            // expensive.
            long startMillis;
            long endMillis;
            if (mView == mStartDateButton) {
                // The start date was changed.
                int yearDuration = endTime.year - startTime.year;
                int monthDuration = endTime.month - startTime.month;
                int monthDayDuration = endTime.monthDay - startTime.monthDay;

                startTime.year = year;
                startTime.month = month;
                startTime.monthDay = monthDay;
                startMillis = startTime.normalize(true);

                // Also update the end date to keep the duration constant.
                endTime.year = year + yearDuration;
                endTime.month = month + monthDuration;
                endTime.monthDay = monthDay + monthDayDuration;
                endMillis = endTime.normalize(true);

                // Update tz in case the start time switched from/to DLS
                populateTimezone(startMillis);
            } else {
                // The end date was changed.
                startMillis = startTime.toMillis(true);
                endTime.year = year;
                endTime.month = month;
                endTime.monthDay = monthDay;
                endMillis = endTime.normalize(true);

                // Do not allow an event to have an end time before the start
                // time.
                if (endTime.before(startTime)) {
                    endTime.set(startTime);
                    endMillis = startMillis;
                }
                // Call populateTimezone if we support end time zone as well
            }

            /*
             * M: If the start or end date has changed then update the repeats.
             */
            populateRepeats();
            mPreStartTime = null;

            setDate(mStartDateButton, startMillis);
            setDate(mEndDateButton, endMillis);
            setTime(mEndTimeButton, endMillis); // In case end time had to be
            // reset
            updateHomeTime();
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int month, int monthDay) {
            onDateSetImpl(year, month, monthDay);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            onDateSetImpl(year, month, monthDay);
        }
    }

    // Fills in the date and time fields
    private void populateWhen() {
        long startMillis = mStartTime.toMillis(false /* use isDst */);
        long endMillis = mEndTime.toMillis(false /* use isDst */);
        setDate(mStartDateButton, startMillis);
        setDate(mEndDateButton, endMillis);

        setTime(mStartTimeButton, startMillis);
        setTime(mEndTimeButton, endMillis);

        mStartDateButton.setOnClickListener(new DateClickListener(mStartTime));
        mEndDateButton.setOnClickListener(new DateClickListener(mEndTime));

        mStartTimeButton.setOnClickListener(new TimeClickListener(mStartTime));
        mEndTimeButton.setOnClickListener(new TimeClickListener(mEndTime));
    }

    // Implements OnTimeZoneSetListener
    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        setTimezone(tzi.mTzId);
        updateHomeTime();
    }

    private void setTimezone(String timeZone) {
        mTimezone = timeZone;
        mStartTime.timezone = mTimezone;
        long timeMillis = mStartTime.normalize(true);
        mEndTime.timezone = mTimezone;
        mEndTime.normalize(true);

        populateTimezone(timeMillis);
    }

    private void populateTimezone(long eventStartTime) {
        if (mTzPickerUtils == null) {
            mTzPickerUtils = new TimeZonePickerUtils(mActivity);
        }
        CharSequence displayName =
                mTzPickerUtils.getGmtDisplayName(mActivity, mTimezone, eventStartTime, true);

        mTimezoneTextView.setText(displayName);
        mTimezoneButton.setText(displayName);
    }

    private void showTimezoneDialog() {
        Bundle b = new Bundle();
        b.putLong(TimeZonePickerDialog.BUNDLE_START_TIME_MILLIS, mStartTime.toMillis(false));
        b.putString(TimeZonePickerDialog.BUNDLE_TIME_ZONE, mTimezone);

        FragmentManager fm = mActivity.getFragmentManager();
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm
                .findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);
        if (tzpd != null) {
            tzpd.dismiss();
        }
        tzpd = new TimeZonePickerDialog();
        tzpd.setArguments(b);
        tzpd.setOnTimeZoneSetListener(EditEventView.this);
        tzpd.show(fm, FRAG_TAG_TIME_ZONE_PICKER);
    }

    private void populateRepeats() {
        Resources r = mActivity.getResources();
        String repeatString;
        boolean enabled;
        /**
         * M: adjust the repeat mode according the event time duration. If
         * getMinRepeatMode return 0, the recurrence rule couldn't be applied.
         * If recurrence rule doesn't match the event duration, it should adjust
         * it, else just some field of the current recurrence rule.
         */
        long duration = mEndTime.toMillis(false) - mStartTime.toMillis(false);
        int minRepeatMode = RecurrencePickerDialog.getMinRepeatMode(duration);
        if (!TextUtils.isEmpty(mRrule)) {
            if (minRepeatMode == 0) {
                repeatString = r.getString(R.string.does_not_repeat);
                enabled = false;
                mRrule = null;
            } else {
                mEventRecurrence.setStartDate(mStartTime);

                /**
                 * M: If the the current repeat rule doesn't match the event
                 * duration, just reset to the correct one else just adjust some
                 * field in the origin one.
                 * @{
                 * */
                if (mEventRecurrence.freq < minRepeatMode) {
                    String defaultRule = getDefaultRruleByFrequecy(
                            minRepeatMode, mStartTime.weekDay);
                    mEventRecurrence.parse(defaultRule);

                } else {
                    if (mPreStartTime == null) {
                        mPreStartTime = new Time(mStartTime);
                        mPreStartTime.normalize(false);
                    }
                    recalculateEventRecurrenceRule(mPreStartTime, mStartTime, mEndTime, mEventRecurrence);
                }
                /** @} */
                mRrule = mEventRecurrence.toString();

                repeatString = EventRecurrenceFormatter.getRepeatString(mActivity, r,
                        mEventRecurrence, true);

                if (repeatString == null) {
                    repeatString = r.getString(R.string.custom);
                    Log.e(TAG, "Can't generate display string for " + mRrule);
                    enabled = false;
                } else {
                    // TODO Should give option to clear/reset rrule
                    enabled = RecurrencePickerDialog.canHandleRecurrenceRule(mEventRecurrence);
                    if (!enabled) {
                        Log.e(TAG, "UI can't handle " + mRrule);
                    }
                }
            }
        } else {
            repeatString = r.getString(R.string.does_not_repeat);
            enabled = minRepeatMode == 0 ? false : true;
        }

        mRruleButton.setText(repeatString);

        // Don't allow the user to make exceptions recurring events.
        if (mModel.mOriginalSyncId != null) {
            enabled = false;
        }
        mRruleButton.setOnClickListener(this);
        mRruleButton.setEnabled(enabled);
    }

    private class DateClickListener implements View.OnClickListener {
        private Time mTime;

        public DateClickListener(Time time) {
            mTime = time;
        }

        @Override
        public void onClick(View v) {
            if (!mView.hasWindowFocus()) {
                // Don't do anything if the activity if paused. Since Activity doesn't
                // have a built in way to do this, we would have to implement one ourselves and
                // either cast our Activity to a specialized activity base class or implement some
                // generic interface that tells us if an activity is paused. hasWindowFocus() is
                // close enough if not quite perfect.
                return;
            }

            ///M: keep DatePickerDialog and TimePickerDialog when rotate device @{
            if (v.getId() == R.id.start_date) {
                Log.d(DATE_TIME_TAG, "DateClickListener, mStartDateButton is clicked.");
                setDateTimeViewId(ID_START_DATE);
            }
            if (v.getId() == R.id.end_date) {
                Log.d(DATE_TIME_TAG, "DateClickListener, mEndDateButton is clicked.");
                setDateTimeViewId(ID_END_DATE);
            }
            ///@}


            if (v == mStartDateButton) {
                mDateSelectedWasStartDate = true;
            } else {
                mDateSelectedWasStartDate = false;
            }

            final DateListener listener = new DateListener(v);
            if (mDatePickerDialog != null) {
                mDatePickerDialog.dismiss();
            }
            mDatePickerDialog = LunarDatePickerDialog.newInstance(listener,
                    mTime.year, mTime.month, mTime.monthDay);
            mDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(mActivity));
            mDatePickerDialog.setYearRange(Utils.YEAR_MIN, Utils.YEAR_MAX);
            mDatePickerDialog.show(mActivity.getFragmentManager(), FRAG_TAG_DATE_PICKER);
            /** M:*/
            boolean showLunarHeader = mExt.isExtensionEnabled() && mExt.isEditingLunarDate();
            mDatePickerDialog.setShowLunarHeader(showLunarHeader);
        }
    }

    public static class CalendarsAdapter extends ResourceCursorAdapter {
        public CalendarsAdapter(Context context, int resourceId, Cursor c) {
            super(context, resourceId, c);
            setDropDownViewResource(R.layout.calendars_dropdown_item);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View colorBar = view.findViewById(R.id.color);
            int colorColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
            int nameColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
            int ownerColumn = cursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
            if (colorBar != null) {
                colorBar.setBackgroundColor(Utils.getDisplayColorFromColor(cursor
                        .getInt(colorColumn)));
            }

            //FR593012-Wentao-Wan-001 begin
            /*TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                String displayName = cursor.getString(nameColumn);
                name.setText(displayName);

                TextView accountName = (TextView) view.findViewById(R.id.account_name);
                if (accountName != null) {
                    accountName.setText(cursor.getString(ownerColumn));
                    accountName.setVisibility(TextView.VISIBLE);
                }*/
            long id = cursor.getLong(cursor.getColumnIndex(Calendars._ID));
            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                String displayName = cursor.getString(nameColumn);
                boolean localHostAccountFlag = (id==1) && (displayName.equalsIgnoreCase(PC_SYNC_ACCOUNT_DISPLAY_NAME));
                if(localHostAccountFlag){
                  displayName = context.getString(R.string.default_calendar_name);
                  name.setTextColor(0xFF000000);
                }
                name.setText(displayName);

                TextView accountName = (TextView) view.findViewById(R.id.account_name);
                if (accountName != null) {
                   if(localHostAccountFlag){
                      String account = context.getString(R.string.default_calendar_account);
                      accountName.setText(account);
                   }else{
                      accountName.setText(cursor.getString(ownerColumn));
                    }
                    accountName.setVisibility(TextView.VISIBLE);
                }
            //FR593012-Wentao-Wan-001 end
            }
        }
    }

    /**
     * Does prep steps for saving a calendar event.
     *
     * This triggers a parse of the attendees list and checks if the event is
     * ready to be saved. An event is ready to be saved so long as a model
     * exists and has a calendar it can be associated with, either because it's
     * an existing event or we've finished querying.
     *
     * @return false if there is no model or no calendar had been loaded yet,
     * true otherwise.
     */
    public boolean prepareForSave() {
        if (mModel == null || (mCalendarsCursor == null && mModel.mUri == null)) {
            return false;
        }
        return fillModelFromUI();
    }

    public boolean fillModelFromReadOnlyUi() {
        if (mModel == null || (mCalendarsCursor == null && mModel.mUri == null)) {
            return false;
        }
        mModel.mReminders = EventViewUtils.reminderItemsToReminders(
                    mReminderItems, mReminderMinuteValues, mReminderMethodValues);
        mModel.mReminders.addAll(mUnsupportedReminders);
        mModel.normalizeReminders();
        int status = EventInfoFragment.getResponseFromButtonId(
                mResponseRadioGroup.getCheckedRadioButtonId());
        if (status != Attendees.ATTENDEE_STATUS_NONE) {
            mModel.mSelfAttendeeStatus = status;
        }
        return true;
    }

    // This is called if the user clicks on one of the buttons: "Save",
    // "Discard", or "Delete". This is also called if the user clicks
    // on the "remove reminder" button.
    @Override
    public void onClick(View view) {
        if (view == mRruleButton) {
            Bundle b = new Bundle();
            b.putLong(RecurrencePickerDialog.BUNDLE_START_TIME_MILLIS,
                    mStartTime.toMillis(false));
            /*
             * M: put event end time to the bundle.
             */
            b.putLong(RecurrencePickerDialog.BUNDLE_EVENT_TIME_DURATION,
                    (mEndTime.toMillis(false) - mStartTime.toMillis(false)));
            b.putString(RecurrencePickerDialog.BUNDLE_TIME_ZONE, mStartTime.timezone);

            // TODO may be more efficient to serialize and pass in EventRecurrence
            b.putString(RecurrencePickerDialog.BUNDLE_RRULE, mRrule);

            FragmentManager fm = mActivity.getFragmentManager();
            RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm
                    .findFragmentByTag(FRAG_TAG_RECUR_PICKER);
            if (rpd != null) {
                rpd.dismiss();
            }
            rpd = new RecurrencePickerDialog();
            rpd.setArguments(b);
            rpd.setOnRecurrenceSetListener(EditEventView.this);
            rpd.show(fm, FRAG_TAG_RECUR_PICKER);
            return;
        }

        // This must be a click on one of the "remove reminder" buttons
        LinearLayout reminderItem = (LinearLayout) view.getParent();
        LinearLayout parent = (LinearLayout) reminderItem.getParent();
        parent.removeView(reminderItem);
        mReminderItems.remove(reminderItem);
        updateRemindersVisibility(mReminderItems.size());
        EventViewUtils.updateAddReminderButton(mView, mReminderItems, mModel.mCalendarMaxReminders);
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        Log.d(TAG, "Old rrule:" + mRrule);
        Log.d(TAG, "New rrule:" + rrule);
        mRrule = rrule;
        if (mRrule != null) {
            mEventRecurrence.parse(mRrule);
        }
        populateRepeats();
    }

    ///M: do request focus action, if there is no focus on the view. @{
    public void requestFocus() {
        /**
         * M: If there is no calendar account, the input method should be
         * hidden.
         */
        InputMethodManager imm = (InputMethodManager) mActivity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mCalendarsCursor == null || mCalendarsCursor.getCount() == 0) {
            View focusView = mActivity.getCurrentFocus();
            if (focusView != null) {
                imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                focusView.clearFocus();
            }
            Log.i(TAG, "no calendar account, dismiss inputmethod. focusView="
                    + focusView);
            return;
        }

        if (!(mTitleTextView.isFocused() || mLocationTextView.isFocused()
                || mDescriptionTextView.isFocused() || mAttendeesList
                    .isFocused())) {
            Log.i(TAG, "mTitleTextView no focus, request it.");
            mView.requestFocus(View.FOCUS_FORWARD);
            mTitleTextView.requestFocus();
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } else {
            Log.i(TAG, "mTitleTextView have focus, just do nothing.");
        }
    }
    ///@}

    // This is called if the user cancels the "No calendars" dialog.
    // The "No calendars" dialog is shown if there are no syncable calendars.
    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mLoadingCalendarsDialog) {
            mLoadingCalendarsDialog = null;
            mSaveAfterQueryComplete = false;
        } else if (dialog == mNoCalendarsDialog) {
            mDone.setDoneCode(Utils.DONE_REVERT);
            mDone.run();
            return;
        }
    }

    // This is called if the user clicks on a dialog button.
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mNoCalendarsDialog) {
            mDone.setDoneCode(Utils.DONE_REVERT);
            mDone.run();
            if (which == DialogInterface.BUTTON_POSITIVE) {
                //huanglin 20140104 for PR576713
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                boolean superMode = Boolean.parseBoolean(SystemProperties.get("sys.supermode.key", "false"));
                if (superMode)
                {
                    Toast.makeText(builder.getContext(), R.string.cannot_use_in_ultimate_mode, Toast.LENGTH_SHORT)
                    .show();
                    return;
                }
                Intent nextIntent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                final String[] array = {"com.android.calendar"};
                nextIntent.putExtra(Settings.EXTRA_AUTHORITIES, array);
                ///M: remove NEW_TASK flag.Keep same with add account in Calendar Setting.
                nextIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mActivity.startActivity(nextIntent);
            }
        }else if (dialog == mAddDefaultCalendarsDialog){
            mDone.setDoneCode(Utils.DONE_REVERT);
            mDone.run();
        }
    }

    // Goes through the UI elements and updates the model as necessary
    private boolean fillModelFromUI() {
        if (mModel == null) {
            return false;
        }
        mModel.mReminders = EventViewUtils.reminderItemsToReminders(mReminderItems,
                mReminderMinuteValues, mReminderMethodValues);
        mModel.mReminders.addAll(mUnsupportedReminders);
        mModel.normalizeReminders();
        mModel.mHasAlarm = mReminderItems.size() > 0;
        mModel.mTitle = mTitleTextView.getText().toString();
        mModel.mAllDay = mAllDayCheckBox.isChecked();
        mModel.mLocation = mLocationTextView.getText().toString();
        mModel.mDescription = mDescriptionTextView.getText().toString();
        if (TextUtils.isEmpty(mModel.mLocation)) {
            mModel.mLocation = null;
        }
        if (TextUtils.isEmpty(mModel.mDescription)) {
            mModel.mDescription = null;
        }

        int status = EventInfoFragment.getResponseFromButtonId(mResponseRadioGroup
                .getCheckedRadioButtonId());
        if (status != Attendees.ATTENDEE_STATUS_NONE) {
            mModel.mSelfAttendeeStatus = status;
        }

        if (mAttendeesList != null) {
            mEmailValidator.setRemoveInvalid(true);
            mAttendeesList.performValidation();
            mModel.mAttendeesList.clear();
            ///M:@{
            String address = mAttendeesList.getText().toString();
            if (!TextUtils.isEmpty(address) && isHasInvalidAddress(address, mEmailValidator)) {
                MTKToast.toast(mActivity, R.string.attendees_invalid_tip);
            }
            ///@}
            mModel.addAttendees(mAttendeesList.getText().toString(), mEmailValidator);
            /**M: let mHasAttendeeData keep with the attendee number in mAttendeesList. @{*/
            if(mModel.mAttendeesList.size() > 0) {
                mModel.mHasAttendeeData = true;
            }
            /**@}*/
            mEmailValidator.setRemoveInvalid(false);
        }

        // If this was a new event we need to fill in the Calendar information
        if (mModel.mUri == null) {
            mModel.mCalendarId = mCalendarsSpinner.getSelectedItemId();
            int calendarCursorPosition = mCalendarsSpinner.getSelectedItemPosition();
            if (mCalendarsCursor.moveToPosition(calendarCursorPosition)) {
                String defaultCalendar = mCalendarsCursor.getString(
                        EditEventHelper.CALENDARS_INDEX_OWNER_ACCOUNT);
                Utils.setSharedPreference(
                        mActivity, GeneralPreferences.KEY_DEFAULT_CALENDAR, defaultCalendar);
                mModel.mOwnerAccount = defaultCalendar;
                mModel.mOrganizer = defaultCalendar;
                mModel.mCalendarId = mCalendarsCursor.getLong(EditEventHelper.CALENDARS_INDEX_ID);
            }
        }

        if (mModel.mAllDay) {
            ///M: @{
            LogUtil.v(TAG, "all-day event, mTimezone set to UTC");
            ///@}
            // Reset start and end time, increment the monthDay by 1, and set
            // the timezone to UTC, as required for all-day events.
            mTimezone = Time.TIMEZONE_UTC;
            mStartTime.hour = 0;
            mStartTime.minute = 0;
            mStartTime.second = 0;
            mStartTime.timezone = mTimezone;
            mModel.mStart = mStartTime.normalize(true);

            mEndTime.hour = 0;
            mEndTime.minute = 0;
            mEndTime.second = 0;
            mEndTime.timezone = mTimezone;
            // When a user see the event duration as "X - Y" (e.g. Oct. 28 - Oct. 29), end time
            // should be Y + 1 (Oct.30).
            final long normalizedEndTimeMillis =
                    mEndTime.normalize(true) + DateUtils.DAY_IN_MILLIS;
            if (normalizedEndTimeMillis < mModel.mStart) {
                // mEnd should be midnight of the next day of mStart.
                mModel.mEnd = mModel.mStart + DateUtils.DAY_IN_MILLIS;
            } else {
                mModel.mEnd = normalizedEndTimeMillis;
            }
        } else {
            mStartTime.timezone = mTimezone;
            mEndTime.timezone = mTimezone;
            mModel.mStart = mStartTime.toMillis(true);
            mModel.mEnd = mEndTime.toMillis(true);
        }
        mModel.mTimezone = mTimezone;
        mModel.mAccessLevel = mAccessLevelSpinner.getSelectedItemPosition();
        // TODO set correct availability value
        mModel.mAvailability = mAvailabilityValues.get(mAvailabilitySpinner
                .getSelectedItemPosition());

        // rrrule
        // If we're making an exception we don't want it to be a repeating
        // event.
        if (mModification == EditEventHelper.MODIFY_SELECTED) {
            mModel.mRrule = null;
        } else {
            mModel.mRrule = mRrule;
        }

        return true;
    }

    public EditEventView(Activity activity, View view, EditDoneRunnable done,
            boolean timeSelectedWasStartTime, boolean dateSelectedWasStartDate) {
        ///M: #extension# @{
        mExt = ExtensionFactory.getEditEventViewExt(activity, this);
        ///@}

        mActivity = activity;
        mView = view;
        mDone = done;

        // cache top level view elements
        mLoadingMessage = (TextView) view.findViewById(R.id.loading_message);
        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);

        // cache all the widgets
        mCalendarsSpinner = (Spinner) view.findViewById(R.id.calendars_spinner);
        mTitleTextView = (TextView) view.findViewById(R.id.title);
        mLocationTextView = (AutoCompleteTextView) view.findViewById(R.id.location);
        mDescriptionTextView = (TextView) view.findViewById(R.id.description);
        mTimezoneLabel = (TextView) view.findViewById(R.id.timezone_label);
        mStartDateButton = (Button) view.findViewById(R.id.start_date);
        mEndDateButton = (Button) view.findViewById(R.id.end_date);
        mWhenView = (TextView) mView.findViewById(R.id.when);
        mTimezoneTextView = (TextView) mView.findViewById(R.id.timezone_textView);
        mStartTimeButton = (Button) view.findViewById(R.id.start_time);
        mEndTimeButton = (Button) view.findViewById(R.id.end_time);
        mTimezoneButton = (Button) view.findViewById(R.id.timezone_button);
        mTimezoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimezoneDialog();
            }
        });
        mTimezoneRow = view.findViewById(R.id.timezone_button_row);
        mStartTimeHome = (TextView) view.findViewById(R.id.start_time_home_tz);
        mStartDateHome = (TextView) view.findViewById(R.id.start_date_home_tz);
        mEndTimeHome = (TextView) view.findViewById(R.id.end_time_home_tz);
        mEndDateHome = (TextView) view.findViewById(R.id.end_date_home_tz);
        mAllDayCheckBox = (CheckBox) view.findViewById(R.id.is_all_day);
        mRruleButton = (Button) view.findViewById(R.id.rrule);
        mAvailabilitySpinner = (Spinner) view.findViewById(R.id.availability);
        mAccessLevelSpinner = (Spinner) view.findViewById(R.id.visibility);
        mCalendarSelectorGroup = view.findViewById(R.id.calendar_selector_group);
        mCalendarSelectorWrapper = view.findViewById(R.id.calendar_selector_wrapper);
        mCalendarStaticGroup = view.findViewById(R.id.calendar_group);
        mRemindersGroup = view.findViewById(R.id.reminders_row);
        mResponseGroup = view.findViewById(R.id.response_row);
        mOrganizerGroup = view.findViewById(R.id.organizer_row);
        mAttendeesGroup = view.findViewById(R.id.add_attendees_row);
        mLocationGroup = view.findViewById(R.id.where_row);
        mDescriptionGroup = view.findViewById(R.id.description_row);
        mStartHomeGroup = view.findViewById(R.id.from_row_home_tz);
        mEndHomeGroup = view.findViewById(R.id.to_row_home_tz);
        mAttendeesList = (MultiAutoCompleteTextView) view.findViewById(R.id.attendees);

        mColorPickerNewEvent = view.findViewById(R.id.change_color_new_event);
        mColorPickerExistingEvent = view.findViewById(R.id.change_color_existing_event);

        mTitleTextView.setTag(mTitleTextView.getBackground());
        mLocationTextView.setTag(mLocationTextView.getBackground());
        mLocationAdapter = new EventLocationAdapter(activity);
        mLocationTextView.setAdapter(mLocationAdapter);
        mLocationTextView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Dismiss the suggestions dropdown.  Return false so the other
                    // side effects still occur (soft keyboard going away, etc.).
                    mLocationTextView.dismissDropDown();
                }
                return false;
            }
        });

        mAvailabilityExplicitlySet = false;
        mAllDayChangingAvailability = false;
        mAvailabilityCurrentlySelected = -1;
        mAvailabilitySpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                // The spinner's onItemSelected gets called while it is being
                // initialized to the first item, and when we explicitly set it
                // in the allDay checkbox toggling, so we need these checks to
                // find out when the spinner is actually being clicked.

                // Set the initial selection.
                if (mAvailabilityCurrentlySelected == -1) {
                    mAvailabilityCurrentlySelected = position;
                }

                if (mAvailabilityCurrentlySelected != position &&
                        !mAllDayChangingAvailability) {
                    mAvailabilityExplicitlySet = true;
                } else {
                    mAvailabilityCurrentlySelected = position;
                    mAllDayChangingAvailability = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) { }
        });


        mDescriptionTextView.setTag(mDescriptionTextView.getBackground());
        mAttendeesList.setTag(mAttendeesList.getBackground());
        mOriginalPadding[0] = mLocationTextView.getPaddingLeft();
        mOriginalPadding[1] = mLocationTextView.getPaddingTop();
        mOriginalPadding[2] = mLocationTextView.getPaddingRight();
        mOriginalPadding[3] = mLocationTextView.getPaddingBottom();
        mEditViewList.add(mTitleTextView);
        mEditViewList.add(mLocationTextView);
        mEditViewList.add(mDescriptionTextView);
        mEditViewList.add(mAttendeesList);

        mViewOnlyList.add(view.findViewById(R.id.when_row));
        mViewOnlyList.add(view.findViewById(R.id.timezone_textview_row));

        mEditOnlyList.add(view.findViewById(R.id.all_day_row));
        mEditOnlyList.add(view.findViewById(R.id.availability_row));
        mEditOnlyList.add(view.findViewById(R.id.visibility_row));
        mEditOnlyList.add(view.findViewById(R.id.from_row));
        mEditOnlyList.add(view.findViewById(R.id.to_row));
        mEditOnlyList.add(mTimezoneRow);
        mEditOnlyList.add(mStartHomeGroup);
        mEditOnlyList.add(mEndHomeGroup);

        mResponseRadioGroup = (RadioGroup) view.findViewById(R.id.response_value);
        mRemindersContainer = (LinearLayout) view.findViewById(R.id.reminder_items_container);

        mTimezone = Utils.getTimeZone(activity, null);
        mIsMultipane = activity.getResources().getBoolean(R.bool.tablet_config);
        mStartTime = new Time(mTimezone);
        mEndTime = new Time(mTimezone);
        mEmailValidator = new Rfc822Validator(null);
        initMultiAutoCompleteTextView((RecipientEditTextView) mAttendeesList);

        ///M:add input filter @{
        IEditTextExt extension = ExtensionFactory.getEditTextExt();
        extension.setLengthInputFilter((EditText) mTitleTextView, mActivity, TITLE_MAX_LENGTH);
        extension.setLengthInputFilter((EditText) mLocationTextView, mActivity, LOCATION_MAX_LENGTH);
        extension.setLengthInputFilter((EditText) mDescriptionTextView, mActivity, DESCRIPTION_MAX_LENGTH);
        ///@}
        
        // Display loading screen
        setModel(null);

        FragmentManager fm = activity.getFragmentManager();
        RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm
                .findFragmentByTag(FRAG_TAG_RECUR_PICKER);
        if (rpd != null) {
            rpd.setOnRecurrenceSetListener(this);
        }
        TimeZonePickerDialog tzpd = (TimeZonePickerDialog) fm
                .findFragmentByTag(FRAG_TAG_TIME_ZONE_PICKER);
        if (tzpd != null) {
            tzpd.setOnTimeZoneSetListener(this);
        }
        TimePickerDialog tpd = (TimePickerDialog) fm.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (tpd != null) {
            View v;
            mTimeSelectedWasStartTime = timeSelectedWasStartTime;
            if (timeSelectedWasStartTime) {
                v = mStartTimeButton;
            } else {
                v = mEndTimeButton;
            }
            tpd.setOnTimeSetListener(new TimeListener(v));
        }
        mDatePickerDialog = (LunarDatePickerDialog) fm.findFragmentByTag(FRAG_TAG_DATE_PICKER);
        if (mDatePickerDialog != null) {
            View v;
            mDateSelectedWasStartDate = dateSelectedWasStartDate;
            if (dateSelectedWasStartDate) {
                v = mStartDateButton;
            } else {
                v = mEndDateButton;
            }
            mDatePickerDialog.setOnDateSetListener(new DateListener(v));
        }
    }


    /**
     * Loads an integer array asset into a list.
     */
    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<Integer>(size);

        for (int i = 0; i < size; i++) {
            list.add(vals[i]);
        }

        return list;
    }

    /**
     * Loads a String array asset into a list.
     */
    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }

    private void prepareAvailability() {
        Resources r = mActivity.getResources();

        mAvailabilityValues = loadIntegerArray(r, R.array.availability_values);
        mAvailabilityLabels = loadStringArray(r, R.array.availability);
        // Copy the unadulterated availability labels for all-day toggling.
        mOriginalAvailabilityLabels = new ArrayList<String>();
        mOriginalAvailabilityLabels.addAll(mAvailabilityLabels);

        if (mModel.mCalendarAllowedAvailability != null) {
            EventViewUtils.reduceMethodList(mAvailabilityValues, mAvailabilityLabels,
                    mModel.mCalendarAllowedAvailability);
        }

        mAvailabilityAdapter = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_spinner_item, mAvailabilityLabels);
        mAvailabilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAvailabilitySpinner.setAdapter(mAvailabilityAdapter);
    }

    /**
     * Prepares the reminder UI elements.
     * <p>
     * (Re-)loads the minutes / methods lists from the XML assets, adds/removes items as
     * needed for the current set of reminders and calendar properties, and then creates UI
     * elements.
     */
    private void prepareReminders() {
        CalendarEventModel model = mModel;
        Resources r = mActivity.getResources();

        // Load the labels and corresponding numeric values for the minutes and methods lists
        // from the assets.  If we're switching calendars, we need to clear and re-populate the
        // lists (which may have elements added and removed based on calendar properties).  This
        // is mostly relevant for "methods", since we shouldn't have any "minutes" values in a
        // new event that aren't in the default set.
        mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);
        mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);
        mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);
        mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);

        // Remove any reminder methods that aren't allowed for this calendar.  If this is
        // a new event, mCalendarAllowedReminders may not be set the first time we're called.
        if (mModel.mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mModel.mCalendarAllowedReminders);
        }

        int numReminders = 0;
        if (model.mHasAlarm) {
            ArrayList<ReminderEntry> reminders = model.mReminders;
            numReminders = reminders.size();
            // Insert any minute values that aren't represented in the minutes list.
            for (ReminderEntry re : reminders) {
                if (mReminderMethodValues.contains(re.getMethod())) {
                    EventViewUtils.addMinutesToList(mActivity, mReminderMinuteValues,
                            mReminderMinuteLabels, re.getMinutes());
                }
            }

            // Create a UI element for each reminder.  We display all of the reminders we get
            // from the provider, even if the count exceeds the calendar maximum.  (Also, for
            // a new event, we won't have a maxReminders value available.)
            mUnsupportedReminders.clear();
            for (ReminderEntry re : reminders) {
                if (mReminderMethodValues.contains(re.getMethod())
                        || re.getMethod() == Reminders.METHOD_DEFAULT) {
                    EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                            mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                            mReminderMethodLabels, re, Integer.MAX_VALUE, null);
                } else {
                    // TODO figure out a way to display unsupported reminders
                    mUnsupportedReminders.add(re);
                }
            }
        }

        updateRemindersVisibility(numReminders);
        EventViewUtils.updateAddReminderButton(mView, mReminderItems, mModel.mCalendarMaxReminders);
    }

    /**
     * Fill in the view with the contents of the given event model. This allows
     * an edit view to be initialized before the event has been loaded. Passing
     * in null for the model will display a loading screen. A non-null model
     * will fill in the view's fields with the data contained in the model.
     *
     * @param model The event model to pull the data from
     */
    public void setModel(CalendarEventModel model) {
        mModel = model;

        // Need to close the autocomplete adapter to prevent leaking cursors.
        if (mAddressAdapter != null && mAddressAdapter instanceof EmailAddressAdapter) {
            ((EmailAddressAdapter)mAddressAdapter).close();
            mAddressAdapter = null;
        }

        if (model == null) {
            // Display loading screen
            mLoadingMessage.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.GONE);
            return;
        }

        boolean canRespond = EditEventHelper.canRespond(model);

        long begin = model.mStart;
        long end = model.mEnd;
        mTimezone = model.mTimezone; // this will be UTC for all day events

        ///M: If the begim & end Millis <= 0,also set it to mStartTime &
        // mEndTime
        // because if the timezone if bore utc,the millis may be less than 0 @{
        // Set up the starting times
        mStartTime.timezone = mTimezone;
        mStartTime.set(begin);
        mStartTime.normalize(true);

        mEndTime.timezone = mTimezone;
        mEndTime.set(end);
        mEndTime.normalize(true);
        ///@}
        
        mRrule = model.mRrule;
        if (!TextUtils.isEmpty(mRrule)) {
            mEventRecurrence.parse(mRrule);
        }

        if (mEventRecurrence.startDate == null) {
            mEventRecurrence.startDate = mStartTime;
        }

        ///M:remove the "Guest" when it is local account. @{
        setAttendeesGroupVisibility(model.mAccountType);
        ///@}
        // If the user is allowed to change the attendees set up the view and
        // validator
        if (!model.mHasAttendeeData) {
            mAttendeesGroup.setVisibility(View.GONE);
        }

        mAllDayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAllDayViewsVisibility(isChecked);
            }
        });

        boolean prevAllDay = mAllDayCheckBox.isChecked();
        mAllDay = false; // default to false. Let setAllDayViewsVisibility update it as needed
        if (model.mAllDay) {
            mAllDayCheckBox.setChecked(true);
            // put things back in local time for all day events
            mTimezone = Utils.getTimeZone(mActivity, null);
            mStartTime.timezone = mTimezone;
            mEndTime.timezone = mTimezone;
            mEndTime.normalize(true);
        } else {
            mAllDayCheckBox.setChecked(false);
        }
        // On a rotation we need to update the views but onCheckedChanged
        // doesn't get called
        if (prevAllDay == mAllDayCheckBox.isChecked()) {
            setAllDayViewsVisibility(prevAllDay);
        }

        populateTimezone(mStartTime.normalize(true));

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mActivity);
        String defaultReminderString = prefs.getString(
                GeneralPreferences.KEY_DEFAULT_REMINDER, GeneralPreferences.NO_REMINDER_STRING);
        mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);

        prepareReminders();
        prepareAvailability();

        View reminderAddButton = mView.findViewById(R.id.reminder_add);
        View.OnClickListener addReminderOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addReminder();
            }
        };
        reminderAddButton.setOnClickListener(addReminderOnClickListener);

        if (!mIsMultipane) {
            mView.findViewById(R.id.is_all_day_label).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mAllDayCheckBox.setChecked(!mAllDayCheckBox.isChecked());
                        }
                    });
        }

        if (model.mTitle != null) {
            mTitleTextView.setTextKeepState(model.mTitle);
        }

        if (model.mIsOrganizer || TextUtils.isEmpty(model.mOrganizer)
                || model.mOrganizer.endsWith(GOOGLE_SECONDARY_CALENDAR)) {
            mView.findViewById(R.id.organizer_label).setVisibility(View.GONE);
            mView.findViewById(R.id.organizer).setVisibility(View.GONE);
            mOrganizerGroup.setVisibility(View.GONE);
        } else {
            ((TextView) mView.findViewById(R.id.organizer)).setText(model.mOrganizerDisplayName);
        }

        if (model.mLocation != null) {
            mLocationTextView.setTextKeepState(model.mLocation);
        }

        if (model.mDescription != null) {
            mDescriptionTextView.setTextKeepState(model.mDescription);
        }

        int availIndex = mAvailabilityValues.indexOf(model.mAvailability);
        if (availIndex != -1) {
            mAvailabilitySpinner.setSelection(availIndex);
        }
        mAccessLevelSpinner.setSelection(model.mAccessLevel);

        View responseLabel = mView.findViewById(R.id.response_label);
        if (canRespond) {
            int buttonToCheck = EventInfoFragment
                    .findButtonIdForResponse(model.mSelfAttendeeStatus);
            mResponseRadioGroup.check(buttonToCheck); // -1 clear all radio buttons
            mResponseRadioGroup.setVisibility(View.VISIBLE);
            responseLabel.setVisibility(View.VISIBLE);
        } else {
            responseLabel.setVisibility(View.GONE);
            mResponseRadioGroup.setVisibility(View.GONE);
            mResponseGroup.setVisibility(View.GONE);
        }

        // added by Fujuan.Lin for RR565704 begin
        // change the display color if a day is a facebook friend's birthday.
        boolean isFacebookBirthday = !TextUtils.isEmpty(model.mFrinedId);
        Account accountFB = null;
        if (Utils.IS_FACEBOOK_DEVICE && isFacebookBirthday) {
            //displayColor = Utils.getFBBithdayColor();
            accountFB = Utils.getFacebookAccount(mActivity);
        }
        // added by Fujuan.Lin for RR565704 end
        if (model.mUri != null) {
            // This is an existing event so hide the calendar spinner
            // since we can't change the calendar.
            View calendarGroup = mView.findViewById(R.id.calendar_selector_group);
            calendarGroup.setVisibility(View.GONE);
            TextView tv = (TextView) mView.findViewById(R.id.calendar_textview);
            //FR593012-Wentao-Wan-001 begin
            /*tv.setText(model.mCalendarDisplayName);
            tv = (TextView) mView.findViewById(R.id.calendar_textview_secondary);
            if (tv != null) {
                tv.setText(model.mOwnerAccount);
            }*/
            boolean localHostAccountFlag = (model.mCalendarId == 1) &&
                                           (model.mCalendarDisplayName.equalsIgnoreCase(PC_SYNC_ACCOUNT_DISPLAY_NAME));
            if(localHostAccountFlag){
               String displayName = mActivity.getString(R.string.default_calendar_name);
               tv.setText(displayName);
            }else{
               tv.setText(model.mCalendarDisplayName);
            }
            // added by Fujuan.Lin for RR565704 begin
            if (Utils.IS_FACEBOOK_DEVICE && isFacebookBirthday && accountFB != null) {
                tv.setText(accountFB.name);
            }
			// added by Fujuan.Lin for RR565704 end
         //yuanding add it for bug:761406 20140813 start  
           /* tv = (TextView) mView.findViewById(R.id.calendar_textview_secondary);*/  
            if (tv != null) {
               if (localHostAccountFlag) {
                  String account = mActivity.getString(R.string.default_calendar_account);
                  tv.setText(account);
                 }else{
                  tv.setText(model.mOwnerAccount);
                 }
		        //added by Fujuan.Lin for RR565704 begin
                if (Utils.IS_FACEBOOK_DEVICE && isFacebookBirthday && accountFB != null) {
                    tv.setText(mActivity.getString(R.string.default_calendar_account_facebook));
                } 
                // added by Fujuan.Lin for RR565704 end
            }
          //yuanding add it for bug:761406 20140813 end  
        } else {
            View calendarGroup = mView.findViewById(R.id.calendar_group);
            calendarGroup.setVisibility(View.GONE);
        }
        if (model.isEventColorInitialized()) {
            updateHeadlineColor(model, model.getEventColor());
        }

        populateWhen();
        populateRepeats();
        updateAttendees(model.mAttendeesList);

        updateView();
        mScrollView.setVisibility(View.VISIBLE);
        mLoadingMessage.setVisibility(View.GONE);
        sendAccessibilityEvent();

        ///M: extend setModel @{
        mExt.setExtUI(model);
        ///@}
    }

    public void updateHeadlineColor(CalendarEventModel model, int displayColor) {
	 // added by Fujuan.Lin for RR565704 begin
        // change the display color if a day is a facebook friend's birthday.
        boolean isFacebookBirthday = !TextUtils.isEmpty(model.mFrinedId);
        Account accountFB = null;
        if (Utils.IS_FACEBOOK_DEVICE && isFacebookBirthday) {
            displayColor = Utils.getFBBithdayColor();
        }
        // added by Fujuan.Lin for RR565704 end
        if (model.mUri != null) {
            if (mIsMultipane) {
                mView.findViewById(R.id.calendar_textview_with_colorpicker)
                    .setBackgroundColor(displayColor);
            } else {
                mView.findViewById(R.id.calendar_group).setBackgroundColor(displayColor);
            }
        } else {
            setSpinnerBackgroundColor(displayColor);
        }
    }

    private void setSpinnerBackgroundColor(int displayColor) {
        if (mIsMultipane) {
            mCalendarSelectorWrapper.setBackgroundColor(displayColor);
        } else {
            mCalendarSelectorGroup.setBackgroundColor(displayColor);
        }
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager am =
            (AccessibilityManager) mActivity.getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled() || mModel == null) {
            return;
        }
        StringBuilder b = new StringBuilder();
        addFieldsRecursive(b, mView);
        CharSequence msg = b.toString();

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(getClass().getName());
        event.setPackageName(mActivity.getPackageName());
        event.getText().add(msg);
        event.setAddedCount(msg.length());

        am.sendAccessibilityEvent(event);
    }

    private void addFieldsRecursive(StringBuilder b, View v) {
        if (v == null || v.getVisibility() != View.VISIBLE) {
            return;
        }
        if (v instanceof TextView) {
            CharSequence tv = ((TextView) v).getText();
            if (!TextUtils.isEmpty(tv.toString().trim())) {
                b.append(tv + PERIOD_SPACE);
            }
        } else if (v instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) v;
            int id = rg.getCheckedRadioButtonId();
            if (id != View.NO_ID) {
                b.append(((RadioButton) (v.findViewById(id))).getText() + PERIOD_SPACE);
            }
        } else if (v instanceof Spinner) {
            Spinner s = (Spinner) v;
            if (s.getSelectedItem() instanceof String) {
                String str = ((String) (s.getSelectedItem())).trim();
                if (!TextUtils.isEmpty(str)) {
                    b.append(str + PERIOD_SPACE);
                }
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int children = vg.getChildCount();
            for (int i = 0; i < children; i++) {
                addFieldsRecursive(b, vg.getChildAt(i));
            }
        }
    }

    /**
     * Creates a single line string for the time/duration
     */
    protected void setWhenString() {
        String when;
        int flags = DateUtils.FORMAT_SHOW_DATE;
        String tz = mTimezone;
        if (mModel.mAllDay) {
            flags |= DateUtils.FORMAT_SHOW_WEEKDAY;
            tz = Time.TIMEZONE_UTC;
        } else {
            flags |= DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(mActivity)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
        }
        long startMillis = mStartTime.normalize(true);
        long endMillis = mEndTime.normalize(true);
        sStringBuilder.setLength(0);
        when = DateUtils
                .formatDateRange(mActivity, sFormatter, startMillis, endMillis, flags, tz).toString();
        mWhenView.setText(when);
    }

    /**
     * Configures the Calendars spinner.  This is only done for new events, because only new
     * events allow you to select a calendar while editing an event.
     * <p>
     * We tuck a reference to a Cursor with calendar database data into the spinner, so that
     * we can easily extract calendar-specific values when the value changes (the spinner's
     * onItemSelected callback is configured).
     */
    public void setCalendarsCursor(Cursor cursor, boolean userVisible, long selectedCalendarId) {
        // If there are no syncable calendars, then we cannot allow
        // creating a new event.
        mCalendarsCursor = cursor;
        if (cursor == null || cursor.getCount() == 0) {
            // Cancel the "loading calendars" dialog if it exists
            if (mSaveAfterQueryComplete) {
                mLoadingCalendarsDialog.cancel();
            }
            if (!userVisible) {
                return;
            }
            // Create an error message for the user that, when clicked,
            // will exit this activity without saving the event.
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(R.string.no_syncable_calendars).setIconAttribute(
                    android.R.attr.alertDialogIcon).setMessage(R.string.no_calendars_found)
                    .setPositiveButton(R.string.add_account, this)
                    .setNegativeButton(android.R.string.no, this).setOnCancelListener(this);
            mNoCalendarsDialog = builder.show();
            return;
        }

        int selection;
        if (selectedCalendarId != -1) {
            selection = findSelectedCalendarPosition(cursor, selectedCalendarId);
        } else {
            selection = findDefaultCalendarPosition(cursor);
        }
        //huanglin 20140104 for PR576713
        boolean superMode = Boolean.parseBoolean(SystemProperties.get("sys.supermode.key", "false"));
        //huanglin 20131031 for PR525368
        // PR-532895-yunbo.xiang@tcl.com-001 begin
        long id = 0;
        String displayName = "";
        if(cursor.moveToFirst()){
            int nameColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
            id = cursor.getLong(cursor.getColumnIndex(Calendars._ID));
            displayName = cursor.getString(nameColumn);
        }
        // PR-532895-yunbo.xiang@tcl.com-001 end
        boolean localHostAccountFlag = (id==1) && (displayName.equalsIgnoreCase(PC_SYNC_ACCOUNT_DISPLAY_NAME));
        if (superMode && !localHostAccountFlag){
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.select_default_calendar_first)
                    .setPositiveButton(android.R.string.ok, this);
            mAddDefaultCalendarsDialog = builder.show();
            return;
        }
        if (superMode){
            selection = 0;
        }

        // populate the calendars spinner
        CalendarsAdapter adapter = new CalendarsAdapter(mActivity,
            R.layout.calendars_spinner_item, cursor);
        mCalendarsSpinner.setAdapter(adapter);
        mCalendarsSpinner.setOnItemSelectedListener(this);
        mCalendarsSpinner.setSelection(selection);
        if (superMode){
            mCalendarsSpinner.setEnabled(false);
        }

        if (mSaveAfterQueryComplete) {
            mLoadingCalendarsDialog.cancel();
            if (prepareForSave() && fillModelFromUI()) {
                int exit = userVisible ? Utils.DONE_EXIT : 0;
                mDone.setDoneCode(Utils.DONE_SAVE | exit);
                mDone.run();
            } else if (userVisible) {
                mDone.setDoneCode(Utils.DONE_EXIT);
                mDone.run();
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "SetCalendarsCursor:Save failed and unable to exit view");
            }
            return;
        }
    }

    /**
     * Updates the view based on {@link #mModification} and {@link #mModel}
     */
    public void updateView() {
        if (mModel == null) {
            return;
        }
        if (EditEventHelper.canModifyEvent(mModel)) {
            setViewStates(mModification);
        } else {
            setViewStates(Utils.MODIFY_UNINITIALIZED);
        }
    }

    private void setViewStates(int mode) {
        // Extra canModify check just in case
        if (mode == Utils.MODIFY_UNINITIALIZED || !EditEventHelper.canModifyEvent(mModel)) {
            setWhenString();

            for (View v : mViewOnlyList) {
                v.setVisibility(View.VISIBLE);
            }
            for (View v : mEditOnlyList) {
                v.setVisibility(View.GONE);
            }
            for (View v : mEditViewList) {
                v.setEnabled(false);
                v.setBackgroundDrawable(null);
            }
            mCalendarSelectorGroup.setVisibility(View.GONE);
            mCalendarStaticGroup.setVisibility(View.VISIBLE);
            mRruleButton.setEnabled(false);
            if (EditEventHelper.canAddReminders(mModel)) {
                mRemindersGroup.setVisibility(View.VISIBLE);
            } else {
                mRemindersGroup.setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(mLocationTextView.getText())) {
                mLocationGroup.setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(mDescriptionTextView.getText())) {
                mDescriptionGroup.setVisibility(View.GONE);
            }
        } else {
            for (View v : mViewOnlyList) {
                v.setVisibility(View.GONE);
            }
            for (View v : mEditOnlyList) {
                v.setVisibility(View.VISIBLE);
            }
            for (View v : mEditViewList) {
                v.setEnabled(true);
                if (v.getTag() != null) {
                    v.setBackgroundDrawable((Drawable) v.getTag());
                    v.setPadding(mOriginalPadding[0], mOriginalPadding[1], mOriginalPadding[2],
                            mOriginalPadding[3]);
                }
            }
            if (mModel.mUri == null) {
                mCalendarSelectorGroup.setVisibility(View.VISIBLE);
                mCalendarStaticGroup.setVisibility(View.GONE);
            } else {
                mCalendarSelectorGroup.setVisibility(View.GONE);
                mCalendarStaticGroup.setVisibility(View.VISIBLE);
            }
            if (mModel.mOriginalSyncId == null) {
                mRruleButton.setEnabled(true);
            } else {
                mRruleButton.setEnabled(false);
                mRruleButton.setBackgroundDrawable(null);
            }
            /// M:Check the status of repeat spinner, don't allow the user to make exceptions
            //recurring events, and don't enable it when time is not in range. @{
            populateRepeats();
            /// @}
            mRemindersGroup.setVisibility(View.VISIBLE);

            mLocationGroup.setVisibility(View.VISIBLE);
            mDescriptionGroup.setVisibility(View.VISIBLE);
        }
        setAllDayViewsVisibility(mAllDayCheckBox.isChecked());
    }

    public void setModification(int modifyWhich) {
        mModification = modifyWhich;
        updateView();
        updateHomeTime();
    }

    private int findSelectedCalendarPosition(Cursor calendarsCursor, long calendarId) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }
        int calendarIdColumn = calendarsCursor.getColumnIndexOrThrow(Calendars._ID);
        int position = 0;
        calendarsCursor.moveToPosition(-1);
        while (calendarsCursor.moveToNext()) {
            if (calendarsCursor.getLong(calendarIdColumn) == calendarId) {
                return position;
            }
            position++;
        }
        return 0;
    }

    // Find the calendar position in the cursor that matches calendar in
    // preference
    private int findDefaultCalendarPosition(Cursor calendarsCursor) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }

        String defaultCalendar = Utils.getSharedPreference(
                mActivity, GeneralPreferences.KEY_DEFAULT_CALENDAR, (String) null);

        int calendarsOwnerIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);

        ///M: error handling @{
        if (calendarsOwnerIndex < 0) {
            LogUtil.w(TAG, "getColumnIndexOrThrow(Calendar.OWNER_ACCOUNT) failed, return 0");
            return 0;
        }
        /// @}

        int accountNameIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
        int accountTypeIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);
        int position = 0;
        calendarsCursor.moveToPosition(-1);
        while (calendarsCursor.moveToNext()) {
            String calendarOwner = calendarsCursor.getString(calendarsOwnerIndex);
            if (defaultCalendar == null) {
                // There is no stored default upon the first time running.  Use a primary
                // calendar in this case.
                if (calendarOwner != null &&
                        calendarOwner.equals(calendarsCursor.getString(accountNameIndex)) &&
                        !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(
                                calendarsCursor.getString(accountTypeIndex))) {
                    return position;
                }
            } else if (defaultCalendar.equals(calendarOwner)) {
                // Found the default calendar.
                return position;
            }
            position++;
        }
        return 0;
    }

    private void updateAttendees(HashMap<String, Attendee> attendeesList) {
        if (attendeesList == null || attendeesList.isEmpty()) {
            return;
        }
        mAttendeesList.setText(null);
        for (Attendee attendee : attendeesList.values()) {

            // TODO: Please remove separator when Calendar uses the chips MR2 project

            // Adding a comma separator between email addresses to prevent a chips MR1.1 bug
            // in which email addresses are concatenated together with no separator.
            mAttendeesList.append(attendee.mEmail + ", ");
        }
    }

    private void updateRemindersVisibility(int numReminders) {
        if (numReminders == 0) {
            mRemindersContainer.setVisibility(View.GONE);
        } else {
            mRemindersContainer.setVisibility(View.VISIBLE);
        }
        
        ///M: if reminders can't be added more, the "add" button should gone. @{
        View reminderAddButton = mView.findViewById(R.id.reminder_add);
        if (numReminders >= mModel.mCalendarMaxReminders) {
            reminderAddButton.setVisibility(View.GONE);
        } else {
            reminderAddButton.setVisibility(View.VISIBLE);
        }
        /// @}
    }

    /**
     * Add a new reminder when the user hits the "add reminder" button.  We use the default
     * reminder time and method.
     */
    private void addReminder() {
        // TODO: when adding a new reminder, make it different from the
        // last one in the list (if any).
        if (mDefaultReminderMinutes == GeneralPreferences.NO_REMINDER) {
            EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                    mReminderMinuteValues, mReminderMinuteLabels,
                    mReminderMethodValues, mReminderMethodLabels,
                    ReminderEntry.valueOf(GeneralPreferences.REMINDER_DEFAULT_TIME),
                    mModel.mCalendarMaxReminders, null);
        } else {
            EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                    mReminderMinuteValues, mReminderMinuteLabels,
                    mReminderMethodValues, mReminderMethodLabels,
                    ReminderEntry.valueOf(mDefaultReminderMinutes),
                    mModel.mCalendarMaxReminders, null);
        }
        updateRemindersVisibility(mReminderItems.size());
        EventViewUtils.updateAddReminderButton(mView, mReminderItems, mModel.mCalendarMaxReminders);
    }

    // From com.google.android.gm.ComposeActivity
    private MultiAutoCompleteTextView initMultiAutoCompleteTextView(RecipientEditTextView list) {
        if (ChipsUtil.supportsChipsUi()) {
            mAddressAdapter = new RecipientAdapter(mActivity);
            list.setAdapter((BaseRecipientAdapter) mAddressAdapter);
            list.setOnFocusListShrinkRecipients(false);
        } else {
            mAddressAdapter = new EmailAddressAdapter(mActivity);
            list.setAdapter((EmailAddressAdapter)mAddressAdapter);
        }
        list.setTokenizer(new Rfc822Tokenizer());
        list.setValidator(mEmailValidator);

        // NOTE: assumes no other filters are set
        list.setFilters(sRecipientFilters);

        return list;
    }

    /**
     * From com.google.android.gm.ComposeActivity Implements special address
     * cleanup rules: The first space key entry following an "@" symbol that is
     * followed by any combination of letters and symbols, including one+ dots
     * and zero commas, should insert an extra comma (followed by the space).
     */
    private static InputFilter[] sRecipientFilters = new InputFilter[] { new Rfc822InputFilter() };

    private void setDate(TextView view, long millis) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH
                | DateUtils.FORMAT_ABBREV_WEEKDAY;

        // Unfortunately, DateUtils doesn't support a timezone other than the
        // default timezone provided by the system, so we have this ugly hack
        // here to trick it into formatting our time correctly. In order to
        // prevent all sorts of craziness, we synchronize on the TimeZone class
        // to prevent other threads from reading an incorrect timezone from
        // calls to TimeZone#getDefault()
        // TODO fix this if/when DateUtils allows for passing in a timezone
        String dateString = null;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(mTimezone));
            ///M: #extension# the date string is extended
            ///TODO: the whole "setDate" should be extended @{
            dateString = mExt.getDateStringFromMillis(millis);
            ///@}
            if (TextUtils.isEmpty(dateString)) {
                dateString = DateUtils.formatDateTime(mActivity, millis, flags);
            }
            // setting the default back to null restores the correct behavior
            TimeZone.setDefault(null);
        }
        view.setText(dateString);
    }

    private void setTime(TextView view, long millis) {
        int flags = DateUtils.FORMAT_SHOW_TIME;
        flags |= DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
        if (DateFormat.is24HourFormat(mActivity)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }

        // Unfortunately, DateUtils doesn't support a timezone other than the
        // default timezone provided by the system, so we have this ugly hack
        // here to trick it into formatting our time correctly. In order to
        // prevent all sorts of craziness, we synchronize on the TimeZone class
        // to prevent other threads from reading an incorrect timezone from
        // calls to TimeZone#getDefault()
        // TODO fix this if/when DateUtils allows for passing in a timezone
        String timeString;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(mTimezone));
            timeString = DateUtils.formatDateTime(mActivity, millis, flags);
            TimeZone.setDefault(null);
        }
        view.setText(timeString);
    }

    /**
     * @param isChecked
     */
    protected void setAllDayViewsVisibility(boolean isChecked) {
        if (isChecked) {
            if (mEndTime.hour == 0 && mEndTime.minute == 0) {
                if (mAllDay != isChecked) {
                    mEndTime.monthDay--;
                }

                long endMillis = mEndTime.normalize(true);

                // Do not allow an event to have an end time
                // before the
                // start time.
                if (mEndTime.before(mStartTime)) {
                    mEndTime.set(mStartTime);
                    endMillis = mEndTime.normalize(true);
                }
                setDate(mEndDateButton, endMillis);
                setTime(mEndTimeButton, endMillis);
            }

            mStartTimeButton.setVisibility(View.GONE);
            mEndTimeButton.setVisibility(View.GONE);
            mTimezoneRow.setVisibility(View.GONE);
        } else {
            if (mEndTime.hour == 0 && mEndTime.minute == 0) {
                if (mAllDay != isChecked) {
                    mEndTime.monthDay++;
                }

                long endMillis = mEndTime.normalize(true);
                setDate(mEndDateButton, endMillis);
                setTime(mEndTimeButton, endMillis);
            }
            mStartTimeButton.setVisibility(View.VISIBLE);
            mEndTimeButton.setVisibility(View.VISIBLE);
            mTimezoneRow.setVisibility(View.VISIBLE);
        }

        // If this is a new event, and if availability has not yet been
        // explicitly set, toggle busy/available as the inverse of all day.
        if (mModel.mUri == null && !mAvailabilityExplicitlySet) {
            // Values are from R.arrays.availability_values.
            // 0 = busy
            // 1 = available
            int newAvailabilityValue = isChecked? 1 : 0;
            if (mAvailabilityAdapter != null && mAvailabilityValues != null
                    && mAvailabilityValues.contains(newAvailabilityValue)) {
                // We'll need to let the spinner's listener know that we're
                // explicitly toggling it.
                mAllDayChangingAvailability = true;

                String newAvailabilityLabel = mOriginalAvailabilityLabels.get(newAvailabilityValue);
                int newAvailabilityPos = mAvailabilityAdapter.getPosition(newAvailabilityLabel);
                mAvailabilitySpinner.setSelection(newAvailabilityPos);
            }
        }

        mAllDay = isChecked;
        updateHomeTime();
    }

    public void setColorPickerButtonStates(int[] colorArray) {
        setColorPickerButtonStates(colorArray != null && colorArray.length > 0);
    }

    public void setColorPickerButtonStates(boolean showColorPalette) {
        if (showColorPalette) {
            mColorPickerNewEvent.setVisibility(View.VISIBLE);
            mColorPickerExistingEvent.setVisibility(View.VISIBLE);
        } else {
            /// M: if event color can not be edited, make it gone
            mColorPickerNewEvent.setVisibility(View.GONE);
            mColorPickerExistingEvent.setVisibility(View.GONE);
        }
    }

    public boolean isColorPaletteVisible() {
        return mColorPickerNewEvent.getVisibility() == View.VISIBLE ||
                mColorPickerExistingEvent.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // This is only used for the Calendar spinner in new events, and only fires when the
        // calendar selection changes or on screen rotation
        Cursor c = (Cursor) parent.getItemAtPosition(position);
        if (c == null) {
            // TODO: can this happen? should we drop this check?
            Log.w(TAG, "Cursor not set on calendar item");
            return;
        }

        // Do nothing if the selection didn't change so that reminders will not get lost
        int idColumn = c.getColumnIndexOrThrow(Calendars._ID);
        long calendarId = c.getLong(idColumn);
        int colorColumn = c.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
        int color = c.getInt(colorColumn);
        int displayColor = Utils.getDisplayColorFromColor(color);

        // Prevents resetting of data (reminders, etc.) on orientation change.
        if (calendarId == mModel.mCalendarId && mModel.isCalendarColorInitialized() &&
                displayColor == mModel.getCalendarColor()) {
            return;
        }

        setSpinnerBackgroundColor(displayColor);

        mModel.mCalendarId = calendarId;
        mModel.setCalendarColor(displayColor);
        mModel.mCalendarAccountName = c.getString(EditEventHelper.CALENDARS_INDEX_ACCOUNT_NAME);
        mModel.mCalendarAccountType = c.getString(EditEventHelper.CALENDARS_INDEX_ACCOUNT_TYPE);
        mModel.setEventColor(mModel.getCalendarColor());

        setColorPickerButtonStates(mModel.getCalendarEventColors());

        // Update the max/allowed reminders with the new calendar properties.
        int maxRemindersColumn = c.getColumnIndexOrThrow(Calendars.MAX_REMINDERS);
        mModel.mCalendarMaxReminders = c.getInt(maxRemindersColumn);
        int allowedRemindersColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_REMINDERS);
        mModel.mCalendarAllowedReminders = c.getString(allowedRemindersColumn);
        int allowedAttendeeTypesColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_ATTENDEE_TYPES);
        mModel.mCalendarAllowedAttendeeTypes = c.getString(allowedAttendeeTypesColumn);
        int allowedAvailabilityColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_AVAILABILITY);
        mModel.mCalendarAllowedAvailability = c.getString(allowedAvailabilityColumn);

        // Discard the current reminders and replace them with the model's default reminder set.
        // We could attempt to save & restore the reminders that have been added, but that's
        // probably more trouble than it's worth.
        mModel.mReminders.clear();
        mModel.mReminders.addAll(mModel.mDefaultReminders);
        mModel.mHasAlarm = mModel.mReminders.size() != 0;

        // Update the UI elements.
        mReminderItems.clear();
        LinearLayout reminderLayout =
            (LinearLayout) mScrollView.findViewById(R.id.reminder_items_container);
        reminderLayout.removeAllViews();
        prepareReminders();
        prepareAvailability();

        ///M: #extension# #PC Sync# @{
        mExt.onAccountItemSelected(c);
        ///@}
    }

    /**
     * Checks if the start and end times for this event should be displayed in
     * the Calendar app's time zone as well and formats and displays them.
     */
    private void updateHomeTime() {
        String tz = Utils.getTimeZone(mActivity, null);
        if (!mAllDayCheckBox.isChecked() && !TextUtils.equals(tz, mTimezone)
                && mModification != EditEventHelper.MODIFY_UNINITIALIZED) {
            int flags = DateUtils.FORMAT_SHOW_TIME;
            boolean is24Format = DateFormat.is24HourFormat(mActivity);
            if (is24Format) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            long millisStart = mStartTime.toMillis(false);
            long millisEnd = mEndTime.toMillis(false);

            boolean isDSTStart = mStartTime.isDst != 0;
            boolean isDSTEnd = mEndTime.isDst != 0;

            // First update the start date and times
            String tzDisplay = TimeZone.getTimeZone(tz).getDisplayName(
                    isDSTStart, TimeZone.LONG, Locale.getDefault());
            StringBuilder time = new StringBuilder();

            sStringBuilder.setLength(0);
            time.append(DateUtils
                    .formatDateRange(mActivity, sFormatter, millisStart, millisStart, flags, tz))
                    .append(" ").append(tzDisplay);
            mStartTimeHome.setText(time.toString());

            flags = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY;
            sStringBuilder.setLength(0);
            mStartDateHome
                    .setText(DateUtils.formatDateRange(
                            mActivity, sFormatter, millisStart, millisStart, flags, tz).toString());

            // Make any adjustments needed for the end times
            if (isDSTEnd != isDSTStart) {
                tzDisplay = TimeZone.getTimeZone(tz).getDisplayName(
                        isDSTEnd, TimeZone.LONG, Locale.getDefault());
            }
            flags = DateUtils.FORMAT_SHOW_TIME;
            if (is24Format) {
                flags |= DateUtils.FORMAT_24HOUR;
            }

            // Then update the end times
            time.setLength(0);
            sStringBuilder.setLength(0);
            time.append(DateUtils.formatDateRange(
                    mActivity, sFormatter, millisEnd, millisEnd, flags, tz)).append(" ").append(tzDisplay);
            mEndTimeHome.setText(time.toString());

            flags = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY;
            sStringBuilder.setLength(0);
            mEndDateHome.setText(DateUtils.formatDateRange(
                            mActivity, sFormatter, millisEnd, millisEnd, flags, tz).toString());

            mStartHomeGroup.setVisibility(View.VISIBLE);
            mEndHomeGroup.setVisibility(View.VISIBLE);
        } else {
            mStartHomeGroup.setVisibility(View.GONE);
            mEndHomeGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

//////////////////////////////////////////////////////////////////////////
///M: MTK code below

    ///M: the extension of this view @{
    private IEditEventViewExt mExt;
    ///@}

    @Override
    public void resetDateButton() {
        setDate(mStartDateButton, mStartTime.toMillis(false /* use isDst */));
        setDate(mEndDateButton, mEndTime.toMillis(false /* use isDst */));
    }

    @Override
    public CalendarEventModel getModel() {
        return mModel;
    }

    //TODO: MTK should remove it?
    @Override
    public android.app.DatePickerDialog.OnDateSetListener getOnDateSetListener(View view) {
        return new DateListener(view);
    }

    @Override
    public void setAttendeesGroupVisibility(int visible) {
        mAttendeesGroup.setVisibility(visible);
    }

    /**
     * M: View is not under the control of Fragment's life cycle. so if we want
     * to do some thing in the life cycle, we have to define a method like this
     * to handle the requirement. for example, the language changed unexpectedly.
     */
    public void doOnResume(long calendarId) {
        mExt.updateDatePickerSelection();
        /**
         * M: When the main thread received the query result, the
         * activity/fragment may be paused sometimes, the no account dialog
         * would not be shown, and then the activity/fragment resumed again, the
         * no account dialog would not show again no longer. In order to avoid
         * the case described above, it should do some following workaround.
         */
        if (mCalendarsCursor != null && mCalendarsCursor.getCount() == 0
                && mNoCalendarsDialog == null) {
            setCalendarsCursor(mCalendarsCursor, true, calendarId);
        }
    }

    ///M:Max Input length @{
    /**
     * M: Because of overlapping display, limit the max length of title and
     * location from 2000 to 1600. It should be to note the overlapping display
     * phenomenon only occurs on single line text view.
     */
    private static final int TITLE_MAX_LENGTH = 1600;
    private static final int LOCATION_MAX_LENGTH = 1600;
    private static final int DESCRIPTION_MAX_LENGTH = 10000;
    ///@}

    ///M: @{
    /**
     * DialogManager is used to manage all potential dialogs of this
     * EditEventView It remembered whether there exists a dialog. Use it to
     * determine whether a dialog can be shown.
     */
    private class DialogManager implements DialogInterface.OnDismissListener,
            DialogInterface.OnShowListener {

        private boolean mIsAnyDialogShown = false;

        public boolean isAnyDialogShown() {
            return mIsAnyDialogShown;
        }

        public void dialogShown() {
            if (isAnyDialogShown()) {
                LogUtil.w(TAG,
                        "There is already a dialog shown, but another dialog is "
                                + "going to show.");
            }
            mIsAnyDialogShown = true;
        }

        private void dialogDismissed() {
            if (!isAnyDialogShown()) {
                LogUtil.w(TAG,
                        "There is no dialog shown, but some dialog dismissed.");
            }
            mIsAnyDialogShown = false;

            LogUtil.d(DATE_TIME_TAG, "dialog dismissed.");
            setDateTimeViewId(ID_INVALID);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            dialogDismissed();
        }

        @Override
        public void onShow(DialogInterface dialog) {
            dialogShown();
        }
    }
    /// @}

    /**
     * Checks whether a string attendee address has invalid address.
     * M: change from EditEventHeper.getAddressesFromList()
     */
    private boolean isHasInvalidAddress(String list, Rfc822Validator validator) {
        LinkedHashSet<Rfc822Token> addresses = new LinkedHashSet<Rfc822Token>();
        Rfc822Tokenizer.tokenize(list, addresses);
        if (validator == null) {
            return false;
        }

        boolean isHasInvalidAttendee = false;
        Iterator<Rfc822Token> addressIterator = addresses.iterator();
        while (addressIterator.hasNext()) {
            Rfc822Token address = addressIterator.next();
            if (!validator.isValid(address.getAddress())) {
                isHasInvalidAttendee = true;
            }
        }
        return isHasInvalidAttendee;
    }

    ///M:#Guest#@{
    private void setAttendeesGroupVisibility(String accountType) {
        if (ACCOUNT_TYPE_LOCAL.equals(accountType)) {
            mAttendeesGroup.setVisibility(View.GONE);
        } else {
            mAttendeesGroup.setVisibility(View.VISIBLE);
        }
    }
    ///@}
    private static final String ACCOUNT_TYPE_LOCAL = "local";

    ///M: keep DatePickerDialog and TimePickerDialog when rotate device @{
    private static final String DATE_TIME_TAG = TAG + "::date_time_debug_tag";

    // ids of the date & time buttons
    private static final int ID_START_DATE = 1;
    private static final int ID_END_DATE = 2;
    private static final int ID_START_TIME = 3;
    private static final int ID_END_TIME = 4;
    static final int ID_INVALID = 0;

    ////////////////////////////////////////////////////////////////////////////
    // these three methods expose dialog manager functionalities
    boolean isAnyDialogShown() {
        LogUtil.d(DATE_TIME_TAG, "isAnyDialogShown()");

        return mDialogManager.isAnyDialogShown();
    }

    void setDialogShown() {
        LogUtil.d(DATE_TIME_TAG, "getDateTimeOnDismissListener()");

        mDialogManager.onShow(null);
    }

    // when a dialog is dismissed, this MUST be called to reset the
    // dialog manager to correct state
    OnDismissListener getDateTimeOnDismissListener() {
        LogUtil.d(DATE_TIME_TAG, "getDateTimeOnDismissListener()");

        return mDialogManager;
    }
    ////////////////////////////////////////////////////////////////////////////

    /*
     * Set identifier in EditEventActivity to hold which button is clicked. Then
     * we can restore the dialog correctly.
     */
    private void setDateTimeViewId(int id) {
        if (mActivity instanceof EditEventActivity) {
            LogUtil.d(DATE_TIME_TAG, "setDateTimeViewId(), id: " + id);

            EditEventActivity eea = (EditEventActivity) mActivity;
            eea.setDateTimeViewId(id);
        }
    }

    android.app.TimePickerDialog.OnTimeSetListener getDateTimeOnTimeSetListener(View v) {
        return new TimeListener(v);
    }

    /**
     * Get the View identified by id.
     * 
     * @param id
     *            identifier of the view
     * @return a View if exists, null otherwise
     */
    View getDateTimeView(int id) {
        LogUtil.d(DATE_TIME_TAG, "getDateTimeView(), id: " + id);

        if (id == ID_START_DATE) {
            return mStartDateButton;
        }
        if (id == ID_END_DATE) {
            return mEndDateButton;
        }
        if (id == ID_START_TIME) {
            return mStartTimeButton;
        }
        if (id == ID_END_TIME) {
            return mEndTimeButton;
        }

        return null;
    }
    ///@}

    /**
     * M: adjust some field of EventRecurrence according to the new start and
     * end date.
     */
    private static void recalculateEventRecurrenceRule(Time oldStartDate, Time startDate, Time endDate, EventRecurrence er) {
        // adjust the field until to valid value.
        if (er.until != null) {
            Time t = new Time(endDate);
            t.hour = 0;
            t.minute = 0;
            t.second = 0;
            long newEnd = t.normalize(false);

            t.parse(er.until);
            long oldEnd = t.normalize(false);
            if (newEnd > oldEnd) {
                Time newUntil = new Time(startDate);

                switch (er.freq) {
                case EventRecurrence.DAILY:
                case EventRecurrence.WEEKLY:
                    newUntil.month += 1 ;
                    break;
                case EventRecurrence.MONTHLY:
                    newUntil.month += 3;
                    break;
                case EventRecurrence.YEARLY:
                    newUntil.year += 3;
                    break;
                default:
                    break;
                }
                newUntil.switchTimezone(Time.TIMEZONE_UTC);
                newUntil.normalize(false);
                er.until = newUntil.format2445();
                er.count = 0;
            }
        }

        int count = 0;
        switch (er.freq) {
        case EventRecurrence.WEEKLY:
            boolean[] weeklyByDayOfWeek = new boolean[7];
            Arrays.fill(weeklyByDayOfWeek, false);
            for (int i = 0; i < er.bydayCount; i++) {
                int dayOfWeek = EventRecurrence.day2TimeDay(er.byday[i]);
                weeklyByDayOfWeek[dayOfWeek] = true;
            }
            weeklyByDayOfWeek[oldStartDate.weekDay] = false;
            weeklyByDayOfWeek[startDate.weekDay] = true;

            count = 0;
            for (int i = 0; i < 7; i++) {
                if (weeklyByDayOfWeek[i]) {
                    count++;
                }
            }
            if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                er.byday = new int[count];
                er.bydayNum = new int[count];
            }
            er.bydayCount = count;

            for (int i = 6; i >= 0; i--) {
                if (weeklyByDayOfWeek[i]) {
                    er.bydayNum[--count] = 0;
                    er.byday[count] = EventRecurrence.timeDay2Day(i);
                }
            }
            break;
        case EventRecurrence.MONTHLY:
            if (er.bymonthCount > 0 && er.bydayCount > 0) {
                LogUtil.d(TAG, "some error accourred, er.bymonthCount="
                        + er.bymonthCount + ", er.bydayCount=" + er.bydayCount);
                er.bydayCount = 0;
                er.byday = null;
                er.bydayNum = null;
                er.bymonthCount = 1;
            }
            if (er.bymonthCount > 0) {
                if (er.bymonthday == null || er.bymonthdayCount < 1) {
                    er.bymonthday = new int[1];
                }
                er.bymonthday[0] = startDate.monthDay;
                er.bymonthdayCount = 1;
            } else if (er.bydayCount > 0) {
                count = 1;
                if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                    er.byday = new int[count];
                    er.bydayNum = new int[count];
                }
                er.bydayCount = count;
                er.byday[0] = EventRecurrence.timeDay2Day(startDate.weekDay);
                er.bydayNum[0] = (startDate.monthDay + 6) / 7;
            }
            break;
        default:
            break;
        }
    }

    /**
     * M: default repeat rule string for daily, weekly, monthly and yearly.
     */
    private static final String DEFAULT_REPEAT_RULE_DAILY = "FREQ=DAILY;WKST=SU";
    private static final String DEFAULT_REPEAT_RULE_WEEKLY = "FREQ=WEEKLY;WKST=SU;BYDAY=";
    private static final String DEFAULT_REPEAT_RULE_MONTHLY = "FREQ=MONTHLY;WKST=SU";
    private static final String DEFAULT_REPEAT_RULE_YEARLY = "FREQ=YEARLY;WKST=SU";

    /**
     * M: get the default repeat rule string used to initialize the
     * EventRecurrence object.
     * @param frq
     * @param weekDay
     * @return
     */
    private static String getDefaultRruleByFrequecy(int frq, int weekDay) {
        switch (frq) {
        case EventRecurrence.DAILY:
            return DEFAULT_REPEAT_RULE_DAILY;
        case EventRecurrence.WEEKLY:
            return (DEFAULT_REPEAT_RULE_WEEKLY + weekday2String(frq));
        case EventRecurrence.MONTHLY:
            return DEFAULT_REPEAT_RULE_MONTHLY;
        case EventRecurrence.YEARLY:
            return DEFAULT_REPEAT_RULE_YEARLY;
        default:
            Log.w(TAG, "bad repeat frequecy  argument: " + frq);
            return DEFAULT_REPEAT_RULE_DAILY;
        }
    }

    /**
     * convert the weekday to string.
     * @param day day in week.
     * @return string
     */
    private static String weekday2String(int day) {
        switch (day) {
        case Time.SUNDAY:
            return "SU";
        case Time.MONDAY:
            return "MO";
        case Time.TUESDAY:
            return "TU";
        case Time.WEDNESDAY:
            return "WE";
        case Time.THURSDAY:
            return "TH";
        case Time.FRIDAY:
            return "FR";
        case Time.SATURDAY:
            return "SA";
        default:
            Log.w(TAG, "bad weekday argument: " + day);
            return "SU";
        }
    }
}
