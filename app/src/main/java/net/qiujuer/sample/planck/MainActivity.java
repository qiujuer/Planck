package net.qiujuer.sample.planck;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.qiujuer.library.planck.Planck;
import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.file.FixedFileSizeGenerator;
import net.qiujuer.library.planck.integration.okhttp.OkHttpDataProvider;
import net.qiujuer.library.planck.utils.IoUtil;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static Planck mPlanck;

    @SuppressLint("WildThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File cacheRoot = StorageUtil.getIndividualCacheDirectory(this);
        if (!cacheRoot.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheRoot.mkdirs();
        }

        if (mPlanck == null) {
            mPlanck = new Planck.Builder(new OkHttpDataProvider(), cacheRoot)
                    .setFileLengthGenerator(new FixedFileSizeGenerator(1024 * 1024 - 1))
                    .build();
        }

        new Thread(this, "Planck-TEST-Thread").start();
    }

    @Override
    public void run() {
        PlanckSource planckSource = mPlanck.get("http://mysns1.video.meipai.com/5bd738bb88b5dzhxcz368i2998.mp4");
        try {
            long length = 0;
            try {
                length = planckSource.length(10000);
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i("TAG", "Length:" + length);

            if (length < 0) {
                return;
            }

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ignored) {
                return;
            }

            int bufferSize = 1024 * 1024 + 20;
            int offset = 19;
            byte[] buffer = new byte[bufferSize];

            long pos = 0;
            while (length > 0) {
                int size = 0;
                try {
                    size = planckSource.get(pos, buffer, offset, bufferSize - offset, 10000);
                    if (size < 0) {
                        return;
                    }
                } catch (IOException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pos += size;
                length -= size;
                digest.update(buffer, offset, size);
            }

            if (length == 0) {
                String hexString = HashUtils.convertToHexString(digest.digest());
                Log.i("TAG", "Hash:" + hexString + " " + ("02edbfb82dc3485d3795843ada9fe683".equalsIgnoreCase(hexString)));
            } else {
                Log.w("TAG", "size:" + 0 + " length:" + length + " pos:" + pos);
            }
        } finally {
            IoUtil.close(planckSource);
        }
    }
}
