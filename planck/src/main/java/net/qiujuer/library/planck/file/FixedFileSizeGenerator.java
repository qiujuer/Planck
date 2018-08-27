package net.qiujuer.library.planck.file;

/**
 * Use a fixed cache file size for any situation.
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/27
 */
public class FixedFileSizeGenerator implements FileLengthGenerator {
    private final long mMaxPartialSize;

    public FixedFileSizeGenerator(long maxPartialSize) {
        mMaxPartialSize = maxPartialSize;
    }

    @Override
    public long generatePlanckCacheFileMaxLength(String url, long totalLength) {
        return mMaxPartialSize;
    }
}
