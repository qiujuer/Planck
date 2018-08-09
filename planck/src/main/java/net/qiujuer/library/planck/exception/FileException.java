package net.qiujuer.library.planck.exception;

import java.io.File;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class FileException extends RuntimeException {
    public static final int FILE_NOT_FIND = 10000;
    public static final int CREATE_ERROR = 10001;
    public static final int INIT_NEW_FILE_ERROR = 10002;

    public FileException(File file, int code) {
        super(String.format("FileException with:[%s]-[%s]", code, file.getAbsolutePath()));
    }

    public FileException(File file, int code, Throwable throwable) {
        super(String.format("FileException with:[%s]-[%s]-[%s]", code, file.getAbsolutePath(), throwable.getMessage()));
    }
}
