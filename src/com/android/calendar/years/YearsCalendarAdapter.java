
package com.android.calendar.years;

import android.content.Context;
import android.graphics.Point;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
//yuanding add it for PR:651124 20140418 start
import com.android.calendar.AllInOneActivity;
//yuanding add it for PR:651124 20140418 end
import com.android.calendar.R;
import com.android.calendar.CalendarController;
import com.android.calendar.Utils;

/**
 * this is adapter to show 1970-2036 years
 * 
 * @author archermind
 */
public class YearsCalendarAdapter extends BaseAdapter {
    // min year in calendar
    private final int START_YEAR = CalendarController.MIN_CALENDAR_YEAR;
    private final int MAX_YEAR = CalendarController.MAX_CALENDAR_YEAR; // 2036
    private final int MAX_NUM = MAX_YEAR - START_YEAR + 1; // max show 67 years

    private int[] mYears = new int[MAX_NUM];
    private Context mContext = null;
    private final int WIDTH_YEAR;
    private final int HEIGHT_YEAR;
    // selected time callback
    private OnSelectTimeListener mOnSelectTimeCallback = null;

    public YearsCalendarAdapter(Context context, OnSelectTimeListener callback) {
        mContext = context;
        mOnSelectTimeCallback = callback;
        //init view size
        int offset = (int)context.getResources().getDimension(R.dimen.year_w_h_offset);
		//yuanding add it for PR:651124 20140418 start
        int width = Utils.getScreenMetrics(mContext).widthPixels;
        int height = width + offset;
       //yuanding modify width for PR651124 20140424 start
        if (AllInOneActivity.horizontalScreen) {//Horizontal screen
        //yuanding modify it for PR:635523 20140506 start
			width = 900;
        	height = 515;
		//yuanding modify it for PR:635523 20140506 end	
        }
       //yuanding modify width for PR651124 20140424 end
        WIDTH_YEAR = width;
        HEIGHT_YEAR = height;
		//yuanding add it for PR:651124 20140418 end
        init();
    }

    /**
     * init years array
     */
    private void init() {
        for (int i = 0; i < MAX_NUM; ++i) {
            mYears[i] = START_YEAR + i;
        }
    }

    /**
     * get specify time position at listview
     * 
     * @param time: specify time to get pos
     * @return
     */
    public int getPositionByTime(Time time) {
        if (time == null || time.year > MAX_YEAR || time.year < START_YEAR) {
            time = new Time();
            time.setToNow();
        }
        int year = time.year;
        int position = year - START_YEAR;
        if (position > -1 && position < MAX_NUM)
            return position;
        return 0;
    }

    /**
     * get spec position time
     * 
     * @param pos : specify pos to time
     * @return
     */
    public Time getTimeByPosition(int pos) {
        int year = 0;
        if (pos > -1 && pos < MAX_NUM) {
            year = pos;
        }

        Time time = new Time();
        time.setToNow();
        if (year != 0) {
            time.set(time.monthDay, time.month, mYears[year]);
        }

        return time;
    }

    @Override
    public int getCount() {
        return MAX_NUM;
    }

    @Override
    public Object getItem(int position) {
        if (position > -1 && position < MAX_NUM)
            return mYears[position];
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (position > -1 && position < MAX_NUM)
            return position;
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int year = mYears[position];

        if (convertView == null) {
            convertView = generateYearItemView(year);
        } else {
            if (convertView instanceof YearListItemView) {
                ((YearListItemView) convertView).setTime(year);
            }
        }
        return convertView;
    }

    /**
     * gerenrate a new YearListItemView with specify time
     * 
     * @param time
     * @return
     */
    private View generateYearItemView(int year) {
        YearListItemView view = new YearListItemView(mContext, year, mOnSelectTimeCallback);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(WIDTH_YEAR, HEIGHT_YEAR);
        view.setLayoutParams(params);
        return view;
    }
}
