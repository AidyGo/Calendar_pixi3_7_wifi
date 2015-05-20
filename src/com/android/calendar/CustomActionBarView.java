
package com.android.calendar;

import android.content.Context;
import android.content.res.Configuration;    //hong.zhan add it for PR:661525 20140428
import libcore.icu.LocaleData;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;           //hong.zhan add it for PR:661525 20140428
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.android.calendar.R;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.bottombar.BottomBarCustomView;     //hong.zhan add it for PR:661525 20140428

import java.util.Locale;

public class CustomActionBarView {

    private RelativeLayout mLayout;
    private Context mContext;
    private ImageButton mBtnIntoYear;
    private TextView mTvYear;
    private TextView mTvMonth;
    private ImageButton mAddEvent;		//hong.zhan add it for PR:661525 20140428
    private ImageButton mBtnSearch;

    private int mCurrentView;
    private static String[] sShortMonths;
    private static String[] sLongMonths;
    private boolean mLdrtl = false;

    public CustomActionBarView(Context context) {
        mContext = context;

        LocaleData localeData = LocaleData.get(Locale.getDefault());
        sShortMonths = localeData.shortMonthNames;
      //add and remove by xiaoyu.qian for pr:831159 begin
        //sLongMonths = localeData.longMonthNames;
        sLongMonths=localeData.longStandAloneMonthNames;
      //add and remove by xiaoyu.qian for pr:831159 end
        //if locale is ar,fa,iw, layout is right to left
        String lan = Locale.getDefault().getLanguage();
        if ("ar".equals(lan) || "iw".equals(lan) || "fa".equals(lan)) {
            mLdrtl = true;
        }
        
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = (RelativeLayout) inflater.inflate(R.layout.custom_actionbar, null);

        mBtnIntoYear = (ImageButton) mLayout.findViewById(R.id.btn_into_year);
        mBtnSearch = (ImageButton) mLayout.findViewById(R.id.btn_search);
        //modify by canzhang.zhan for PR 768567 start 
		//hong.zhan add it for PR:661525 20140428 start
        if(mContext.getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE){
        	mAddEvent=(ImageButton)mLayout.findViewById(R.id.btn_add_event);	
        	mAddEvent.setVisibility(View.VISIBLE);
        }
		//hong.zhan add it for PR:661525 20140428 end
        //modify by canzhang.zhan for PR 768567 end
        mTvYear = (TextView) mLayout.findViewById(R.id.tv_year);
        mTvMonth = (TextView) mLayout.findViewById(R.id.tv_month);
        
    }

    /**
     * get custom action bar 
     * @return
     */
    public View getCustomActionBarView() {
        return mLayout;
    }

    /*
     * set intoyear btn listener
     */
    public void setIntoYearListener(View.OnClickListener listener) {
        mBtnIntoYear.setOnClickListener(listener);
    }

    /*
     * set search btn listener
     */
    public void setSearchListener(View.OnClickListener listener) {
        mBtnSearch.setOnClickListener(listener);
    }

    /*
     * set addevent btn listener  hong.zhan add it for PR:661525 20140428
     */
    public void setAddEventListener(View.OnClickListener listener){
    	mAddEvent.setOnClickListener(listener);
    }
	
    /**
     * if sub fragment changed, update actionbar layout to just new fragment
     * 
     * @param type
     */
    public void updateView(int type) {
        switch (type) {
            case ViewType.MONTH: {
                updateToMonth();
                break;
            }
            case ViewType.DAY:
            case ViewType.WEEK:
            case ViewType.AGENDA: {
                updateToWeekDay();
                break;
            }
            case ViewType.YEAR: {
                updateToYear();
                break;
            }
            case ViewType.CURRENT: {
                break;
            }
        }

        mCurrentView = type;
    }

    void updateToMonth() {
        mBtnIntoYear.setVisibility(View.VISIBLE);
        mTvYear.setVisibility(View.VISIBLE);
        mTvMonth.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams pm = (LayoutParams) mTvMonth.getLayoutParams();
        pm.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
        pm.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        pm.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        pm.setMargins(0, 0, 0, 0);
        mTvMonth.setLayoutParams(pm);
    }

    void updateToYear() {
        mBtnIntoYear.setVisibility(View.GONE);
        mTvYear.setVisibility(View.GONE);
        mTvMonth.setVisibility(View.GONE);
    }

    void updateToWeekDay() {
        mBtnIntoYear.setVisibility(View.GONE);
        mTvYear.setVisibility(View.GONE);
        mTvMonth.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams pm = (LayoutParams) mTvMonth.getLayoutParams();
        pm.addRule(RelativeLayout.CENTER_IN_PARENT, 0);

        if (mLdrtl) {
            pm.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            pm.setMargins(0, 0, 15, 0);
        }else{
            pm.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            pm.setMargins(15, 0, 0, 0);
        }
        mTvMonth.setLayoutParams(pm);
    }

    /**
     * update title when time changed
     */
    public void updateTitle(Time time) {
        String year = time.format("%Y");
        mTvYear.setText(year);

        String monthLong = sLongMonths[time.month];
        mTvMonth.setText(monthLong);
    }
}
