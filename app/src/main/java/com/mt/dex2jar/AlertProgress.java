package com.mt.dex2jar;

/*
 * Created by aantik
 * 10/5/2026 8:28 PM
 *
 *   ⋆   ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *      |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */

import android.content.Context;
import android.view.*;
import android.app.Activity;
import android.widget.*;
import android.app.AlertDialog;

public class AlertProgress {
    Context context;
    Activity activity;
    AlertDialog.Builder _gop;
    AlertDialog oreo;
    ProgressBar progress;

    public AlertProgress(Context mContext) {
        this.context = mContext;
        this.activity = (Activity) context;

        _gop = new AlertDialog.Builder(activity);
        _gop.setTitle("Processing...");
        _gop.setMessage("Converting Dex to Jar...");
        _gop.setCancelable(false);

        progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);

        oreo = _gop.create();
        oreo.setView(progress, 50, 10, 50, 0);
        oreo.setCancelable(false);
    }

    public void setTitle(final String title) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                oreo.setTitle(title);
            }
        });
    }

    public void setMessage(final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                oreo.setMessage(message);
            }
        });
    }

    public void setProgress(final int value, final int max) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.VISIBLE);
                progress.setProgress(value);
                progress.setMax(max);
            }
        });
    }

    public void setIndeterminate(final boolean bool) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.setIndeterminate(bool);
            }
        });
    }

    public void show() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                oreo.show();
            }
        });
    }

    public void dismiss() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                oreo.dismiss();
            }
        });
    }
}
