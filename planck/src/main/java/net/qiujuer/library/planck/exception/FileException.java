package net.qiujuer.library.planck.exception;

import java.io.File;
import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class FileException {
    public static final int FILE_NOT_FIND = 10000;
    public static final int CREATE_ERROR = 10001;
    public static final int INIT_NEW_FILE_ERROR = 10002;

    public static IOException throwIOException(File file, int code) {
        return new IOException(String.format("FileException with:[%s]-[%s]", code, file.getAbsolutePath()));
    }

    public static IOException throwIOException(File file, int code, Throwable throwable) {
        return new IOException(String.format("FileException with:[%s]-[%s]-[%s]", code, file.getAbsolutePath(), throwable.getMessage()));
    }
}
