package com.android.calendar.bottombar;

import com.android.calendar.R;
import com.android.calendar.Utils;

import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class BottomBarCustomView extends LinearLayout {

    public static final int  ADD_TODAY_BUTTON = 0 ;
    public static final int  ADD_CREATE_EVENTS_BUTTON = 1 ;

    private ImageView mImageView ;

    public BottomBarCustomView(Context context , int mode , String timezome) {
           super(context, null , R.attr.bottomBarImageButtonStyle);
           setGravity(Gravity.CENTER);

           mImageView = new ImageView(context);

           if(mode == ADD_TODAY_BUTTON){

              mImageView.setImageResource(R.drawable.today_icon);
              LayerDrawable icon = (LayerDrawable) mImageView.getDrawable();
              Utils.setTodayIcon(icon, context, timezome);

           }else if (mode == ADD_CREATE_EVENTS_BUTTON){
              mImageView.setImageResource(R.drawable.ic_menu_add_event_holo_light);
           }

           addView(mImageView);
    }
}
