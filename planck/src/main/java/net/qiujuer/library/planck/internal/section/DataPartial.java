package net.qiujuer.library.planck.internal.section;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/9
 */
public interface DataPartial {
    int length();

    void load();

    boolean isLoaded();
    
    int get(int position, byte[] buffer, int offset, int size);
}
