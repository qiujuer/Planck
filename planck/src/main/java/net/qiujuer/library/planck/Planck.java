package net.qiujuer.library.planck;

import android.content.Context;
import android.support.annotation.NonNull;

import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.file.FileNameGenerator;

import java.io.File;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class Planck {
    private File mCacheRoot;
    private DataProvider mDataProvider;
    private FileNameGenerator mFileNameGenerator;

    private Planck(Builder builder) {

    }


    public PlanckSource get(String url){
        return
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
            return new Planck(this);
        }
    }
}
