package net.qiujuer.library.planck;

import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface PlanckSource {
    long length();

    void position(int position) throws IOException;

    int read(byte[] buffer, int offset, int size) throws IOException;
}
