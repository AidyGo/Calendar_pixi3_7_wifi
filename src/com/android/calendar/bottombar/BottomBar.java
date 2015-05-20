package com.android.calendar.bottombar;

import android.app.FragmentTransaction;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class BottomBar {

    /**
     * Create and return a new {@link Tab}.
     * This tab will not be included in the bottom bar until it is added.
     *
     * <p>Very often tabs will be used to switch between {@link Fragment}
     * objects.  Here is a typical implementation of such tabs:</p>
     *
     * @return A new Tab
     *
     * @see #addTab(Tab)
     */
    public abstract Tab newTab();

    /**
     * The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    public abstract void addTab(Tab tab);

    /**
     * The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    public abstract void addTab(Tab tab, boolean setSelected);

    /**
     *  The tab will be inserted at <code>position</code>.
     *  If this is the first tab to be added it will become
     *  the selected tab.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    public abstract void addTab(Tab tab, int position);

    /**
     * The tab will be insterted at <code>position</code>.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    public abstract void addTab(Tab tab, int position, boolean setSelected);

    /**
     * Remove a tab from the bottom bar. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param tab The tab to remove
     */
    public abstract void removeTab(Tab tab);

    /**
     * Remove a tab from the bottom bar. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param position Position of the tab to remove
     */
    public abstract void removeTabAt(int position);

    /**
     * Remove all tabs from the bottom bar and deselect the current tab.
     */
    public abstract void removeAllTabs();

    /**
     * Select the specified tab. If it is not a child of this bottom bar it will be added.
     *
     * @param tab Tab to select
     */
    public abstract void selectTab(Tab tab);

    /**
     * Returns the currently selected tab and there is at least
     * one tab present.
     *
     * @return The currently selected tab or null
     */
    public abstract Tab getSelectedTab();

    /**
     * Returns the tab at the specified index.
     *
     * @param index Index value in the range 0-get
     * @return
     */
    public abstract Tab getTabAt(int index);

    /**
     * Returns the number of tabs currently registered with the action bar.
     * @return Tab count
     */
    public abstract int getTabCount();

    /**
     * A tab in the bottom bar.
     *
     * <p>Tabs manage the hiding and showing of {@link Fragment}s.
     */
    public static abstract class Tab {
        /**
         * An invalid position for a tab.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        /**
         * Return the current position of this tab in the bottom bar.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in
         *         the bottom bar.
         */
        public abstract int getPosition();

        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        public abstract Drawable getIcon();

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        public abstract CharSequence getText();

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         * @return The current instance for call chaining
         */
        public abstract Tab setIcon(Drawable icon);

        /**
         * Set the icon displayed on this tab.
         *
         * @param resId Resource ID referring to the drawable to use as an icon
         * @return The current instance for call chaining
         */
        public abstract Tab setIcon(int resId);

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not
         * room to display the entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        public abstract Tab setText(CharSequence text);

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not
         * room to display the entire string.
         *
         * @param resId A resource ID referring to the text that should be displayed
         * @return The current instance for call chaining
         */
        public abstract Tab setText(int resId);

        /**
         * Set a custom view to be used for this tab. This overrides values set by
         * {@link #setText(CharSequence)} and {@link #setIcon(Drawable)}.
         *
         * @param view Custom view to be used as a tab.
         * @return The current instance for call chaining
         */
        public abstract Tab setCustomView(View view);

        /**
         * Set a custom view to be used for this tab. This overrides values set by
         * {@link #setText(CharSequence)} and {@link #setIcon(Drawable)}.
         *
         * @param layoutResId A layout resource to inflate and use as a custom tab view
         * @return The current instance for call chaining
         */
        public abstract Tab setCustomView(int layoutResId);

        /**
         * Retrieve a previously set custom view for this tab.
         *
         * @return The custom view set by {@link #setCustomView(View)}.
         */
        public abstract View getCustomView();

        /**
         * Give this Tab an arbitrary object to hold for later use.
         *
         * @param obj Object to store
         * @return The current instance for call chaining
         */
        public abstract Tab setTag(Object obj);

        /**
         * @return This Tab's tag object.
         */
        public abstract Object getTag();

        /**
         * Set the {@link TabListener} that will handle switching to and from this tab.
         * All tabs must have a TabListener set before being added to the BottomBar.
         *
         * @param listener Listener to handle tab selection events
         * @return The current instance for call chaining
         */
        public abstract Tab setTabListener(TabListener listener);

        /**
         * Select this tab. Only valid if the tab has been added to the bottom bar.
         */
        public abstract void select();

        /**
         * Set a description of this tab's content for use in accessibility support.
         * If no content description is provided the title will be used.
         *
         * @param resId A resource ID referring to the description text
         * @return The current instance for call chaining
         * @see #setContentDescription(CharSequence)
         * @see #getContentDescription()
         */
        public abstract Tab setContentDescription(int resId);

        /**
         * Set a description of this tab's content for use in accessibility support.
         * If no content description is provided the title will be used.
         *
         * @param contentDesc Description of this tab's content
         * @return The current instance for call chaining
         * @see #setContentDescription(int)
         * @see #getContentDescription()
         */
        public abstract Tab setContentDescription(CharSequence contentDesc);

        /**
         * Gets a brief description of this tab's content for use in accessibility support.
         *
         * @return Description of this tab's content
         * @see #setContentDescription(CharSequence)
         * @see #setContentDescription(int)
         */
        public abstract CharSequence getContentDescription();
    }

    /**
     * Callback interface invoked when a tab is focused, unfocused, added, or removed.
     */
    public interface TabListener {
        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         * @param ft A {@link FragmentTransaction} for queuing fragment operations to execute
         *        during a tab switch. The previous tab's unselect and this tab's select will be
         *        executed in a single transaction. This FragmentTransaction does not support
         *        being added to the back stack.
         */
        public void onTabSelected(Tab tab, FragmentTransaction ft);

        /**
         * Called when a tab exits the selected state.
         *
         * @param tab The tab that was unselected
         * @param ft A {@link FragmentTransaction} for queuing fragment operations to execute
         *        during a tab switch. This tab's unselect and the newly selected tab's select
         *        will be executed in a single transaction. This FragmentTransaction does not
         *        support being added to the back stack.
         */
        public void onTabUnselected(Tab tab, FragmentTransaction ft);

        /**
         * Called when a tab that is already selected is chosen again by the user.
         * Some applications may use this action to return to the top level of a category.
         *
         * @param tab The tab that was reselected.
         * @param ft A {@link FragmentTransaction} for queuing fragment operations to execute
         *        once this method returns. This FragmentTransaction does not support
         *        being added to the back stack.
         */
        public void onTabReselected(Tab tab, FragmentTransaction ft);
    }
	
}
