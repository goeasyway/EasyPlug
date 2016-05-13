package net.goeasyway.easyand.utils;

import android.util.Log;

public class LogUtils {
	private static final String TAG = "EasyAnd_Framework";

	public static void i(String tag, String msg) {
		Log.i(TAG, tag + " " + msg);
	}
	
	public static void w(String tag, String msg) {
		Log.w(TAG, tag + " " + msg);
	}

	public static void e(String tag, String msg) {
		Log.e(TAG, tag + " " + msg);
        // TODO 记录到文件
	}

}
