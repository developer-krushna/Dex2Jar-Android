package com.mt.dex2jar;

import android.content.Context;
import android.view.*;
import android.app.Activity;
import android.widget.*;
import android.graphics.Color;
import android.app.AlertDialog.Builder;
import android.graphics.drawable.GradientDrawable;
import android.app.*;
public class AlertProgress{
	Context context;
	Activity activity;
	AlertDialog.Builder process;
	AlertDialog alert;
	TextView textview_mesage;
	TextView textview_title;
	ProgressBar progress;
	
	public AlertProgress(Context mContext){
		this.context = mContext;
		this.activity = (Activity)context;
		
		process = new Builder(activity);
		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		View view = View.inflate(context, R.layout.progress_dlg, null);
		textview_mesage = view.findViewById(R.id.message);
		progress = view.findViewById(R.id.progress);
		textview_title = view.findViewById(R.id.title);
		
		process.setCancelable(false);
		process.setView(view);
		
		alert = process.create();
		
		int cornerRadius = 20;
		GradientDrawable ରାଧେ = new GradientDrawable();
		ରାଧେ.setColor(Color.parseColor("#FFFFFF"));
		ରାଧେ.setCornerRadius(cornerRadius);
		alert.setCancelable(false);
		alert.getWindow().setBackgroundDrawable(ରାଧେ);
		
	}
	
	public void setTitle(final String title)  {
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				textview_title.setText(title);
			}
		});
	}
	public void setMessage(final String message){
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				
				textview_mesage.setText(message);
				
			}
		});
	}
	
	public void setProgress(final int value, final int max) {
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				
				progress.setVisibility(View.VISIBLE);
				progress.setProgress(value);
				progress.setMax(max);
			}
		});
	}
	public void setIndeterminate(final boolean bool){
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run()  {
				if(bool){
					progress.setIndeterminate(true);
				}else{
					progress.setIndeterminate(false);
				}
				
				
			}
		});
		
	}
	
	public void show() {
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				
				alert.show();
			}
		});
	}
	
	public void dismiss(){
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				
				alert.dismiss();
			}
		});
	}
}
