package net.qiujuer.library.planck;

import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface PlanckSource {
    /**
     * Get current source length.
     *
     * @param timeout timeout
     * @return available length; returns -1 on timeout
     */
    long length(int timeout);

    /**
     * Load form new position index
     *
     * @param position new position index
     * @param timeout  timeout
     * @return returns the current position on success; returns -1 on timeout;
     * returns -2 on timeout Interrupted
     * @throws IOException position values invalid
     */
    long load(long position, int timeout) throws IOException;

    /**
     * Get data to buffer
     *
     * @param position read to buffer form position index
     * @param buffer   buffer container
     * @param offset   buffer offset
     * @param size     read data max length
     * @param timeout  timeout
     * @return number of successful reads, returns -1 on timeout
     * @throws IOException parameters values invalid
     */
    int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException;

    /**
     * Close current source
     */
    void close();
}
