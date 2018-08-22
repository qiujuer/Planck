package net.qiujuer.library.planck.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface StreamFetcher {
    int INVALID_INTEGER = -1;

    /**
     * Callback that must be called when data has been loaded and is available, or when the load
     * fails.
     */
    interface DataCallback {

        /**
         * Called with the loaded data if the load succeeded, or with {@code null} if the load failed.
         */
        void onDataReady(@Nullable InputStream stream);

        /**
         * Called when the load fails.
         *
         * @param e a non-null {@link Exception} indicating why the load failed.
         */
        void onLoadFailed(@NonNull Exception e);
    }

    enum Priority {
        IMMEDIATE,
        HIGH,
        NORMAL,
        LOW
    }

    /**
     * Fetch data from which a resource can be decoded.
     * <p>
     * <p> This will always be called on background thread so it is safe to perform long running tasks
     * here. Any third party libraries called must be thread safe (or move the work to another thread)
     * since this method will be called from a thread in a
     * {@link java.util.concurrent.ExecutorService}
     * that may have more than one background thread. </p>
     * <p>
     * You <b>MUST</b> use the {@link DataCallback} once the request is complete.
     * <p>
     * You are free to move the fetch work to another thread and call the callback from there.
     * <p>
     * <p> This method will only be called when the corresponding resource is not in the cache. </p>
     * <p>
     * <p> Note - this method will be run on a background thread so blocking I/O is safe. </p>
     *
     * @param priority The priority with which the request should be completed.
     * @param callback The callback to use when the request is complete
     * @see #cleanup() where the data retuned will be cleaned up
     */
    void loadData(@NonNull Priority priority, @NonNull DataCallback callback);

    /**
     * Cleanup or recycle any resources used by this data fetcher. This method will be called in a
     * finally block after the data provided by {@link #loadData(Priority, DataCallback)} has been decoded.
     * <p>
     * <p> Note - this method will be run on a background thread so blocking I/O is safe. </p>
     */
    void cleanup();
}
