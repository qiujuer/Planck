package net.qiujuer.library.planck;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface PlanckSource extends Closeable {
    int INVALID_VALUES_INIT_NETWORK_ERROR = -10001;
    int INVALID_VALUES_INIT_INTERRUPTED = -10002;
    int INVALID_VALUES_INIT_TIMEOUT = -10003;
    int INVALID_VALUES_SOURCE_STREAM_INTERRUPT = -10101;

    /**
     * Get current source length.
     *
     * @param timeout timeout
     * @return available length; -1 Source init InterruptedException
     * @throws IOException      source is close, or source not find, or read source error
     * @throws TimeoutException read source length timeout
     */
    long length(int timeout) throws IOException, TimeoutException;

    /**
     * Load to new position index
     *
     * @param position new position index
     * @param timeout  timeout
     * @return returns the current readable position on success
     * @throws IOException      position values invalid
     * @throws TimeoutException on timeout
     */
    long load(long position, int timeout) throws IOException, TimeoutException;

    /**
     * Get data to buffer
     *
     * @param position read to buffer form position index
     * @param buffer   buffer container
     * @param offset   buffer offset
     * @param size     read data max length
     * @param timeout  timeout
     * @return number of successful reads
     * @throws IOException      parameters values invalid
     * @throws TimeoutException on timeout
     */
    int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException;
}
