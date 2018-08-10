package net.qiujuer.library.planck.utils;

import java.io.File;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/10
 */
public class CacheUtil {
    public static final String CACHE_FILE_EXTENSION = ".mp";
    public static final String CACHE_TEMP_FILE_EXTENSION = ".tmp";

    public static String generateName(String name, int index, long startPos, long size, long totalSize) {
        return String.format("%s-%s-%s/%s/%s", name, index, startPos, size, totalSize);
    }

    public static boolean isTemp(String fileName) {
        return fileName.endsWith(CACHE_TEMP_FILE_EXTENSION);
    }

    public static boolean convertToOfficialCache(File file) {
        String absolutePath = file.getAbsolutePath();
        int index = absolutePath.lastIndexOf(CACHE_TEMP_FILE_EXTENSION);
        if (index == -1) {
            return false;
        }
        absolutePath = absolutePath.substring(0, index);
        File dest = new File(absolutePath, CACHE_FILE_EXTENSION);
        return file.renameTo(dest);
    }

    public static CacheInfo getCacheInfo(File file) {
        String name = file.getName();
        name = name.substring(0, name.lastIndexOf("\\."));
        String[] array = name.split("[-/]");
        if (array.length != 5) {
            throw new IllegalArgumentException("File cannot load cache info:" + file.getAbsolutePath());
        }

        String cName = array[0];
        int cIndex = Integer.parseInt(array[1]);
        long cStartPos = Long.parseLong(array[2]);
        long cSize = Long.parseLong(array[3]);
        long cTotalSize = Long.parseLong(array[4]);

        return new CacheInfo(cName, cIndex, cStartPos, cSize, cTotalSize);
    }

    /**
     * Only create file name, cannot create file in disk
     *
     * @param fileNameNonExt File name without extension
     * @return Temp file
     */
    public static File generateTempFile(String fileNameNonExt) {
        return new File(fileNameNonExt, CacheUtil.CACHE_TEMP_FILE_EXTENSION);
    }

    public static class CacheInfo {
        public final String mName;
        public final int mIndex;
        public final long mStartPos;
        public final long mSize;
        public final long mTotalSize;

        public CacheInfo(String name, int index, long startPos, long size, long totalSize) {
            mName = name;
            mIndex = index;
            mStartPos = startPos;
            mSize = size;
            mTotalSize = totalSize;
        }
    }
}