package net.qiujuer.library.planck.file;

/**
 * Single cache file maximum length
 * When the request stream data is greater than the length, the partial storage scheme is adopted.
 * The maximum length of each partial file is the current set value.
 * <p>
 * Bonus: the maximum length of first partial cache file will be used if there are already first cached file.
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/27
 */
public interface FileLengthGenerator {
    /**
     * Gets the current maximum cache file size that needs to be stored.
     * In general, we recommend setting it to 16MB;
     * set to 4MB if stored for short video;
     * it is recommended to set it to 2MB if the flow is slow.
     *
     * @param url         The Url of the file that needs to be downloaded
     * @param totalLength Gets the total length of files
     * @return Single cache file maximum length
     */
    long generatePlanckCacheFileMaxLength(String url, long totalLength);
}
