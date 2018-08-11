package net.qiujuer.library.planck.internal.section;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.data.StreamFetcher;
import net.qiujuer.library.planck.exception.FileException;
import net.qiujuer.library.planck.exception.StreamInterruptException;
import net.qiujuer.library.planck.utils.CacheUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存文件包括尾部8字节描述当前下载情况
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class TempDataPartial extends CacheDataPartial implements StreamFetcher.DataCallback {
    private static final int MAX_READ_ERROR_COUNT = 3;
    private static final int TEMP_DATA_FOOTER_LEN = 8;
    private static final int TEMP_STREAM_BUFFER_SIZE = 512;
    private final String mUrl;
    private final DataProvider mProvider;
    private final AtomicLong mWritePos = new AtomicLong();
    private final Object mDataLock = mWritePos;
    private final Object mFetcherLock = new Object();
    private final CacheUtil.CacheInfo mCacheInfo;

    private StreamFetcher mFetcher;

    public TempDataPartial(File file, String url, DataProvider provider) {
        super(file, CacheDataPartial.DEFAULT_WRITE_FILE_MODE);
        mUrl = url;
        mProvider = provider;
        mCacheInfo = CacheUtil.getCacheInfo(file);
    }

    @Override
    protected void doInit() throws IOException {
        long fileDataLength;
        if (mFile.exists()) {
            fileDataLength = mFile.length();
        } else {
            if (!mFile.createNewFile()) {
                throw FileException.throwIOException(mFile, FileException.CREATE_ERROR);
            }
            fileDataLength = 0;
        }

        super.doInit();

        if (fileDataLength > TEMP_DATA_FOOTER_LEN) {
            mRandomAccessFile.seek(fileDataLength - TEMP_DATA_FOOTER_LEN);
            mWritePos.set(mRandomAccessFile.readLong());
        }

        // First Load data
        retryLoadDataFromProvider();
    }

    @Override
    protected long getPartialLength() {
        return mCacheInfo.mSize;
    }

    @Override
    protected long doLoad(final long position, int timeout) throws IOException, TimeoutException {
        AtomicLong writePos = mWritePos;

        if (writePos.get() >= position) {
            return writePos.get();
        }

        if (writePos.get() < mCacheInfo.mSize) {
            retryLoadDataFromProvider();
        }

        final long finishWaitTime = SystemClock.elapsedRealtime() + timeout;
        while (writePos.get() < position) {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime > finishWaitTime) {
                throw new TimeoutException("Load data timeout, targetPos:" + position + ", currentPos:" + writePos.get());
            }

            // Waiting position changed
            synchronized (mDataLock) {
                // Secondary confirmation
                if (writePos.get() < position) {
                    try {
                        mDataLock.wait(finishWaitTime - currentTime);
                    } catch (InterruptedException e) {
                        throw new IOException("Load data thread InterruptedException, targetPos:" + position + ", currentPos:" + writePos.get());
                    }
                }
            }

            // After waking up, need to check if the exception caused it
            if (mFetcher == null && writePos.get() < position) {
                // Abnormal awaken
                retryLoadDataFromProvider();
                // Throw custom exception
                throw new StreamInterruptException("Load data StreamInterruptException, targetPos:" + position + ", currentPos:" + writePos.get());
            }
        }
        return writePos.get();
    }

    @Override
    protected synchronized int doGet(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        long loadPos = position + size;

        try {
            long loadEndPos = doLoad(loadPos, timeout);
            if (loadEndPos > position) {
                return super.doGet(position, buffer, offset, size, timeout);
            } else {
                return 0;
            }
        } catch (StreamInterruptException ignored) {
            return PlanckSource.INVALID_VALUES_SOURCE_STREAM_INTERRUPT;
        }
    }

    @Override
    protected void doClose() {
        super.doClose();
        releaseFetcher();
    }

    @Override
    public void onDataReady(@Nullable InputStream stream) {
        if (stream == null) {
            return;
        }

        final AtomicLong writePos = mWritePos;
        final int bufferSize = TEMP_STREAM_BUFFER_SIZE;
        final byte[] buffer = new byte[bufferSize];
        try {
            // Current cache position
            final long cacheWritePos = writePos.get();

            if (!mCacheInfo.mSupportRandomReading && cacheWritePos > 0) {
                // Un SupportRandomReading
                // Read the data and discard it
                int totalReadErrorCount = MAX_READ_ERROR_COUNT;
                long discardedCountSize = cacheWritePos;
                while (discardedCountSize > 0) {
                    int maxOnceReadSize = (int) Math.min(bufferSize, discardedCountSize);
                    int size = stream.read(buffer, 0, maxOnceReadSize);
                    if (size > 0) {
                        discardedCountSize -= size;
                    } else {
                        if ((--totalReadErrorCount) <= 0) {
                            return;
                        }
                    }
                }
            }

            // Really read the data to the local cache
            final RandomAccessFile randomAccessFile = mRandomAccessFile;
            long countOfReads = mCacheInfo.mSize - cacheWritePos;
            long currentCacheWritePos = cacheWritePos;
            int totalReadErrorCount = MAX_READ_ERROR_COUNT;
            while (countOfReads > 0) {
                int maxOnceReadSize = (int) Math.min(bufferSize, countOfReads);
                int size = stream.read(buffer, 0, maxOnceReadSize);
                if (size > 0) {
                    synchronized (mDataLock) {
                        randomAccessFile.seek(currentCacheWritePos);
                        randomAccessFile.write(buffer, 0, size);
                        currentCacheWritePos = writePos.addAndGet(size);
                        randomAccessFile.writeLong(currentCacheWritePos);
                        countOfReads -= size;
                        notifyProgressChanged();
                    }
                } else {
                    if ((--totalReadErrorCount) <= 0) {
                        break;
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            releaseFetcher();
            notifyProgressChanged();
        }
    }

    @Override
    public void onLoadFailed(@NonNull Exception e) {
        releaseFetcher();
    }

    private void retryLoadDataFromProvider() {
        synchronized (mFetcherLock) {
            final CacheUtil.CacheInfo cacheInfo = mCacheInfo;
            final long totalSize = cacheInfo.mSize;
            final long writePos = mWritePos.get();

            if (writePos < totalSize && mFetcher == null) {
                long downStart = cacheInfo.mStartPos + (cacheInfo.mSupportRandomReading ? writePos : 0);
                long downSize = totalSize - writePos;
                StreamFetcher fetcher = mProvider.buildStreamFetcher(mUrl, downStart, downSize);
                fetcher.loadData(StreamFetcher.Priority.HIGH, this);
                mFetcher = fetcher;
            }
        }
    }

    private void notifyProgressChanged() {
        synchronized (mDataLock) {
            try {
                mDataLock.notifyAll();
            } catch (Exception ignore) {
            }
        }
    }

    private void releaseFetcher() {
        synchronized (mFetcherLock) {
            if (mFetcher != null) {
                mFetcher.cleanup();
                mFetcher = null;
            }
        }
    }
}
