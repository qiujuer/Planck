package net.qiujuer.sample.planck;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.qiujuer.library.planck.Planck;
import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.integration.okhttp.OkHttpDataProvider;
import net.qiujuer.library.planck.utils.StorageUtil;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        new Thread(this, "Planck-TEST-Thread1").start();
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

        Log.e("TAG", "Length:" + length);

        if (length < 0) {
            return;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignored) {
            return;
        }

        int bufferSize = 512;
        byte[] buffer = new byte[bufferSize];

        long pos = 0;
        while (length > 0) {
            int size = 0;
            try {
                size = planckSource.get(pos, buffer, 0, bufferSize, 10000);
                if (size < 0) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            pos += size;
            length -= size;
            digest.update(buffer, 0, size);
        }

        if (length == 0) {
            String hexString = HashUtils.convertToHexString(digest.digest());
            Log.e("TAG", "Hash:" + hexString + " " + ("89313db3df7c08af0cf68680285e79f2".equalsIgnoreCase(hexString)));
        } else {
            Log.e("TAG", "size:" + 0 + " length:" + length + " pos:" + pos);
        }
    }
}
