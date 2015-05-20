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
/*12/24/13   |Yuansheng.Zhao   |FR556557-Yuansheng-Zhao-001 |The week number is displayed according to this setting.*/
/*================================================================================================*/
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

package com.android.calendar;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import android.view.Menu;
import android.view.MenuItem;
//yuanding add it for PR:651124 20140418 start
import android.view.Surface;
//yuanding add it for PR:651124 20140418 end
import android.view.View;
import android.view.KeyEvent;//added by Fujuan.Lin for RR565704
import android.view.accessibility.AccessibilityEvent;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;

import com.android.calendar.CalendarController.EventHandler;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.extensions.AllInOneMenuExtensions;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import com.android.calendar.alerts.AlertUtils;
import com.android.calendar.bottombar.BottomBar;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import com.android.calendar.bottombar.BottomBar.TabListener;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import com.android.calendar.bottombar.BottomBarImpl;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import com.android.calendar.month.MonthByWeekFragment;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsFragment;
import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.PDebug;
import com.mediatek.calendar.GoToDatePickerDialogFragment;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.IOptionsMenuExt;

import com.jrdcom.calendar.FBAuthorizeActivity;//added by Fujuan.Lin for RR565704
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import com.android.calendar.years.YearsFragment;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13

import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

/// M: implement OnDateSetListener to listen date set
public class AllInOneActivity extends AbstractCalendarActivity implements EventHandler,OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener,
        ActionBar.OnNavigationListener, OnSuggestionListener, TabListener {//modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
    private static final String TAG = "AllInOneActivity";
    private static final boolean DEBUG = false;
    private static final String EVENT_INFO_FRAGMENT_TAG = "EventInfoFragment";
    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    private static final String BUNDLE_KEY_RESTORE_VIEW = "key_restore_view";
    private static final String BUNDLE_KEY_CHECK_ACCOUNTS = "key_check_for_accounts";
    /// M: bundle key for "mIsInSearchMode" @{
    private static final String BUNDLE_KEY_IS_IN_SEARCH_MODE = "key_search_mode";
    private static final String BUNDLE_KEY_SEARCH_STRING = "key_search_string";
    // @}
    private static final int HANDLER_KEY = 0;
    private static float mScale = 0;

    // Indices of buttons for the drop down menu (tabs replacement)
    // Must match the strings in the array buttons_list in arrays.xml and the
    // OnNavigationListener
    private static final int BUTTON_DAY_INDEX = 0;
    private static final int BUTTON_WEEK_INDEX = 1;
    private static final int BUTTON_MONTH_INDEX = 2;
    private static final int BUTTON_AGENDA_INDEX = 3;

    private CalendarController mController;
    private static boolean mIsMultipane;
    private static boolean mIsTabletConfig;
    private static boolean mShowAgendaWithMonth;
    private static boolean mShowEventDetailsWithAgenda;
    private boolean mOnSaveInstanceStateCalled = false;
    private boolean mBackToPreviousView = false;
    private ContentResolver mContentResolver;
    private int mPreviousView;
    private int mCurrentView;
    private boolean mPaused = true;
    private boolean mUpdateOnResume = false;
    private boolean mHideControls = false;
    private boolean mShowSideViews = true;
    private boolean mShowWeekNum = false;
    private TextView mHomeTime;
    private TextView mDateRange;
    private TextView mWeekTextView;
    private View mMiniMonth;
    private View mCalendarsList;
    private View mMiniMonthContainer;
    private View mSecondaryPane;
    private String mTimeZone;
    private boolean mShowCalendarControls;
    private boolean mShowEventInfoFullScreenAgenda;
    private boolean mShowEventInfoFullScreen;
    private int mWeekNum;
    private int mCalendarControlsAnimationTime;
    private int mControlsAnimateWidth;
    private int mControlsAnimateHeight;

    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private int mIntentAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
    private boolean mIntentAllDay = false;
    /// M: flag indicate whether in search mode @{
    private boolean mIsInSearchMode = false;
    private String mSearchString = null;
    // @}
    //modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
    // Action bar and Navigation bar (left side of Action bar)
    private ActionBar mActionBar;
    //private ActionBar.Tab mDayTab;
    //private ActionBar.Tab mWeekTab;
    //private ActionBar.Tab mMonthTab;
    //private ActionBar.Tab mAgendaTab;
    private BottomBar mBottomBar;
    private BottomBar.Tab mWeekTab;
    private BottomBar.Tab mMonthTab;
    private BottomBar.Tab mAgendaTab;
    //modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
    private SearchView mSearchView;
    private MenuItem mSearchMenu;
    private MenuItem mControlsMenu;
    private Menu mOptionsMenu;
    //private CalendarViewAdapter mActionBarMenuSpinnerAdapter;//deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
    private QueryHandler mHandler;
    private boolean mCheckForAccounts = true;
	//yuanding add it for PR:671615  20140513 start
    private int EVENT_EXTRALONG1=2;
	private int EVENT_EXTRALONG2=52;
	private int EVENT_EXTRALONG3=20;
    //yuanding add it for PR:671615  20140513 end
    private String mHideString;
    private String mShowString;
    //yuanding add it for PR:651124 20140418 start
    private int rotation = 0;
    public static boolean horizontalScreen = false;
	//yuanding add it for PR:651124 20140418 end
    DayOfMonthDrawable mDayOfMonthIcon;

    int mOrientation;

    CustomActionBarView mActionBarView;//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13

    // Params for animating the controls on the right
    private LayoutParams mControlsParams;
    private LinearLayout.LayoutParams mVerticalControlsParams;

    private AllInOneMenuExtensionsInterface mExtensions = ExtensionsFactory
            .getAllInOneMenuExtensions();

    private final AnimatorListener mSlideAnimationDoneListener = new AnimatorListener() {

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(android.animation.Animator animation) {
            int visibility = mShowSideViews ? View.VISIBLE : View.GONE;
            mMiniMonth.setVisibility(visibility);
            mCalendarsList.setVisibility(visibility);
            mMiniMonthContainer.setVisibility(visibility);
        }

        @Override
        public void onAnimationRepeat(android.animation.Animator animation) {
        }

        @Override
        public void onAnimationStart(android.animation.Animator animation) {
        }
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mCheckForAccounts = false;
            try {
                // If the query didn't return a cursor for some reason return
                if (cursor == null || cursor.getCount() > 0 || isFinishing()) {
                    return;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Bundle options = new Bundle();
            options.putCharSequence("introMessage",
                    getResources().getString(R.string.create_an_account_desc));
            options.putBoolean("allowSkip", true);

            AccountManager am = AccountManager.get(AllInOneActivity.this);
            am.addAccount("com.google", CalendarContract.AUTHORITY, null, options,
                    AllInOneActivity.this,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            if (future.isCancelled()) {
                                return;
                            }
                            try {
                                Bundle result = future.getResult();
                                boolean setupSkipped = result.getBoolean("setupSkipped");

                                if (setupSkipped) {
                                    Utils.setSharedPreference(AllInOneActivity.this,
                                            GeneralPreferences.KEY_SKIP_SETUP, true);
                                }

                            } catch (OperationCanceledException ignore) {
                                // The account creation process was canceled
                            } catch (IOException ignore) {
                            } catch (AuthenticatorException ignore) {
                            }
                        }
                    }, null);
        }
    }

    private final Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            updateSecondaryTitleFields(-1);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        }
    };

    // runs every midnight/time changes and refreshes the today icon
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        }
    };


    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };

    BroadcastReceiver mCalIntentReceiver;

    /**
     * M: the options menu extension
     */
    private IOptionsMenuExt mOptionsMenuExt;

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (DEBUG)
            Log.d(TAG, "New intent received " + intent.toString());
        // Don't change the date if we're just returning to the app's home
        if (Intent.ACTION_VIEW.equals(action)
                && !intent.getBooleanExtra(Utils.INTENT_KEY_HOME, false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                millis = Utils.timeFromIntentInMillis(intent);
            }
            if (millis != -1 && mViewEventId == -1 && mController != null) {
                Time time = new Time(mTimeZone);
                time.set(millis);
                time.normalize(true);
                mController.sendEvent(this, EventType.GO_TO, time, time, -1, ViewType.CURRENT);
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        PDebug.Start("AllInOneActivity.onCreate");

        PDebug.Start("AllInOneActivity.onCreate.superOnCreate");
        if (Utils.getSharedPreference(this, OtherPreferences.KEY_OTHER_1, false)) {
            setTheme(R.style.CalendarTheme_WithActionBarWallpaper);
        }
        super.onCreate(icicle);
        PDebug.EndAndStart("AllInOneActivity.onCreate.superOnCreate", "AllInOneActivity.onCreate.restoreState");
        //yuanding add it for PR:651124 20140418 start
        rotation = getDisplayRotation(this);
     
        if (rotation == 90 || rotation == 270) {
        	horizontalScreen =true;
        } else {
        	horizontalScreen = false;
        }
         //yuanding add it for PR:651124 20140418 end
        /**
         * M: we should restore search state while we still in search mode @{
         */
        if (icicle != null) {
            if (icicle.containsKey(BUNDLE_KEY_CHECK_ACCOUNTS)) {
                mCheckForAccounts = icicle.getBoolean(BUNDLE_KEY_CHECK_ACCOUNTS);
            }
            if (icicle.containsKey(BUNDLE_KEY_IS_IN_SEARCH_MODE)) {
                mIsInSearchMode = icicle.getBoolean(BUNDLE_KEY_IS_IN_SEARCH_MODE, false);
            }
            if (icicle.containsKey(BUNDLE_KEY_SEARCH_STRING)) {
                mSearchString = icicle.getString(BUNDLE_KEY_SEARCH_STRING, null);
            }
        }
        /** @} */
        // Launch add google account if this is first time and there are no
        // accounts yet
        if (mCheckForAccounts
                && !Utils.getSharedPreference(this, GeneralPreferences.KEY_SKIP_SETUP, false)) {

            mHandler = new QueryHandler(this.getContentResolver());
            mHandler.startQuery(0, null, Calendars.CONTENT_URI, new String[] {
                Calendars._ID
            }, null, null /* selection args */, null /* sort order */);
        }

        // This needs to be created before setContentView
        mController = CalendarController.getInstance(this);


        // Get time from intent or icicle
        long timeMillis = -1;
        int viewType = -1;
        final Intent intent = getIntent();
        if (icicle != null) {
            timeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            viewType = icicle.getInt(BUNDLE_KEY_RESTORE_VIEW, -1);
        } else {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                // Open EventInfo later
                timeMillis = parseViewAction(intent);
            }

            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
            //FR593012-Wentao-Wan-001 begin
              if("android.intent.action.CALENDARWIDGET".equals(action)) {
                  viewType = ViewType.AGENDA;
              } else if("android.intent.action.CALENDARWIDGET.month".equals(action)) {
                  viewType = ViewType.MONTH;
              }
            //FR593012-Wentao-Wan-001 end
        }

        if (viewType == -1 || viewType > ViewType.MAX_VALUE) {
            viewType = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        Time t = new Time(mTimeZone);
        t.set(timeMillis);

        if (DEBUG) {
            if (icicle != null && intent != null) {
                Log.d(TAG, "both, icicle:" + icicle.toString() + "  intent:" + intent.toString());
            } else {
                Log.d(TAG, "not both, icicle:" + icicle + " intent:" + intent);
            }
        }
        PDebug.EndAndStart("AllInOneActivity.onCreate.restoreState", "AllInOneActivity.onCreate.initVariables");

        Resources res = getResources();
        mHideString = res.getString(R.string.hide_controls);
        mShowString = res.getString(R.string.show_controls);
        mOrientation = res.getConfiguration().orientation;
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mControlsAnimateWidth = (int)res.getDimension(R.dimen.calendar_controls_width);
            if (mControlsParams == null) {
                mControlsParams = new LayoutParams(mControlsAnimateWidth, 0);
            }
            mControlsParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            // Make sure width is in between allowed min and max width values
            mControlsAnimateWidth = Math.max(res.getDisplayMetrics().widthPixels * 45 / 100,
                    (int)res.getDimension(R.dimen.min_portrait_calendar_controls_width));
            mControlsAnimateWidth = Math.min(mControlsAnimateWidth,
                    (int)res.getDimension(R.dimen.max_portrait_calendar_controls_width));
        }

        mControlsAnimateHeight = (int)res.getDimension(R.dimen.calendar_controls_height);
       //yuanding modify it for PR:635523 20140505 start
        mHideControls = !Utils.getSharedPreference(
                this, GeneralPreferences.KEY_SHOW_CONTROLS, false);
	   //yuanding modify it for PR:635523 20140505 end
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mIsTabletConfig = Utils.getConfigBool(this, R.bool.tablet_config);
        mShowAgendaWithMonth = Utils.getConfigBool(this, R.bool.show_agenda_with_month);
        mShowCalendarControls =
                Utils.getConfigBool(this, R.bool.show_calendar_controls);
        mShowEventDetailsWithAgenda =
            Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        mShowEventInfoFullScreenAgenda =
            Utils.getConfigBool(this, R.bool.agenda_show_event_info_full_screen);
        mShowEventInfoFullScreen =
            Utils.getConfigBool(this, R.bool.show_event_info_full_screen);
        mCalendarControlsAnimationTime = res.getInteger(R.integer.calendar_controls_animation_time);
        Utils.setAllowWeekForDetailView(mIsMultipane);
        PDebug.EndAndStart("AllInOneActivity.onCreate.initVariables", "AllInOneActivity.onCreate.setContentView");

        // setContentView must be called before configureActionBar
        setContentView(R.layout.all_in_one);

        if (mIsTabletConfig) {
            mDateRange = (TextView) findViewById(R.id.date_bar);
            mWeekTextView = (TextView) findViewById(R.id.week_num);
        } else {
            mDateRange = (TextView) getLayoutInflater().inflate(R.layout.date_range_title, null);
        }
        PDebug.EndAndStart("AllInOneActivity.onCreate.setContentView", "AllInOneActivity.onCreate.configureActionBar");

        // configureActionBar auto-selects the first tab you add, so we need to
        // call it before we set up our own fragments to make sure it doesn't
        // overwrite us
        configureActionBar(viewType);
        PDebug.EndAndStart("AllInOneActivity.onCreate.configureActionBar", "AllInOneActivity.onCreate.getViews");

        mHomeTime = (TextView) findViewById(R.id.home_time);
        mMiniMonth = findViewById(R.id.mini_month);
        if (mIsTabletConfig && mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mMiniMonth.setLayoutParams(new RelativeLayout.LayoutParams(mControlsAnimateWidth,
                    mControlsAnimateHeight));
        }
        mCalendarsList = findViewById(R.id.calendar_list);
        mMiniMonthContainer = findViewById(R.id.mini_month_container);
        mSecondaryPane = findViewById(R.id.secondary_pane);

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);
        PDebug.End("AllInOneActivity.onCreate.getViews");

        initFragments(timeMillis, viewType, icicle);

        // Listen for changes that would require this to be refreshed
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mContentResolver = getContentResolver();

        ///M: the option menu extension @{
        mOptionsMenuExt = ExtensionFactory.getAllInOneOptionMenuExt(this);
        ///@}

        PDebug.EndAndStart("AllInOneActivity.onCreate", "AllInOneActivity.onCreate->DayFragment.onCreate");
    }

//yuanding add it for PR:651124 20140418 start
    private int getDisplayRotation(Activity activity) {  
        if(activity == null)  
            return 0;  
          
        int rotation = activity.getWindowManager().getDefaultDisplay()  
                .getRotation();  
        switch (rotation) {  
        case Surface.ROTATION_0:  
            return 0;  
        case Surface.ROTATION_90:  
            return 90;  
        case Surface.ROTATION_180:  
            return 180;  
        case Surface.ROTATION_270:  
            return 270;  
        }  
        return 0;  
    }
//yuanding add it for PR:651124 20140418 end   

    private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.valueOf(data.getLastPathSegment());
                    if (mViewEventId != -1) {
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        mIntentAttendeeResponse = intent.getIntExtra(
                            ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);
                        mIntentAllDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);
                        timeMillis = mIntentEventStartMillis;
                    }
                } catch (NumberFormatException e) {
                    // Ignore if mViewEventId can't be parsed
                }
            }
        }
        return timeMillis;
    }

    //FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
    private void configureActionBar(int viewType) {
        //createButtonsSpinner(viewType, mIsTabletConfig);
        createTabs();
        mActionBar = getActionBar();
        if (mIsMultipane) {
            mActionBar.setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        } else {
            mActionBar.setDisplayOptions(0);
        }
    	//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM );
        android.app.ActionBar.LayoutParams lp =
                new android.app.ActionBar.LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT);

        mActionBarView = new CustomActionBarView(this);
        mActionBarView.setIntoYearListener(mClickYearsListener);
        mActionBarView.setSearchListener(mSearchListener);
		//hong.zhan add it for PR:661525 20140428 start
        if(this.getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE){
        	mActionBarView.setAddEventListener(mAddEventListener);	 
        }
		//hong.zhan add it for PR:661525 20140428 end
        mActionBar.setCustomView(mActionBarView.getCustomActionBarView(), lp);
    	//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
    }

	//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
    private void createTabs() {
        mBottomBar = getBottomBar(this);
        if (mBottomBar == null) {
            Log.w(TAG, "ActionBar is null.");
        } else {
            mMonthTab = mBottomBar.newTab();
            mMonthTab.setText(getString(R.string.month_view));
            mMonthTab.setTabListener(this);
            mBottomBar.addTab(mMonthTab);
            mWeekTab = mBottomBar.newTab();
            mWeekTab.setText(getString(R.string.week_view));
            mWeekTab.setTabListener(this);
            mBottomBar.addTab(mWeekTab);
            mAgendaTab = mBottomBar.newTab();
            mAgendaTab.setText(getString(R.string.agenda_view));
            mAgendaTab.setTabListener(this);
            mBottomBar.addTab(mAgendaTab);
        }
    }

	//deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
   /*private void createButtonsSpinner(int viewType, boolean tabletConfig) {
        // If tablet configuration , show spinner with no dates
        mActionBarMenuSpinnerAdapter = new CalendarViewAdapter (this, viewType, !tabletConfig);
        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(mActionBarMenuSpinnerAdapter, this);
        switch (viewType) {
            case ViewType.AGENDA:
                mActionBar.setSelectedNavigationItem(BUTTON_AGENDA_INDEX);
                break;
            case ViewType.DAY:
                mActionBar.setSelectedNavigationItem(BUTTON_DAY_INDEX);
                break;
            case ViewType.WEEK:
                mActionBar.setSelectedNavigationItem(BUTTON_WEEK_INDEX);
                break;
            case ViewType.MONTH:
                mActionBar.setSelectedNavigationItem(BUTTON_MONTH_INDEX);
                break;
            default:
                mActionBar.setSelectedNavigationItem(BUTTON_DAY_INDEX);
                break;
       }
    }*/
    //FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
    // Clear buttons used in the agenda view
    private void clearOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        MenuItem cancelItem = mOptionsMenu.findItem(R.id.action_cancel);
        if (cancelItem != null) {
            cancelItem.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        PDebug.EndAndStart("DayFragment.onCreateView->AllInOneActivity.onResume", "AllInOneActivity.onResume");

        PDebug.Start("AllInOneActivity.onResume.superOnResume");
        super.onResume();
        PDebug.EndAndStart("AllInOneActivity.onResume.superOnResume", "AllInOneActivity.onResume.updateTitle");

        ///M:#clear all events# to update the view. @{
        if (mOnSaveInstanceStateCalled && mController != null && mIsClearEventsCompleted) {
            mIsClearEventsCompleted = false;
            switch (mCurrentView) {
            case ViewType.MONTH:// send event change event to update month
                                // view(261245).
                mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, mCurrentView);
                LogUtil.v(TAG, "After CLEAR EVENTS COMPLETED, send Event EVENTS_CHANGED.");
                break;
            }
        }
        ///@}

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        mOnSaveInstanceStateCalled = false;
        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
        if (mUpdateOnResume) {
            initFragments(mController.getTime(), mController.getViewType(), null);
            mUpdateOnResume = false;
        }
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);//update the timezone
        Time t = new Time(mTimeZone);
        t.set(mController.getTime());
        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, -1, ViewType.CURRENT,
                mController.getDateFlags(), null, null);
        // Make sure the drop-down menu will get its date updated at midnight
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        //if (mActionBarMenuSpinnerAdapter != null) {
        //    mActionBarMenuSpinnerAdapter.refresh(this);
        //}
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        if (mControlsMenu != null) {
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }
        mPaused = false;
        PDebug.EndAndStart("AllInOneActivity.onResume.updateTitle", "AllInOneActivity.onResume.invalidateOptionsMenu");

        if (mViewEventId != -1 && mIntentEventStartMillis != -1 && mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > mIntentEventStartMillis && currentMillis < mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            mController.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, mViewEventId,
                    mIntentEventStartMillis, mIntentEventEndMillis, -1, -1,
                    EventInfo.buildViewExtraLong(mIntentAttendeeResponse,mIntentAllDay),
                    selectedTime);
            mViewEventId = -1;
            mIntentEventStartMillis = -1;
            mIntentEventEndMillis = -1;
            mIntentAllDay = false;
        }
        Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        // Make sure the today icon is up to date
        invalidateOptionsMenu();
        PDebug.End("AllInOneActivity.onResume.invalidateOptionsMenu");

        mCalIntentReceiver = Utils.setTimeChangesReceiver(this, mTimeChangesUpdater);
        /**M: reset the listener for Goto date picker(use to do onDateSet after rotate.)*/
        resetGotoDateSetListener();

        PDebug.EndAndStart("AllInOneActivity.onResume", "AllInOneActivity.onResume->DayFragment.onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();

        mController.deregisterEventHandler(HANDLER_KEY);
        mPaused = true;
        mHomeTime.removeCallbacks(mHomeTimeUpdater);
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        //if (mActionBarMenuSpinnerAdapter != null) {
        //    mActionBarMenuSpinnerAdapter.onPause();
        //}
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        mContentResolver.unregisterContentObserver(mObserver);
        if (isFinishing()) {
            // Stop listening for changes that would require this to be refreshed
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        // FRAG_TODO save highlighted days of the week;
        if (mController.getViewType() != ViewType.EDIT) {
            Utils.setDefaultView(this, mController.getViewType());
        }
        Utils.resetMidnightUpdater(mHandler, mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, mCalIntentReceiver);
    }

    @Override
    protected void onUserLeaveHint() {
        mController.sendEvent(this, EventType.USER_HOME, null, null, -1, ViewType.CURRENT);
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putInt(BUNDLE_KEY_RESTORE_VIEW, mCurrentView);
        if (mCurrentView == ViewType.EDIT) {
            outState.putLong(BUNDLE_KEY_EVENT_ID, mController.getEventId());
        } else if (mCurrentView == ViewType.AGENDA) {
            FragmentManager fm = getFragmentManager();
            Fragment f = fm.findFragmentById(R.id.main_pane);
            if (f instanceof AgendaFragment) {
                outState.putLong(BUNDLE_KEY_EVENT_ID, ((AgendaFragment)f).getLastShowEventId());
            }
        }
        outState.putBoolean(BUNDLE_KEY_CHECK_ACCOUNTS, mCheckForAccounts);
        /**
         *  M: save search mode @{
         */
        if (mSearchMenu == null || !mSearchMenu.isActionViewExpanded()) {
            mIsInSearchMode = false;
        } else {
            mIsInSearchMode = true;
            mSearchString = (mSearchView != null) ? mSearchView.getQuery().toString() : null;
            outState.putString(BUNDLE_KEY_SEARCH_STRING, mSearchString);
        }
        outState.putBoolean(BUNDLE_KEY_IS_IN_SEARCH_MODE, mIsInSearchMode);
        /** @} */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        mController.deregisterAllEventHandlers();

        CalendarController.removeInstance(this);
    }

    private void initFragments(long timeMillis, int viewType, Bundle icicle) {
        PDebug.Start("AllInOneActivity.initFragments");

        if (DEBUG) {
            Log.d(TAG, "Initializing to " + timeMillis + " for view " + viewType);
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mShowCalendarControls) {
            //FR593012-Wentao-Wan-001 begin
            //Fragment miniMonthFrag = new MonthByWeekFragment(timeMillis, true);
            Fragment miniMonthFrag = new MonthByWeekFragment(timeMillis, false);
            //FR593012-Wentao-Wan-001 end
            ft.replace(R.id.mini_month, miniMonthFrag);
            mController.registerEventHandler(R.id.mini_month, (EventHandler) miniMonthFrag);

            Fragment selectCalendarsFrag = new SelectVisibleCalendarsFragment();
            ft.replace(R.id.calendar_list, selectCalendarsFrag);
            mController.registerEventHandler(
                    R.id.calendar_list, (EventHandler) selectCalendarsFrag);
        }
        if (!mShowCalendarControls || viewType == ViewType.EDIT) {
            mMiniMonth.setVisibility(View.GONE);
            mCalendarsList.setVisibility(View.GONE);
        }

        EventInfo info = null;
        if (viewType == ViewType.EDIT) {
            mPreviousView = GeneralPreferences.getSharedPreferences(this).getInt(
                    GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);

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

            long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
            long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
            info = new EventInfo();
            if (end != -1) {
                info.endTime = new Time();
                info.endTime.set(end);
            }
            if (begin != -1) {
                info.startTime = new Time();
                info.startTime.set(begin);
            }
            info.id = eventId;
            // We set the viewtype so if the user presses back when they are
            // done editing the controller knows we were in the Edit Event
            // screen. Likewise for eventId
            mController.setViewType(viewType);
            mController.setEventId(eventId);
        } else {
            ///M:If current view is same as the next view.don't change the mPreviousView
            ///so when prefers change,it can handle right  whether back to previous view or 
            ///finish when press back key.@{
            if (mCurrentView != viewType) {
                mPreviousView = viewType;
            } else {
                LogUtil.v(TAG, "don't modify mPreviousView's value.mCurrentView:" + mCurrentView + ",viewType:"
                        + viewType + ",mPreviousView:" + mPreviousView);
            }
            ///@}
           
        }

        setMainPane(ft, R.id.main_pane, viewType, timeMillis, true);
        ft.commit(); // this needs to be after setMainPane()

        Time t = new Time(mTimeZone);
        t.set(timeMillis);
        if (viewType == ViewType.AGENDA && icicle != null) {
            mController.sendEvent(this, EventType.GO_TO, t, null,
                    icicle.getLong(BUNDLE_KEY_EVENT_ID, -1), viewType);
        } else if (viewType != ViewType.EDIT) {
            mController.sendEvent(this, EventType.GO_TO, t, null, -1, viewType);
        }
        PDebug.End("AllInOneActivity.initFragments");
    }

    @Override
    public void onBackPressed() {
        if (mCurrentView == ViewType.EDIT || mBackToPreviousView) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, mPreviousView);
        } else {
            super.onBackPressed();
            Utils.clearBufferEX();//added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
        }
    }

    //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
    View.OnClickListener mSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                doSearch();
            }
    };
    
    //hong.zhan add it for PR:661525 20140428 start
    View.OnClickListener mAddEventListener=new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
				doAddEvent();
		}
	};
	
	
	void doAddEvent(){
		  Time t = null;
          int viewType = ViewType.CURRENT;
          long extras = CalendarController.EXTRA_GOTO_TIME;
          t = new Time();
          t.set(mController.getTime());
          t.second = 0;
          if (t.minute > 30) {
             t.hour++;
             t.minute = 0;
          } else if (t.minute > 0 && t.minute < 30) {
             t.minute = 30;
            }
          mController.sendEventRelatedEvent(this, EventType.CREATE_EVENT, -1, t.toMillis(true), 0, 0, 0, -1);
          mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
	}
	//hong.zhan add it for PR:661525 20140428 end
	
    void doSearch(){
        /**
         * M: If in search mode, enter @{
         */
        enterSearchMode();
        
        // Note: we should set search string after enterSearchMode(), because
        // enterSearchMode() will
        // set it to null
        if (mSearchView != null) {
            // restore search string to UI
            mSearchView.setQuery(mSearchString, false);
        }
    }
    //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.all_in_one_title_bar, menu);

        // Add additional options (if any).
        Integer extensionMenuRes = mExtensions.getExtensionMenuResource(menu);
        if (extensionMenuRes != null) {
            getMenuInflater().inflate(extensionMenuRes, menu);
        }

        mSearchMenu = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenu.getActionView();
        if (mSearchView != null) {
            Utils.setUpSearchView(mSearchView, this);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnSuggestionListener(this);
        }

        // Hide the "show/hide controls" button if this is a phone
        // or the view type is "Month" or "Agenda".
       //yuanding modify it for PR:635523 20140505 start
        mControlsMenu = menu.findItem(R.id.action_show_controls);
	   //yuanding modify it for PR:635523 20140505 end
        if (!mShowCalendarControls) {
            if (mControlsMenu != null) {
                mControlsMenu.setVisible(false);
                mControlsMenu.setEnabled(false);
            }
        } else if (mControlsMenu != null && mController != null
                    && (mController.getViewType() == ViewType.MONTH ||
                        mController.getViewType() == ViewType.AGENDA)) {
            mControlsMenu.setVisible(false);
            mControlsMenu.setEnabled(false);
        } else if (mControlsMenu != null){
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        //yuanding add it for PR635523 20140410 start
		 MenuItem menuItem = menu.findItem(R.id.action_today);
	 //yuanding delete it for PR:680960 20140526 start
       /* if (Utils.isJellybeanOrLater()) {
            // replace the default top layer drawable of the today icon with a
            // custom drawable that shows the day of the month of today
            LayerDrawable icon = (LayerDrawable) menuItem.getIcon();
            Utils.setTodayIcon(icon, this, mTimeZone);
        } else {
            menuItem.setIcon(R.drawable.ic_menu_today_no_date_holo_light);
         } */
      //yuanding delete it for PR:680960 20140526 end
		 //yuanding add it for PR635523 20140410 end
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        ///M: options menu extension @{
        mOptionsMenuExt.onCreateOptionsMenu(menu);
        ///@}
        /**
         * M: If in search mode, enter @{
         */
        if (mIsInSearchMode) {
            enterSearchMode();
            // Note: we should set search string after enterSearchMode(), because enterSearchMode() will
            // set it to null
            if (mSearchView != null) {
                // restore search string to UI
                mSearchView.setQuery(mSearchString, false);
            }
        }
        /** @} */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * M: Exit search mode if one non-search item is selected, otherwise, enter search mode@{
         */
        if (item.getItemId() != R.id.action_search) {
            exitSearchMode();
        } else {
            enterSearchMode();
        }
        /** @} */
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin 
        //yuanding add it for PR635523 20140410 start
		Time t = null;
        int viewType = ViewType.CURRENT;
        long extras = CalendarController.EXTRA_GOTO_TIME;
		//yuanding add it for PR635523 20140410 end
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        final int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            	// added by Fujuan.Lin for RR565704 begin
            	// Add facebook features ,if it's a facebook device 
            	// we need authorization infomation.We start an 
            	// activity to archive that.
                if (Utils.IS_FACEBOOK_DEVICE) {
                    Intent intent = new Intent(this, FBAuthorizeActivity.class);
                    startActivity(intent);
                }
                // added by Fujuan.Lin for RR565704 end
            mController.refreshCalendars();
            return true;
         }
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        //yuanding add it for PR635523 20140410 start
		else if (itemId == R.id.action_today) {
            viewType = ViewType.CURRENT;
            t = new Time(mTimeZone);
            t.setToNow();
            extras |= CalendarController.EXTRA_GOTO_TODAY;
            mController.sendMessageHandler(CalendarController.MESSAGE_ID_GOTO_TIME, t);//FR593012-Wentao-Wan-001
            mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
            return true;
            
        }  
		//yuanding add it for PR635523 20140410 end
		/*else if (itemId == R.id.action_create_event) {
            t = new Time();
            ///M: modify for month view, if user want to create event from month view, just set now to start time.@{
            //FR593012-Wentao-Wan-001 begin
            
            int viewtype = mController.getViewType();
            if(viewtype == ViewType.MONTH) {
                t.setToNow();
             } else {
                t.set(mController.getTime());
             }
             
             t.set(mController.getTime());
             //FR593012-Wentao-Wan-001 wnd
             t.second = 0;
             ///@}
             if (t.minute > 30) {
                t.hour++;
                t.minute = 0;
             } else if (t.minute > 0 && t.minute < 30) {
                t.minute = 30;
             }
             mController.sendEventRelatedEvent(
                 this, EventType.CREATE_EVENT, -1, t.toMillis(true), 0, 0, 0, -1);
             return true;
         } */
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        else if (itemId == R.id.action_select_visible_calendars) {
            mController.sendEvent(this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                    0, 0);
            return true;
        } else if (itemId == R.id.action_settings) {
            mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, 0, 0);
            return true;
		//yuanding modify it for PR:635523 20140505 start
        } else if (itemId == R.id.action_show_controls) {
        //yuanding modify it for PR:635523 20140505 end
            mHideControls = !mHideControls;
            Utils.setSharedPreference(
                    this, GeneralPreferences.KEY_SHOW_CONTROLS, !mHideControls);
            item.setTitle(mHideControls ? mShowString : mHideString);
            if (!mHideControls) {
                mMiniMonth.setVisibility(View.VISIBLE);
                mCalendarsList.setVisibility(View.VISIBLE);
                mMiniMonthContainer.setVisibility(View.VISIBLE);
            }
            final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this, "controlsOffset",
                    mHideControls ? 0 : mControlsAnimateWidth,
                    mHideControls ? mControlsAnimateWidth : 0);
            slideAnimation.setDuration(mCalendarControlsAnimationTime);
            ObjectAnimator.setFrameDelay(0);
            slideAnimation.start();
            return true;
        } else if (itemId == R.id.action_search) {
            return false;
        /// M: function go to @{
        } else if (itemId == R.id.action_go_to) {
            launchDatePicker();
            return true;
        /// @}
        } else {
            ///M: extension of options menu @{
            if (mOptionsMenuExt.onOptionsItemSelected(item.getItemId())) {
                return true;
            }
            ///@}
            return mExtensions.handleItemSelected(item, this);
        }
      //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
      //mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
      //return true;
      //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
    }

    /**
     * Sets the offset of the controls on the right for animating them off/on
     * screen. ProGuard strips this if it's not in proguard.flags
     *
     * @param controlsOffset The current offset in pixels
     */
    public void setControlsOffset(int controlsOffset) {
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMiniMonth.setTranslationX(controlsOffset);
            mCalendarsList.setTranslationX(controlsOffset);
            mControlsParams.width = Math.max(0, mControlsAnimateWidth - controlsOffset);
            mMiniMonthContainer.setLayoutParams(mControlsParams);
        } else {
            mMiniMonth.setTranslationY(controlsOffset);
            mCalendarsList.setTranslationY(controlsOffset);
            if (mVerticalControlsParams == null) {
                mVerticalControlsParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, mControlsAnimateHeight);
            }
            mVerticalControlsParams.height = Math.max(0, mControlsAnimateHeight - controlsOffset);
            mMiniMonthContainer.setLayoutParams(mVerticalControlsParams);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // FR556557-Yuansheng-Zhao-001 begin
        //if (key.equals(GeneralPreferences.KEY_WEEK_START_DAY)) {
        if (key.equals(GeneralPreferences.KEY_WEEK_START_DAY) || key.equals(GeneralPreferences.KEY_FIRST_WEEK_OPTION)) {
        // FR556557-Yuansheng-Zhao-001 end
            if (mPaused) {
                mUpdateOnResume = true;
            } else {
                initFragments(mController.getTime(), mController.getViewType(), null);
            }
        }
    }

    private void setMainPane(
            FragmentTransaction ft, int viewId, int viewType, long timeMillis, boolean force) {
        PDebug.Start("AllInOneActivity.setMainPane");

        if (mOnSaveInstanceStateCalled) {
            PDebug.End("AllInOneActivity.setMainPane");
            return;
        }
        if (!force && mCurrentView == viewType) {
            PDebug.End("AllInOneActivity.setMainPane");
            return;
        }

        // Remove this when transition to and from month view looks fine.
        boolean doTransition = viewType != ViewType.MONTH && mCurrentView != ViewType.MONTH;
        FragmentManager fragmentManager = getFragmentManager();
        // Check if our previous view was an Agenda view
        // TODO remove this if framework ever supports nested fragments
        if (mCurrentView == ViewType.AGENDA) {
            // If it was, we need to do some cleanup on it to prevent the
            // edit/delete buttons from coming back on a rotation.
            Fragment oldFrag = fragmentManager.findFragmentById(viewId);
            if (oldFrag instanceof AgendaFragment) {
                ((AgendaFragment) oldFrag).removeFragments(fragmentManager);
            }
        }

        if (viewType != mCurrentView) {
            // The rules for this previous view are different than the
            // controller's and are used for intercepting the back button.
            if (mCurrentView != ViewType.EDIT && mCurrentView > 0) {
                mPreviousView = mCurrentView;
            }
            mCurrentView = viewType;
        }

       	//modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        mActionBarView.updateView(viewType);

        // Create new fragment
        Fragment frag = null;
        Fragment secFrag = null;
        switch (viewType) {
            case ViewType.AGENDA:
                if (mBottomBar != null && (mBottomBar.getSelectedTab() != mAgendaTab)) {
                    mBottomBar.selectTab(mAgendaTab);
                }
                frag = new AgendaFragment(timeMillis, false);
                //ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("agenda");
                break;
            case ViewType.DAY:
            case ViewType.WEEK:
                if (mBottomBar != null && (mBottomBar.getSelectedTab() != mWeekTab)) {
                    mBottomBar.selectTab(mWeekTab);
                }
                /// M: pass in the context
                frag = new DayFragment(this, timeMillis, 1);
                //ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("day");
                break;
            case ViewType.MONTH:
                if (mBottomBar != null && (mBottomBar.getSelectedTab() != mMonthTab)) {
                    mBottomBar.selectTab(mMonthTab);
                }
                frag = new MonthByWeekFragment(timeMillis, false);
                if (mShowAgendaWithMonth) {
                    secFrag = new AgendaFragment(timeMillis, false);
                }
               //ExtensionsFactory.getAnalyticsLogger(getBaseContext()).trackView("month");
                break;

            case ViewType.YEAR:
                if (mBottomBar != null) {
                    mBottomBar.selectTab(null);
                }
                frag = new YearsFragment(this,timeMillis);
                break;

            default:
                throw new IllegalArgumentException(
                        "Must be Agenda, Day, Week, or Month ViewType, not " + viewType);
        }

        // Update the current view so that the menu can update its look according to the
        // current view.
       /* if (mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.setMainView(viewType);
            if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(timeMillis);
            }
        }*/
       //modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end


        // Show date only on tablet configurations in views different than Agenda
        if (!mIsTabletConfig) {
            mDateRange.setVisibility(View.GONE);
        } else if (viewType != ViewType.AGENDA) {
            mDateRange.setVisibility(View.VISIBLE);
        } else {
            mDateRange.setVisibility(View.GONE);
        }

        // Clear unnecessary buttons from the option menu when switching from the agenda view
        if (viewType != ViewType.AGENDA) {
            clearOptionsMenu();
        }

        boolean doCommit = false;
        if (ft == null) {
            doCommit = true;
            ft = fragmentManager.beginTransaction();
        }

        if (doTransition) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        ft.replace(viewId, frag);
        if (mShowAgendaWithMonth) {

            // Show/hide secondary fragment

            if (secFrag != null) {
                ft.replace(R.id.secondary_pane, secFrag);
                mSecondaryPane.setVisibility(View.VISIBLE);
            } else {
                mSecondaryPane.setVisibility(View.GONE);
                Fragment f = fragmentManager.findFragmentById(R.id.secondary_pane);
                if (f != null) {
                    ft.remove(f);
                }
                mController.deregisterEventHandler(R.id.secondary_pane);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Adding handler with viewId " + viewId + " and type " + viewType);
        }
        // If the key is already registered this will replace it
        mController.registerEventHandler(viewId, (EventHandler) frag);
        if (secFrag != null) {
            mController.registerEventHandler(viewId, (EventHandler) secFrag);
        }

        if (doCommit) {
            if (DEBUG) {
                Log.d(TAG, "setMainPane AllInOne=" + this + " finishing:" + this.isFinishing());
            }
            ft.commit();
        }

        PDebug.End("AllInOneActivity.setMainPane");
    }

    //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
    // send event to show years interface
    void showYears() {
        if (mCurrentView != ViewType.YEAR) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.YEAR);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){
            if(mCurrentView == ViewType.YEAR) {
                mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private View.OnClickListener mClickYearsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showYears();
        }
    };
    //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end

    private void setTitleInActionBar(EventInfo event) {
        if (event.eventType != EventType.UPDATE_TITLE || mActionBar == null) {
            return;
        }

        final long start = event.startTime.toMillis(false /* use isDst */);
        long end;
        if (event.endTime != null) {
            end = event.endTime.toMillis(false /* use isDst */);
        } else {
            end = start;
        }
        /// M: make sure end >= start. @{
        if(start > end) {
            end = Utils.getLastDisplayTimeInCalendar(this).toMillis(false);
        }
        /// @}
        //yuanding add it for PR:671615  20140513 start
        int aa=(int) event.extraLong;
        if(aa==EVENT_EXTRALONG1||aa==EVENT_EXTRALONG3){
            aa=EVENT_EXTRALONG2;
		}
		final String msg = Utils.formatDateRange(this, start, end, aa);
		//yuanding add it for PR:671615  20140513 end
        CharSequence oldDate = mDateRange.getText();
        mDateRange.setText(msg);
        updateSecondaryTitleFields(event.selectedTime != null ? event.selectedTime.toMillis(true)
                : start);
        if (!TextUtils.equals(oldDate, msg)) {
            mDateRange.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            if (mShowWeekNum && mWeekTextView != null) {
                mWeekTextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }

        //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        if(event.selectedTime != null){
            mActionBarView.updateTitle(event.selectedTime);  
        }else{
            mActionBarView.updateTitle(event.startTime);  
        }
        //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
    }

    private void updateSecondaryTitleFields(long visibleMillisSinceEpoch) {
        mShowWeekNum = Utils.getShowWeekNumber(this);
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        if (visibleMillisSinceEpoch != -1) {
            int weekNum = Utils.getWeekNumberFromTime(visibleMillisSinceEpoch, this);
            mWeekNum = weekNum;
        }

        if (mShowWeekNum && (mCurrentView == ViewType.WEEK) && mIsTabletConfig
                && mWeekTextView != null) {
            String weekString = getResources().getQuantityString(R.plurals.weekN, mWeekNum,
                    mWeekNum);
            mWeekTextView.setText(weekString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (visibleMillisSinceEpoch != -1 && mWeekTextView != null
                && mCurrentView == ViewType.DAY && mIsTabletConfig) {
            Time time = new Time(mTimeZone);
            time.set(visibleMillisSinceEpoch);
            int julianDay = Time.getJulianDay(visibleMillisSinceEpoch, time.gmtoff);
            time.setToNow();
            int todayJulianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
            String dayString = Utils.getDayOfWeekString(julianDay, todayJulianDay,
                    visibleMillisSinceEpoch, this);
            mWeekTextView.setText(dayString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (mWeekTextView != null && (!mIsTabletConfig || mCurrentView != ViewType.DAY)) {
            mWeekTextView.setVisibility(View.GONE);
        }

        if (mHomeTime != null
                && (mCurrentView == ViewType.DAY || mCurrentView == ViewType.WEEK
                        || mCurrentView == ViewType.AGENDA)
                && !TextUtils.equals(mTimeZone, Time.getCurrentTimezone())) {
            Time time = new Time(mTimeZone);
            time.setToNow();
            long millis = time.toMillis(true);
            boolean isDST = time.isDst != 0;
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            // Formats the time as
            String timeString = (new StringBuilder(
                    Utils.formatDateRange(this, millis, millis, flags))).append(" ").append(
                    TimeZone.getTimeZone(mTimeZone).getDisplayName(
                            isDST, TimeZone.LONG, Locale.getDefault())).toString();
            mHomeTime.setText(timeString);
            mHomeTime.setVisibility(View.VISIBLE);
            // Update when the minute changes
            mHomeTime.removeCallbacks(mHomeTimeUpdater);
            mHomeTime.postDelayed(
                    mHomeTimeUpdater,
                    DateUtils.MINUTE_IN_MILLIS - (millis % DateUtils.MINUTE_IN_MILLIS));
        } else if (mHomeTime != null) {
            mHomeTime.setVisibility(View.GONE);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.VIEW_EVENT | EventType.UPDATE_TITLE;
    }

    @Override
    public void handleEvent(EventInfo event) {
        long displayTime = -1;
        if (event.eventType == EventType.GO_TO) {
            if ((event.extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS) != 0) {
                mBackToPreviousView = true;
            } else if (event.viewType != mController.getPreviousViewType()
                    && event.viewType != ViewType.EDIT) {
                // Clear the flag is change to a different view type
                mBackToPreviousView = false;
            }

            setMainPane(
                    null, R.id.main_pane, event.viewType, event.startTime.toMillis(false), false);
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            if (mShowCalendarControls) {
                int animationSize = (mOrientation == Configuration.ORIENTATION_LANDSCAPE) ?
                        mControlsAnimateWidth : mControlsAnimateHeight;
                boolean noControlsView = event.viewType == ViewType.MONTH || event.viewType == ViewType.AGENDA;
                if (mControlsMenu != null) {
                    mControlsMenu.setVisible(!noControlsView);
                    mControlsMenu.setEnabled(!noControlsView);
                }
                if (noControlsView || mHideControls) {
                    // hide minimonth and calendar frag
                    mShowSideViews = false;
                    if (!mHideControls) {
                            final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                    "controlsOffset", 0, animationSize);
                            slideAnimation.addListener(mSlideAnimationDoneListener);
                            slideAnimation.setDuration(mCalendarControlsAnimationTime);
                            ObjectAnimator.setFrameDelay(0);
                            slideAnimation.start();
                    } else {
                        mMiniMonth.setVisibility(View.GONE);
                        mCalendarsList.setVisibility(View.GONE);
                        mMiniMonthContainer.setVisibility(View.GONE);
                    }
                } else {
                    // show minimonth and calendar frag
                    mShowSideViews = true;
                    mMiniMonth.setVisibility(View.VISIBLE);
                    mCalendarsList.setVisibility(View.VISIBLE);
                    mMiniMonthContainer.setVisibility(View.VISIBLE);
                    if (!mHideControls &&
                            (mController.getPreviousViewType() == ViewType.MONTH ||
                             mController.getPreviousViewType() == ViewType.AGENDA)) {
                        final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                "controlsOffset", animationSize, 0);
                        slideAnimation.setDuration(mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0);
                        slideAnimation.start();
                    }
                }
            }
            displayTime = event.selectedTime != null ? event.selectedTime.toMillis(true)
                    : event.startTime.toMillis(true);
          //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
            /*if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(displayTime);
            }*/
          //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        } else if (event.eventType == EventType.VIEW_EVENT) {

            // If in Agenda view and "show_event_details_with_agenda" is "true",
            // do not create the event info fragment here, it will be created by the Agenda
            // fragment

            if (mCurrentView == ViewType.AGENDA && mShowEventDetailsWithAgenda) {
                if (event.startTime != null && event.endTime != null) {
                    // Event is all day , adjust the goto time to local time
                    if (event.isAllDay()) {
                        Utils.convertAlldayUtcToLocal(
                                event.startTime, event.startTime.toMillis(false), mTimeZone);
                        Utils.convertAlldayUtcToLocal(
                                event.endTime, event.endTime.toMillis(false), mTimeZone);
                    }
                    mController.sendEvent(this, EventType.GO_TO, event.startTime, event.endTime,
                            event.selectedTime, event.id, ViewType.AGENDA,
                            CalendarController.EXTRA_GOTO_TIME, null, null);
                } else if (event.selectedTime != null) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                        event.selectedTime, event.id, ViewType.AGENDA);
                }
            } else {
                // TODO Fix the temp hack below: && mCurrentView !=
                // ViewType.AGENDA
                if (event.selectedTime != null && mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                            event.selectedTime, -1, ViewType.CURRENT);
                }
                int response = event.getResponse();
                if ((mCurrentView == ViewType.AGENDA && mShowEventInfoFullScreenAgenda) ||
                        ((mCurrentView == ViewType.DAY || (mCurrentView == ViewType.WEEK) ||
                                mCurrentView == ViewType.MONTH) && mShowEventInfoFullScreen)){
                    // start event info as activity
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);
                    intent.setData(eventUri);
                    intent.setClass(this, EventInfoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(EXTRA_EVENT_BEGIN_TIME, event.startTime.toMillis(false));
                    intent.putExtra(EXTRA_EVENT_END_TIME, event.endTime.toMillis(false));
                    intent.putExtra(ATTENDEE_STATUS, response);
                    startActivity(intent);
                } else {
                    // start event info as a dialog
                    EventInfoFragment fragment = new EventInfoFragment(this,
                            event.id, event.startTime.toMillis(false),
                            event.endTime.toMillis(false), response, true,
                            EventInfoFragment.DIALOG_WINDOW_STYLE,
                            null /* No reminders to explicitly pass in. */);
                    fragment.setDialogParams(event.x, event.y, mActionBar.getHeight());
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    // if we have an old popup replace it
                    Fragment fOld = fm.findFragmentByTag(EVENT_INFO_FRAGMENT_TAG);
                    if (fOld != null && fOld.isAdded()) {
                        ft.remove(fOld);
                    }
                    ft.add(fragment, EVENT_INFO_FRAGMENT_TAG);
                    ft.commit();
                }
                /// M: dismiss any notification of the given event, if any one of them exists
                // make sure event.id != -1
                // @{
                AlertUtils.removeEventNotification(this, event.id,
                        event.startTime != null ? event.startTime.toMillis(false) : -1,
                        event.endTime != null ? event.endTime.toMillis(false) : -1);
                /// @}
            }
            displayTime = event.startTime.toMillis(true);
        } else if (event.eventType == EventType.UPDATE_TITLE) {
            setTitleInActionBar(event);
          //deleted modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
            /*if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(mController.getTime());
            }*/
          //deleted modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        }
        updateSecondaryTitleFields(displayTime);
    }

    // Needs to be in proguard whitelist
    // Specified as listener via android:onClick in a layout xml
    public void handleSelectSyncedCalendarsClicked(View v) {
        mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, null, 0, 0,
                CalendarController.EXTRA_GOTO_TIME, null,
                null);
    }

    @Override
    public void eventsChanged() {
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        /// M: exit search mode @{
        exitSearchMode();
        // @}
        mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, query,
                getComponentName());
        return true;
    }
    //deleted modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
    /*@Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Log.w(TAG, "TabSelected AllInOne=" + this + " finishing:" + this.isFinishing());
        if (tab == mDayTab && mCurrentView != ViewType.DAY) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
        } else if (tab == mWeekTab && mCurrentView != ViewType.WEEK) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
        } else if (tab == mMonthTab && mCurrentView != ViewType.MONTH) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
        } else if (tab == mAgendaTab && mCurrentView != ViewType.AGENDA) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
        } else {
            Log.w(TAG, "TabSelected event from unknown tab: "
                    + (tab == null ? "null" : tab.getText()));
            Log.w(TAG, "CurrentView:" + mCurrentView + " Tab:" + tab.toString() + " Day:" + mDayTab
                    + " Week:" + mWeekTab + " Month:" + mMonthTab + " Agenda:" + mAgendaTab);
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }*/
    //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case CalendarViewAdapter.DAY_BUTTON_INDEX:
                if (mCurrentView != ViewType.DAY) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
                }
                break;
            case CalendarViewAdapter.WEEK_BUTTON_INDEX:
                if (mCurrentView != ViewType.WEEK) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
                }
                break;
            case CalendarViewAdapter.MONTH_BUTTON_INDEX:
                if (mCurrentView != ViewType.MONTH) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                }
                break;
            case CalendarViewAdapter.AGENDA_BUTTON_INDEX:
                if (mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
                }
                break;
            default:
                Log.w(TAG, "ItemSelected event from unknown button: " + itemPosition);
                break;
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        /// M: exit search mode @{
        exitSearchMode();
        // @}
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        /// M: enter search mode @{
        enterSearchMode();
        // @}
        return false;
    }

    // added by Fujuan.Lin for RR565704 begin
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	// if it's a facebook device , and it has a sns key we
    	// get the user to the facebook authorize activity when the 
    	// sns key is pressed.
        if (Utils.IS_FACEBOOK_DEVICE && KeyEvent.KEYCODE_SNS == keyCode) {
            if (Utils.isFacebookLogin(this)) {
                Intent intent = new Intent(this, FBAuthorizeActivity.class);
                intent.putExtra(FBAuthorizeActivity.EXTRA_FKEY_INVOKE, true);
                startActivity(intent);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
    // added by Fujuan.Lin for RR565704 end

  ///M: to mark that it finished one clear all events operation.
    private static boolean mIsClearEventsCompleted = false;
    
    ///M:#clear all events#.@{
    public static void setClearEventsCompletedStatus(boolean status) {
        mIsClearEventsCompleted = status;
    }
    ///@}

    ///M: launch date picker dialog fragment
    private static final String GOTO_FRAGMENT_TAG = "goto_frag";
    private DatePickerDialog mGotoDatePickerDialog;
    OnDateSetListener mGotoDateSetlistener = new OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
            LogUtil.d(TAG, "date set: " + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

            Time t = new Time(mTimeZone);
            t.year = year;
            t.month = monthOfYear;
            t.monthDay = dayOfMonth;

            long extras = CalendarController.EXTRA_GOTO_TIME | CalendarController.EXTRA_GOTO_TODAY;
            int viewType = ViewType.CURRENT;
            mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
        }
    };

    private void resetGotoDateSetListener() {
        mGotoDatePickerDialog = (DatePickerDialog) getFragmentManager().findFragmentByTag(GOTO_FRAGMENT_TAG);
        if (mGotoDatePickerDialog != null) {
            LogUtil.v(TAG, "resetGotoDateSetListener. ");
            mGotoDatePickerDialog.setOnDateSetListener(mGotoDateSetlistener);
        }
    }

    public void launchDatePicker() {
        //FR593012-Wentao-Wan-001 begin
        /*
        Time t = new Time(mTimeZone);
        t.setToNow();
        */
        //FR593012-Wentao-Wan-001 end
        int startOfWeek = Utils.getFirstDayOfWeek(this);
        // Utils returns Time days while CalendarView wants Calendar days
        if (startOfWeek == Time.SATURDAY) {
            startOfWeek = Calendar.SATURDAY;
        } else if (startOfWeek == Time.SUNDAY) {
            startOfWeek = Calendar.SUNDAY;
        } else {
            startOfWeek = Calendar.MONDAY;
        }
        //FR593012-Wentao-Wan-001 begin
        //mGotoDatePickerDialog = DatePickerDialog.newInstance(mGotoDateSetlistener, t.year, t.month, t.monthDay);
        mGotoDatePickerDialog = DatePickerDialog.newInstance(mGotoDateSetlistener, mController.getCalendarTime().year,
              mController.getCalendarTime().month, mController.getCalendarTime().monthDay);
        //FR593012-Wentao-Wan-001 end
        mGotoDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(this));
        mGotoDatePickerDialog.setYearRange(Utils.YEAR_MIN, Utils.YEAR_MAX);
        mGotoDatePickerDialog.show(getFragmentManager(), GOTO_FRAGMENT_TAG);
    }
    ///@}

    /**
     * M: Exit activity's search mode, control UI on action bar, set search mode flag
     */
    private void exitSearchMode() {
        mIsInSearchMode = false;
        if ((mSearchMenu != null) && mSearchMenu.isActionViewExpanded()) {
            mSearchMenu.collapseActionView();
        }
    }

    /**
     * M: Enter activity's search mode, control UI on action bar, set search mode flag
     */
    private void enterSearchMode() {
        mIsInSearchMode = true;
        if ((mSearchMenu != null) && !mSearchMenu.isActionViewExpanded()) {
            mSearchMenu.expandActionView();
        }
    }

   //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
   private BottomBar getBottomBar(Activity activty) {
        return new BottomBarImpl(activty);
   }

   @Override
   public void onTabSelected(BottomBar.Tab tab, FragmentTransaction ft) {
       Log.w(TAG, "TabSelected AllInOne=" + this + " finishing:" + this.isFinishing());
       if (tab == mWeekTab && mCurrentView != ViewType.WEEK) {
           mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
       } else if (tab == mMonthTab && mCurrentView != ViewType.MONTH) {
           mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
       } else if (tab == mAgendaTab && mCurrentView != ViewType.AGENDA) {
           mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
       } else {
           Log.w(TAG, "TabSelected event from unknown tab: "
                   + (tab == null ? "null" : tab.getText()));
           Log.w(TAG, "CurrentView:" + mCurrentView + " Tab:" + tab.toString()
                   + " Week:" + mWeekTab + " Month:" + mMonthTab + " Agenda:" + mAgendaTab);
       }
   }

   @Override
   public void onTabUnselected(BottomBar.Tab tab, FragmentTransaction ft) {}

   @Override
   public void onTabReselected(BottomBar.Tab tab, FragmentTransaction ft) {}
   //added for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
}
