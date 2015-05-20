package com.android.calendar;

import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.mediatek.calendar.lunar.LunarUtil;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.Log;
import android.view.View;

public class SingleDayHeaderView extends View {

    private static final String TAG = "SingleDayHeaderView";
    private static final boolean DEBUG = true;
    private float DAY_TEXT_FONT_SIZE = 18;
    private int TEXT_SIZE_LUNAR = 12;
    private float mScale = 0;

    private int mNeedDrawDay = -1 ;
    private int mMonthLength;
    private boolean isNeedDrawBackground = false ;
    private Rect r = new Rect();
    private Paint mSelectedDayPaint = new Paint();
    private Paint mTodayPaint = new Paint();
    private Paint mWeekNumPaint = new Paint();
    private Paint mLunarPaint = new Paint();
    private Paint p = new Paint();
    private int mWeekNumColor;
    private boolean isTodayNumber = false ;
    private Time mTime;

    private CalendarController mController;

    private final boolean mCanShowLunar;
    private final LunarUtil mLunarUtil;
    private String mLunarText;

    private int mDaySeparatorInnerColor;
    private int mDayPressedColor;
    private int mTodayColor;
    private int DAY_SEPARATOR_INNER_WIDTH = 1;
    private int WEEK_HEADER_NAME_HEIGHT = 20 ;
    private int DAY_TOP_PADDING = 8 ;
    private int LUNAR_TOP_PADDING = 3 ;
    private static final int mClickedAlpha = 128;


    public SingleDayHeaderView(Context context) {
       super(context);
       Resources resources = getContext().getResources();
       DAY_TEXT_FONT_SIZE = resources.getInteger(R.integer.text_size_week_number);
       mWeekNumColor = resources.getColor(R.color.month_day_number);
       mDaySeparatorInnerColor = resources.getColor(R.color.month_grid_lines);
       mDayPressedColor = resources.getColor(R.color.day_clicked_background_color/*R.color.today_background_color*/);
       mTodayColor = resources.getColor(R.color.today_background_color);

       if (mScale == 0) {
           mScale = context.getResources().getDisplayMetrics().density;
           if (mScale != 1) {
              DAY_TEXT_FONT_SIZE *= mScale;
              DAY_SEPARATOR_INNER_WIDTH *=mScale;
              WEEK_HEADER_NAME_HEIGHT *= mScale;
              DAY_TOP_PADDING *= mScale;
              LUNAR_TOP_PADDING *= mScale;
              TEXT_SIZE_LUNAR *= mScale;
           }
       }


       mController  = CalendarController.getInstance(context);
       mLunarUtil = LunarUtil.getInstance(context);
       mCanShowLunar = mLunarUtil.canShowLunarCalendar();

       mWeekNumPaint.setFakeBoldText(false);
       mWeekNumPaint.setAntiAlias(true);
       mWeekNumPaint.setTextSize(DAY_TEXT_FONT_SIZE);
       mWeekNumPaint.setStyle(Style.FILL);
       mWeekNumPaint.setTypeface(Typeface.DEFAULT);

       mLunarPaint.setFakeBoldText(false);
       mLunarPaint.setAntiAlias(true);
       mLunarPaint.setTextSize(TEXT_SIZE_LUNAR);
       mLunarPaint.setStyle(Style.FILL);

       mSelectedDayPaint.setFakeBoldText(false);
       mSelectedDayPaint.setAntiAlias(true);
       mSelectedDayPaint.setStyle(Style.FILL);
       mSelectedDayPaint.setColor(mDayPressedColor);
       mSelectedDayPaint.setAlpha(mClickedAlpha);
       
       mTodayPaint.setFakeBoldText(false);
       mTodayPaint.setAntiAlias(true);
       mTodayPaint.setStyle(Style.FILL);
       mTodayPaint.setColor(mTodayColor);
    }

	@Override
    protected void onDraw(Canvas canvas) {
       if(DEBUG) Log.i(TAG, "onDraw");
       drawToday(canvas);
       drawSelectedDay(canvas);
       drawDaySeparators(canvas);

       if(isTodayNumber){
          mWeekNumPaint.setColor(Color.WHITE);
          mLunarPaint.setColor(Color.WHITE);
       }else{
          mWeekNumPaint.setColor(mWeekNumColor);
          mLunarPaint.setColor(mWeekNumColor);
        }

       String dateNumStr = String.valueOf(mNeedDrawDay);
       float stringWidth = mWeekNumPaint.measureText(dateNumStr);

       mWeekNumPaint.setAntiAlias(true);
       float x =(getWidth()-stringWidth)/2 ;
       float y = getTop() + WEEK_HEADER_NAME_HEIGHT + DAY_TOP_PADDING ;
       canvas.drawText(dateNumStr,x , y, mWeekNumPaint);

       if(mCanShowLunar && mLunarText != null){
         FontMetrics fm = mLunarPaint.getFontMetrics();
         float lunarWidth = mLunarPaint.measureText(mLunarText);
         float lunarHeight = (float) Math.ceil(fm.descent - fm.top) + 2;
         x = (getWidth()-lunarWidth)/2 ;
         y = y + lunarHeight + LUNAR_TOP_PADDING ;
         String[] words = mLunarText.split(LunarUtil.DELIM);
         canvas.drawText(words[0],x , y, mLunarPaint);
       }
    }

    private void drawDaySeparators(Canvas canvas) {
        p.setAntiAlias(true);
        p.setStyle(Style.FILL);
        p.setColor(mDaySeparatorInnerColor);
        p.setStrokeWidth(DAY_SEPARATOR_INNER_WIDTH);

        int startX = DAY_SEPARATOR_INNER_WIDTH ;
        int startY = getTop();
        int stopX  = startX ;
        int stopY  = startY + getHeight();
        canvas.drawLine(startX,startY, stopX,stopY, p);
    }

    private void drawSelectedDay(Canvas canvas) {
        if (isNeedDrawBackground && !isTodayNumber) {
            r.left = DAY_SEPARATOR_INNER_WIDTH;
            r.right = getWidth();
            r.top = 0;
            r.bottom = getHeight();
            canvas.drawRect(r, mSelectedDayPaint);
        }
    }

    private void drawToday(Canvas canvas) {
        if (isTodayNumber) {
            r.left = DAY_SEPARATOR_INNER_WIDTH;
            r.right = getWidth();
            r.top = 0;
            r.bottom = getHeight();
            canvas.drawRect(r, mTodayPaint);
        }
    }
    
    public void isSelectedDay(boolean isSelected){
        isNeedDrawBackground = isSelected;

    }
    public void setNeedDrawDay(int day){
       if (day > mMonthLength) {
           mNeedDrawDay = day- mMonthLength;
        }else{
           mNeedDrawDay = day;
        }
    }

    public void setMonthLength(int length){
       mMonthLength = length;
    }

    public void setSingleTime(Time time, int day){
       Time temp = new Time(time);
       temp.monthDay+=day;
       temp.normalize(true);

       mTime = temp ;
       mLunarText = mLunarUtil.getLunarFestivalChineseString(mTime.year, mTime.month + 1, mTime.monthDay);

       Time currentTime = new Time(mTime.timezone);
       currentTime.set(System.currentTimeMillis());
       boolean isSameY = mTime.year == currentTime.year;
       boolean isSameM = mTime.month == currentTime.month;
       boolean isSameD = mTime.monthDay == currentTime.monthDay;

       isTodayNumber = isSameY && isSameM && isSameD ;

       if (DEBUG) Log.i(TAG, "setSingleTime() method invoke, mTime is "+mTime.toString()+", mLunarText is :"+mLunarText);
    }

    public void upateDayView(){
       if(isNeedDrawBackground && mTime != null){
          int viewType = ViewType.CURRENT;
          long extras = CalendarController.EXTRA_GOTO_TIME;
          mController.sendEvent(this, EventType.GO_TO, mTime, null, mTime, -1, viewType, extras, null, null);
       }
    }

    public Time getTime(){
       return mTime;
    }
}
