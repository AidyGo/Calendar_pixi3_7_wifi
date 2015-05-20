package com.android.calendar.bottombar;

import java.util.ArrayList;

import com.android.calendar.R;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

public class BottomBarImpl extends BottomBar {

    private static final int INVALID_POSITION = -1;

    private Context mContext;
    private Activity mActivity;
    private TabImpl mSelectedTab;
    private TabContainerView mTabContainerView;
    private BottomBarView mBottomBarView;
    private ArrayList<TabImpl> mTabs = new ArrayList<TabImpl>();

    private int mSavedTabPosition = INVALID_POSITION;

    public BottomBarImpl(Activity activity) {
        mActivity = activity;
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        init(decor);
    }

    private void init(View decor) {
       mContext = decor.getContext();
       // init the UI work
       mBottomBarView = (BottomBarView) decor.findViewById(R.id.bottom_bar);
    }

    private void ensureTabsExist() {
       if (mTabContainerView != null) {
           return;
        }

       TabContainerView tabScroller = new TabContainerView(mContext);
       mBottomBarView.setEmbeddedTabView(tabScroller);
       mTabContainerView = tabScroller;
    }

   @Override
   public Tab newTab() {
       return new TabImpl();
   }

   @Override
   public void addTab(Tab tab) {
       addTab(tab, mTabs.isEmpty());
   }

   @Override
   public void addTab(Tab tab, boolean setSelected) {
       ensureTabsExist();
       mTabContainerView.addTab(tab, setSelected);
       configureTab(tab, mTabs.size());

       if (setSelected) {
           selectTab(tab);
        }
   }

   @Override
   public void addTab(Tab tab, int position) {
       addTab(tab, position, mTabs.isEmpty());
   }

   @Override
   public void addTab(Tab tab, int position, boolean setSelected) {
       ensureTabsExist();
       mTabContainerView.addTab(tab, position, setSelected);
       configureTab(tab, position);

       if (setSelected) {
           selectTab(tab);
        }
    }

    @Override
    public void removeTab(Tab tab) {
        removeTabAt(tab.getPosition());
    }

    @Override
    public void removeTabAt(int position) {
       if (mTabContainerView == null) {
          // No tabs around to remove
          return;
        }

       int selectedTabPosition = mSelectedTab != null ? mSelectedTab
           .getPosition() : mSavedTabPosition;
       mTabContainerView.removeTabAt(position);
       TabImpl removedTab = mTabs.remove(position);

       if (removedTab != null) {
           removedTab.setPosition(-1);
        }

       final int newTabCount = mTabs.size();

       for (int i = position; i < newTabCount; i++) {
            mTabs.get(i).setPosition(i);
        }

       if (selectedTabPosition == position) {
           selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0,position - 1)));
        }

    }

    @Override
    public void removeAllTabs() {
      if (mSelectedTab != null) {
          selectTab(null);
       }

      mTabs.clear();

      if (mTabContainerView != null) {
          mTabContainerView.removeAllTabs();
      }
      mSavedTabPosition = INVALID_POSITION;
    }

    private void configureTab(Tab tab, int position) {
       final TabImpl tabi = (TabImpl) tab;
       final BottomBar.TabListener callback = tabi.getCallback();

       if (callback == null) {
           throw new IllegalStateException("Bottom Bar Tab must have a Callback");
        }

       tabi.setPosition(position);
       mTabs.add(position, tabi);

       final int count = mTabs.size();

       for (int i = position + 1; i < count; i++) {
            mTabs.get(i).setPosition(i);
        }
    }

    @Override
    public void selectTab(Tab tab) {
        final FragmentTransaction trans = mActivity.getFragmentManager().beginTransaction().disallowAddToBackStack();

        if (mSelectedTab == tab) {
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabReselected(mSelectedTab, trans);
                mTabContainerView.animateToTab(tab.getPosition());
              }
        } else {
            mTabContainerView.setTabSelected(tab != null ? tab.getPosition(): Tab.INVALID_POSITION);

            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabUnselected(mSelectedTab, trans);
              }

            mSelectedTab = (TabImpl) tab;

            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabSelected(mSelectedTab, trans);
              }
        }

      if (!trans.isEmpty()) {
           trans.commit();
       }
    }

    @Override
    public Tab getSelectedTab() {
       return mSelectedTab;
    }

    @Override
    public Tab getTabAt(int index) {
       return mTabs.get(index);
    }

    @Override
    public int getTabCount() {
       return mTabs.size();
    }

    public class TabImpl extends BottomBar.Tab {
       private BottomBar.TabListener mCallback;
       private Object mTag;
       private Drawable mIcon;
       private CharSequence mText;
       private CharSequence mContentDesc;
       private int mPosition = -1;
       private View mCustomView;

       @Override
       public int getPosition() {
          return mPosition;
       }

       public void setPosition(int position) {
          mPosition = position;
       }

       @Override
       public Drawable getIcon() {
          return mIcon;
       }

       public BottomBar.TabListener getCallback() {
          return mCallback;
       }

       @Override
       public CharSequence getText() {
          return mText;
       }

       @Override
       public Tab setIcon(Drawable icon) {
          mIcon = icon;

          if (mPosition >= 0) {
            mTabContainerView.updateTab(mPosition);
            }
         return this;
        }

       @Override
       public Tab setIcon(int resId) {
          return setIcon(mContext.getResources().getDrawable(resId));
        }

       @Override
       public Tab setText(CharSequence text) {
           mText = text;

           if (mPosition >= 0) {
               mTabContainerView.updateTab(mPosition);
            }
           return this;
        }

       @Override
       public Tab setText(int resId) {
           return setText(mContext.getResources().getText(resId));
       }

       @Override
       public Tab setCustomView(View view) {
          mCustomView = view;

          if (mPosition >= 0) {
              mTabContainerView.updateTab(mPosition);
          }
          return this;
       }

       @Override
       public Tab setCustomView(int layoutResId) {
          // TODO Auto-generated method stub
          return setCustomView(LayoutInflater.from(mContext).inflate(layoutResId, null));
       }

       @Override
       public View getCustomView() {
          return mCustomView;
       }

       @Override
       public Tab setTag(Object obj) {
          mTag = obj;
          return this;
      }

       @Override
       public Object getTag() {
          return mTag;
      }

       @Override
       public Tab setTabListener(TabListener listener) {
           mCallback = listener;
           return this;
       }

       @Override
       public void select() {
           selectTab(this);
       }

       @Override
       public Tab setContentDescription(int resId) {
           return setContentDescription(mContext.getResources().getText(resId));
        }

       @Override
       public Tab setContentDescription(CharSequence contentDesc) {
           mContentDesc = contentDesc;

           if (mPosition >= 0) {
               mTabContainerView.updateTab(mPosition);
            }
           return this;
        }

       @Override
       public CharSequence getContentDescription() {
           return mContentDesc;
       }

    }
}
