package net.qiujuer.library.planck.internal.section;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.data.StreamFetcher;
import net.qiujuer.library.planck.exception.FileException;
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
    private static final int TEMP_DATA_FOOTER_LEN = 8;
    private final String mUrl;
    private final DataProvider mProvider;
    private final AtomicLong mWritePos = new AtomicLong();
    private final Object mDataLock = mWritePos;
    private final Object mFetcherLock = new Object();
    private StreamFetcher mFetcher;
    private CacheUtil.CacheInfo mCacheInfo;

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
    protected long doLoad(long position, int timeout) throws IOException, TimeoutException {
        if (mWritePos.get() >= position) {
            return mWritePos.get();
        }

        if (mWritePos.get() < mCacheInfo.mSize) {
            retryLoadDataFromProvider();
        }

        final long finishWaitTime = SystemClock.elapsedRealtime() + timeout;
        while (mWritePos.get() < position) {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime > finishWaitTime) {
                throw new TimeoutException("Load data timeout, pos:" + position + ", currentPos:" + mWritePos.get());
            }

            synchronized (mDataLock) {
                try {
                    mDataLock.wait(finishWaitTime - currentTime);
                    if (mFetcher == null && mWritePos.get() < position) {
                        // Abnormal awaken
                        retryLoadDataFromProvider();
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new IOException("Load data timeout InterruptedException, pos:" + position + ", currentPos:" + mWritePos.get());
                }
            }
        }
        return mWritePos.get();
    }

    @Override
    protected synchronized int doGet(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        long loadPos = position + size;
        long loadEndPos = doLoad(loadPos, timeout);
        if (loadEndPos > position) {
            return super.doGet(position, buffer, offset, size, timeout);
        } else {
            return 0;
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

        final byte[] buffer = new byte[512];
        try {
            long needDownloadSize = mCacheInfo.mSize - mWritePos.get();
            int errorCount = 3;
            while (needDownloadSize > 0) {
                int size = stream.read(buffer);
                if (size > 0) {
                    errorCount = 3;
                    size = (int) Math.min(size, needDownloadSize);

                    final RandomAccessFile randomAccessFile = mRandomAccessFile;
                    synchronized (mDataLock) {
                        randomAccessFile.seek(mWritePos.get());
                        randomAccessFile.write(buffer, 0, size);
                        long pos = mWritePos.addAndGet(size);
                        randomAccessFile.writeLong(pos);
                        needDownloadSize -= size;
                        notifyProgressChanged();
                    }
                } else {
                    if ((--errorCount) <= 0) {
                        break;
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            notifyProgressChanged();
            releaseFetcher();
        }
    }

    @Override
    public void onLoadFailed(@NonNull Exception e) {
        releaseFetcher();
    }


    private void retryLoadDataFromProvider() {
        synchronized (mFetcherLock) {
            if (mWritePos.get() < mCacheInfo.mSize && mFetcher == null) {
                long downStart = mCacheInfo.mStartPos + mWritePos.get();
                long downSize = mCacheInfo.mSize - mWritePos.get();
                mFetcher = mProvider.buildStreamFetcher(mUrl, downStart, downSize);
                mFetcher.loadData(StreamFetcher.Priority.HIGH, this);
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
