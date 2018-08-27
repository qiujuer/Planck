package net.qiujuer.sample.planck;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.qiujuer.library.planck.Planck;
import net.qiujuer.library.planck.PlanckSource;
import net.qiujuer.library.planck.file.FixedFileSizeGenerator;
import net.qiujuer.library.planck.integration.okhttp.OkHttpDataProvider;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static Planck mPlanck;

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
                    .setFileLengthGenerator(new FixedFileSizeGenerator(128 * 1024))
                    .build();
        }

        new Thread(this, "Planck-TEST-Thread1").start();
        new Thread(this, "Planck-TEST-Thread2").start();
        new Thread(this, "Planck-TEST-Thread3").start();
        new Thread(this, "Planck-TEST-Thread4").start();
        new Thread(this, "Planck-TEST-Thread5").start();
        new Thread(this, "Planck-TEST-Thread6").start();
        new Thread(this, "Planck-TEST-Thread7").start();
        new Thread(this, "Planck-TEST-Thread8").start();
        new Thread(this, "Planck-TEST-Thread9").start();
        new Thread(this, "Planck-TEST-Thread10").start();
        new Thread(this, "Planck-TEST-Thread11").start();
        new Thread(this, "Planck-TEST-Thread12").start();
        new Thread(this, "Planck-TEST-Thread13").start();
    }

    @Override
    public void run() {
        PlanckSource planckSource = mPlanck.get("http://mysns1.video.meipai.com/6423448346911900673.mp4");
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

            int bufferSize = 50;
            byte[] buffer = new byte[bufferSize];

            long pos = 0;
            while (length > 0) {
                int size = 0;
                try {
                    size = planckSource.get(pos, buffer, 0, bufferSize, 10000);
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
                digest.update(buffer, 0, size);
            }

            if (length == 0) {
                String hexString = HashUtils.convertToHexString(digest.digest());
                Log.i("TAG", "Hash:" + hexString + " " + ("89313db3df7c08af0cf68680285e79f2".equalsIgnoreCase(hexString)));
            } else {
                Log.w("TAG", "size:" + 0 + " length:" + length + " pos:" + pos);
            }
        } finally {
            planckSource.close();
        }
    }
}
