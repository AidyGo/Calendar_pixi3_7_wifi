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

package com.android.calendar.month;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnLayoutChangeListener;
import android.app.ActionBar.LayoutParams;

import com.android.calendar.CalendarController;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.Event;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.event.CreateEventDialogFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import com.android.calendar.eventlist.EventListView;
public class MonthByWeekFragment extends SimpleDayPickerFragment implements
        CalendarController.EventHandler, LoaderManager.LoaderCallbacks<Cursor>, OnScrollListener,
        OnTouchListener, EventDragView.DragListener {//FR593012-Wentao-Wan-001
    private static final String TAG = "MonthFragment";
    private static final String TAG_EVENT_DIALOG = "event_dialog";

    private CreateEventDialogFragment mEventDialog;

    // Selection and selection args for adding event queries
    private static final String WHERE_CALENDARS_VISIBLE = Calendars.VISIBLE + "=1";
    private static final String INSTANCES_SORT_ORDER = Instances.START_DAY + ","
            + Instances.START_MINUTE + "," + Instances.TITLE;
    protected static boolean mShowDetailsInMonth = false;

    protected float mMinimumTwoMonthFlingVelocity;
    protected boolean mIsMiniMonth;
    protected boolean mHideDeclined;

    protected int mFirstLoadedJulianDay;
    protected int mLastLoadedJulianDay;

    private static final int WEEKS_BUFFER = 1;
    // How long to wait after scroll stops before starting the loader
    // Using scroll duration because scroll state changes don't update
    // correctly when a scroll is triggered programmatically.
    private static final int LOADER_DELAY = 200;
    // The minimum time between requeries of the data if the db is
    // changing
    private static final int LOADER_THROTTLE_DELAY = 500;

    private CursorLoader mLoader;
    private Uri mEventUri;
    private final Time mDesiredDay = new Time();

    private volatile boolean mShouldLoad = true;
    private boolean mUserScrolled = false;

    private int mEventsLoadingDelay;
    private boolean mShowCalendarControls;
    private boolean mIsDetached;
    //FR593012-Wentao-Wan-001 begin
    private static DisplayMetrics mDisplayMetrics;
    public static final String EVENTS_COUNT_CHANGE_BROADCAST_FOR_UPDATE = "com.android.broadcast.list.noEvents";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
       @Override
       public void onReceive(Context context, Intent intent) {
         // TODO Auto-generated method stub
           if(mNoEventHint == null || intent == null){
             //avoid NullException , Even impossible
              return ;
            }
           int count = intent.getIntExtra("Count", 0);
           if(count == 0){
              mNoEventHint.setVisibility(View.VISIBLE);
           }else{
              mNoEventHint.setVisibility(View.GONE);
           }
        }
    };

    private void registerReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(EVENTS_COUNT_CHANGE_BROADCAST_FOR_UPDATE);
        getActivity().registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver(){
        getActivity().unregisterReceiver(mReceiver);
    }
    //FR593012-Wentao-Wan-001 end

    private Handler mEventDialogHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final FragmentManager manager = getFragmentManager();
            if (manager != null) {
                Time day = (Time) msg.obj;
                mEventDialog = new CreateEventDialogFragment(day);
                mEventDialog.show(manager, TAG_EVENT_DIALOG);
            }
        }
    };


    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            String tz = Utils.getTimeZone(mContext, mTZUpdater);
            mSelectedDay.timezone = tz;
            mSelectedDay.normalize(true);
            mTempTime.timezone = tz;
            mFirstDayOfMonth.timezone = tz;
            mFirstDayOfMonth.normalize(true);
            mFirstVisibleDay.timezone = tz;
            mFirstVisibleDay.normalize(true);
            if (mAdapter != null) {
                mAdapter.refresh();
            }
        }
    };


    private final Runnable mUpdateLoader = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                if (!mShouldLoad || mLoader == null) {
                    return;
                }
                // Stop any previous loads while we update the uri
                stopLoader();

                // Start the loader again
                mEventUri = updateUri();

                mLoader.setUri(mEventUri);
                mLoader.startLoading();
                mLoader.onContentChanged();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Started loader with uri: " + mEventUri);
                }
            }
        }
    };
    // Used to load the events when a delay is needed
    Runnable mLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsDetached) {
                mLoader = (CursorLoader) getLoaderManager().initLoader(0, null,
                        MonthByWeekFragment.this);
            }
        }
    };


    /**
     * Updates the uri used by the loader according to the current position of
     * the listview.
     *
     * @return The new Uri to use
     */
    private Uri updateUri() {
        SimpleWeekView child = (SimpleWeekView) mListView.getChildAt(0);
        if (child != null) {
            int julianDay = child.getFirstJulianDay();
            mFirstLoadedJulianDay = julianDay;
        }
        // -1 to ensure we get all day events from any time zone
        mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
        long start = mTempTime.toMillis(true);
        mLastLoadedJulianDay = mFirstLoadedJulianDay + (mNumWeeks + 2 * WEEKS_BUFFER) * 7;
        // +1 to ensure we get all day events from any time zone
        mTempTime.setJulianDay(mLastLoadedJulianDay + 1);
        long end = mTempTime.toMillis(true);

        // Create a new uri with the updated times
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return builder.build();
    }

    // Extract range of julian days from URI
    private void updateLoadedDays() {
        List<String> pathSegments = mEventUri.getPathSegments();
        int size = pathSegments.size();
        if (size <= 2) {
            return;
        }
        long first = Long.parseLong(pathSegments.get(size - 2));
        long last = Long.parseLong(pathSegments.get(size - 1));
        mTempTime.set(first);
        mFirstLoadedJulianDay = Time.getJulianDay(first, mTempTime.gmtoff);
        mTempTime.set(last);
        mLastLoadedJulianDay = Time.getJulianDay(last, mTempTime.gmtoff);
    }

    protected String updateWhere() {
        // TODO fix selection/selection args after b/3206641 is fixed
        String where = WHERE_CALENDARS_VISIBLE;
        if (mHideDeclined /*M: || !mShowDetailsInMonth*/) {
            where += " AND " + Instances.SELF_ATTENDEE_STATUS + "!="
                    + Attendees.ATTENDEE_STATUS_DECLINED;
        }
        return where;
    }

    private void stopLoader() {
        synchronized (mUpdateLoader) {
            mHandler.removeCallbacks(mUpdateLoader);
            if (mLoader != null) {
                mLoader.stopLoading();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Stopped loader from loading");
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTZUpdater.run();
        if (mAdapter != null) {
            mAdapter.setSelectedDay(mSelectedDay);
        }
        mIsDetached = false;

        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        mMinimumTwoMonthFlingVelocity = viewConfig.getScaledMaximumFlingVelocity() / 2;
        Resources res = activity.getResources();
        mShowCalendarControls = Utils.getConfigBool(activity, R.bool.show_calendar_controls);
        // Synchronized the loading time of the month's events with the animation of the
        // calendar controls.
        if (mShowCalendarControls) {
            mEventsLoadingDelay = res.getInteger(R.integer.calendar_controls_animation_time);
        }
        mShowDetailsInMonth = res.getBoolean(R.bool.show_details_in_month);
    }

    @Override
    public void onDetach() {
        mIsDetached = true;
        super.onDetach();
        if (mShowCalendarControls) {
            if (mListView != null) {
                mListView.removeCallbacks(mLoadingRunnable);
            }
        }
    }

    @Override
    protected void setUpAdapter() {
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mShowWeekNumber = Utils.getShowWeekNumber(mContext);

        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, mNumWeeks);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_SHOW_WEEK, mShowWeekNumber ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_WEEK_START, mFirstDayOfWeek);
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_IS_MINI, mIsMiniMonth ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_JULIAN_DAY,
                Time.getJulianDay(mSelectedDay.toMillis(true), mSelectedDay.gmtoff));
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_DAYS_PER_WEEK, mDaysPerWeek);
        if (mAdapter == null) {
            mAdapter = new MonthByWeekAdapter(getActivity(), weekParams, mEventDialogHandler);
            mAdapter.registerDataSetObserver(mObserver);
        } else {
            mAdapter.updateParams(weekParams);
        }
        mAdapter.notifyDataSetChanged();
    }
    //FR593012-Wentao-Wan-001 begin
    public static RelativeLayout mLinearLayout = null;//modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
    private EventListView mEventListView = null;
    private TextView mNoEventHint ;
    private EventDragView mDragLine;
    //FR593012-Wentao-Wan-001 end
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v;
        if (mIsMiniMonth) {
            v = inflater.inflate(R.layout.month_by_week, container, false);
        }
        else {
            //v = inflater.inflate(R.layout.full_month_by_week, container, false);//FR593012-Wentao-Wan-001
            v = inflater.inflate(R.layout.full_month_by_week_event, container, false);
        }
        mDayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);
        //FR593012-Wentao-Wan-001 begin
        //mLinearLayout = (LinearLayout) v.findViewById(R.id.month_event);
        mLinearLayout = (RelativeLayout) v.findViewById(R.id.month_event);//modified for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13
        mEventListView = (EventListView)v.findViewById(R.id.events_list);
        mEventListView.setClickable(true);
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 begin
        //mDragLine = (EventDragView)v.findViewById(R.id.line_button);
        //mDragLine.setListener(this);
        //deleted for FR602091 Porting Wave3 Calendar to soul45 by yubin.yi.hz at 2014-02-13 end
        mDisplayMetrics = getResources().getDisplayMetrics();
        mNoEventHint = (TextView)v.findViewById(R.id.noEvent);
        //FR593012-Wentao-Wan-001 end
        return v;
    }

    //FR593012-Wentao-Wan-001 begin
    @Override
    public void onResume() {
        CalendarController.getInstance(getActivity()).registerHandler(mHandler);
        super.onResume();
		      //yuanding add it for PR:656515 20140425 start
               IntentFilter filter = new IntentFilter();
               filter.addAction("com.event.ACTION_BACK");
               mContext.registerReceiver(mmReceiver, filter);
               //yuanding add it for PR:656515 20140425 end
        registerReceiver();
        //PR574410-Yuansheng.Zhao-001 begin
        mHideDeclined=Utils.getHideDeclinedEvents(mContext);
        mEventListView.setHideDeclinedEvents(mHideDeclined);
        //PR574410-Yuansheng.Zhao-001 end
        //mListView.refresh
        mHandler.removeCallbacks(mUpdateLoader);
        mShouldLoad = true;
        mHandler.postDelayed(mUpdateLoader, LOADER_DELAY);

        mEventListView.goTo(CalendarController.getInstance(mContext)
                .getCalendarTime(), -1, null, true, false);
        mEventListView.onResume();
    }


			//yuanding add it for PR:656515 20140425 start
			private BroadcastReceiver mmReceiver = new BroadcastReceiver(){
					public void onReceive(Context context, Intent intent) {
							if(intent.getAction().equals("com.event.ACTION_BACK")){
									mEventListView.goTo(CalendarController.getInstance(mContext)
							.getCalendarTime(), -1, null, true, false);
							}
					};
		};
	
           //yuanding add it for PR:656515 20140425 end



    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver();
		//yuanding add it for PR:656515 20140425 start
		 mContext.unregisterReceiver(mmReceiver);
		//yuanding add it for PR:656515 20140425 end
        mEventListView.onPause();
        CalendarController.getInstance(getActivity()).deregisterHandler();
    }
    //FR593012-Wentao-Wan-001 end

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setSelector(new StateListDrawable());
        mListView.setOnTouchListener(this);

        if (!mIsMiniMonth) {
            mListView.setBackgroundColor(getResources().getColor(R.color.month_bgcolor));
        }

        // To get a smoother transition when showing this fragment, delay loading of events until
        // the fragment is expended fully and the calendar controls are gone.
        if (mShowCalendarControls) {
            mListView.postDelayed(mLoadingRunnable, mEventsLoadingDelay);
        } else {
            mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this);
        }
        mAdapter.setListView(mListView);
    }

    public MonthByWeekFragment() {
        //FR593012-Wentao-Wan-001 begin
        //this(System.currentTimeMillis(), true);
        this(System.currentTimeMillis(), false);
        //FR593012-Wentao-Wan-001 end
    }

    public MonthByWeekFragment(long initialTime, boolean isMiniMonth) {
        super(initialTime);
        mIsMiniMonth = isMiniMonth;
        mHandler = new MessageHandler();//FR593012-Wentao-Wan-001
    }

    @Override
    protected void setUpHeader() {
        if (mIsMiniMonth) {
            super.setUpHeader();
            return;
        }

        mDayLabels = new String[7];
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            mDayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_MEDIUM).toUpperCase();
        }
    }

    // TODO
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mIsMiniMonth) {
            return null;
        }
        CursorLoader loader;
        synchronized (mUpdateLoader) {
            mFirstLoadedJulianDay =
                    Time.getJulianDay(mSelectedDay.toMillis(true), mSelectedDay.gmtoff)
                    - (mNumWeeks * 7 / 2);
            mEventUri = updateUri();
            String where = updateWhere();

            loader = new CursorLoader(
                    getActivity(), mEventUri, Event.EVENT_PROJECTION, where,
                    null /* WHERE_CALENDARS_SELECTED_ARGS */, INSTANCES_SORT_ORDER);
            loader.setUpdateThrottle(LOADER_THROTTLE_DELAY);
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Returning new loader with uri: " + mEventUri);
        }
        return loader;
    }

    @Override
    public void doResumeUpdates() {
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mShowWeekNumber = Utils.getShowWeekNumber(mContext);
        boolean prevHideDeclined = mHideDeclined;
        mHideDeclined = Utils.getHideDeclinedEvents(mContext);
        if (prevHideDeclined != mHideDeclined && mLoader != null) {
            mLoader.setSelection(updateWhere());
        }
        mDaysPerWeek = Utils.getDaysPerWeek(mContext);
        updateHeader();
        mAdapter.setSelectedDay(mSelectedDay);
        mTZUpdater.run();
        mTodayUpdater.run();
        /** M: @{ */
        if (mIsAccountChanged) {
            mUpdateLoader.run();
            mIsAccountChanged = false;
        }
        /**@}*/
        //FR593012-Wentao-Wan-001 begin
        //goTo(mSelectedDay.toMillis(true), false, true, false);
        goTo(mSelectedDay.toMillis(true), false, true, false,true);
        //FR593012-Wentao-Wan-001 end
    }

    //FR593012-Wentao-Wan-001 begin
    public void updateClickedDayParameter(Time time){
        Time currentTime = new Time(time.timezone);
        currentTime.set(System.currentTimeMillis());
        //Compare whether selectday is equal to current day or not
        boolean isSameYear = time.year == currentTime.year;
        boolean isSameMonth = time.month == currentTime.month;
        boolean isSameDay = time.monthDay == currentTime.monthDay;

        if(isSameYear && isSameMonth && isSameDay){
           //if the clicked day is current day. not highlight the day.
           Utils.clearBufferEX();
           return ;
        }
        int julianDay = Time.getJulianDay(time.toMillis(true), time.gmtoff);
        Utils.setJulianDayInGeneral(time, julianDay);
        /*//Delete by guangqian.peng for PR707270
        int selectedWeek = Utils.getWeeksSinceEpochFromJulianDay(julianDay, mFirstDayOfWeek);
        Utils.setSelectedWeekDayEX(selectedWeek);
        */
        Utils.setSelectedJulianDayEX(julianDay);
    }
    //FR593012-Wentao-Wan-001 end

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        synchronized (mUpdateLoader) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Found " + data.getCount() + " cursor entries for uri " + mEventUri);
            }
            CursorLoader cLoader = (CursorLoader) loader;
            if (mEventUri == null) {
                mEventUri = cLoader.getUri();
                updateLoadedDays();
            }
            if (cLoader.getUri().compareTo(mEventUri) != 0) {
                // We've started a new query since this loader ran so ignore the
                // result
                return;
            }
            ArrayList<Event> events = new ArrayList<Event>();
            Event.buildEventsFromCursor(
                    events, data, mContext, mFirstLoadedJulianDay, mLastLoadedJulianDay);
            ((MonthByWeekAdapter) mAdapter).setEvents(mFirstLoadedJulianDay,
                    mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, events);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void eventsChanged() {
        // TODO remove this after b/3387924 is resolved
        if (mLoader != null) {
            mLoader.forceLoad();
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.GO_TO) {
            boolean animate = true;
            if (mDaysPerWeek * mNumWeeks * 2 < Math.abs(
                    Time.getJulianDay(event.selectedTime.toMillis(true), event.selectedTime.gmtoff)
                    - Time.getJulianDay(mFirstVisibleDay.toMillis(true), mFirstVisibleDay.gmtoff)
                    - mDaysPerWeek * mNumWeeks / 2)) {
                animate = false;
            }
            mDesiredDay.set(event.selectedTime);
            mDesiredDay.normalize(true);
            /// M: whether if animate the selected day @{
            boolean animateSelectedDay = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
            /// @}
            boolean animateToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
            //FR593012-Wentao-Wan-001 begin
            //boolean delayAnimation = goTo(event.selectedTime.toMillis(true), animate, true, false);
            boolean delayAnimation = goTo(event.selectedTime.toMillis(true), animate, true, false,true);
            //FR593012-Wentao-Wan-001 end
            /// M: if animate the selected day
            if (animateSelectedDay && (mAdapter != null)) {// Modified by guangqian.peng for PR714398.
                /// M: set real selected time @{
                ((MonthByWeekAdapter) mAdapter).setRealSelectedDay(event.selectedTime);
                /// @}
                // If we need to flash today start the animation after any
                // movement from listView has ended.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        /// M: animate the selected day
                        ((MonthByWeekAdapter) mAdapter).animateSelectedDay();
                        mAdapter.notifyDataSetChanged();
                    }
                }, delayAnimation ? GOTO_SCROLL_DURATION : 0);
            }
        } else if (event.eventType == EventType.EVENTS_CHANGED) {
            eventsChanged();
        }
    }

    @Override
    protected void setMonthDisplayed(Time time, boolean updateHighlight) {
        super.setMonthDisplayed(time, updateHighlight);
        if (!mIsMiniMonth) {
            boolean useSelected = false;
            if (time.year == mDesiredDay.year && time.month == mDesiredDay.month) {
                mSelectedDay.set(mDesiredDay);
                mAdapter.setSelectedDay(mDesiredDay);
                useSelected = true;
            } else {
                mSelectedDay.set(time);
                mAdapter.setSelectedDay(time);
            }
            CalendarController controller = CalendarController.getInstance(mContext);
            if (mSelectedDay.minute >= 30) {
                mSelectedDay.minute = 30;
            } else {
                mSelectedDay.minute = 0;
            }
            long newTime = mSelectedDay.normalize(true);
            if (newTime != controller.getTime() && mUserScrolled) {
                long offset = useSelected ? 0 : DateUtils.WEEK_IN_MILLIS * mNumWeeks / 3;
                controller.setTime(newTime + offset);
            }
            ///M:@{
            ///M:mTempTime is the time to move to
            controller.sendEvent(this, EventType.UPDATE_TITLE, time, time, mTempTime, -1,
                    ViewType.CURRENT, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                            | DateUtils.FORMAT_SHOW_YEAR, null, null);
            ///@}
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        synchronized (mUpdateLoader) {
            if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mShouldLoad = false;
                stopLoader();
                mDesiredDay.setToNow();
            } else {
                mHandler.removeCallbacks(mUpdateLoader);
                mShouldLoad = true;
                mHandler.postDelayed(mUpdateLoader, LOADER_DELAY);
            }
        }
        if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            mUserScrolled = true;
        }

        mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mDesiredDay.setToNow();
        return false;
        // TODO post a cleanup to push us back onto the grid if something went
        // wrong in a scroll such as the user stopping the view but not
        // scrolling
    }

    //FR593012-Wentao-Wan-001 begin
    class MessageHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case OnScrollListener.SCROLL_STATE_IDLE:
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
            case OnScrollListener.SCROLL_STATE_FLING:
                onScrollStateChanged((AbsListView)msg.obj,msg.what);
                break;
            case CalendarController.MESSAGE_ID_GOTO_TIME:
                Time gotoTime = (Time)msg.obj;
                updateClickedDayParameter(gotoTime);
                mEventListView.goTo(gotoTime, -1, null, true, false);
                break;
            case CalendarController.MESSAGE_ID_SET_TIME:
                mSelectedDay = (Time)msg.obj;
                break;
            default:
                break;
            }
        }
    }

    private int mCoverViewHeight = 0;
    @Override
    public void onDragStart() {
        mCoverViewHeight = mLinearLayout.getHeight();
        mLinearLayout.addOnLayoutChangeListener(new OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight,
                    int oldBottom) {
            }
        });

    }

    @Override
    public void onDraging(int deltaY) {
       float desinity = 0.0f ;

       if(mDisplayMetrics == null){
           desinity = 3.0f; // default value is in need for diablox
       }else{
           desinity = mDisplayMetrics.density;
        }

        int realHeight = mCoverViewHeight + deltaY;
        ViewGroup.LayoutParams params = mLinearLayout.getLayoutParams();
        params.height = realHeight;
        if (realHeight >= 85 * desinity){
            mLinearLayout.setLayoutParams(params);
        } else {
            params.height = (int) (85 * desinity);
            mLinearLayout.setLayoutParams(params);
            return;
        }
    }

    @Override
    public void onDragEnd() {
        // TODO Auto-generated method stub

    }
    //FR593012-Wentao-Wan-001 end

    /**M: add observer @{*/
    private boolean mIsAccountChanged = false;
    private ContentResolver mContentResolver;
    /**
     * observer the calendars table.
     */
    private final ContentObserver mCalendarsObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG, "mCalendarsObserver, onChange");
            mIsAccountChanged = true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentResolver = mContext.getContentResolver();
        mContentResolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI,
                true, mCalendarsObserver);
    }
    @Override
    public void onDestroy() {
        mContentResolver.unregisterContentObserver(mCalendarsObserver);
        super.onDestroy();
    }
    /**@}*/
}
