package net.qiujuer.library.planck.exception;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class HttpException extends RuntimeException {
    public HttpException(String message, int code) {
        super(String.format("HttpException with:[%s]-[%s]", code, message));
    }
}
