package net.qiujuer.library.planck;

import net.qiujuer.library.planck.data.DataBehavior;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface PlanckSource extends DataBehavior {
    /**
     * Get current source length.
     *
     * @param timeout timeout
     * @return available length; returns -1 on timeout
     */
    long length(int timeout);
}
