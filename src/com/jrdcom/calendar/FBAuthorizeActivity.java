/*********************************************************************************************************/
/*                                                                   Date : 09/2012 */
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
/*   file  :  FBAuthorizeActivity.java                                              */
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.view.KeyEvent;

import com.android.calendar.R;
import com.android.calendar.Utils;

public class FBAuthorizeActivity extends Activity {
    private static final String TAG = "FBAuthorizeActivity";

    public static final String EXTRA_RETRY_SESSION = "isRetry";

    public static final String EXTRA_FKEY_INVOKE = "fKeyInvoke";

    public static final String EXTRA_IGNORE_WHILE_RUN = "ignoreWhileRun";

    private boolean mBeRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBeRetry = getIntent().getBooleanExtra(EXTRA_RETRY_SESSION, false);

        boolean fKeyInvoke = getIntent().getBooleanExtra(EXTRA_FKEY_INVOKE, false);
        boolean ignoreWhileRun = getIntent().getBooleanExtra(EXTRA_IGNORE_WHILE_RUN, false);

        if (!Utils.isFacebookLogin(this) || !Utils.IS_FACEBOOK_DEVICE) {
            finish();
            return;
        }

        if (FBBirthdayImportService.isImporting()) {
            if (ignoreWhileRun) {
                finish();
                return;
            }

            if (!mBeRetry) {
                if (FBBirthdayImportService.isInRetrySession(FBAuthorizeActivity.this)) {
                    FBBirthdayImportService.cancelRetry(FBAuthorizeActivity.this);
                }
            }
            Intent intent = new Intent(FBBirthdayImportReporter.ACTION_IMPORT_PROGRESS);
            startActivity(intent);
            finish();
        } else {
            if (fKeyInvoke && isFBFriendsBirthdayImported()) {
                showFBFirendsBirthdayList();
            } else if (fKeyInvoke) {
                showDialog(R.string.import_birthday);
            } else {
                startImport();
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (id == R.string.import_birthday) {
            return new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage(R.string.import_birthday_msg)
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startImport();
                        }
                    }).setOnKeyListener(new DialogInterface.OnKeyListener() {
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_BACK:
                                    finish();
                                default:
                                    break;
                            }
                            return true;
                        }
                    }).create();
        }
        return super.onCreateDialog(id, args);
    }

    private boolean isFBFriendsBirthdayImported() {
        String selection = Events.FBFRIEND_ID + " IS NOT NULL AND " + Events.DELETED + "=0";
        Cursor cursor = getContentResolver().query(Events.CONTENT_URI, new String[] {
            Events._ID
        }, selection, null, null);
        try {
            if (cursor != null && cursor.getCount() > 0) {
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private void showFBFirendsBirthdayList() {
        Intent intent = new Intent(this, FBFriendBirthdayListActivity.class);
        startActivity(intent);
        finish();
    }

    private void startImport() {
        // if in start import manually, stop retry session first
        if (!mBeRetry) {
            if (FBBirthdayImportService.isInRetrySession(FBAuthorizeActivity.this)) {
                FBBirthdayImportService.cancelRetry(FBAuthorizeActivity.this);
            }
        }

        // TODO maybe we need choose which calendar to save the birthday events
        // first.
        Intent service = new Intent(this, FBBirthdayImportService.class);
        startService(service);
        finish();
    }
}
