package net.qiujuer.library.planck.internal;

import android.support.annotation.NonNull;

import net.qiujuer.library.planck.PlanckSource;

import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class UsageFinalizePlanckSource implements PlanckSource, UsageFinalize {
    private final PlanckSource mPlanckSource;
    private int mUsageCount;

    public UsageFinalizePlanckSource(@NonNull PlanckSource planckSource) {
        mPlanckSource = planckSource;
    }

    @Override
    public long length(int timeout) {
        return mPlanckSource.length(timeout);
    }

    @Override
    public long load(long position, int timeout) throws IOException {
        return mPlanckSource.load(position, timeout);
    }

    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException {
        return mPlanckSource.get(position, buffer, offset, size, timeout);
    }

    @Override
    public synchronized void usage() {
        mUsageCount++;
    }

    @Override
    public synchronized boolean isFinalize() {
        return mUsageCount <= 0;
    }

    @Override
    public synchronized void close() {
        if (mUsageCount <= 0) {
            return;
        }
        mUsageCount--;
        if (mUsageCount <= 0) {
            mPlanckSource.close();
        }
    }
}
