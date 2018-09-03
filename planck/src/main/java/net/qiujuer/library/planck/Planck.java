package net.qiujuer.library.planck;

import android.support.annotation.NonNull;

import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.file.FileLengthGenerator;
import net.qiujuer.library.planck.file.FileNameGenerator;
import net.qiujuer.library.planck.file.FixedFileSizeGenerator;
import net.qiujuer.library.planck.file.Md5FileNameGenerator;
import net.qiujuer.library.planck.internal.ProxyPlanckSource;
import net.qiujuer.library.planck.internal.contract.Initializer;
import net.qiujuer.library.planck.internal.contract.UsageFinalizer;
import net.qiujuer.library.planck.utils.ClearAllRunnable;
import net.qiujuer.library.planck.utils.IoUtil;
import net.qiujuer.library.planck.utils.Logger;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class Planck {
    private final static String TAG = Planck.class.getSimpleName();
    private final static long DEFAULT_PARTIAL_SIZE = 1024 * 1024;
    private final File mCacheRoot;
    private final DataProvider mDataProvider;
    private final FileNameGenerator mFileNameGenerator;
    private final FileLengthGenerator mFileLengthGenerator;
    private final Map<String, PlanckSource> mSourceMap = new HashMap<>();
    private final ExecutorService mExecutor;

    private Planck(Builder builder) {
        mCacheRoot = builder.mCacheRoot;
        mDataProvider = builder.mDataProvider;
        mFileNameGenerator = builder.mFileNameGenerator;
        mFileLengthGenerator = builder.mFileLengthGenerator;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(20), new PlanckThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        mExecutor = threadPoolExecutor;
    }

    /**
     * Get proxy to local cache source from url
     *
     * @param url File url or Network path
     * @return PlanckSource, you can read data with it.
     */
    public synchronized PlanckSource get(final String url) {
        final String key = mFileNameGenerator.generatePlanckCacheFileName(url);
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
                Logger.d(TAG, "Source usage count:" + count);
            }
        }
    }

    /**
     * If you need clear cache files, please use {@link #getUsingSourceKeys()}
     */
    @Deprecated
    public void clearAll() {
        synchronized (mSourceMap) {
            for (String key : mSourceMap.keySet()) {
                PlanckSource source = mSourceMap.get(key);
                IoUtil.close(source);
            }
        }

        try {
            mExecutor.submit(new ClearAllRunnable()).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mSourceMap.size() > 0) {
            //noinspection deprecation
            clearAll();
        }
    }

    /**
     * When you remove files, you should remove files that are not in use and ignore them when they are in use;
     * The current method can get the file prefix being used, You need to call fileName.contains(getUsingSourceKeys()[0]) to check if ignore.
     *
     * @return file prefix being with using
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public String[] getUsingSourceKeys() {
        Set<String> strings = mSourceMap.keySet();
        return strings.toArray(new String[strings.size()]);
    }

    private void dispatchInitializer(Initializer initializer) {
        initializer.startWith(mExecutor);
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
        public long maxPartialSize(String httpUrl, long totalSize) {
            return mFileLengthGenerator.generatePlanckCacheFileMaxLength(httpUrl, totalSize);
        }

        @Override
        public void outOfSource(String httpUrl) {
            final String key = mFileNameGenerator.generatePlanckCacheFileName(httpUrl);
            synchronized (mSourceMap) {
                if (mSourceMap.containsKey(key)) {
                    mSourceMap.remove(key);
                }
            }
        }

        @Override
        public void post(Runnable runnable) {
            mExecutor.execute(runnable);
        }
    };

    public interface Store {
        File cacheRoot();

        DataProvider dataProvider();

        FileNameGenerator fileNameGenerator();

        long maxPartialSize(String httpUrl, long totalSize);

        void outOfSource(String httpUrl);

        void post(Runnable runnable);
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private File mCacheRoot;
        private DataProvider mDataProvider;
        private FileNameGenerator mFileNameGenerator;
        private FileLengthGenerator mFileLengthGenerator;

        public Builder(@NonNull DataProvider provider, @NonNull File cacheRoot) {
            mDataProvider = provider;
            mCacheRoot = cacheRoot;
        }

        public Builder setFileNameGenerator(FileNameGenerator generator) {
            mFileNameGenerator = generator;
            return this;
        }

        public Builder setFileLengthGenerator(FileLengthGenerator generator) {
            mFileLengthGenerator = generator;
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

            if (mFileLengthGenerator == null) {
                mFileLengthGenerator = new FixedFileSizeGenerator(DEFAULT_PARTIAL_SIZE);
            }

            return new Planck(this);
        }
    }

    private static class PlanckThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "Planck-WorkPool-";

        public Thread newThread(@NonNull Runnable r) {
            SecurityManager s = System.getSecurityManager();
            ThreadGroup group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();

            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.MAX_PRIORITY) {
                t.setPriority(Thread.MAX_PRIORITY);
            }
            return t;
        }
    }

}
