package net.qiujuer.library.planck.internal.section;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.exception.FileException;
import net.qiujuer.library.planck.exception.StreamInterruptException;
import net.qiujuer.library.planck.utils.IoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidParameterException;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class CacheDataPartial implements DataPartial {
    static final String DEFAULT_READ_FILE_MODE = "r";
    static final String DEFAULT_WRITE_FILE_MODE = "rw";

    final File mFile;
    final Object mDataLock = new Object();
    RandomAccessFile mRandomAccessFile;

    private final long mFileLength;
    private final String mFileModel;
    private boolean mInit;

    public CacheDataPartial(@NonNull File file) {
        this(file, DEFAULT_READ_FILE_MODE);
    }

    CacheDataPartial(@NonNull File file, String fileModel) {
        mFile = file;
        mFileModel = fileModel;
        mFileLength = file.exists() ? file.length() : 0;
    }

    private synchronized void initStream() throws IOException {
        if (!mInit) {
            try {
                doInitStream();
            } finally {
                mInit = true;
            }
        } else {
            synchronized (mDataLock) {
                if (mRandomAccessFile == null) {
                    throw FileException.throwIOException(mFile, FileException.FILE_NOT_FIND);
                }
            }
        }
    }

    @Override
    public final long length(int timeout) throws IOException, TimeoutException {
        return getPartialLength(timeout);
    }

    @Override
    public final long load(long position, int timeout) throws IOException, TimeoutException {
        final long finishTime = SystemClock.elapsedRealtime() + timeout;
        final long len = length(timeout);
        if (position >= len) {
            throw new IOException("Load parameter anomaly, pos:" + position);
        }

        timeout = (int) (finishTime - SystemClock.elapsedRealtime());
        if (timeout <= 0) {
            throw new TimeoutException("Load data timeout.");
        }

        initStream();

        try {
            return doLoad(position, timeout);
        } catch (StreamInterruptException ignored) {
            return PlanckSource.INVALID_VALUES_SOURCE_STREAM_INTERRUPT;
        }
    }

    @Override
    public final int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        final long finishTime = SystemClock.elapsedRealtime() + timeout;
        final long len = length(timeout);
        if (position >= len || size <= 0) {
            throw new IOException("Read parameter anomaly, pos:" + position + ", size:" + size + ", offset:" + offset);
        }

        timeout = (int) (finishTime - SystemClock.elapsedRealtime());
        if (timeout <= 0) {
            throw new TimeoutException("Read data timeout.");
        }

        initStream();

        if ((position + size) > len) {
            size = (int) (len - position);
        }

        return doGet(position, buffer, offset, size, timeout);
    }

    @Override
    public final void close() {
        doClose();
    }

    protected void doInitStream() throws IOException {
        synchronized (mDataLock) {
            try {
                mRandomAccessFile = new RandomAccessFile(mFile, mFileModel);
            } catch (FileNotFoundException e) {
                throw FileException.throwIOException(mFile, FileException.FILE_NOT_FIND);
            }
        }
    }

    protected long getPartialLength(int timeout) throws IOException, TimeoutException {
        return mFileLength;
    }

    protected long doLoad(long position, int timeout) throws IOException, TimeoutException {
        return mFileLength - 1;
    }

    protected int doGet(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        if (position < 0) {
            throw new InvalidParameterException("Position invalid:" + position);
        }
        int count;
        synchronized (mDataLock) {
            mRandomAccessFile.seek(position);
            count = mRandomAccessFile.read(buffer, offset, size);
        }
        if (count < 0) {
            throw new IOException("RandomAccessFile read failed: position = [" + position + "], offset = [" + offset + "], size = [" + size + "], timeout = [" + timeout + "], count = [" + count + "]");
        }
        return count;
    }

    protected synchronized void doClose() {
        synchronized (mDataLock) {
            IoUtil.close(mRandomAccessFile);
            mRandomAccessFile = null;
        }
        mInit = false;
    }
}
