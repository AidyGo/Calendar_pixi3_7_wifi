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

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class EventDragView extends View {

    private Context mContext;
    private boolean mIsDraging;
    private int mStartY = 0;
    private int screenWidth, screenHeight, mLastX, mLastY;
    private DragListener mListener = null;
    
    interface DragListener {
        public void onDragStart();
        public void onDraging(int deltaY);
        public void onDragEnd();
    }

    public EventDragView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }
    
    /**
     * 
     * @param listener
     */
    public void setListener(DragListener listener) {
        mListener = listener;
    }
    
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        final int action = ev.getAction();
        final int action_code = action & MotionEvent.ACTION_MASK;

        int[] viewLoc = new int[2];
        this.getLocationInWindow(viewLoc);
        int touchX, touchY;

        touchX = (int) (viewLoc[0] + ev.getX());
        touchY = (int) (viewLoc[1] + ev.getY());

        switch (action_code) {
            case MotionEvent.ACTION_DOWN:
                mIsDraging = true;
                mStartY = touchY;
                if (mListener != null) {
                    mListener.onDragStart();
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mIsDraging) {
                    int deltaY = mStartY - touchY;
                    if (mListener != null) {
                        mListener.onDraging(deltaY);
                    }
                }
                break;
                
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsDraging) {
                    mIsDraging = false;
                    if (mListener != null) {
                        mListener.onDragEnd();
                    }
                }
                break;
        }
        return true;
    }

}
