package net.qiujuer.library.planck;

import net.qiujuer.library.planck.data.DataBehavior;

import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface PlanckSource extends DataBehavior {
    int INVALID_VALUES_INIT_NETWORK_ERROR = -10001;
    int INVALID_VALUES_INIT_INTERRUPTED = -10002;
    int INVALID_VALUES_INIT_TIMEOUT = -10003;

    /**
     * Get current source length.
     *
     * @param timeout timeout
     * @return available length; -1 Source init InterruptedException
     * @throws TimeoutException on timeout
     */
    long length(int timeout) throws TimeoutException;
}
