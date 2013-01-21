package com.opengoss.nodehub.utils;

import android.app.AlertDialog;
import android.content.Context;


public class Helper {
	private static AlertDialog alert;
	
	public static void alert(Context act, int strId) {
		String msg = act.getResources().getString(strId);
		alert(act, msg);
	}
	
	public static void alert(Context act, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setMessage(msg);
		alert = builder.create();
		alert.show();
	}	
}
