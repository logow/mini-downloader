package cn.logow.util.download;

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
    public void onCancel(DownloadContext context, int bytesTransferred) {

    }

    @Override
    public void onError(DownloadContext context, Throwable e) throws DownloadException {
        if (e instanceof DownloadException) {
            throw (DownloadException)e;
        } else {
            throw new DownloadException(e);
        }
    }
}
