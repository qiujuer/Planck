package net.qiujuer.library.planck;

import android.support.annotation.NonNull;
import android.util.Log;

import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.file.FileNameGenerator;
import net.qiujuer.library.planck.file.Md5FileNameGenerator;
import net.qiujuer.library.planck.internal.ProxyPlanckSource;
import net.qiujuer.library.planck.internal.contract.Initializer;
import net.qiujuer.library.planck.internal.contract.UsageFinalizer;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class Planck {
    private final static String TAG = "Planck";
    private final static long DEFAULT_PARTIAL_SIZE = 512 * 1024;
    private final long mMaxPartialSize = DEFAULT_PARTIAL_SIZE;
    private final File mCacheRoot;
    private final DataProvider mDataProvider;
    private final FileNameGenerator mFileNameGenerator;
    private final Map<String, PlanckSource> mSourceMap = new HashMap<>();
    private final ExecutorService mExecutor;

    private Planck(Builder builder) {
        mCacheRoot = builder.mCacheRoot;
        mDataProvider = builder.mDataProvider;
        mFileNameGenerator = builder.mFileNameGenerator;
        mExecutor = Executors.newSingleThreadExecutor(new PlanckThreadFactory());
    }

    public synchronized PlanckSource get(final String url) {
        final String key = mFileNameGenerator.generate(url);
        PlanckSource source = null;
        try {
            synchronized (mSourceMap) {
                if (mSourceMap.containsKey(key)) {
                    source = mSourceMap.get(key);
                } else {
                    ProxyPlanckSource proxySource = new ProxyPlanckSource(this.mStore, url);
                    source = proxySource;
                    mSourceMap.put(key, proxySource);
                    dispatchInitializer(proxySource.initializer());
                }
            }
            return source;
        } finally {
            assert source != null;
            int count = ((UsageFinalizer) source).onceUsage();
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Source usage count:" + count);
            }
        }
    }

    private void dispatchInitializer(Initializer initializer) {
        Future<?> future = mExecutor.submit(initializer);
        initializer.setFuture(future);
    }

    private final Store mStore = new Store() {
        @Override
        public File cacheRoot() {
            return mCacheRoot;
        }

        @Override
        public DataProvider dataProvider() {
            return mDataProvider;
        }

        @Override
        public FileNameGenerator fileNameGenerator() {
            return mFileNameGenerator;
        }

        @Override
        public long maxPartialSize(long totalSize) {
            return mMaxPartialSize;
        }

        @Override
        public void outOfSource(String httpUrl) {
            final String key = mFileNameGenerator.generate(httpUrl);
            synchronized (mSourceMap) {
                if (mSourceMap.containsKey(key)) {
                    mSourceMap.remove(key);
                }
            }
        }
    };

    public interface Store {
        File cacheRoot();

        DataProvider dataProvider();

        FileNameGenerator fileNameGenerator();

        long maxPartialSize(long totalSize);

        void outOfSource(String httpUrl);
    }

    public static final class Builder {
        private File mCacheRoot;
        private DataProvider mDataProvider;
        private FileNameGenerator mFileNameGenerator;

        public Builder(@NonNull DataProvider provider, @NonNull File cacheRoot) {
            mDataProvider = provider;
            mCacheRoot = cacheRoot;
        }

        public Builder setFileNameGenerator(FileNameGenerator fileNameGenerator) {
            mFileNameGenerator = fileNameGenerator;
            return this;
        }

        public Planck build() {
            if (mCacheRoot == null || !mCacheRoot.exists()) {
                throw new InvalidParameterException("CacheRoot is null or does not exist.");
            }

            if (mDataProvider == null) {
                throw new InvalidParameterException("DataProvider is null.");
            }

            if (mFileNameGenerator == null) {
                mFileNameGenerator = new Md5FileNameGenerator();
            }

            return new Planck(this);
        }
    }

    private static class PlanckThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "Planck-ThreadPool-";

        public Thread newThread(@NonNull Runnable r) {
            SecurityManager s = System.getSecurityManager();
            ThreadGroup group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();

            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.MAX_PRIORITY)
                t.setPriority(Thread.MAX_PRIORITY);
            return t;
        }
    }

}
