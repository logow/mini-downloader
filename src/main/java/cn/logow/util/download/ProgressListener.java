package cn.logow.util.download;

public class ProgressListener extends DefaultDownloadListener {

    private long startTime;
    private long endTime;
    private int bytesTransferred;
    private long millisElapsed;

    @Override
    public void onStart(DownloadContext context) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onProgress(DownloadContext context, int bytes, long timeMillis) {
        bytesTransferred += bytes;
        millisElapsed += timeMillis;
    }

    @Override
    public void onComplete(DownloadContext context) {
        endTime = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getBytesTransferred() {
        return bytesTransferred;
    }

    public long getMillisElapsed() {
        return millisElapsed;
    }
}
