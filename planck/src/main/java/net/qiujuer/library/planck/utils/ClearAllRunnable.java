package net.qiujuer.library.planck.utils;

/**
 * Performed after the data has been cleaned,
 * to confirm that the cleaning operation has been complete.
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/17
 */
public class ClearAllRunnable implements Runnable {
    @Override
    public void run() {
        Logger.d("ClearAllRunnable", "Runnable do run it.");
    }
}
