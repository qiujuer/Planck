package net.qiujuer.library.planck.internal;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.internal.section.DataPartial;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
class PartialPlanckSource implements PlanckSource {
    private DataPartial[] mDataPartials;
    private final long mTotalSize;
    private final long mPartialSize;

    PartialPlanckSource(DataPartial[] partials, long totalSize, long partialSize) {
        mDataPartials = partials;
        mTotalSize = totalSize;
        mPartialSize = partialSize;
    }

    @Override
    public long length(int timeout) {
        return mTotalSize;
    }

    @Override
    public long load(long position, int timeout) throws IOException, TimeoutException {
        final long partialSize = mPartialSize;

        int partIndex = (int) (position / partialSize);
        long partPos = position - (partIndex * partialSize);
        DataPartial partial = mDataPartials[partIndex];
        long partReadablePos = partial.load(partPos, timeout);

        return partialSize * (partIndex + 1) + partReadablePos;
    }


    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        final long partialSize = mPartialSize;

        int partIndex = (int) (position / partialSize);
        long partPos = position - (partIndex * partialSize);
        DataPartial partial = mDataPartials[partIndex];

        return partial.get(partPos, buffer, offset, size, timeout);
    }

    @Override
    public void close() {
        for (DataPartial dataPartial : mDataPartials) {
            dataPartial.close();
        }
    }
}
