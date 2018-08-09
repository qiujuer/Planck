package net.qiujuer.library.planck.internal;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public interface UsageFinalize {
    void usage();

    boolean isFinalize();

    void close();
}
