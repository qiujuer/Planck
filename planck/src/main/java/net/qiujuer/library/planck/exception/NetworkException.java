package net.qiujuer.library.planck.exception;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class NetworkException extends RuntimeException {
    public static final int CUSTOM_ERROR_GET_DATA_INFO = 10001;
    public NetworkException(String message, int code) {
        super(String.format("NetworkException with:[%s]-[%s]", code, message));
    }
}
