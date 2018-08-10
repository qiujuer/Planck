package net.qiujuer.library.planck.internal.section;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.qiujuer.library.planck.data.StreamFetcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class NetworkDataPartial implements DataPartial, StreamFetcher.DataCallback {
    private final StreamFetcher mFetcher;
    private final File mTempFile;
    private long mStartPosition;
    private long mSize;
    private RandomAccessFile mRandomAccessFile;

    public NetworkDataPartial(File tempFile, int startPosition, long size, StreamFetcher fetcher) {
        mTempFile = tempFile;
        mStartPosition = startPosition;
        mSize = size;
        mFetcher = fetcher;
    }

    @Override
    public long length() {
        return mSize;
    }

    @Override
    public long load(long position, int timeout) throws IOException, TimeoutException {
        return 0;
    }

    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        return 0;
    }

    @Override
    public void close() {
        mFetcher.cancel();
    }

    @Override
    public void onDataReady(@Nullable InputStream data) {

    }

    @Override
    public void onLoadFailed(@NonNull Exception e) {

    }
}
