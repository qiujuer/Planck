package net.qiujuer.library.integration.okhttp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
final class ContentLengthInputStream extends FilterInputStream {
    private final long contentLength;
    private int readSoFar;

    @NonNull
    static InputStream obtain(@NonNull InputStream other, long contentLength) {
        return new ContentLengthInputStream(other, contentLength);
    }

    private ContentLengthInputStream(@NonNull InputStream in, long contentLength) {
        super(in);
        this.contentLength = contentLength;
    }

    @Override
    public synchronized int available() throws IOException {
        return (int) Math.max(contentLength - readSoFar, in.available());
    }

    @Override
    public synchronized int read() throws IOException {
        int value = super.read();
        checkReadSoFarOrThrow(value >= 0 ? 1 : -1);
        return value;
    }

    @Override
    public int read(@NonNull byte[] buffer) throws IOException {
        return read(buffer, 0 /*byteOffset*/, buffer.length /*byteCount*/);
    }

    @Override
    public synchronized int read(@NonNull byte[] buffer, int byteOffset, int byteCount)
            throws IOException {
        return checkReadSoFarOrThrow(super.read(buffer, byteOffset, byteCount));
    }

    private int checkReadSoFarOrThrow(int read) throws IOException {
        if (read >= 0) {
            readSoFar += read;
        } else if (contentLength - readSoFar > 0) {
            throw new IOException("Failed to read all expected data"
                    + ", expected: " + contentLength
                    + ", but read: " + readSoFar);
        }
        return read;
    }
}

