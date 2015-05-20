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
/*   Author :  Neo Skunkworks-Kevin CHEN                                           */
/*   Role :    Calendar                                                             */
/*   Reference documents :                                                          */
/*=======================================================================================================*/
/* Comments:                                                                        */
/*   file  :  FBBirthdayImportReporter.java                                         */
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
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import com.android.calendar.R;
import com.jrdcom.calendar.FBBirthdayImportService.ImportProgressListener;
import com.jrdcom.calendar.FBBirthdayImportService.ImportServiceBinder;

public class FBBirthdayImportReporter extends Activity {
    public static final String ACTION_IMPORT_PROGRESS = "jrdcom.intent.action.SHOW_IMPORT_PROGRESS";

    public static final String ACTION_IMPORT_RESULT = "jrdcom.intent.action.SHOW_IMPORT_RESULT";

    public static final String EXTRA_TOTAL_IMPORTED = "totalImported";

    public static final String EXTRA_REQUEST_ERROR = "requestError";

    FBBirthdayImportService mService;

    ProgressDialog mProgressDialog;

    AlertDialog mResultDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent.getAction().equals(ACTION_IMPORT_PROGRESS)) {
            showImportProgress();
        } else if (intent.getAction().equals(ACTION_IMPORT_RESULT)) {
            if (intent.hasExtra(EXTRA_TOTAL_IMPORTED)) {
                int totalImported = intent.getIntExtra(EXTRA_TOTAL_IMPORTED, -1);
                if (totalImported > 0) {
                    showImportResult(getString(R.string.import_result_msg, totalImported));
                } else {
                    finish();
                }
            } else if (intent.hasExtra(EXTRA_REQUEST_ERROR)) {
                showImportResult(getString(R.string.import_error_msg));
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mResultDialog != null && mResultDialog.isShowing()) {
            mResultDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        if (getIntent().getAction().equals(ACTION_IMPORT_PROGRESS)) {
            bindService(new Intent(this, FBBirthdayImportService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (mService != null) {
            mService.unRegisterProgressListener();
            unbindService(mConnection);
        }
        if (getIntent().getAction().equals(ACTION_IMPORT_RESULT)) {
            finish();
        }
        super.onStop();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName name) {
            finish();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((ImportServiceBinder) service).getService();
            if (!FBBirthdayImportService.isImporting()) {
                finish();
            }
            mService.registerProgressListener(mProgressListener);
            if (mService.getProgressMax() != -1) {
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(mService.getProgressMax());
            }
            if (mService.getCurrentProgress() != -1) {
                mProgressDialog.setProgress(mService.getCurrentProgress());
            }
            if (!TextUtils.isEmpty(mService.getCurrentFriend())) {
                mProgressDialog.setMessage(getText(R.string.importing_msg) + "\n"
                        + mService.getCurrentFriend());
            }
        }
    };

    private ImportProgressListener mProgressListener = new ImportProgressListener() {
        public void onProgressUpdate(String curItem, int curStep, int progressMax) {
            if (progressMax > 0) {
                if (mProgressDialog.isIndeterminate()) {
                    mProgressDialog.setIndeterminate(false);
                    mProgressDialog.setMax(progressMax);
                }
            }
            if (curStep >= 0) {
                mProgressDialog.setProgress(curStep);
            }
            if (!TextUtils.isEmpty(curItem)) {
                mProgressDialog.setMessage(getText(R.string.importing_msg) + "\n" + curItem);
            }
        }

        public void onErrorEnd(int errCode) {
            /* showImportResult(getString(R.string.import_error_msg)); */
            finish();
        }

        public void onSuccessEnd(int totalImported) {
            /*
             * showImportResult(getString(R.string.import_result_msg,
             * totalImported));
             */
            finish();
        }

        public void onCancelEnd() {
            finish();
        };
    };

    private void showImportProgress() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        mProgressDialog.setTitle(this.getText(R.string.import_birthday));
        mProgressDialog.setMessage(this.getText(R.string.importing_msg));
        mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                this.getText(R.string.button_label_hide), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                this.getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        stopService(new Intent(FBBirthdayImportReporter.this,
                                FBBirthdayImportService.class));
                        finish();
                    }
                });
        // PR-259873-Neo Skunkworks-david.zhang-001 begin
        mProgressDialog.setOnKeyListener(new FBKeyListener(this));
        /**
         * mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener()
         * { public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent
         * event) { return true; } });
         */
        // PR-259873-Neo Skunkworks-david.zhang-001 end

        // PR-312466-Neo Skunkworks-david.zhang-001 begin
        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                FBBirthdayImportReporter.this.finish();
            }
        });
        // PR-312466-Neo Skunkworks-david.zhang-001 end

        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
    }

    private void showImportResult(String msg) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mResultDialog = new AlertDialog.Builder(FBBirthdayImportReporter.this)
                .setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.import_birthday)
                .setMessage(msg)
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                // PR-259873-Neo Skunkworks-david.zhang-001 begin
                .setOnKeyListener(new FBKeyListener(this)).create();
        /**
         * .setOnKeyListener(new DialogInterface.OnKeyListener() { public
         * boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
         * return true; } }).create();
         */
        // PR-259873-Neo Skunkworks-david.zhang-001 end

        // PR-312466-Neo Skunkworks-david.zhang-001 begin
        mResultDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                FBBirthdayImportReporter.this.finish();
            }
        });
        // PR-312466-Neo Skunkworks-david.zhang-001 end

        mResultDialog.show();
    }

    // PR-259873-Neo Skunkworks-david.zhang-001 begin
    private class FBKeyListener extends View implements DialogInterface.OnKeyListener {
        public FBKeyListener(Context context) {
            super(context);
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (KeyEvent.KEYCODE_BACK == keyCode) {
                finish();
                return true;
            } else if (KeyEvent.KEYCODE_SEARCH == keyCode) {
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    }
    // PR-259873-Neo Skunkworks-david.zhang-001 end
}
