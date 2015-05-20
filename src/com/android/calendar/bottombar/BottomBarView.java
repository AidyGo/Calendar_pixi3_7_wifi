package com.android.calendar.bottombar;

import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import android.content.Context;
import android.content.res.Configuration;    //hong.zhan add it for PR:661525 20140429
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class BottomBarView extends ViewGroup {

    public static final String TAG = "BottomBarView";

    private TabContainerView mTabContainerView;
    private BottomBarCustomView mTodayIconView ;
    private BottomBarCustomView mCreateEventsView;
    private static DisplayMetrics mDisplayMetrics ;
    private int mBottomBarPadding;
    protected CalendarController mController;
    private int mContentHeight;
    private int mCustomViewWidth ;
    private String mTimeZone;

    private static final int BOTTOM_BAR_PADDING = 10;

    public BottomBarView(Context context, AttributeSet attrs) {
         super(context, attrs);
         mController  = CalendarController.getInstance(context);
         mTimeZone = Utils.getTimeZone(context, null);
         mContentHeight = (int) context.getResources().getDimension(R.dimen.bottom_bar_default_height);
         mDisplayMetrics = getResources().getDisplayMetrics();
         mBottomBarPadding = (int) (BOTTOM_BAR_PADDING * mDisplayMetrics.density);
         mCustomViewWidth = mContentHeight + mBottomBarPadding;

         mTodayIconView = new BottomBarCustomView(context,BottomBarCustomView.ADD_TODAY_BUTTON,mTimeZone);
         mTodayIconView.setOnClickListener(mImageViewLisntener);
         addView(mTodayIconView,  new LinearLayout.LayoutParams(mCustomViewWidth ,LayoutParams.MATCH_PARENT));

         //hong.zhan modify it for PR:661525 20140429 start
         if(context.getResources().getConfiguration().orientation!= Configuration.ORIENTATION_LANDSCAPE){
        	 mCreateEventsView = new BottomBarCustomView(context,BottomBarCustomView.ADD_CREATE_EVENTS_BUTTON,mTimeZone);
        	 mCreateEventsView.setOnClickListener(mImageViewLisntener);
        	 addView(mCreateEventsView,  new LinearLayout.LayoutParams(mCustomViewWidth ,LayoutParams.MATCH_PARENT));
         }
		 //hong.zhan modify it for PR:661525 20140429 end
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
       final int conentHeight = b - t - getPaddingBottom() - getPaddingTop();
       final int contentWidth = r - l - getPaddingRight() - getPaddingLeft();

       if (conentHeight <= 0) {
           // Nothing to do , if we don't see anything
           return;
        }

       final boolean isLayoutRtl = isLayoutRtl() ;// should support ar ,fa iw layout
       // In LTR mode, we start from left padding and go to the right; in RTL mode, we start
       // from the padding right and go to the left (in reverse way)
       int x = isLayoutRtl ? r - l - getPaddingRight() : getPaddingLeft();
       final int y = getPaddingTop();

       int horizontalGap = (contentWidth - mTodayIconView.getMeasuredWidth() * 2 - 
                 mTabContainerView.getMeasuredWidth())/2 ;

       if (mTodayIconView != null) {
           x += positionChild(mTodayIconView, x, y, conentHeight, isLayoutRtl);
           x = next(x, horizontalGap, isLayoutRtl);
       }

       if (mTabContainerView != null) {
           x += positionChild(mTabContainerView, x , y, conentHeight, isLayoutRtl);
           x = next(x, horizontalGap, isLayoutRtl);
        }

       if (mCreateEventsView != null) {
           x += positionChild(mCreateEventsView, x , y, conentHeight, isLayoutRtl);
           x = next(x, horizontalGap, isLayoutRtl);
       }
       //other for custom view to need complete
    }

    private int positionChild(View child, int x, int y, int contentHeight, boolean reverse) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        if (reverse) {
            child.layout(x - childWidth, childTop, x, childTop + childHeight);
        } else {
            child.layout(x, childTop, x + childWidth, childTop + childHeight);
        }

        return  (reverse ? -childWidth : childWidth);
    }
	
    private int next(int x, int val, boolean isRtl) {
        return isRtl ? x - val : x + val;
    }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
       // TODO Auto-generated method stub
       final int childCount = getChildCount();

       int widthMode = MeasureSpec.getMode(widthMeasureSpec);

       if (widthMode != MeasureSpec.EXACTLY) {
           throw new IllegalStateException(getClass().getSimpleName()+ 
                " can only be used "+ "with android:layout_width=\"match_parent\" (or fill_parent)");
       }

       int heightMode = MeasureSpec.getMode(heightMeasureSpec);
       if (heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

       int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
       int maxHeight = mContentHeight >= 0 ?mContentHeight : MeasureSpec.getSize(heightMeasureSpec);

       Log.i(TAG, "onMeasure() : mContentHeight is "+mContentHeight+", contentWidth is "+contentWidth);

       final int verticalPadding = getPaddingTop() + getPaddingBottom();
       final int paddingLeft = getPaddingLeft();
       final int paddingRight = getPaddingRight();
       final int height = maxHeight - verticalPadding;

       int availableWidth = contentWidth - paddingLeft - paddingRight;

       if (mTabContainerView != null) {
           availableWidth = Math.max(0, availableWidth);
           mTabContainerView.measure(
                   MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                   MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
       }

       if(mTodayIconView != null){
    	   mTodayIconView.measure(
                   MeasureSpec.makeMeasureSpec(mCustomViewWidth , MeasureSpec.EXACTLY),
                   MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
       }
       if(mCreateEventsView != null){
          mCreateEventsView.measure(
                    MeasureSpec.makeMeasureSpec(mCustomViewWidth , MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
       //need to do custom view

       if (mContentHeight <= 0) {
           int measuredHeight = 0;
           for (int i = 0; i < childCount; i++) {
               View v = getChildAt(i);
               int paddedViewHeight = v.getMeasuredHeight() + verticalPadding;
               if (paddedViewHeight > measuredHeight) {
                   measuredHeight = paddedViewHeight;
               }
           }
           setMeasuredDimension(contentWidth, measuredHeight);
       } else {
           setMeasuredDimension(contentWidth, maxHeight);
        }
       
    }

    public void setEmbeddedTabView(TabContainerView tabs) {
       if (mTabContainerView != null) {
           removeView(mTabContainerView);
        }
       mTabContainerView = tabs;
       addView(mTabContainerView);
       ViewGroup.LayoutParams lp = mTabContainerView.getLayoutParams();
       lp.width = LayoutParams.WRAP_CONTENT;
       lp.height = LayoutParams.MATCH_PARENT;
       tabs.setAllowCollapse(false);
    }

    private final OnClickListener mImageViewLisntener = new OnClickListener() {
        @Override
        public void onClick(View v) {
          Time t = null;
          int viewType = ViewType.CURRENT;
          long extras = CalendarController.EXTRA_GOTO_TIME;

          if(v == mTodayIconView){
              viewType = ViewType.CURRENT;
              t = new Time(mTimeZone);
              t.setToNow();
              extras |= CalendarController.EXTRA_GOTO_TODAY;
              mController.sendMessageHandler(CalendarController.MESSAGE_ID_GOTO_TIME, t);
          }else if(v == mCreateEventsView){
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
              return;
          }
          mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
        }};

}
