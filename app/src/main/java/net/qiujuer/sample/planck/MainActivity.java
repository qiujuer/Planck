package net.qiujuer.sample.planck;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.qiujuer.library.integration.okhttp.OkHttpDataProvider;
import net.qiujuer.library.planck.Planck;
import net.qiujuer.library.planck.PlanckSource;

public class MainActivity extends AppCompatActivity implements Runnable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(this).start();
    }

    @Override
    public void run() {
        Planck planck = new Planck.Builder(this)
                .setDataProvider(new OkHttpDataProvider())
                .build();

        PlanckSource planckSource = planck.get("http://mysns1.video.meipai.com/6423448346911900673.mp4");
        long length = planckSource.length(10000);
        byte[] buffer1 = new byte[20];
        byte[] buffer2 = new byte[20];
        try {
            planckSource.get(0, buffer1, 0, 10, 10000);
            planckSource.get(5, buffer2, 0, 10, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
