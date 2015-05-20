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
/*   Author :  Neo Skunkworks-david.zhang                                           */
/*   Role :    Calendar                                                             */
/*   Reference documents :                                                          */
/*=======================================================================================================*/
/* Comments:                                                                        */
/*   file  :  FBFriendBirthdayListActivity.java                                     */
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

import java.util.Formatter;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.calendar.R;

public class FBFriendBirthdayListActivity extends Activity {
    private StringBuilder mStringBuilder;

    private Formatter mFormatter;

    private static final String[] PROJECTION = new String[] {
            Events._ID, Events.FBFRIEND_NAME, Events.DTSTART,
    };

    private int INDEX_COLUMN_ID = 0;

    private int INDEX_COLUMN_FBFRIEND_NAME = 1;

    private int INDEX_COLUMN_DTSTART = 2;
    
    private Cursor cursor = null;//added by yubin.yi.hz for close cursor
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String selection = Events.FBFRIEND_ID + " IS NOT NULL AND " + Events.DELETED + "=0";
        //modified by yubin.yi.hz for close cursor begin
        cursor = getContentResolver().query(Events.CONTENT_URI, PROJECTION, selection, null,
                Events.DTSTART);
        //modifid by yubin.yi.hz for close cursor end

        if (cursor == null) {
            finish();
            return;
        }
        int totalBirthday = cursor.getCount();
        if (totalBirthday <= 0) {
            cursor.close();
            finish();
            return;
        }

        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        ListAdapter adapter = new FBBirthdayAdapter(this, cursor);

        AlertDialog dialog = new AlertDialog.Builder(this).setIcon(R.drawable.birthday_import)
                .setTitle(R.string.title_fb_birthday).setAdapter(adapter, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                // PR-259873-Neo Skunkworks-david.zhang-001 begin
                .setOnKeyListener(new FBKeyListener(this)).create();
        /**
         * .setOnKeyListener(new DialogInterface.OnKeyListener() { public
         * boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
         * //PR-259911-Neo Skunkworks-david.zhang-001 begin
         * if(KeyEvent.KEYCODE_BACK==keyCode){ finish(); }
         * //PR-259911-Neo Skunkworks-david.zhang-001 begin return true; } }).create();
         */
        // PR-259873-Neo Skunkworks-david.zhang-001 end
        ListView birthdayList = dialog.getListView();
        dialog.show();

        if (birthdayList == null) {
            return;
        }
        Time curTime = new Time();
        curTime.set(System.currentTimeMillis());
        Time time = new Time();
        time.month = curTime.month;
        time.monthDay = curTime.monthDay;
        selection = Events.FBFRIEND_ID + " IS NOT NULL AND " + Events.DELETED + "=0 AND "
                + Events.DTSTART + "<" + time.toMillis(true);
        Cursor passBirthdayCursor = getContentResolver().query(Events.CONTENT_URI, PROJECTION,
                selection, null, Events.DTSTART);
        try {
            if (passBirthdayCursor == null) {
                return;
            }
            int passBirthdayCount = passBirthdayCursor.getCount();
            if (passBirthdayCount <= 0 || passBirthdayCount == totalBirthday) {
                return;
            }
            birthdayList.setSelection(passBirthdayCount);
        } finally {
            if (passBirthdayCursor != null) {
                passBirthdayCursor.close();
            }
        }
    }
    
    //added by yubin.yi.hz for close cursor begin
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (cursor != null) {
    		cursor.close();
    	}
    }
    //added by yubin.yi.hz for close cursor end
    
    private String getFormatedTime(long startMillis) {
        int flags;
        flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_NO_YEAR;
        if (DateFormat.is24HourFormat(this)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }
        mStringBuilder.setLength(0);
        String formatTime = DateUtils.formatDateRange(this, mFormatter, startMillis, startMillis,
                flags).toString();

        return formatTime;
    }

    class FBBirthdayAdapter extends CursorAdapter {
        private LayoutInflater mDialogInflater;

        public FBBirthdayAdapter(Context context, Cursor c) {
            super(context, c);
            final Context dialogContext = new ContextThemeWrapper(context,
                    android.R.style.Theme_Light);
            mDialogInflater = (LayoutInflater) dialogContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mDialogInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String formatedTime = getFormatedTime(Long.valueOf(cursor
                    .getString(INDEX_COLUMN_DTSTART)));
            String name = cursor.getString(INDEX_COLUMN_FBFRIEND_NAME);
            String text = getResources()
                    .getString(R.string.bithday_item_format, formatedTime, name);
            ((TextView) view).setText(text);
        }

    }

    // PR-259873-Neo Skunkworks-david.zhang-001 begin
    private class FBKeyListener extends View implements DialogInterface.OnKeyListener {
        public FBKeyListener(Context context) {
            super(context);
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            // PR-259911-Neo Skunkworks-david.zhang-001 begin
            if (KeyEvent.KEYCODE_BACK == keyCode) {
                finish();
                return true;
            }
            // PR-259911-Neo Skunkworks-david.zhang-001 begin
            else if (KeyEvent.KEYCODE_SEARCH == keyCode) {
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    }
    // PR-259873-Neo Skunkworks-david.zhang-001 end
}
