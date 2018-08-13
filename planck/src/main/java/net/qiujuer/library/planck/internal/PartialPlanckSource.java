package net.qiujuer.library.planck.internal;

import android.os.SystemClock;

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

        return partReadablePos < 0 ? partReadablePos : partialSize * partIndex + partReadablePos;
    }

    @Override
    public int get(final long position, final byte[] buffer, final int offset, final int size, final int timeout) throws IOException, TimeoutException {
        final long endTime = SystemClock.elapsedRealtime() + timeout;

        final long partialSize = mPartialSize;
        final DataPartial[] allChild = mDataPartials;
        final int childSize = allChild.length;

        int partIndex = (int) (position / partialSize);
        long partStartPos = position - (partIndex * partialSize);

        int needReadSize = size;
        int partBufferOffset = offset;
        int totalLoadSize = 0;

        while (true) {
            DataPartial part = allChild[partIndex];

            // Part size
            long partSize = part.length();

            // Part max consumed size of offset partStartPos
            long partMaxConsumedSize = partSize - partStartPos;

            int partBufferSize;
            if (needReadSize > partMaxConsumedSize) {
                partBufferSize = (int) partMaxConsumedSize;
                needReadSize -= (int) (partMaxConsumedSize);
            } else {
                partBufferSize = needReadSize;
                needReadSize = 0;
            }

            // Check time out
            final int partTimeOut = (int) (endTime - SystemClock.elapsedRealtime());
            if (partTimeOut <= 0) {
                if (totalLoadSize == 0) {
                    throw new TimeoutException();
                }
                // Cancel next part load
                break;
            }

            // Load data to buffer
            int partLoadSize = 0;
            try {
                partLoadSize = part.get(partStartPos, buffer, partBufferOffset, partBufferSize, partTimeOut);
            } catch (Exception e) {
                // catch all exception
                if (totalLoadSize == 0) {
                    throw e;
                }
            }

            // Current part load failed
            if (partLoadSize < 0) {
                if (totalLoadSize == 0) {
                    totalLoadSize = partLoadSize;
                }
                break;
            }

            // Load succeed
            totalLoadSize += partLoadSize;

            // If part load full, but need more
            if (needReadSize > 0 && partLoadSize == partBufferSize && (++partIndex) < childSize) {
                partBufferOffset += partStartPos + 1;
                partStartPos = 0;
            } else {
                break;
            }
        }
        return totalLoadSize;
    }

    @Override
    public void close() {
        for (DataPartial dataPartial : mDataPartials) {
            dataPartial.close();
        }
    }
}
