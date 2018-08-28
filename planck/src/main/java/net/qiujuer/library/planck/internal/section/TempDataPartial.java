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
import net.qiujuer.library.planck.utils.IoUtil;
import net.qiujuer.library.planck.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache file include Footer 8 bytes
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class TempDataPartial extends CacheDataPartial implements StreamFetcher.DataCallback {
    private static final String TAG = TempDataPartial.class.getSimpleName();
    private static final int MAX_READ_ERROR_COUNT = 3;
    private static final int TEMP_DATA_FOOTER_LEN = 8;
    private static final int TEMP_STREAM_BUFFER_SIZE = 512;
    private final String mUrl;
    private final DataProvider mProvider;
    private final AtomicLong mWritePos = new AtomicLong(0);
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
    protected void doInitStream(final File file, final String fileMode) throws IOException {
        long fileDataLength;
        if (file.exists()) {
            fileDataLength = file.length();
        } else {
            if (!file.createNewFile()) {
                throw FileException.throwIOException(file, FileException.CREATE_ERROR);
            }
            fileDataLength = 0;
        }

        super.doInitStream(file, fileMode);

        if (fileDataLength > TEMP_DATA_FOOTER_LEN) {
            long maxFileLen = mCacheInfo.mSize;
            synchronized (mDataLock) {
                mRandomAccessFile.seek(fileDataLength - TEMP_DATA_FOOTER_LEN);
                long pos = mRandomAccessFile.readLong();
                if (pos < 0 || pos > maxFileLen) {
                    Logger.w(TAG, "Load Parameters pos error:" + pos);
                    pos = Math.max(0, Math.min(pos, maxFileLen));
                }
                mWritePos.set(pos);
            }
        }

        // First Load data
        retryLoadDataFromProvider();
    }

    @Override
    protected long getPartialLength(int timeout) {
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
    protected int doGet(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
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
        releaseFetcher();
        super.doClose();
    }

    @Override
    public void onDataReady(@Nullable InputStream stream) {
        if (stream == null) {
            releaseFetcher();
            return;
        }

        final AtomicLong writePos = mWritePos;
        final int bufferSize = TEMP_STREAM_BUFFER_SIZE;
        final byte[] buffer = new byte[bufferSize];
        try {
            // Current cache position
            final long cacheWritePos = writePos.get();
            final CacheUtil.CacheInfo cacheInfo = mCacheInfo;

            if (!cacheInfo.mSupportRandomReading && cacheWritePos > 0) {
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
            if (randomAccessFile == null) {
                // This partial is closed
                return;
            }

            long countOfReads = cacheInfo.mSize - cacheWritePos;
            int totalReadErrorCount = MAX_READ_ERROR_COUNT;
            while (countOfReads > 0) {
                int maxOnceReadSize = (int) Math.min(bufferSize, countOfReads);
                int size = stream.read(buffer, 0, maxOnceReadSize);
                if (size > 0) {
                    synchronized (mDataLock) {
                        randomAccessFile.seek(writePos.getAndAdd(size));
                        randomAccessFile.write(buffer, 0, size);
                        randomAccessFile.writeLong(writePos.get());
                        notifyProgressChanged();
                    }
                    countOfReads -= size;
                } else {
                    if ((--totalReadErrorCount) <= 0) {
                        break;
                    }
                }
            }

            // sync to local file
            randomAccessFile.getFD().sync();

            synchronized (mDataLock) {
                if (countOfReads == 0 && randomAccessFile.length() == (mCacheInfo.mSize + TEMP_DATA_FOOTER_LEN)) {
                    randomAccessFile.setLength(mCacheInfo.mSize);
                    // Close current random
                    IoUtil.close(randomAccessFile);
                    mRandomAccessFile = null;

                    File currentTempFile = getFile();
                    File file = CacheUtil.convertToOfficialCache(currentTempFile);
                    if (file == null) {
                        Logger.w(TAG, "ConvertToOfficialCache failed:" + currentTempFile.getName());
                        // restore it
                        super.doInitStream(currentTempFile, CacheDataPartial.DEFAULT_READ_FILE_MODE);
                    } else {
                        Logger.d(TAG, "ConvertToOfficialCache succeed:[" + currentTempFile.getName() + ", " + file.getName() + "]");
                        // change to new read io
                        super.doInitStream(file, CacheDataPartial.DEFAULT_READ_FILE_MODE);
                    }
                } else {
                    Logger.w(TAG, "Cannot convertToOfficialCache: countOfReads:" + countOfReads
                            + ", ioLen:" + randomAccessFile.length() + ", needSize:" + mCacheInfo.mSize);
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
        synchronized (mDataLock) {
            synchronized (mFetcherLock) {
                final CacheUtil.CacheInfo cacheInfo = mCacheInfo;
                final long totalSize = cacheInfo.mSize;
                final long writePos = mWritePos.get();

                if (writePos < totalSize && mFetcher == null) {
                    long downStart = cacheInfo.mStartPos + (cacheInfo.mSupportRandomReading ? writePos : 0);
                    long downSize = totalSize - writePos;
                    StreamFetcher fetcher = mFetcher = mProvider.buildStreamFetcher(mUrl, downStart, downSize);
                    fetcher.loadData(StreamFetcher.Priority.HIGH, this);
                }
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
