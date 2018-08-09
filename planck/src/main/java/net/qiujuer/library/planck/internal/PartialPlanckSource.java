package net.qiujuer.library.planck.internal;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.data.DataInfo;
import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.exception.NetworkException;
import net.qiujuer.library.planck.internal.section.CacheDataPartial;
import net.qiujuer.library.planck.internal.section.DataPartial;
import net.qiujuer.library.planck.internal.section.TempDataPartial;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class PartialPlanckSource implements PlanckSource {
    public static final String PLANCK_FILE_EXTENSION = ".mp";
    private final int mMaxPartialSize = 64 * 1024; // 64Kb
    private final String mHttpUrl;
    private final String mFileNamePrefix;
    private final File mCacheRoot;
    private final DataProvider mProvider;
    private DataPartial[] mDataPartials;
    private boolean mInit;
    private long mLength;
    private long mPartialSize;

    public PartialPlanckSource(String httpUrl, String fileNamePrefix, File cacheRoot, DataProvider provider) {
        mHttpUrl = httpUrl;
        mFileNamePrefix = fileNamePrefix;
        mCacheRoot = cacheRoot;
        mProvider = provider;
    }

    private void init() {
        if (mInit) {
            return;
        }

        final String fileNamePrefix = mFileNamePrefix;
        final File[] childFiles = mCacheRoot.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith(fileNamePrefix);
            }
        });

        final long totalLength;
        final long partialSize;
        if (childFiles == null || childFiles.length == 0) {
            DataInfo dataInfo = mProvider.loadDataInfo(mHttpUrl);
            if (dataInfo == null) {
                throw new NetworkException("Load data info error", NetworkException.CUSTOM_ERROR_GET_DATA_INFO);
            } else {
                totalLength = dataInfo.getLength();
                if (dataInfo.isSupportAcceptRangesOperation()) {
                    partialSize = mMaxPartialSize;
                } else {
                    partialSize = totalLength;
                }
            }
        } else {
            final String firstChildFileNamePrefix = fileNamePrefix + "-0-";
            File firstChildFile = null;
            for (File childFile : childFiles) {
                if (childFile.getName().startsWith(firstChildFileNamePrefix)) {
                    firstChildFile = childFile;
                    break;
                }
            }

            if (firstChildFile == null) {
                // Delete all cache
                for (File childFile : childFiles) {
                    //noinspection ResultOfMethodCallIgnored
                    childFile.delete();
                }

                // TODO reload
                return;
            }

            totalLength = getTotalLengthFormFile(firstChildFile);
            partialSize = getPartialSizeFormFile(firstChildFile);
        }

        // The number of files is equal to the number of partial files that are all loaded
        final int fileCount = (int) ((totalLength + partialSize - 1) / partialSize);
        DataPartial[] dataPartials = new DataPartial[fileCount];
        for (int i = 0; i < fileCount; i++) {
            String fileName = String.format("%s-%s-%s", fileNamePrefix, i, totalLength);
            String fileNameWithExt = fileName + PLANCK_FILE_EXTENSION;
            File file = new File(fileNameWithExt);
            DataPartial partial;
            if (file.exists()) {
                partial = new CacheDataPartial(file);
            } else {
                long childPartialSize = Math.min(partialSize, totalLength - partialSize * i);
                File tempFile = TempDataPartial.findOrCreateTempFile(fileName, childPartialSize);
                partial = new TempDataPartial(tempFile);
            }
            dataPartials[i] = partial;
        }

        mLength = totalLength;
        mPartialSize = partialSize;
        mDataPartials = dataPartials;

        mInit = true;
    }

    @Override
    public long length(int timeout) {
        init();
        return mLength;
    }

    @Override
    public long load(long position, int timeout) throws IOException {
        init();

        int index = (int) (position / mPartialSize);
        long partialPosition = position - (index * mPartialSize);
        DataPartial dataPartial = mDataPartials[index];

        boolean loaded = dataPartial.isLoaded(partialPosition);
        if (!loaded) {
            try {
                Thread.sleep(timeout);
                loaded = dataPartial.isLoaded(partialPosition);
                if (!loaded) {
                    return -1;
                }
            } catch (InterruptedException e) {
                return -2;
            }
        }

        return position;
    }


    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException {
        init();

        long loadPos = position + size;
        long loadPosRet = load(loadPos, timeout);
        if (loadPos == loadPosRet) {
            int index = (int) (position / mPartialSize);
            long partialPosition = position - (index * mPartialSize);
            DataPartial dataPartial = mDataPartials[index];
            return dataPartial.get(partialPosition, buffer, offset, size);
        } else {
            return (int) loadPosRet;
        }
    }

    @Override
    public void close() {
        if (mDataPartials == null) {
            return;
        }
        for (DataPartial dataPartial : mDataPartials) {
            dataPartial.close();
        }
    }

    private static long getTotalLengthFormFile(File file) {
        String name = file.getName();
        int index = name.lastIndexOf("-");
        int endIndex = name.lastIndexOf(".");
        String numberStr = name.substring(index, endIndex);
        return Long.parseLong(numberStr);
    }

    private static long getPartialSizeFormFile(File file) {
        if (file.getName().endsWith(TempDataPartial.TEMP_FILE_SUFFIX)) {
            // This is temp file, we must minus temp footer len
            return file.length() - TempDataPartial.TEMP_DATA_FOOTER_LEN;
        } else {
            return file.length();
        }
    }
}