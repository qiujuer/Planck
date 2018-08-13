package net.qiujuer.library.planck.integration.okhttp;

import android.support.annotation.NonNull;

import net.qiujuer.library.planck.data.StreamFetcher;
import net.qiujuer.library.planck.exception.NetworkException;
import net.qiujuer.library.planck.utils.IoUtil;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class OkHttpStreamFetcher implements StreamFetcher, okhttp3.Callback {
    private final String mUrl;
    private final long mPosition;
    private final long mSize;
    private final Call.Factory mClient;

    private InputStream mStream;
    private ResponseBody mResponseBody;
    private DataCallback mCallback;

    private volatile Call mCall;

    public OkHttpStreamFetcher(String url, long position, long size, Call.Factory client) {
        mUrl = url;
        mPosition = position;
        mSize = size;
        mClient = client;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback callback) {
        Request.Builder requestBuilder = new Request.Builder().url(mUrl);
        if (mPosition != StreamFetcher.INVALID_INTEGER && mSize != StreamFetcher.INVALID_INTEGER) {
            final long endIndex = mPosition + mSize - 1;
            requestBuilder.addHeader("RANGE", "bytes=" + mPosition + "-" + endIndex);
        }
        Request request = requestBuilder.build();
        this.mCallback = callback;

        mCall = mClient.newCall(request);
        mCall.enqueue(this);
    }

    @Override
    public void cleanup() {
        IoUtil.close(mStream);
        IoUtil.close(mResponseBody);
        mStream = null;
        mResponseBody = null;
        mCallback = null;
    }

    @Override
    public void cancel() {
        Call local = mCall;
        if (local != null) {
            local.cancel();
        }
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
        mCallback.onLoadFailed(e);
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        ResponseBody responseBody = mResponseBody = response.body();
        if (response.isSuccessful()) {
            if (responseBody == null) {
                mCallback.onDataReady(null);
                return;
            }
            long contentLength = responseBody.contentLength();
            mStream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
            mCallback.onDataReady(mStream);
        } else {
            mCallback.onLoadFailed(new NetworkException(response.message(), response.code()));
        }
    }
}
