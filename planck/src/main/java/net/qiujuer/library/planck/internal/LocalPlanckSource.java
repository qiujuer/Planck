package net.qiujuer.library.planck.internal;

import net.qiujuer.library.planck.PlanckSource;

import java.io.File;
import java.io.InputStream;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class LocalPlanckSource implements PlanckSource {
    private final File mFile;
    private InputStream mInputStream;

    public LocalPlanckSource(File file) {
        mFile = file;
    }


    @Override
    public long length() {
        return mInputStream.available();
    }

    @Override
    public void position(int position) {

    }

    @Override
    public int read(byte[] buffer, int offset, int size) {
        return 0;
    }
}
