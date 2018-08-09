package net.qiujuer.library.planck.utils;

import android.util.Log;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class Logger {
    private static final String TAG_PRE = "[Planck]-";

    public static void w(String tag, String msg) {
        Log.w(TAG_PRE + tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(TAG_PRE + tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(TAG_PRE + tag, msg);
    }
}
