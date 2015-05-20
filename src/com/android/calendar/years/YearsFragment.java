
package com.android.calendar.years;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.calendar.CalendarController;
import com.android.calendar.Utils;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.R;

//import com.android.calendar.R;

/**
 * this fragment to show 1970-2036 years and months in each years
 */
public class YearsFragment extends Fragment implements OnSelectTimeListener,
        CalendarController.EventHandler {
    final String TAG = YearsFragment.class.getSimpleName();
    private Activity mActivity = null;
    private Context mContext = null;
    private ListView mYearListView = null;
    private YearsCalendarAdapter mAdapter = null;
    private CalendarController mController;
    private Time mTime = new Time();
    private boolean mInited = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mController = CalendarController.getInstance(getActivity());
    }
	//yuanding add it for PR:651111 20140416 start
	public YearsFragment() {
		       }
    //yuanding add it for PR:651111 20140416 end
    public YearsFragment(Context context, long timeMillis) {
        mContext = context;
        mTime = Utils.getValidTimeInCalendar(context, timeMillis);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        view = inflater.inflate(R.layout.calendar_years_list, null);
        initListView(view);

        return view;
    }

    /**
     * init list adapter and something
     * 
     * @param v
     */
    void initListView(View v) {
        mYearListView = (ListView) v.findViewById(R.id.listview_years);
        mYearListView.setDivider(null);
        mYearListView.setScrollbarFadingEnabled(false);
        if (mAdapter == null) {
            mAdapter = new YearsCalendarAdapter(mActivity, this);
        }
        mYearListView.setAdapter(mAdapter);
        mInited = true;
        // goto today
        gotoDate(mTime);
    }

    /**
     * goto the specify day,if null,goto today
     * 
     * @param t
     */
    private void gotoDate(Time t) {
        if (t == null) {
            t = mController.getCalendarTime();
        }
        // specify year is vaild? [1970--2036]
        if (t.year < CalendarController.MIN_CALENDAR_YEAR
                || t.year > CalendarController.MAX_CALENDAR_YEAR) {
            return;
        }

        setListViewPostion(t);
    }

    /**
     * set list pos to spec time
     * 
     * @param time
     */
    private void setListViewPostion(Time time) {
        int pos = mAdapter.getPositionByTime(time);
        mYearListView.setSelection(pos);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * call back when selected one day in MiniMonthInYearView
     */
    @Override
    public void onSelectTime(Time time) {
        mController.sendEvent(this, EventType.GO_TO, time, time, -1, ViewType.MONTH);
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO;
    }

    @Override
    public void handleEvent(EventInfo event) {
        if (!mInited) {
            return;
        }

        if (event.eventType == EventType.GO_TO) {
            if (event.selectedTime != null) {
                mTime.set(event.selectedTime);
            } else if (event.startTime != null) {
                mTime.set(event.startTime);
            }
            gotoDate(mTime);
        }
    }

    @Override
    public void eventsChanged() {

    }
}
