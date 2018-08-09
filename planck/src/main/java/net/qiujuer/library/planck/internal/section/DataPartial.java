package net.qiujuer.library.planck.internal.section;

import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public interface DataPartial {
    long length();

    boolean isLoaded(long position);

    int get(long position, byte[] buffer, int offset, int size) throws IOException;

    void close();
}
