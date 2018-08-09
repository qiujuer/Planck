package net.qiujuer.library.planck.internal;

import android.support.annotation.NonNull;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.internal.section.CacheDataPartial;

import java.io.File;
import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class LocalPlanckSource implements PlanckSource {
    private CacheDataPartial mCacheDataPartial;

    public LocalPlanckSource(@NonNull File file) {
        mCacheDataPartial = new CacheDataPartial(file);
    }

    @Override
    public synchronized long length(int timeout) {
        return mCacheDataPartial.length();
    }

    @Override
    public synchronized long load(long position, int timeout) throws IOException {
        return mCacheDataPartial.isLoaded(position) ? position : 0;
    }

    @Override
    public synchronized int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException {
        return mCacheDataPartial.get(position, buffer, offset, size);
    }

    @Override
    public synchronized void close() {
        mCacheDataPartial.close();
    }
}
