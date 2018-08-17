package net.qiujuer.library.planck.internal.contract;

import java.util.concurrent.ExecutorService;
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
    private boolean mOnceDone;
    private boolean mSucceed = true;
    private Future mFuture;
    private ExecutorService mExecutorService;

    protected abstract boolean onInitialize();

    @Override
    public final void run() {
        mSucceed = this.onInitialize();
        mFuture = null;
        mOnceDone = true;
    }

    public final void cancel() {
        Future future = mFuture;
        if (future != null) {
            future.cancel(true);
        }
        mFuture = null;
        mExecutorService = null;
    }

    public final void retryOnFailed() {
        if (mExecutorService == null) {
            return;
        }

        if (mOnceDone && !mSucceed) {
            mOnceDone = false;
            mSucceed = true;
            startWith(mExecutorService);
        }
    }

    public final void startWith(ExecutorService executorService) {
        if (executorService == null) {
            return;
        }

        if (!mOnceDone) {
            mExecutorService = executorService;
            mFuture = executorService.submit(this);
        }
    }
}
