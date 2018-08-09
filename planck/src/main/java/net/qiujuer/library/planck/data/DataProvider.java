package net.qiujuer.library.planck.data;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public interface DataProvider {
    DataInfo loadDataInfo(String url);

    StreamFetcher buildStreamFetcher(String url, int position, int size);
}
