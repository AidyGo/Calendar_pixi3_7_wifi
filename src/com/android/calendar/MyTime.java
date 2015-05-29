/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.calendar;

import android.text.format.Time;

public class MyTime {

    /**
     * This array is indexed by the weekDay field (SUNDAY=0, MONDAY=1, etc.) and gives a number that
     * can be added to the yearDay to give the closest Wednesday yearDay.
     */
    private static final int[] sWednesdayOffset = {
            3, 2, 1, 0, -1, -2, -3
    };

    /**
     * This array is indexed by the weekDay field (SUNDAY=0, MONDAY=1, etc.) and gives a number that
     * can be added to the yearDay to give the closest Tuesday yearDay.
     */
    private static final int[] sTuesdayOffset = {
            2, 1, 0, -1, -2, -3, 3
    };

    /**
     * This array is indexed by the weekDay field (SUNDAY=0, MONDAY=1, etc.) and gives a number that
     * can be added to the yearDay to give the closest Tuesday yearDay.
     */
    private static final int[] sMondayOffset = {
            -6, 0, -1, -2, -3, -4, -5
    };

    /**
     * This array is indexed by the weekDay field (SUNDAY=0, MONDAY=1, etc.) and gives a number that
     * can be added to the yearDay to give the closest Tuesday yearDay.
     */
    private static final int[] sSaturdayOffset = {
            -1, -2, -3, -4, -5, -6, 0
    };

    /**
     * This array is indexed by the weekDay field (SUNDAY=0, MONDAY=1, etc.) and gives a number that
     * can be added to the yearDay to give the closest Tuesday yearDay.
     */
    private static final int[] sSundayOffset = {
            0, -1, -2, -3, -4, -5, -6
    };

    public static int getWeekNumber(int firstDayOfWeek, String firstWeekOfYear, Time time) {
        int weekNumber = 0;
        /**
         * Sunday have been customized as the first day of a week. And first week of the year start
         * on first 4-day week.
         */
        if (firstDayOfWeek == 0 && "1".equals(firstWeekOfYear)) {
            // Get the year day for the closest Wednesday
            int closestWednesday = time.yearDay + sWednesdayOffset[time.weekDay];

            // Year days start at 0
            if (closestWednesday >= 0 && closestWednesday <= 364) {
                weekNumber = closestWednesday / 7 + 1;
            }

            // The week crosses a year boundary.
            Time temp = new Time(time);
            temp.monthDay += sWednesdayOffset[time.weekDay];
            temp.normalize(true /* ignore isDst */);
            weekNumber = temp.yearDay / 7 + 1;
        } else if (firstDayOfWeek == 6 && "1".equals(firstWeekOfYear)) {
            /**
             * Saturday have been customized as the first day of a week. And first week of the year
             * start on first 4-day week.
             */
            // Get the year day for the closest Tuesday
            int closestTuesday = time.yearDay + sTuesdayOffset[time.weekDay];

            // Year days start at 0
            if (closestTuesday >= 0 && closestTuesday <= 364) {
                weekNumber = closestTuesday / 7 + 1;
            }

            // The week crosses a year boundary.
            Time temp = new Time(time);
            temp.monthDay += sTuesdayOffset[time.weekDay];
            temp.normalize(true /* ignore isDst */);
            weekNumber = temp.yearDay / 7 + 1;
        } else if (firstDayOfWeek == 0 && "2".equals(firstWeekOfYear)) {
            /**
             * Sunday have been customized as the first day of a week. And the first week of the
             * year start on first full week.
             */
            // Get the year day for the closest Sunday
            int closestSunday = time.yearDay + sSundayOffset[time.weekDay];

            // Year days start at 0
            if (closestSunday >= 0 && closestSunday <= 364) {
                weekNumber = closestSunday / 7 + 1;
            }

            // The week crosses a year boundary.
            Time temp = new Time(time);
            temp.monthDay += sSundayOffset[time.weekDay];
            temp.normalize(true /* ignore isDst */);
            weekNumber = temp.yearDay / 7 + 1;
        } else if (firstDayOfWeek == 1 && "2".equals(firstWeekOfYear)) {
            /**
             * Monday have been customized as the first day of a week. And the first week of the
             * year start on first full week.
             */
            // Get the year day for the closest Monday
            int closestMonday = time.yearDay + sMondayOffset[time.weekDay];

            // Year days start at 0
            if (closestMonday >= 0 && closestMonday <= 364) {
                weekNumber = closestMonday / 7 + 1;
            }

            // The week crosses a year boundary.
            Time temp = new Time(time);
            temp.monthDay += sMondayOffset[time.weekDay];
            temp.normalize(true /* ignore isDst */);
            weekNumber = temp.yearDay / 7 + 1;
        } else if (firstDayOfWeek == 6 && "2".equals(firstWeekOfYear)) {
            /**
             * Saturday have been customized as the first day of a week. And the first week of the
             * year start on first full week.
             */
            // Get the year day for the closest Saturday
            int closestSaturday = time.yearDay + sSaturdayOffset[time.weekDay];

            // Year days start at 0
            if (closestSaturday >= 0 && closestSaturday <= 364) {
                weekNumber = closestSaturday / 7 + 1;
            }

            // The week crosses a year boundary.
            Time temp = new Time(time);
            temp.monthDay += sSaturdayOffset[time.weekDay];
            temp.normalize(true /* ignore isDst */);
            weekNumber = temp.yearDay / 7 + 1;
        }
        return weekNumber;
    }

}
