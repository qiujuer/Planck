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
    private StreamFetcher mFetcher;
    private CacheUtil.CacheInfo mCacheInfo;
    private long mWritePos = 0;

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
            mWritePos = mRandomAccessFile.readLong();
        }

        if (mFetcher == null) {
            long downStart = mCacheInfo.mStartPos + mWritePos;
            long downSize = mCacheInfo.mSize - mWritePos;
            mFetcher = mProvider.buildStreamFetcher(mUrl, downStart, downSize);
            mFetcher.loadData(StreamFetcher.Priority.HIGH, this);
        }
    }

    @Override
    protected long getPartialLength() {
        return mCacheInfo.mSize;
    }

    @Override
    protected long doLoad(long position, int timeout) throws IOException, TimeoutException {
        if (mWritePos >= position) {
            return mWritePos;
        }

        final long finishWaitTime = SystemClock.currentThreadTimeMillis() + timeout;
        while (position < mWritePos) {
            long currentTime = SystemClock.currentThreadTimeMillis();
            if (currentTime > finishWaitTime) {
                throw new TimeoutException("Load data timeout, pos:" + position + ", currentPos:" + mWritePos);
            }

            synchronized (this) {
                try {
                    this.wait(finishWaitTime - currentTime);
                } catch (InterruptedException e) {
                    throw new IOException("Load data timeout InterruptedException, pos:" + position + ", currentPos:" + mWritePos);
                }
            }
        }
        return mWritePos;
    }

    @Override
    protected int doGet(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        long loadPos = position + size;
        doLoad(loadPos, timeout);
        return super.doGet(position, buffer, offset, size, timeout);
    }

    @Override
    protected void doClose() {
        super.doClose();
        if (mFetcher != null) {
            mFetcher.cleanup();
            mFetcher = null;
        }
    }

    @Override
    public void onDataReady(@Nullable InputStream stream) {
        if (stream == null) {
            return;
        }

        final RandomAccessFile randomAccessFile = mRandomAccessFile;
        final byte[] buffer = new byte[512];
        try {
            long waitCount = mCacheInfo.mSize - mWritePos;
            int errorCount = 3;
            while (waitCount > 0) {
                int size = stream.read(buffer);
                if (size > 0) {
                    errorCount = 3;
                    size = (int) Math.min(size, waitCount);
                    synchronized (this) {
                        randomAccessFile.seek(mWritePos);
                        randomAccessFile.write(buffer, 0, size);
                        mWritePos += size;
                        randomAccessFile.writeLong(mWritePos);
                        waitCount -= size;
                        try {
                            this.notifyAll();
                        } catch (Exception ignore) {
                        }
                    }
                } else {
                    if ((--errorCount) <= 0) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mFetcher.cleanup();
        }
    }

    @Override
    public void onLoadFailed(@NonNull Exception e) {
        if (mFetcher != null) {
            mFetcher.cleanup();
            mFetcher = null;
        }
    }
}
