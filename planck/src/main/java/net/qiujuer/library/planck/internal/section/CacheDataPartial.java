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

    private final File mFile;
    private final long mFileLength;
    protected RandomAccessFile mRandomAccessFile;
    private boolean mInit;

    public CacheDataPartial(@NonNull File file) {
        mFile = file;
        mFileLength = file.exists() ? file.length() : 0;
    }

    private void init() {
        if (!mInit) {
            try {
                mRandomAccessFile = new RandomAccessFile(mFile, DEFAULT_READ_FILE_MODE);
            } catch (FileNotFoundException e) {
                throw new FileException(mFile, FileException.FILE_NOT_FIND);
            } finally {
                mInit = true;
            }
        } else {
            if (mRandomAccessFile == null) {
                throw new FileException(mFile, FileException.FILE_NOT_FIND);
            }
        }
    }

    @Override
    public long length() {
        return mFileLength;
    }

    @Override
    public long load(long position, int timeout) throws IOException, TimeoutException {
        try {
            init();
        } catch (FileException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
        return mFileLength - 1;
    }

    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException {
        try {
            init();
        } catch (FileException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
        mRandomAccessFile.seek(position);
        return mRandomAccessFile.read(buffer, offset, size);
    }

    @Override
    public synchronized void close() {
        IoUtil.close(mRandomAccessFile);
        mRandomAccessFile = null;
        mInit = false;
    }
}
