package net.qiujuer.library.planck.internal.contract;

/**
 * Used to mark the use status
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public interface UsageFinalizer {
    /**
     * Once use call
     *
     * @return current usage count
     */
    int onceUsage();

    /**
     * Once finalize call
     *
     * @return True, need finish self
     */
    boolean onceFinalize();
}
