package cn.logow.util.download;

import java.io.IOException;
import java.io.InterruptedIOException;

public class DefaultDownloadListener implements DownloadListener {

    @Override
    public void onStart(DownloadContext context) {

    }

    @Override
    public void onProgress(DownloadContext context, int bytes, long timeMillis) {

    }

    @Override
    public void onComplete(DownloadContext context) {

    }

    @Override
    public void onAbort(DownloadContext context, InterruptedIOException e) throws IOException {
        throw e;
    }

    @Override
    public void onError(DownloadContext context, IOException e) throws IOException {
        throw e;
    }
}
