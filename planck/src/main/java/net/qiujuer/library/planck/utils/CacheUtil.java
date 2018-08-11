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

    public static String generateName(String name, int index, long startPos, long size, long totalSize, boolean supportAcceptRange) {
        return String.format("%s-%s-%s-%s-%s-%s", name, index, startPos, size, totalSize, supportAcceptRange ? "A" : "B");
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
        File dest = new File(absolutePath + CACHE_FILE_EXTENSION);
        return file.renameTo(dest);
    }

    public static CacheInfo getCacheInfo(File file) {
        String name = removeNameExtension(file.getName());
        String[] array = name.split("-");
        if (array.length != 6) {
            throw new IllegalArgumentException("File cannot load cache info:" + file.getAbsolutePath());
        }

        String cName = array[0];
        int cIndex = Integer.parseInt(array[1]);
        long cStartPos = Long.parseLong(array[2]);
        long cSize = Long.parseLong(array[3]);
        long cTotalSize = Long.parseLong(array[4]);
        boolean cAcceptRange = array[5].equals("A");

        return new CacheInfo(cName, cIndex, cStartPos, cSize, cTotalSize, cAcceptRange);
    }


    public static String removeNameExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    /**
     * Only create file name, cannot create file in disk
     *
     * @param rootDir        Cache Temp dir
     * @param fileNameNonExt File name without extension
     * @return Temp file
     */
    public static File generateTempFile(File rootDir, String fileNameNonExt) {
        return new File(rootDir, fileNameNonExt + CacheUtil.CACHE_TEMP_FILE_EXTENSION);
    }

    /**
     * Only create file name, cannot create file in disk
     *
     * @param rootDir        Cache dir
     * @param fileNameNonExt File name without extension
     * @return Cache file
     */
    public static File generateCacheFile(File rootDir, String fileNameNonExt) {
        return new File(rootDir, fileNameNonExt + CacheUtil.CACHE_FILE_EXTENSION);
    }

    @SuppressWarnings("WeakerAccess")
    public static class CacheInfo {
        public final String mName;
        public final int mIndex;
        public final long mStartPos;
        public final long mSize;
        public final long mTotalSize;
        public final boolean mAcceptRange;

        public CacheInfo(String name, int index, long startPos, long size, long totalSize, boolean acceptRange) {
            mName = name;
            mIndex = index;
            mStartPos = startPos;
            mSize = size;
            mTotalSize = totalSize;
            mAcceptRange = acceptRange;
        }
    }
}