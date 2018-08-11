package net.qiujuer.library.planck.exception;

import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/11
 */
public class StreamInterruptException extends IOException {
    public StreamInterruptException() {
    }

    public StreamInterruptException(String message) {
        super(message);
    }

    public StreamInterruptException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamInterruptException(Throwable cause) {
        super(cause);
    }
}
