package net.qiujuer.sample.planck;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.qiujuer.library.integration.okhttp.OkHttpDataProvider;
import net.qiujuer.library.planck.Planck;
import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.utils.StorageUtil;

import java.io.File;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements Runnable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(this).start();
    }

    @Override
    public void run() {
        File cacheRoot = StorageUtil.getIndividualCacheDirectory(this);
        if (!cacheRoot.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheRoot.mkdirs();
        }
        Planck planck = new Planck.Builder(new OkHttpDataProvider(), cacheRoot)
                .build();

        PlanckSource planckSource = planck.get("http://mysns1.video.meipai.com/6423448346911900673.mp4");
        long length = 0;
        try {
            length = planckSource.length(10000);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        byte[] buffer1 = new byte[1024 * 64];
        try {
            long pos = 0;
            while (length > 0) {
                int size = planckSource.get(pos, buffer1, 0, 64 * 1024, 10000);
                Log.e("TAG", "Size:" + size + " length:" + length);
                pos += size;
                length -= size;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
