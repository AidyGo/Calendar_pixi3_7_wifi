/********************************************************************************************************/
/*                                                            Date : 12/2012                            */
/*                            PRESENTATION                                                              */
/*              Copyright (c) 2010 JRD Communications, Inc.                                             */
/********************************************************************************************************/
/*                                                                                                      */
/*    This material is company confidential, cannot be reproduced in any                                */
/*    form without the written permission of JRD Communications, Inc.                                   */
/*                                                                                                      */
/*------------------------------------------------------------------------------------------------------*/
/*   Author :  zhikun.chen                                                                              */
/*   Role   :                                                                                           */
/*   Reference documents :                                                                              */
/*------------------------------------------------------------------------------------------------------*/
/*    Comments :                                                                                        */
/*    File     : com.android.calendar.widget.JrdCalendarAppWidgetProvider                               */
/*    Labels   :                                                                                        */
/*======================================================================================================*/
/* Modifications on Features list / Changes Request / Problems Report                                   */
/*------------------------------------------------------------------------------------------------------*/
/* date (M/D/Y)  | author         | Key                          | comment                              */
/*---------------|----------------|------------------------------|--------------------------------------*/
/*01/02/13       | zhikun.chen    |FR337675-zhikun-chen-001      | [ergo] Calendar widget               */
/*---------------|----------------|------------------------------|--------------------------------------*/
/*06/04/2013     | xiaobin yang   |PR462656-xiaobin-yang-001     |Widget display error after change time*/
/*======================================================================================================*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.calendar.widget;

import java.util.ArrayList;
import com.android.calendar.Event;
import com.android.calendar.EventLoader;
import com.android.calendar.R;
import com.android.calendar.Utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;
import android.widget.ListView;
import android.widget.RemoteViews;

import android.appwidget.AppWidgetProviderInfo;
import android.net.Uri;
import android.os.Bundle;

/**
 * Simple widget to show next upcoming calendar event.
 */
public class JrdCalendarAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "JrdCalendarAppWidgetProvider";
    static final boolean LOGD = true;
    protected ListView mListView;

    private EventLoader mEventLoader;
    private static final String CHANGE_MONTH_EVENT = "com.android.broadcast.CHANGE_MONTH_EVENT";
    //An array of which days have events for quick reference
    private boolean[] eventDay = new boolean[31];
    private static int EVENT_NUM_DAYS = 31;
    private String mTimeZone;
    private boolean mTimeZoneChangedFlag = false;
    private boolean mGetNewMillisFlag=false;
    private static final String GET_NEW_DATE="com.android.broadcast.GET_NEW_DATE";
    private static final String SCHEDULE_UPDATED = "com.android.broadcast.SCHEDULED_UPDATE";
    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle calendar-specific updates ourselves because they might be
        // coming in without extras, which AppWidgetProvider then blocks.
        final String action = intent.getAction();
        if (LOGD)
            Log.d(TAG, "JrdAppWidgetProvider got the intent: " + intent.toString());
        if (Utils.getWidgetUpdateAction(context).equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            performUpdate(context, appWidgetManager,
                    appWidgetManager.getAppWidgetIds(getComponentName(context)),
                    null /* no eventIds */, 0);
        }  else if(action.equals(CHANGE_MONTH_EVENT)
                || action.equals(Utils.getWidgetScheduledUpdateAction(context))
                || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_LOCALE_CHANGED)
                || action.equals(Intent.ACTION_DATE_CHANGED)
                || action.equals(Intent.ACTION_PROVIDER_CHANGED)){
            //widget month changed, events need updated
            if(LOGD) Log.d(TAG,"JrdCalendarAppWidgetProvider: month changed, or action_provider_changed");

            // When the time is from 23:59 to 00:00 of next day, the widget
            // should update
            if (action.equals(Utils.getWidgetScheduledUpdateAction(context))) {
                context.sendBroadcast(new Intent(SCHEDULE_UPDATED));
            }

            if (Intent.ACTION_PROVIDER_CHANGED.equals(action)) {
                mGetNewMillisFlag = true;
             }

            if(mEventLoader == null) {
                mEventLoader = new EventLoader(context);
                mEventLoader.startBackgroundThread();
             }

            long calendarMillis = intent.getLongExtra("current_calendar_millis", -1);

            mTimeZoneChangedFlag = false;
            if(action.equals(Intent.ACTION_LOCALE_CHANGED)
            || action.equals(Intent.ACTION_PROVIDER_CHANGED)
            || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                mTimeZoneChangedFlag = true;
            }

            reloadEvents(calendarMillis, context);
        } else {
            super.onReceive(context, intent);
        }
    }

    private void reloadEvents(long calendarMillis, final Context context) {
        mTimeZone = Utils.getTimeZone(context, null);
        if(LOGD) Log.d(TAG,"reloadEvents: mTimeZone ==" + mTimeZone);

        // Get the date for the beginning of the month
        Time monthStart = new Time(mTimeZone);

        if(calendarMillis == -1) {
            //set current time
            monthStart.set(System.currentTimeMillis());
        } else {
            monthStart.set(calendarMillis);
        }

        monthStart.monthDay = 1;
        monthStart.hour = 0;
        monthStart.minute = 0;
        monthStart.second = 0;
        long millis = monthStart.normalize(true);
        final int startDay = Time.getJulianDay(millis, monthStart.gmtoff);
        if(LOGD) Log.d(TAG, "JrdCalendarAppWidgetProvider: startDay ==" + startDay);
        // Load the days with events in the background
        final ArrayList<Event> events = new ArrayList<Event>();
        mEventLoader.loadEventsInBackground(EVENT_NUM_DAYS, events, startDay, new Runnable() {
            public void run() {
                mEventLoader.stopBackgroundThread();

                int numEvents = events.size();
                int firstJulianDay = startDay;

                // Clear out event days
                for (int i = 0; i < EVENT_NUM_DAYS; i++) {
                    eventDay[i] = false;
                }

                // Compute the new set of days with events
                for (int i = 0; i < numEvents; i++) {
                    Event event = events.get(i);
                    int startDay = event.startDay - firstJulianDay;
                    int endDay = event.endDay - firstJulianDay + 1;
                    if (startDay < 31 || endDay >= 0) {
                        if (startDay < 0) {
                            startDay = 0;
                        }
                        if (startDay > 31) {
                            startDay = 31;
                        }
                        if (endDay < 0) {
                            endDay = 0;
                        }
                        if (endDay > 31) {
                            endDay = 31;
                        }
                        for (int j = startDay; j < endDay; j++) {
                            eventDay[j] = true;
                        }
                    }
                }

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                performUpdate(context, appWidgetManager,
                    appWidgetManager.getAppWidgetIds(getComponentName(context)),
                    eventDay /* no eventIds */, startDay);
            }
        }, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
       performUpdate(context, appWidgetManager, appWidgetIds, null /* no eventIds */, 0);
    }


    /**
     * Build {@link ComponentName} describing this specific
     * {@link AppWidgetProvider}
     */
    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, JrdCalendarAppWidgetProvider.class);
    }

    /**
     * Process and push out an update for the given appWidgetIds. This call
     * actually fires an intent to start {@link JrdCalendarAppWidgetService} as a
     * background service which handles the actual update, to prevent ANR'ing
     * during database queries.
     *
     * @param context Context to use when starting {@link JrdCalendarAppWidgetService}.
     * @param appWidgetIds List of specific appWidgetIds to update, or null for
     *            all.
     * @param changedEventIds Specific events known to be changed. If present,
     *            we use it to decide if an update is necessary.
     */
    private void performUpdate(Context context,
            AppWidgetManager appWidgetManager, int[] appWidgetIds,
            boolean[] eventDayArray, int startDay) {
        // Launch over to service so it can perform update
        for (int appWidgetId : appWidgetIds) {
            if (LOGD) Log.d(TAG, "Building jrd widget update...");

	        //add by qiang.luo for PR424095 begin
	        Bundle myOptions = appWidgetManager.getAppWidgetOptions (appWidgetId);
	        int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
	        boolean isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
	        int baseLayout = isKeyguard ? R.layout.jrdappwidgetview : R.layout.jrdappwidget;
	        //add by qiang.luo for PR424095 end
            RemoteViews views = new RemoteViews(context.getPackageName(), baseLayout);
            //PR499710-yanchao-chen-001 start just for supporting lockscreen
            if(isKeyguard && views != null){
                Intent intent = new Intent(SCHEDULE_UPDATED);
                PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
                views.setOnClickPendingIntent(com.jrdcom.internal.R.id.widget_header, pi);
            }//PR499710-yanchao-chen-001 end just for supporting lockscreen

            //set time zone
            if(mTimeZoneChangedFlag) {
                views.setString(R.id.widget_jrdcalendar, "setTimeZone", mTimeZone); //set timezone first
            }

            if (mGetNewMillisFlag) {
                context.sendBroadcast(new Intent(GET_NEW_DATE));
                mGetNewMillisFlag = false;
            }

            //set eventsDayList to JrdCalendarView
            if(eventDayArray != null && eventDayArray.length > 0) {
                String eventDayStrings = String.valueOf(startDay);
                for(int i=0; i<eventDayArray.length; i++) {
                    if(i < eventDayArray.length) {
                        eventDayStrings += ",";
                    }
                    eventDayStrings += eventDayArray[i];
                  }
                views.setString(R.id.widget_jrdcalendar, "setEventsOfMonth", eventDayStrings);
              }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
