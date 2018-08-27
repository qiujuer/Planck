package net.qiujuer.library.planck.file;

/**
 * Cache file name generator
 * You need to make sure that the cache file name corresponding to the url is unique
 * <p>
 * eg: {@link Md5FileNameGenerator}
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface FileNameGenerator {

    /**
     * Get the name of the cache file corresponding to the url,
     * cannot carry extension
     *
     * @param url The url used to get the cache file name
     * @return Cache file name
     */
    String generatePlanckCacheFileName(String url);
}
