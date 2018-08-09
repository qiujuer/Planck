package net.qiujuer.library.planck;

import android.content.Context;
import android.support.annotation.NonNull;

import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.file.FileNameGenerator;
import net.qiujuer.library.planck.file.Md5FileNameGenerator;
import net.qiujuer.library.planck.internal.RemotePlanckSource;
import net.qiujuer.library.planck.internal.UsageFinalizePlanckSource;
import net.qiujuer.library.planck.utils.StorageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class Planck {
    private final File mCacheRoot;
    private final DataProvider mDataProvider;
    private final FileNameGenerator mFileNameGenerator;
    private final Map<String, UsageFinalizePlanckSource> mSourceMap = new HashMap<>();

    private Planck(Builder builder) {
        mCacheRoot = builder.mCacheRoot;
        mDataProvider = builder.mDataProvider;
        mFileNameGenerator = builder.mFileNameGenerator;
    }

    public synchronized PlanckSource get(final String url) {
        final String key = mFileNameGenerator.generate(url);
        UsageFinalizePlanckSource source = null;
        try {
            if (mSourceMap.containsKey(key)) {
                source = mSourceMap.get(key);
            } else {
                RemotePlanckSource remotePlanckSource = new RemotePlanckSource(url, key, mCacheRoot, mDataProvider);
                source = new UsageFinalizePlanckSource(remotePlanckSource);
                mSourceMap.put(key, source);
            }
            return source;
        } finally {
            assert source != null;
            source.usage();
        }
    }

    public static final class Builder {
        private Context mContext;
        private File mCacheRoot;
        private DataProvider mDataProvider;
        private FileNameGenerator mFileNameGenerator;

        public Builder(@NonNull Context context) {
            mContext = context;
        }

        public Builder setDataProvider(DataProvider dataProvider) {
            mDataProvider = dataProvider;
            return this;
        }

        public Builder setCacheRoot(File cacheRoot) {
            mCacheRoot = cacheRoot;
            return this;
        }

        public Builder setFileNameGenerator(FileNameGenerator fileNameGenerator) {
            mFileNameGenerator = fileNameGenerator;
            return this;
        }

        public Planck build() {
            if (mCacheRoot == null) {
                mCacheRoot = StorageUtils.getIndividualCacheDirectory(mContext);
            }

            if (mDataProvider == null) {
                throw new NullPointerException("mDataProvider cannot null.");
            }

            if (mFileNameGenerator == null) {
                mFileNameGenerator = new Md5FileNameGenerator();
            }

            return new Planck(this);
        }
    }
}
