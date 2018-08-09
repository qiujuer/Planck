package net.qiujuer.library.planck.internal;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.internal.section.DataPartial;

import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class RemotePlanckSource implements PlanckSource {
    private final String mHttpUrl;
    private final String mFileNamePrefix;
    private final String mCacheRoot;
    private final String mProvider;
    private DataPartial[] mDataPartials;
    private boolean mInit;

    public RemotePlanckSource(String httpUrl, String fileNamePrefix, String cacheRoot, String provider) {
        mHttpUrl = httpUrl;
        mFileNamePrefix = fileNamePrefix;
        mCacheRoot = cacheRoot;
        mProvider = provider;
    }

    private void init() {
        if (mInit) {
            return;
        }


        mInit = true;
    }

    private void loadFromCache(){

    }

    @Override
    public long length(int timeout) {
        return 0;
    }

    @Override
    public int load(int position, int timeout) throws IOException {
        return 0;
    }

    @Override
    public int get(int position, byte[] buffer, int offset, int size, int timeout) throws IOException {
        return 0;
    }

    @Override
    public void close() {

    }
}
