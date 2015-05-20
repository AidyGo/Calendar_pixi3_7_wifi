
package com.android.calendar.years;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
//yuanding add it for PR:651124 20140418 start
import com.android.calendar.AllInOneActivity;
//yuanding add it for PR:651124 20140418 end
import com.android.calendar.Utils;
import com.android.calendar.R;

/**
 * this class show a year title & 12 mini month view
 * 
 * @author archermind
 */
public class YearListItemView extends LinearLayout {
    private final String TAG = YearListItemView.class.getSimpleName();

    public static int TABLE_MONTH_COLUMNS;
    public static int TABLE_MONTH_ROWS;
    private final float DENSITY = Utils.getScreenMetrics(getContext()).density;
    private OnSelectTimeListener mOnSelectTimeCallback = null;
    private Time mTime = null; // show years time in this view
    private TextView mTvYeasTitle = null; // yeas title, like 2013
    private YearGridLayoutView mYearGridView = null;

    public YearListItemView(Context context, int year, OnSelectTimeListener callback) {
        super(context);
        if (mTime == null) {
            mTime = new Time();
            mTime.setToNow();
        }
        mTime.year = year;
        mTime.normalize(false);

        mOnSelectTimeCallback = callback;

        int width = Utils.getScreenMetrics(getContext()).widthPixels;
        int height = Utils.getScreenMetrics(getContext()).heightPixels;
        TABLE_MONTH_COLUMNS = width < height ? 3 : 4;
        TABLE_MONTH_ROWS = width < height ? 4 : 3;

        initView();
    }

    private void initView() {
		//yuanding add it for PR:651124 20140418 start
    	if (AllInOneActivity.horizontalScreen) {//Horizontal screen
    		setOrientation(LinearLayout.HORIZONTAL);
    	} else {//Vertical screen
    		setOrientation(LinearLayout.VERTICAL);
    	}
		//yuanding add it for PR:651124 20140418 end
        setBackgroundColor(getResources().getColor(R.color.year_background));  //fff5f5f5
        
        initYearTitle();
        initGridView();
    }

    // init the year number title,like 2013
    private void initYearTitle() {
        if (mTvYeasTitle == null) {
            mTvYeasTitle = new TextView(getContext());
            mTvYeasTitle.setTextSize(getResources().getDimensionPixelSize(R.dimen.year_number_text_size) / DENSITY);
            mTvYeasTitle.setTextColor(getResources().getColor(R.color.year_number_color));
            mTvYeasTitle.setGravity(Gravity.LEFT);
            int padding = (int)getResources().getDimension(R.dimen.year_number_padding_left);
			//yuanding add it for PR:651124 20140418 start
            mTvYeasTitle.setPadding(padding, 10, 0, 5);
            //yuanding add it for PR:651124 20140418 end
            addView(mTvYeasTitle, 0);
        }
        int year = mTime.year;
        mTvYeasTitle.setText(String.valueOf(year));
    }

    // init month grid layout view
    private void initGridView() {
        if (mYearGridView == null) {
            mYearGridView = new YearGridLayoutView(getContext(), mTime, TABLE_MONTH_COLUMNS,
                    TABLE_MONTH_ROWS, mOnSelectTimeCallback);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            mYearGridView.setLayoutParams(params);
            addView(mYearGridView, 1);
        } else {
            mYearGridView.setTime(mTime);
        }

    }

    // set new time to view
    public void setTime(int year) {
        if (mTime != null) {
            mTime.year = year;
            mTime.normalize(false);
            initView();
        }
    }

}
