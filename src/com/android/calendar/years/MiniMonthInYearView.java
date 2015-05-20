
package com.android.calendar.years;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.android.calendar.Utils;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.LocaleData;
import com.android.calendar.R;

/**
 * this class draw a mini month in year calendar list item
 * 
 * @author archermind
 */
public class MiniMonthInYearView extends View {
    private static final boolean DEBUG = false;
    private final String TAG = MiniMonthInYearView.class.getSimpleName();
    private float density = Utils.getScreenMetrics(getContext()).density;

    /**
     * default callback with on selected one day
     */
    private OnSelectTimeListener mOnSelectTimeCallback = new OnSelectTimeListener() {

        @Override
        public void onSelectTime(Time time) {
            if (DEBUG)
                Log.d(TAG, "click day: " + time.format3339(true));
        }
    };

    private Time mTime = null;
    private Paint mPaintMonthNumber = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mPaintMini = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mPaintToday = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int MINI_ROWS = 6;
    private final int MINI_COLUMNS = 7;
    private Rect mMiniRect = new Rect(); // each day number draw rect
    private Rect mRect = new Rect();

    private final int paddingLeft = (int) (10 * density);
    private final int paddingRight = (int) (10 * density);
    private final int paddingTop = (int) (6 * density);
    private final int paddingBottom = (int) (6 * density);

    // real draw day number rect
    private int mRealWidth = 0;
    private int mRealHeight = 0;

    private final int CLICK_RADIUS = (int) (6 * density);

    private int mOffset = 0;
    private int mFirstDayOfWeek = 0;

    /**
     * compute first day of month offset with First day of week
     * 
     * @param time
     */
    private void computeOffset(Time time) {
        mFirstDayOfWeek = LocaleData.get(Locale.getDefault()).firstDayOfWeek;
        String language = Locale.getDefault().getLanguage();
        if ("ar".equals(language)) {
            mFirstDayOfWeek = 2;// MONDEY is the first day of the week
        }

        TimeZone tz = TimeZone.getTimeZone(time.timezone);
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(time.toMillis(false));
        c.setFirstDayOfWeek(mFirstDayOfWeek);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.MONTH, this.getId());
        int offset = c.get(Calendar.DAY_OF_WEEK) - c.getFirstDayOfWeek();
        if (offset < 0) {
            offset += 7;
        }
        mOffset = offset;
    }

    public MiniMonthInYearView(Context context) {
        this(context, null, null);
    }

    public MiniMonthInYearView(Context context, Time time, OnSelectTimeListener callback) {
        super(context);

        if (time == null) {
            time = new Time();
            time.setToNow();
        }
        mTime = time;

        if (callback != null) {
            mOnSelectTimeCallback = callback;
        }

        computeOffset(mTime);
        init();
    }

    // init something
    private void init() {
        int font = (int) getResources().getDimension(R.dimen.month_number_font_size);
        mPaintMonthNumber.setTextSize(font);
        mPaintMonthNumber.setColor(getResources().getColor(R.color.month_number_color));

        font = (int) getResources().getDimension(R.dimen.mini_day_number_font_size);
        mPaintMini.setTextSize(font);
        mPaintMini.setColor(getResources().getColor(R.color.mini_day_color));

        mPaintToday.setColor(getResources().getColor(R.color.today_color));

        setOnTouchListener(mOnTouchListener);
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int height = bottom - top;
        mRect.set(0, 0, width, height);

        mRealWidth = width - paddingLeft - paddingRight;
        mRealHeight = height - paddingTop - paddingBottom;
        int miniW = mRealWidth / MINI_COLUMNS;
        int miniH = mRealHeight / MINI_ROWS;
        mMiniRect.set(0, 0, miniW, miniH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawMonthNum(canvas);
        drawAllDay(canvas);
    }

    /**
     * draw month number
     * 
     * @param canvas
     */
    private void drawMonthNum(Canvas canvas) {
        // draw month number
        String month = String.valueOf(this.getId() + 1);
        Rect r = getStrRect(month, mPaintMonthNumber);

        int x = (mRect.width() - r.width()) / 2;
        int y = (mRect.height() - r.height()) / 2;
		//yuanding add it for PR:651124 20140418 start
        canvas.drawText(month, x - r.left, y - r.top - 14, mPaintMonthNumber);
		//yuanding add it for PR:651124 20140418 end
    }

    /**
     * draw all day number [0-31]
     * 
     * @param canvas
     */
    private void drawAllDay(Canvas canvas) {
        int days = mTime.getActualMaximum(Time.MONTH_DAY);
        for (int i = 0; i < MINI_ROWS; ++i) {
            for (int j = 0; j < MINI_COLUMNS; ++j) {
                int day = i * MINI_COLUMNS + j + 1;
                if (days + mOffset >= day && mOffset < day) {
                    boolean today = isToday(day - mOffset);
                    drawDay(canvas, i, j, today);
                }
            }
        }
    }

    /**
     * draw a specify day with row & column
     * 
     * @param canvas
     * @param row
     * @param column
     * @param today
     */
    private void drawDay(Canvas canvas, int row, int column, boolean today) {
        int startX = paddingLeft + column * mMiniRect.width();
        int startY = paddingTop + row * mMiniRect.height();

        int day = row * MINI_COLUMNS + column + 1 - mOffset;
        String dayStr = String.valueOf(day);

        Rect dayRect = getStrRect(dayStr, mPaintMini);
        int drawX = startX + (mMiniRect.width() - dayRect.width()) / 2;
        int drawY = startY + (mMiniRect.height() - dayRect.height()) / 2;

        // draw today indicator
        if (today) {
            Rect bck = new Rect(startX, startY, startX + mMiniRect.width(), startY
                    + mMiniRect.height());
            canvas.drawRect(bck, mPaintToday);

            mPaintMini.setColor(getResources().getColor(R.color.today_font_color));
            canvas.drawText(dayStr, drawX - dayRect.left, drawY - dayRect.top, mPaintMini);
            mPaintMini.setColor(getResources().getColor(R.color.mini_day_color));
        } else {
            canvas.drawText(dayStr, drawX - dayRect.left, drawY - dayRect.top, mPaintMini);
        }
    }

    /**
     * get string rect by paint
     * 
     * @param str
     * @param paint
     * @return
     */
    private Rect getStrRect(String str, Paint paint) {
        Rect r = new Rect();
        paint.getTextBounds(str.toCharArray(), 0, str.toCharArray().length, r);
        return r;
    }

    /**
     * the day is today?
     * 
     * @param day
     * @return
     */
    private boolean isToday(int day) {
        Time time = new Time();
        time.setToNow();
        if (day == time.monthDay && mTime.year == time.year && mTime.month == time.month)
            return true;

        return false;
    }

    private Point mTouchPoint = new Point();
    // the day touch
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    mTouchPoint.x = (int) event.getX();
                    mTouchPoint.y = (int) event.getY();
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (Math.abs(mTouchPoint.x - (int) event.getX()) <= CLICK_RADIUS
                            && Math.abs(mTouchPoint.y - (int) event.getY()) <= CLICK_RADIUS) {
                        // compute touched day by touch event
                        int day = computeTouchDay(event);

                        if (day > 0) {
                            Time time = new Time(mTime);
                            time.set(day, mTime.month, mTime.year);
                            // callback click day listener
                            mOnSelectTimeCallback.onSelectTime(time);
                        } 
                    }
                    break;
                }
            }
            return true;
        }
    };

    /**
     * compute the day you touched
     * 
     * @param event
     * @return
     */
    private int computeTouchDay(MotionEvent event) {
        // touch point
        int tx = (int) event.getX();
        int ty = (int) event.getY();
        int days = mTime.getActualMaximum(Time.MONTH_DAY); // max days in this month

        int realX = tx;
        int realY = ty;

        // is touch point in vaild rect
        if (tx > paddingLeft && tx < (this.getWidth() - paddingRight)
                && ty > paddingTop && ty < (this.getHeight() - paddingBottom)) {
            realX = tx - paddingLeft;
            realY = ty - paddingTop;

            int x = realX / mMiniRect.width();
            int y = realY / mMiniRect.height();

            int day = (y * MINI_COLUMNS + x) + 1 - mOffset;

            if (day <= days) {
                return day;
            }
        }
        return -1;
    }

    /**
     * remove view, can to do some release
     */
    public void onRemove() {
        // Log.i(TAG, "MiniMonthInYearView onRemove id=" + this.getId());
    }

    /**
     * if system time be changed, redraw month calendar view
     */
    public void onSystemTimeChanged() {
        reDraw();
    }

    /**
     * set new time
     * 
     * @param time
     */
    public void setTime(Time time) {
        if (time == null) {
            return;
        }
        mTime = time;
        computeOffset(mTime);

        reDraw();
    }

    /**
     * post a redraw msg
     */
    void reDraw() {
        this.postInvalidate();
    }
}
