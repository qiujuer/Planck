package net.qiujuer.library.planck.internal.section;

import net.qiujuer.library.planck.exception.FileException;
import net.qiujuer.library.planck.utils.IoUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeoutException;

/**
 * 缓存文件包括尾部8字节描述当前下载情况
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class TempDataPartial extends CacheDataPartial {
    public static final int TEMP_DATA_FOOTER_LEN = 8;
    public static final String TEMP_FILE_SUFFIX = ".tmp";

    public TempDataPartial(File file) {
        super(file);
    }

    @Override
    public long length() {
        return super.length() - TEMP_DATA_FOOTER_LEN;
    }

    @Override
    public long load(long position, int timeout) throws IOException, TimeoutException {
        return super.load(position, timeout) - TEMP_DATA_FOOTER_LEN;
    }

    public static File findOrCreateTempFile(String fileNameNonExt, long fileSize) {
        String filePath = fileNameNonExt + TEMP_FILE_SUFFIX;
        File file = new File(filePath);
        if (file.exists()) {
            return file;
        }
        try {
            if (!file.createNewFile()) {
                throw new FileException(file, FileException.CREATE_ERROR);
            }
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(file, CacheDataPartial.DEFAULT_WRITE_FILE_MODE);
                randomAccessFile.setLength(fileSize + TEMP_DATA_FOOTER_LEN);
            } finally {
                IoUtil.close(randomAccessFile);
            }
            return file;
        } catch (IOException e) {
            throw new FileException(file, FileException.INIT_NEW_FILE_ERROR, e);
        }
    }
}
