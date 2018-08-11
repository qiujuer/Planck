package net.qiujuer.library.planck.internal.contract;

import java.util.concurrent.Future;

/**
 * This Initializer, Call with {@link net.qiujuer.library.planck.Planck} thread pool
 * The method {@link #onInitialize()} will run asynchronously
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/11
 */
public abstract class Initializer implements Runnable {
    private boolean mDone;
    private Future mFuture;

    protected abstract boolean onInitialize();

    @Override
    public final void run() {
        synchronized (this) {
            mFuture = null;
            mDone = this.onInitialize();
        }
    }

    public synchronized boolean isInitialSucceed() {
        return mDone;
    }

    public synchronized final void cancel() {
        if (!mDone && mFuture != null) {
            mFuture.cancel(true);
        }
    }

    public synchronized final void setFuture(Future<?> future) {
        if (!mDone) {
            mFuture = future;
        }
    }
}
