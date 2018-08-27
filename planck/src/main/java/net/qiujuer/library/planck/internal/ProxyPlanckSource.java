package net.qiujuer.library.planck.internal;

import android.os.SystemClock;
import android.webkit.URLUtil;

import net.qiujuer.library.planck.Planck;
import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.data.DataInfo;
import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.internal.contract.Initializer;
import net.qiujuer.library.planck.internal.contract.UsageFinalizer;
import net.qiujuer.library.planck.internal.section.CacheDataPartial;
import net.qiujuer.library.planck.internal.section.DataPartial;
import net.qiujuer.library.planck.internal.section.TempDataPartial;
import net.qiujuer.library.planck.utils.CacheUtil;
import net.qiujuer.library.planck.utils.IoUtil;
import net.qiujuer.library.planck.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In this we check cache state, select a good loading strategy
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/11
 */
public class ProxyPlanckSource implements PlanckSource, UsageFinalizer {
    private static final String TAG = ProxyPlanckSource.class.getSimpleName();
    private final static int INIT_TIMEOUT_VALUE = 15 * 1000;
    private final AtomicInteger mUsageCount = new AtomicInteger(0);
    private final AtomicBoolean mClosed = new AtomicBoolean(false);
    private final Planck.Store mPlanckStore;

    private final Object mSourceLock = new Object();
    private final String mSourceUrl;
    private PlanckSource mSource;


    public ProxyPlanckSource(Planck.Store store, String sourceUrl) {
        mPlanckStore = store;
        mSourceUrl = sourceUrl;
    }

    @Override
    public long length(int timeout) throws TimeoutException, IOException {
        checkClosed();
        int initValue = awaitSourceInitialed();
        return initValue == 0 ? mSource.length(timeout) : initValue;
    }

    @Override
    public long load(long position, int timeout) throws IOException, TimeoutException {
        checkClosed();
        int initValue = awaitSourceInitialed();
        return initValue == 0 ? mSource.load(position, timeout) : initValue;
    }

    @Override
    public int get(long position, byte[] buffer, int offset, int size, int timeout) throws IOException, TimeoutException {
        checkClosed();
        int initValue = awaitSourceInitialed();
        return initValue == 0 ? mSource.get(position, buffer, offset, size, timeout) : initValue;
    }

    @Override
    public void close() {
        if (onceFinalize()) {
            mClosed.set(true);
            mInitializer.cancel();
            mPlanckStore.outOfSource(mSourceUrl);
            mPlanckStore.post(mSourceCloseRunnable);
        }
    }

    @Override
    public int onceUsage() {
        return mUsageCount.incrementAndGet();
    }

    @Override
    public boolean onceFinalize() {
        return mUsageCount.decrementAndGet() <= 0;
    }

    private void checkClosed() throws IOException {
        if (mClosed.get()) {
            throw new IOException("Current source is closed.");
        }
    }

    private int awaitSourceInitialed() throws IOException {
        if (mSource == null) {
            mInitializer.retryOnFailed();
            synchronized (mSourceLock) {
                if (mSource == null) {
                    checkClosed();

                    try {
                        long endTime = SystemClock.elapsedRealtime() + INIT_TIMEOUT_VALUE;

                        mSourceLock.wait(INIT_TIMEOUT_VALUE);

                        // Init error
                        if (mSource == null) {
                            return SystemClock.elapsedRealtime() > endTime ? PlanckSource.INVALID_VALUES_INIT_TIMEOUT :
                                    PlanckSource.INVALID_VALUES_INIT_NETWORK_ERROR;
                        }
                    } catch (InterruptedException e) {
                        return PlanckSource.INVALID_VALUES_INIT_INTERRUPTED;
                    }
                }
            }
        }
        return 0;
    }

    private void attachSource(PlanckSource source) {
        synchronized (mSourceLock) {
            mSource = source;
            try {
                mSourceLock.notifyAll();
            } catch (Exception ignored) {
            }
        }
    }

    private void attachWithLocalSource(String filePath) {
        LocalPlanckSource localPlanckSource = new LocalPlanckSource(new File(filePath));
        attachSource(localPlanckSource);
    }

    private boolean attachWithNetwork(final String httpUrl, final String fileNamePrefix) {
        final long totalSize;
        final long partialSize;
        final boolean supportRandomReading;
        DataInfo dataInfo = mPlanckStore.dataProvider().loadDataInfo(httpUrl);
        if (dataInfo == null) {
            attachSource(null);
            return false;
        } else {
            totalSize = dataInfo.getLength();
            supportRandomReading = dataInfo.isSupportRandomReading();
            partialSize = supportRandomReading ? mPlanckStore.maxPartialSize(httpUrl, totalSize) : totalSize;
        }

        attachWithPartialCacheOrTemp(httpUrl, fileNamePrefix,
                totalSize, partialSize, supportRandomReading, mPlanckStore.dataProvider());
        return true;
    }

    private void attachWithPartialCacheOrTemp(final String httpUrl, final String fileNamePrefix,
                                              final long totalSize, final long partialSize,
                                              final boolean acceptRange,
                                              final DataProvider dataProvider) {
        // The number of files is equal to the number of partial files that are all loaded
        final File cacheRoot = mPlanckStore.cacheRoot();

        final int fileCount = (int) ((totalSize + partialSize - 1) / partialSize);

        DataPartial[] dataPartials = new DataPartial[fileCount];
        boolean hasUnfinishedFile = false;
        for (int i = 0; i < fileCount; i++) {
            final long partStartPos = partialSize * i;
            final long partSize = Math.min(partialSize, totalSize - partialSize * i);

            final String fileName = CacheUtil.generateName(fileNamePrefix, i, partStartPos, partSize, totalSize, acceptRange);
            File file = CacheUtil.generateCacheFile(cacheRoot, fileName);

            DataPartial partial;
            if (file.exists()) {
                partial = new CacheDataPartial(file);
            } else {
                File tempFile = CacheUtil.generateTempFile(cacheRoot, fileName);
                partial = new TempDataPartial(tempFile, httpUrl, dataProvider);
                hasUnfinishedFile = true;
            }
            dataPartials[i] = partial;
        }

        if (!hasUnfinishedFile) {
            final String completedFileName = CacheUtil.generateCompletedName(fileNamePrefix, totalSize);
            File completedCacheFile = CacheUtil.generateCacheFile(cacheRoot, completedFileName);
            try {
                if (margePartialCacheFile(completedCacheFile, dataPartials)) {
                    Logger.d(TAG, "MargePartialCacheFile succeed:" + completedCacheFile.getName());
                    removeAndClosePartialCacheFile(dataPartials);
                    attachWithLocalSource(completedCacheFile.getAbsolutePath());
                    return;
                }
            } catch (IOException e) {
                Logger.e(TAG, "MargePartialCacheFile failed:" + completedCacheFile.getName());
            }
        }

        PartialPlanckSource partialPlanckSource = new PartialPlanckSource(dataPartials, totalSize, partialSize);
        attachSource(partialPlanckSource);
    }

    private Runnable mSourceCloseRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mSourceLock) {
                if (mSource != null) {
                    IoUtil.close(mSource);
                    mSource = null;
                }
            }
        }
    };

    public Initializer initializer() {
        return mInitializer;
    }

    private Initializer mInitializer = new Initializer() {
        @Override
        protected boolean onInitialize() {
            if (mClosed.get()) {
                return true;
            }

            final String sourceUrl = mSourceUrl;

            // Check and load local file source
            final boolean isLocalFilePath = !URLUtil.isNetworkUrl(sourceUrl);
            if (isLocalFilePath) {
                attachWithLocalSource(sourceUrl);
                return true;
            }

            final File cacheRoot = mPlanckStore.cacheRoot();
            final String fileNamePrefix = mPlanckStore.fileNameGenerator().generatePlanckCacheFileName(sourceUrl);
            final File[] childFiles = cacheRoot.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.startsWith(fileNamePrefix);
                }
            });

            // Check and load network source
            if (childFiles.length == 0) {
                return attachWithNetwork(sourceUrl, fileNamePrefix);
            }

            // Check and load partial cache source
            final int fileNamePrefixIndex = fileNamePrefix.length();
            final String firstChildFileNameMask = "-0-0-";
            final String completedFileNameMask = "-T-";
            File firstChildFile = null;
            File completedChildFile = null;

            for (File childFile : childFiles) {
                String childFileName = childFile.getName();
                if (firstChildFile == null && childFileName.startsWith(firstChildFileNameMask, fileNamePrefixIndex)) {
                    firstChildFile = childFile;
                } else if (completedChildFile == null && childFileName.startsWith(completedFileNameMask, fileNamePrefixIndex)) {
                    completedChildFile = childFile;
                }

                if (completedChildFile != null && firstChildFile != null) {
                    break;
                }
            }

            // Check and load cache complete file source
            if (completedChildFile != null && completedChildFile.exists()) {
                if (CacheUtil.verifyCompletedFileIsFinish(completedChildFile.getName(), completedChildFile.length())) {
                    // Clear invalid cache file
                    if (childFiles.length > 1) {
                        for (File childFile : childFiles) {
                            if (completedChildFile != childFile) {
                                //noinspection ResultOfMethodCallIgnored
                                childFile.delete();
                            }
                        }
                    }
                    // Start
                    attachWithLocalSource(completedChildFile.getAbsolutePath());
                    return true;
                }
            }


            // Find first file
            if (firstChildFile == null) {
                // Delete all cache
                for (File childFile : childFiles) {
                    //noinspection ResultOfMethodCallIgnored
                    childFile.delete();
                }
                // Reload with network
                return attachWithNetwork(sourceUrl, fileNamePrefix);
            }


            // Load globe CacheInfo from first file
            CacheUtil.CacheInfo cacheInfo = CacheUtil.getCacheInfo(firstChildFile);
            final long totalSize = cacheInfo.mTotalSize;
            final long partialSize = cacheInfo.mSize;
            final boolean acceptRange = cacheInfo.mSupportRandomReading;
            attachWithPartialCacheOrTemp(sourceUrl, fileNamePrefix, totalSize, partialSize, acceptRange, mPlanckStore.dataProvider());
            return true;
        }
    };


    private static void removeAndClosePartialCacheFile(DataPartial[] dataPartials) {
        for (DataPartial dataPartial : dataPartials) {
            IoUtil.close(dataPartial);
            //noinspection ResultOfMethodCallIgnored
            dataPartial.getFile().delete();
        }
    }

    private static boolean margePartialCacheFile(File outFile, DataPartial[] dataPartials) throws IOException {
        if (outFile.exists()) {
            if (!outFile.delete()) {
                return false;
            }
        }

        if (!outFile.createNewFile()) {
            return false;
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(outFile);
            FileChannel out = fileOutputStream.getChannel();
            for (DataPartial dataPartial : dataPartials) {
                File file = dataPartial.getFile();
                long length = file.length();
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    FileChannel inChannel = inputStream.getChannel();
                    for (long p = 0; p < length; ) {
                        p += inChannel.transferTo(p, length - p, out);
                    }
                } finally {
                    IoUtil.close(inputStream);
                }
            }
        } finally {
            IoUtil.close(fileOutputStream);
        }

        return true;
    }
}
