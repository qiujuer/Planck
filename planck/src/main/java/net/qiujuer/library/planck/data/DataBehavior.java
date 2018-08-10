package net.qiujuer.library.planck.data;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/10
 */
public interface DataBehavior {
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

    /**
     * Close current data source
     */
    void close();
}
