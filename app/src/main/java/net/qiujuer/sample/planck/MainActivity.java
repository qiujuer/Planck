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
    PlanckSource mPlanckSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File cacheRoot = StorageUtil.getIndividualCacheDirectory(this);
        if (!cacheRoot.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheRoot.mkdirs();
        }
        Planck planck = new Planck.Builder(new OkHttpDataProvider(), cacheRoot)
                .build();

        mPlanckSource = planck.get("http://mysns1.video.meipai.com/6423448346911900673.mp4");
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(this, "Planck-TEST-Thread").start();
    }

    @Override
    public void run() {
        PlanckSource planckSource = mPlanckSource;
        long length = 0;
        try {
            length = planckSource.length(10000);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        if (length < 0) {
            Log.e("TAG", "Length:" + length);
            return;
        }

        int bufferSize = 1024 * 64;
        byte[] buffer = new byte[bufferSize];

        long pos = 0;
        while (length > 0) {
            int size = 0;
            try {
                size = planckSource.get(pos, buffer, 0, bufferSize, 10000);
                Log.e("TAG", "size:" + size + " length:" + length + " pos:" + pos);
                if (size < 0) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            pos += size;
            length -= size;
        }
    }

}
