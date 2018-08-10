package net.qiujuer.library.planck.internal.section;

import android.support.annotation.NonNull;

import net.qiujuer.library.planck.exception.FileException;
import net.qiujuer.library.planck.utils.IoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class CacheDataPartial implements DataPartial {
    static final String DEFAULT_READ_FILE_MODE = "r";
    static final String DEFAULT_WRITE_FILE_MODE = "rw";

    private final long mFileLength;
    private final String mFileModel;
    final File mFile;
    RandomAccessFile mRandomAccessFile;
    private boolean mInit;

    public CacheDataPartial(@NonNull File file) {
        this(file, DEFAULT_READ_FILE_MODE);
    }

    CacheDataPartial(@NonNull File file, String fileModel) {
        mFile = file;
        mFileModel = fileModel;
        mFileLength = file.exists() ? file.length() : 0;
    }

    private synchronized void init() throws IOException {
        if (!mInit) {
            try {
                doInit();
            } finally {
                mInit = true;
            }
        } else {
            if (mRandomAccessFile == null) {
                throw FileException.throwIOException(mFile, FileException.FILE_NOT_FIND);
            }
        }
    }

    @Override
    public final long length() {
        return getPartialLength();
    }

    @Override
    public final long load(long position, int timeout) throws IOException, TimeoutException {
        init();
        return doLoad(position, timeout);
    }

    @Override
    public final int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        final long len = length();
        if (position > len || size <= 0) {
            throw new IOException("Read parameter anomaly, pos:" + position + ", size:" + size + ", offset:" + offset);
        }

        if ((position + size) > len) {
            size = (int) (len - position);
        }

        init();

        return doGet(position, buffer, offset, size, timeout);
    }

    @Override
    public final void close() {
        doClose();
    }

    protected synchronized void doInit() throws IOException {
        try {
            mRandomAccessFile = new RandomAccessFile(mFile, mFileModel);
        } catch (FileNotFoundException e) {
            throw FileException.throwIOException(mFile, FileException.FILE_NOT_FIND);
        }
    }

    protected long getPartialLength() {
        return mFileLength;
    }

    protected long doLoad(long position, int timeout) throws IOException, TimeoutException {
        return mFileLength - 1;
    }

    protected synchronized int doGet(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        if (position < 0) {
            throw new IOException("doGet() called with1: position = [" + position + "], offset = [" + offset + "], size = [" + size + "], timeout = [" + timeout + "]");
        }
        mRandomAccessFile.seek(position);
        int count = mRandomAccessFile.read(buffer, offset, size);
        if (count < 0) {
            throw new IOException("doGet() called with2: position = [" + position + "], offset = [" + offset + "], size = [" + size + "], timeout = [" + timeout + "]");
        }
        return count;
    }

    protected synchronized void doClose() {
        IoUtil.close(mRandomAccessFile);
        mRandomAccessFile = null;
        mInit = false;
    }
}
