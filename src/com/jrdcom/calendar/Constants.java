/*********************************************************************************************************/
/*                                                                   Date : 10/2012 */
/*                                   PRESENTATION                                   */
/*                     Copyright (c) 2011 JRD Communications, Inc.                  */
/*********************************************************************************************************/
/*                                                                                                       */
/*           This material is company confidential, cannot be reproduced in any     */
/*           form without the written permission of JRD Communications, Inc.        */
/*                                                                                                       */
/*=======================================================================================================*/
/*   Author :  Neo Skunkworks-Kevin CHEN                                            */
/*   Role :    Calendar                                                             */
/*   Reference documents :                                                          */
/*=======================================================================================================*/
/* Comments:                                                                        */
/*   file  :  Constants.java                                                        */
/* Labels  :                                                                        */
/*=======================================================================================================*/
/* Modifications   (month/day/year)                                                 */
/*=======================================================================================================*/
/* date      | author                    |FeatureID    |modification                */
/*==============|===================================|================|====================================*/
/* 09/06/12  | Neo Skunkworks-Kevin CHEN | CR-267788   |                            */
/*=======================================================================================================*/
/* Problems Report(PR/CR)                                                           */
/*=======================================================================================================*/
/* date    | author       | PR #                       |                            */
/*============|==================|===================================|===================================*/
/*            |                  |                                   |                                   */
/*=======================================================================================================*/

package com.jrdcom.calendar;

public class Constants {

    protected static final String APP_ID = "103916456377473";

    protected static final int REQUEST_SUCCESS = -1;

    protected static final int REQUEST_AUTH_FAIL = -2;

    protected static final int REQUEST_FAIL = -3;

    protected static final String FACEBOOK_SESSION_ACTION = "com.orange.facebooksetting.session";

    protected static final String FACEBOOK_SESSION_TOKEN = "FacebookSettingSessionToken";

    protected static final String FACEBOOK_SESSION_EXPIRES = "FacebookSettingSessionExpires";

    protected static final String FACEBOOK_SESSION_REQUEST_ACTION = "com.orange.facebooksetting.REQUEST_SESSION";

    protected static final String FACEBOOK_REQUEST_EXCEPTION = "RequestException";

    protected static final String FACEBOOK_REQUEST_PERMISSIONS = "RequestPermissions";

    protected static final int ADVANCE_IN_MINUTES = 15 * 60;

    /**
     * use for create facebook calendar
     */
    protected static final String CALENDAR_TYPE_LOCAL = "LOCAL";

    protected static final int CALENDAR_DIRTY_FB = 1;

    protected static final int CALENDAR_COLOR_FB = 3368703;

    protected static final int CALENDAR_ACCESS_LEVEL_FB = 700;

    protected static final int CALENDAR_SYNC_EVENTS_FB = 1;

    // PR-312668-Neo skunkworks-Kevin CHEN-001 begin
    /**
     * facebook friends birthday default year . It must be a leap year,and must
     * between 1970 and 2037 as a reference
     */
    public static final int BIRTHDAY_DEFAULT_YEAR = 1972;
    // PR-312668-Neo skunkworks-Kevin CHEN-001 end

}
