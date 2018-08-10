package net.qiujuer.library.integration.okhttp;

import android.text.TextUtils;

import net.qiujuer.library.planck.data.DataInfo;
import net.qiujuer.library.planck.data.DataProvider;
import net.qiujuer.library.planck.data.StreamFetcher;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class OkHttpDataProvider implements DataProvider {
    private static volatile Call.Factory internalClient;
    private final Call.Factory mClient;

    private static Call.Factory getInternalClient() {
        if (internalClient == null) {
            synchronized (OkHttpDataProvider.class) {
                if (internalClient == null) {
                    internalClient = new OkHttpClient();
                }
            }
        }
        return internalClient;
    }

    public OkHttpDataProvider() {
        this(getInternalClient());
    }

    public OkHttpDataProvider(Call.Factory client) {
        mClient = client;
    }

    @Override
    public DataInfo loadDataInfo(String url) {
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = mClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return null;
            }

            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }

            long contentLength = body.contentLength();
            boolean supportRanges = !TextUtils.isEmpty(response.header("Accept-Ranges", null));
            return new DataInfo(contentLength, supportRanges);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public StreamFetcher buildStreamFetcher(String url, long position, long size) {
        return new OkHttpStreamFetcher(url, position, size, mClient);
    }
}
