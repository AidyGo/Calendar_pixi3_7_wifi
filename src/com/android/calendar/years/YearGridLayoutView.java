
package com.android.calendar.years;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

/**
 * this gridlayout to show 12 mini month in yearlistitemview
 * 
 * @author archermind
 */
public class YearGridLayoutView extends GridLayout {
    final String TAG = YearGridLayoutView.class.getSimpleName();
    private final int MONTHS = 12;
    private int mColumns = 3;
    private int mRows = 4;
    private Time mTime = null;
    private MiniMonthInYearView mViews[] = new MiniMonthInYearView[MONTHS];

    private OnSelectTimeListener mOnSelectTimeCallback = null;

    public YearGridLayoutView(Context context) {
        this(context, null, 0, 0, null);
    }

    public YearGridLayoutView(Context context, Time time, int columns, int rows,
            OnSelectTimeListener callback) {
        super(context);
        if (time == null) {
            time = new Time();
            time.setToNow();
        }
        mTime = time;
        mOnSelectTimeCallback = callback;
        // defaults: 4 * 3
        mColumns = columns <= 0 ? YearListItemView.TABLE_MONTH_COLUMNS : columns;
        mRows = rows <= 0 ? YearListItemView.TABLE_MONTH_ROWS : rows;

        registTimeReceiver();
    }

    // set new time
    public void setTime(Time time) {
        if (time == null) {
            return;
        }
        mTime = time;
        layoutViews();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mTimeReceiver);
        clearAllViews();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            layoutViews(right - left, bottom - top);
        }
    }

    void layoutViews() {
        int width = this.getWidth();
        int height = this.getHeight();

        if (width > 0 && height > 0)
            layoutViews(width, height);
    }

    // layout all child view
    void layoutViews(int w, int h) {
        // child view's width & height
        int width = w / mColumns;
        int height = h / mRows;

        initView(width, height);
    }

    void initView(int w, int h) {
        if (mColumns <= 0 || mRows <= 0)
            return;

        // clear all old view at first
        // clearAllViews();

        this.setColumnCount(mColumns);
        this.setRowCount(mRows);

        synchronized (mViews) {
            for (int r = 0; r < mRows; ++r) {
                for (int c = 0; c < mColumns; ++c) {
                    int index = r * mColumns + c;
                    Time time = new Time(mTime);
                    time.month = index;
                    
                    MiniMonthInYearView view = mViews[index];
                    if (view == null) {
                        view = new MiniMonthInYearView(this.getContext(), time,
                                mOnSelectTimeCallback);
                        mViews[index] = view;
                        view.setId(index);
                        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(w, h);
                        view.setLayoutParams(params);
                        addView(view, index);

                        // must layout child view, to fix the bug that some
                        // monthview not invoke the onLayout(...) to draw
                        int left = w * c;
                        int right = left + w;
                        int top = h * r;
                        int bottom = top + h;
                        view.layout(left, top, right, bottom);
                    }
                    view.setTime(time);
                }
            }
        }
    }

    void postDrawAllMonth() {
        int count = this.getChildCount();
        for (int i = 0; i < count; ++i) {
            View view = this.getChildAt(i);
            view.postInvalidate();
        }
    }

    // clear all minimonth view from gridlayout
    void clearAllViews() {
        synchronized (mViews) {
            for (int i = 0; i < MONTHS; ++i) {
                MiniMonthInYearView view = mViews[i];
                if (view != null) {
                    view.onRemove();
                    mViews[i] = null;
                }
            }
            removeAllViews();
        }
    }

    // regist time change receiver
    void registTimeReceiver() {
        IntentFilter it = new IntentFilter();
        it.addAction(Intent.ACTION_TIME_CHANGED);
        it.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        this.getContext().registerReceiver(mTimeReceiver, it);
    }

    TimeReceiver mTimeReceiver = new TimeReceiver();

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mViews) {
                for (int i = 0; i < MONTHS; ++i) {
                    MiniMonthInYearView view = mViews[i];
                    if (view != null) {
                        view.onSystemTimeChanged();
                    }
                }
            }
        }
    }

}
