package com.android.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ViewSwitcher;

public class WeekDayHeaderView extends ViewGroup {

    private static final String TAG = "WeekDayHeaderView";
    private static final boolean DEBUG = true ;
    private final GestureDetector mGestureDetector;
    private final ViewSwitcher mViewSwitcher;
    private final ScrollInterpolator mHScrollInterpolator;
    /**
     * The initial state of the touch mode when we enter this view.
     */
    private static final int TOUCH_MODE_INITIAL_STATE = 0;
    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;

    /**
     * Indicates the touch gesture is a vertical scroll
     */
    private static final int TOUCH_MODE_VSCROLL = 0x20;

    /**
     * Indicates the touch gesture is a horizontal scroll
     */
    private static final int TOUCH_MODE_HSCROLL = 0x40;

    private static int mHorizontalSnapBackThreshold = 128 * 2;

    private int mContentHeight;
    private int mNumDays = 7;
    private int mViewWidth;
    private int mViewHeight;
    private int mViewStartX;
    private float mAnimationDistance = 0;

    private boolean mStartingScroll = false;
    private float mInitialScrollX;
    private float mInitialScrollY;
    private int mPreviousDirection;
    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;

    private int mFirstVisibleDate;
    private int mFirstDayOfWeek;
    private Time mBaseDate;
    private int mMonthLength;

    public WeekDayHeaderView(Context context,ViewSwitcher viewSwitcher){
       super(context);
       if (DEBUG) Log.e(TAG, "WeekDayHeaderView() method invoke");
       mGestureDetector = new GestureDetector(context, new CalendarGestureListener());
       mContentHeight = (int) context.getResources().getDimension(R.dimen.week_header_default_height);
       mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
       mHScrollInterpolator = new ScrollInterpolator();
       mViewSwitcher = viewSwitcher;

       mBaseDate = new Time(Utils.getTimeZone(context, null));
       long millis = System.currentTimeMillis();
       mBaseDate.set(millis);
       recalc();

       initSingleDayView(context);
       setWillNotDraw(false);
    }

    private void initSingleDayView(Context context){

      for(int day = 0 ; day < mNumDays ; day++){
          SingleDayHeaderView singleView = new SingleDayHeaderView(context);
          addView(singleView);
      }
    }

    //In the begin , we need to initialize the current view 's time.
    //not the next view. This method is invoked in the DayFragment
    private void updateSingleDayViewTime(){

      int childCount = getChildCount();

      for(int day = 0 ; day < childCount ; day++){
          SingleDayHeaderView selectedView = (SingleDayHeaderView) getChildAt(day);
          selectedView.setSingleTime(mBaseDate, day);
      }
    }
    public void setSelected(Time time, boolean ignoreTime, boolean animateToday) {
        if (DEBUG) Log.e(TAG, "setSelected() method invoke, time is "+time.toString());
        mBaseDate.set(time);
        recalc();
        //Maybe mBaseDate has changed ,
        //so we need to update every 
        //singleDay's time .Then we can know 
        //which day should be highlighted.
        updateSingleDayViewTime();
        //Update the single day highlighted as the time.
        updateSingleViewByTime(time);
        invalidate();
    }

    public void updateSingleViewByTime(Time t) {
      if(t == null)   return ;

      int childCount = getChildCount();

      for(int i = 0 ; i < childCount ; i++){
         WeekDayHeaderView currentHeader = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
         SingleDayHeaderView selectedView = (SingleDayHeaderView) currentHeader.getChildAt(i);
         Time time = selectedView.getTime();

         if(time == null) continue ;

         boolean isSameY = t.year == time.year;
         boolean isSameM = t.month == time.month;
         boolean isSameD = t.monthDay == time.monthDay; 

         if(isSameY && isSameM && isSameD){
            selectedView.isSelectedDay(true);
         }else{
            selectedView.isSelectedDay(false);
          }
      }
    } 

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
       final int childCount = getChildCount();

       int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	   // yuanding delete it for PR635523 20140404 start
       /*if (widthMode != MeasureSpec.EXACTLY) {
           throw new IllegalStateException(getClass().getSimpleName()+ 
                " can only be used "+ "with android:layout_width=\"match_parent\" (or fill_parent)");
       }  */
       //yuanding delete it for PR635523 20140404 end
       int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	   // yuanding delete it for PR635523 20140404 start
       /*if (heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }*/
       //yuanding delete it for PR635523 20140404 end
       int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
       int maxHeight = mContentHeight >= 0 ?mContentHeight : MeasureSpec.getSize(heightMeasureSpec);

       final int verticalPadding = getPaddingTop() + getPaddingBottom();
       final int paddingLeft = getPaddingLeft();
       final int paddingRight = getPaddingRight();
       final int height = maxHeight - verticalPadding;

       int availableWidth = contentWidth - paddingLeft - paddingRight;

       for(int i =0 ; i < childCount ; i++){
           View view = getChildAt(i);
           availableWidth = Math.max(0, availableWidth);
           view.measure(
                   MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                   MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
       // set the height and width for the weekday header view
       setMeasuredDimension(contentWidth, maxHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
       if (DEBUG) Log.d(TAG, "- onSizeChanged: w is "+w +", h is "+h+", oldw is "+oldw+", oldh is "+oldh);
       mViewWidth = w;
       mViewHeight = h ;
       mHorizontalSnapBackThreshold = w / 7;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
      if (DEBUG) Log.d(TAG, "- onLayout: left is "+l +", top is "+t+", right is "+r+", bottom is "+b);
      final int conentHeight = b - t - getPaddingBottom() - getPaddingTop();
      final int contentWidth = r - l - getPaddingRight() - getPaddingLeft();
      final int childCount = getChildCount();

      if (conentHeight <= 0 || contentWidth <= 0) {
        // Nothing to do , if we don't see anything
          return;
      }
      // should support ar ,fa iw layout
      final boolean isLayoutRtl = false ;

      int x = isLayoutRtl ? r - l - getPaddingRight() : getPaddingLeft();
      final int y = getPaddingTop();

      for(int i = 0 ; i < childCount ; i++){
         x += positionChild(getChildAt(i), x, y, conentHeight, isLayoutRtl); 
         x = next(x, 0, isLayoutRtl);
      }
    }

    private int next(int x, int val, boolean isRtl) {
       return isRtl ? x - val : x + val;
    }

    private int positionChild(View child, int x, int y, int contentHeight, boolean reverse) {
       int childWidth  = mViewWidth / getChildCount();
       int childHeight = child.getMeasuredHeight();
       int childTop = y + (contentHeight - childHeight) / 2;
       if (reverse) {
           child.layout(x - childWidth, childTop, x, childTop + childHeight);
       } else {
           child.layout(x, childTop, x + childWidth, childTop + childHeight);
        }

       return  (reverse ? -childWidth : childWidth);
    }

    private View switchViews(boolean forward, float xOffSet, float width, float velocity) {
       mAnimationDistance = width - xOffSet;

       float progress = Math.abs(xOffSet) / width;
       if (progress > 1.0f) {
           progress = 1.0f;
       }

       float inFromXValue, inToXValue;
       float outFromXValue, outToXValue;
       if (forward) {
           inFromXValue = 1.0f - progress;
           inToXValue = 0.0f;
           outFromXValue = -progress;
           outToXValue = -1.0f;
       } else {
           inFromXValue = progress - 1.0f;
           inToXValue = 0.0f;
           outFromXValue = progress;
           outToXValue = 1.0f;
       }

       // We have to allocate these animation objects each time we switch views
       // because that is the only way to set the animation parameters.
       TranslateAnimation inAnimation = new TranslateAnimation(
               Animation.RELATIVE_TO_SELF, inFromXValue,
               Animation.RELATIVE_TO_SELF, inToXValue,
               Animation.ABSOLUTE, 0.0f,
               Animation.ABSOLUTE, 0.0f);

       TranslateAnimation outAnimation = new TranslateAnimation(
               Animation.RELATIVE_TO_SELF, outFromXValue,
               Animation.RELATIVE_TO_SELF, outToXValue,
               Animation.ABSOLUTE, 0.0f,
               Animation.ABSOLUTE, 0.0f);

       long duration = calculateDuration(width - Math.abs(xOffSet), width, velocity);
       inAnimation.setDuration(duration);
       inAnimation.setInterpolator(mHScrollInterpolator);
       outAnimation.setInterpolator(mHScrollInterpolator);
       outAnimation.setDuration(duration);
       outAnimation.setAnimationListener(new GotoBroadcaster(forward));
       mViewSwitcher.setInAnimation(inAnimation);
       mViewSwitcher.setOutAnimation(outAnimation);

       WeekDayHeaderView view = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
       mViewSwitcher.showNext();
       view = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
       view.requestFocus();
       return view;
    }

    private class GotoBroadcaster implements Animation.AnimationListener {
        public boolean isForward = false ;

        public GotoBroadcaster(boolean forward){
          isForward = forward;
         }

        public void onAnimationEnd(Animation animation) {}
        public void onAnimationRepeat(Animation animation) {}

        public void onAnimationStart(Animation animation) {
            WeekDayHeaderView view = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
            view.mViewStartX = 0;
            SingleDayHeaderView selectedDayView  = view.findSelectedDayAtPosition(isForward ? 0 : 6);
            selectedDayView.upateDayView();

            view = (WeekDayHeaderView) mViewSwitcher.getNextView();
            view.mViewStartX = 0;
            view.clearFocusDay();
        }
    }

    public SingleDayHeaderView findSelectedDayAtPosition(int position){

        SingleDayHeaderView selectedDay = null ; 
        int childCount = getChildCount();

        for(int i = 0 ; i < childCount ; i++){

           SingleDayHeaderView selectedView = (SingleDayHeaderView) getChildAt(i);

           if(position == i){
              selectedView.isSelectedDay(true);
              selectedDay = selectedView;
           }else{
              selectedView.isSelectedDay(false);
            }
        }
        return selectedDay;
    }

    //clear out the focus , make the boolean value
    //isNeedDrawBackground is false;
    public void clearFocusDay(){
        int childCount = getChildCount();
        for(int i = 0 ; i < childCount ; i++){
           SingleDayHeaderView selectedView = (SingleDayHeaderView) getChildAt(i);
           selectedView.isSelectedDay(false);
        }
    }

    private static final int MINIMUM_SNAP_VELOCITY = 2200;

    private class ScrollInterpolator implements Interpolator {

       public ScrollInterpolator() {}

       public float getInterpolation(float t) {
           t -= 1.0f;
           t = t * t * t * t * t + 1;

           if ((1 - t) * mAnimationDistance < 1) {
               cancelAnimation();
           }
           return t;
       }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
      if (DEBUG) Log.e(TAG, "dispatchDraw method invoke");
      int saveCount = canvas.save();
      int count = getChildCount();
      for(int day = 0 ; day < count ; day++){

        if(mViewStartX >= 0){
            //slide the view in the left
             canvas.save();
             SingleDayHeaderView currentView = (SingleDayHeaderView) getChildAt(day);
             currentView.setMonthLength(mMonthLength);
             currentView.setNeedDrawDay(mFirstVisibleDate + day);
             currentView.setSingleTime(mBaseDate, day);
             canvas.translate(-mViewStartX, 0);
             drawChild(canvas, currentView, getDrawingTime());
             currentView.invalidate();
             canvas.restore();

             canvas.save();
             WeekDayHeaderView nextView = (WeekDayHeaderView) mViewSwitcher.getNextView();
             SingleDayHeaderView childnextView = (SingleDayHeaderView) nextView.getChildAt(day);
             childnextView.setMonthLength(nextView.mMonthLength);
             childnextView.setNeedDrawDay(nextView.mFirstVisibleDate + day);
             canvas.translate(mViewWidth-mViewStartX, 0);
             drawChild(canvas, childnextView, getDrawingTime());
             childnextView.invalidate();
             canvas.restore();
         }else{
            //slide the view in the right
             canvas.save();
             SingleDayHeaderView currentView = (SingleDayHeaderView) getChildAt(day);
             currentView.setMonthLength(mMonthLength);
             currentView.setNeedDrawDay(mFirstVisibleDate + day);
             currentView.setSingleTime(mBaseDate, day);
             canvas.translate(-mViewStartX, 0);
             drawChild(canvas, currentView, getDrawingTime());
             currentView.invalidate();
             canvas.restore();

             canvas.save();
             WeekDayHeaderView nextView = (WeekDayHeaderView) mViewSwitcher.getNextView();
             SingleDayHeaderView childnextView = (SingleDayHeaderView) nextView.getChildAt(day);
             childnextView.setMonthLength(nextView.mMonthLength);
             childnextView.setNeedDrawDay(nextView.mFirstVisibleDate + day);
             canvas.translate(-mViewWidth - mViewStartX, 0);
             drawChild(canvas, childnextView, getDrawingTime());
             childnextView.invalidate();
             canvas.restore();
          }
      }

      canvas.restoreToCount(saveCount);
    }

    private void cancelAnimation() {
       Animation in = mViewSwitcher.getInAnimation();
       if (in != null) {
           // cancel() doesn't terminate cleanly.
           in.scaleCurrentDuration(0);
        }
       Animation out = mViewSwitcher.getOutAnimation();
       if (out != null) {
           // cancel() doesn't terminate cleanly.
           out.scaleCurrentDuration(0);
        }
    }
    private long calculateDuration(float delta, float width, float velocity) {

       final float halfScreenSize = width / 2;
       float distanceRatio = delta / width;
       float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
       float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

       velocity = Math.abs(velocity);
       velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);

       long duration = 6 * Math.round(1000 * Math.abs(distance / velocity));

       return duration;
    }

    private float distanceInfluenceForSnapDuration(float f) {
       f -= 0.5f;
       f *= 0.3f * Math.PI / 2.0f;
       return (float) Math.sin(f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
     int action = event.getAction();

     switch (action) {
     case MotionEvent.ACTION_DOWN:
         mStartingScroll = true;
         mGestureDetector.onTouchEvent(event);
         return true;

     case MotionEvent.ACTION_MOVE:
         if (DEBUG) Log.e(TAG, "ACTION_MOVE Cnt=" + event.getPointerCount() + WeekDayHeaderView.this);
         mGestureDetector.onTouchEvent(event);
         return true;

     case MotionEvent.ACTION_UP:
         mStartingScroll = false;
         mGestureDetector.onTouchEvent(event);

         if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            mTouchMode = TOUCH_MODE_INITIAL_STATE;
            if (Math.abs(mViewStartX) > mHorizontalSnapBackThreshold) {
                // The user has gone beyond the threshold so switch views
                if (DEBUG) Log.d(TAG, "- horizontal scroll: switch views");
                switchViews(mViewStartX > 0, mViewStartX, mViewWidth, 0);
                mViewStartX = 0;
                return true;
            } else {
                if (DEBUG) Log.d(TAG, "- horizontal scroll: snap back");
                recalc();
                invalidate();
                mViewStartX = 0;
             }
          }
        return true;

        // This case isn't expected to happen.
     case MotionEvent.ACTION_CANCEL:
         if (DEBUG) Log.e(TAG, "ACTION_CANCEL");
         mGestureDetector.onTouchEvent(event);
         return true;

     default:
         if (DEBUG) Log.e(TAG, "Not MotionEvent " + event.toString());
         if (mGestureDetector.onTouchEvent(event)) {
             return true;
         }
        return super.onTouchEvent(event);
      }
    }

    class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
       @Override
       public boolean onSingleTapUp(MotionEvent ev) {
           if (DEBUG) Log.e(TAG, "GestureDetector.onSingleTapUp");
           WeekDayHeaderView.this.doSingleTapUp(ev);
           return true;
       }

       @Override
       public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
           if (DEBUG) Log.e(TAG, "GestureDetector.onScroll");
           WeekDayHeaderView.this.doScroll(e1, e2, distanceX, distanceY);
           return true;
       }

       @Override
       public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
           if (DEBUG) Log.e(TAG, "GestureDetector.onFling");

           WeekDayHeaderView.this.doFling(e1, e2, velocityX, velocityY);
           return true;
       }

       @Override
       public boolean onDown(MotionEvent ev) {
           if (DEBUG) Log.e(TAG, "GestureDetector.onDown");
           WeekDayHeaderView.this.doDown(ev);
           return true;
       }
   }

    public void doSingleTapUp(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        //hong.zhan modified it for PR:684586 20140609 start
        int whichView = calculateChildViewFromLocation(x,y);
        if(whichView==-1){
        	for(int i=0;i<5;i++){
        		x = ev.getX();
        		y = ev.getY();
        		whichView = calculateChildViewFromLocation(x,y);
        		if(whichView!=-1){
        			break;
        		}
        	}
        }
        if (DEBUG) Log.e(TAG, "GestureDetector.doSingleTapUp : whichView is :"+whichView);
        SingleDayHeaderView selectedDayView = null;
        try {
			selectedDayView = findSelectedDayAtPosition(whichView);
		} catch (NullPointerException e) {
			e.printStackTrace();
			selectedDayView = findSelectedDayAtPosition(0);
		}
        // //hong.zhan modified it for PR:684586 20140609 end
        if(selectedDayView != null){
        		selectedDayView.upateDayView();
          }

        invalidate();
    }

    public void doDown(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_DOWN;
        mViewStartX = 0;
    }

    public void doFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) {
        cancelAnimation();
        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            // Horizontal fling.
            mTouchMode = TOUCH_MODE_INITIAL_STATE;
            if (DEBUG) Log.d(TAG, "doFling: velocityX " + velocityX);
            int deltaX = (int) e2.getX() - (int) e1.getX();
            switchViews(deltaX < 0, mViewStartX, mViewWidth, velocityX);
            mViewStartX = 0;

            return;
        }

    }

    public void doScroll(MotionEvent e1, MotionEvent e2, float deltaX,float deltaY) {
       cancelAnimation();
       if (mStartingScroll) {
           mInitialScrollX = 0;
           mInitialScrollY = 0;
           mStartingScroll = false;
        }

       mInitialScrollX += deltaX;
       mInitialScrollY += deltaY;
       int distanceX = (int) mInitialScrollX;
       int distanceY = (int) mInitialScrollY;

       // If we haven't figured out the predominant scroll direction yet,
       // then do it now.
       if (mTouchMode == TOUCH_MODE_DOWN) {
           int absDistanceX = Math.abs(distanceX);
           int absDistanceY = Math.abs(distanceY);
           mPreviousDirection = 0 ;

           if (absDistanceX > absDistanceY) {
               mTouchMode = TOUCH_MODE_HSCROLL;
               mViewStartX = distanceX;
               initNextView(-mViewStartX);
           } else {
               mTouchMode = TOUCH_MODE_VSCROLL;
            }
       } else if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
          // We are already scrolling horizontally, so check if we
          // changed the direction of scrolling so that the other week
          // is now visible.
          mViewStartX = distanceX;
          if (distanceX != 0) {
              int direction = (distanceX > 0) ? 1 : -1;
              if (direction != mPreviousDirection) {
                  initNextView(-mViewStartX);
                  mPreviousDirection = direction;
              }
          }
       }
      //update the UI
      invalidate();
   }

    private boolean initNextView(int deltaX) {
        // Change the view to the previous day or week
        WeekDayHeaderView view = (WeekDayHeaderView) mViewSwitcher.getNextView();
        Time date = view.mBaseDate;
        date.set(mBaseDate);
        boolean switchForward;
        if (deltaX > 0) {
            date.monthDay -= mNumDays;
            switchForward = false;
        } else {
            date.monthDay += mNumDays;
            switchForward = true;
        }
        date.normalize(true);
        view.recalc();
        view.layout(getLeft(), getTop(), getRight(), getBottom());
        return switchForward;
    }

    private void recalc() {
        mBaseDate.normalize(true);
        adjustToBeginningOfWeek(mBaseDate);

        mFirstVisibleDate = mBaseDate.monthDay;
        if (DEBUG) Log.e(TAG, "recalc() method invoke , mFirstVisibleDate is "+mFirstVisibleDate);
        mMonthLength = mBaseDate.getActualMaximum(Time.MONTH_DAY);

    }

    private void adjustToBeginningOfWeek(Time time) {
        int dayOfWeek = time.weekDay;
        int diff = dayOfWeek - mFirstDayOfWeek;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            time.monthDay -= diff;
            time.normalize(true);
        }
    }

    private int calculateChildViewFromLocation(float x ,float y){
       float xLocation = x ;
       float yLocation = y ;
       //calculate the child view's width
       int width = mViewWidth / getChildCount();
       //hong.zhan modify it for PR:671509 20140512 start
       int viewWidth=width*getChildCount();
	   if(xLocation < mViewWidth &&  yLocation < mViewHeight){
		   int i;
		   if(xLocation>=viewWidth){
			   i=(int)(xLocation / width)-1;
		   }else{
			   i = (int) (xLocation / width) ;
		   }
		   	return i ;
	    	}
	   //hong.zhan modify it for PR:671509 20140512 end
	   return -1 ;
    }

    public void onDayViewScroll(Time t){
 
       Time time = new Time(t);
       adjustToBeginningOfWeek(time);

       int firstVisibleDate = time.monthDay;
       int offset = firstVisibleDate - mFirstVisibleDate;
       if(offset == 0){
          updateSingleViewByTime(t);
          invalidate();
       }else{
          //need some animation support
          int diff = compareToVisibleTimeRange(t);
          switchViews(diff > 0, t);
       }
    }

    public int compareToVisibleTimeRange(Time time) {

        int savedHour = mBaseDate.hour;
        int savedMinute = mBaseDate.minute;
        int savedSec = mBaseDate.second;

        mBaseDate.hour = 0;
        mBaseDate.minute = 0;
        mBaseDate.second = 0;

        if (DEBUG) {
            Log.d(TAG, "Begin " + mBaseDate.toString());
            Log.d(TAG, "Diff  " + time.toString());
        }

        // Compare beginning of range
        int diff = Time.compare(time, mBaseDate);
        if (diff > 0) {
            // Compare end of range
            mBaseDate.monthDay += mNumDays;
            mBaseDate.normalize(true);
            diff = Time.compare(time, mBaseDate);

            if (DEBUG) Log.d(TAG, "End   " + mBaseDate.toString());

            mBaseDate.monthDay -= mNumDays;
            mBaseDate.normalize(true);
            if (diff < 0) {
                // in visible time
                diff = 0;
            } else if (diff == 0) {
                // Midnight of following day
                diff = 1;
            }
        }

        if (DEBUG) Log.d(TAG, "Diff: " + diff);

        mBaseDate.hour = savedHour;
        mBaseDate.minute = savedMinute;
        mBaseDate.second = savedSec;
        return diff;
    }

    public View switchViews(boolean forward , Time goTime) {
        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;

        if (forward) {
            inFromXValue = 1.0f;
            inToXValue = 0.0f;
            outFromXValue = 0;
            outToXValue = -1.0f;
        } else {
            inFromXValue = -1.0f;
            inToXValue = 0.0f;
            outFromXValue = 0;
            outToXValue = 1.0f;
        }

        TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        long duration = 400;
        inAnimation.setDuration(duration);
        outAnimation.setDuration(duration);
        outAnimation.setAnimationListener(new GotoBroadcasterl());
        mViewSwitcher.setInAnimation(inAnimation);
        mViewSwitcher.setOutAnimation(outAnimation);

        WeekDayHeaderView view = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
        mViewSwitcher.showNext();
        view = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
        view.setSelected(goTime, true, false);
        view.requestFocus();

        return view;
    }

    private class GotoBroadcasterl implements Animation.AnimationListener {
        public void onAnimationEnd(Animation animation) {}
        public void onAnimationRepeat(Animation animation) {}
        public void onAnimationStart(Animation animation) {
            WeekDayHeaderView view = (WeekDayHeaderView) mViewSwitcher.getCurrentView();
            view.mViewStartX = 0;
            view = (WeekDayHeaderView) mViewSwitcher.getNextView();
            view.mViewStartX = 0;
            view.clearFocusDay();
        }
    }
}