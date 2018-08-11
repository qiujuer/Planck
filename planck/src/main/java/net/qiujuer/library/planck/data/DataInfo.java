package net.qiujuer.library.planck.data;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class DataInfo {
    private final long mLength;
    private final boolean mSupportAcceptRangesOperation;

    public DataInfo(long length, boolean supportAcceptRangesOperation) {
        mLength = length;
        mSupportAcceptRangesOperation = supportAcceptRangesOperation;
    }

    public boolean isSupportRandomReading() {
        return mSupportAcceptRangesOperation;
    }

    public long getLength() {
        return mLength;
    }
}
