package net.qiujuer.library.planck.internal;

import android.support.annotation.NonNull;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.utils.IoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class LocalPlanckSource implements PlanckSource {
    private static final String DEFAULT_READ_FILE_MODE = "r";
    private final File mFile;
    private RandomAccessFile mRandomAccessFile;
    private boolean mInit;

    public LocalPlanckSource(@NonNull File file) {
        mFile = file;
    }

    private void init() throws IOException {
        if (!mInit) {
            try {
                mRandomAccessFile = new RandomAccessFile(mFile, DEFAULT_READ_FILE_MODE);
            } catch (FileNotFoundException e) {
                throw new IOException("Cannot load local file.");
            } finally {
                mInit = true;
            }
        } else {
            if (mRandomAccessFile == null) {
                throw new IOException("Cannot load local file.");
            }
        }
    }

    @Override
    public synchronized long length(int timeout) {
        return mFile.exists() ? mFile.length() : 0;
    }

    @Override
    public synchronized int load(int position, int timeout) throws IOException {
        init();
        return position;
    }

    @Override
    public synchronized int get(int position, byte[] buffer, int offset, int size, int timeout) throws IOException {
        init();
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
