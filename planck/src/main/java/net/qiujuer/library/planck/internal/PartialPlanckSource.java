package net.qiujuer.library.planck.internal;

import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.data.DataInfo;
import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.exception.NetworkException;
import net.qiujuer.library.planck.internal.section.CacheDataPartial;
import net.qiujuer.library.planck.internal.section.DataPartial;
import net.qiujuer.library.planck.internal.section.TempDataPartial;
import net.qiujuer.library.planck.utils.CacheUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public class PartialPlanckSource implements PlanckSource {
    private final int mMaxPartialSize = 512 * 1024; // 64Kb
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

            CacheUtil.CacheInfo cacheInfo = CacheUtil.getCacheInfo(firstChildFile);

            totalLength = cacheInfo.mTotalSize;
            partialSize = cacheInfo.mSize;
        }

        // The number of files is equal to the number of partial files that are all loaded
        final int fileCount = (int) ((totalLength + partialSize - 1) / partialSize);
        DataPartial[] dataPartials = new DataPartial[fileCount];
        for (int i = 0; i < fileCount; i++) {
            final long currentPartStartPos = partialSize * i;
            final long currentPartSize = Math.min(partialSize, totalLength - partialSize * i);

            final String fileName = CacheUtil.generateName(fileNamePrefix, i, currentPartStartPos, currentPartSize, totalLength);
            String fileNameWithExt = fileName + CacheUtil.CACHE_FILE_EXTENSION;
            File file = new File(fileNameWithExt);
            DataPartial partial;
            if (file.exists()) {
                partial = new CacheDataPartial(file);
            } else {
                File tempFile = CacheUtil.generateTempFile(mCacheRoot, fileName);
                partial = new TempDataPartial(tempFile, mHttpUrl, mProvider);
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
    public long load(long position, int timeout) throws IOException, TimeoutException {
        init();

        final long partialSize = mPartialSize;

        int partIndex = (int) (position / partialSize);
        long partPos = position - (partIndex * partialSize);
        DataPartial partial = mDataPartials[partIndex];
        long partReadablePos = partial.load(partPos, timeout);

        return partialSize * (partIndex + 1) + partReadablePos;
    }


    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        init();

        final long partialSize = mPartialSize;

        int partIndex = (int) (position / partialSize);
        long partPos = position - (partIndex * partialSize);
        DataPartial partial = mDataPartials[partIndex];

        return partial.get(partPos, buffer, offset, size, timeout);
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
}
