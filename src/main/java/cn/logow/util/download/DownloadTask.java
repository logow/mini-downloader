package cn.logow.util.download;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;

public class DownloadTask implements DownloadListener {

    private static final int NEW = 0;
    private static final int RUNNING = 1;
    private static final int COMPLETED = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED    = 5;
    private static final int INTERRUPTED = 6;

    private static final DownloadListener DEFAULT_LISTENER = new DefaultDownloadListener();

    private final URI url;
    private final String savePath;
    private final String filename;
    private volatile int state = NEW;
    private DownloadListener listener = DEFAULT_LISTENER;
    private Object attachment;

    private DownloadTask(URI url, String savePath, String filename) {
        this.url = url;
        this.savePath = savePath;
        this.filename = filename;
    }

    public static DownloadTask create(String url, String saveTo) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url must not be empty");
        }
        if (saveTo == null || saveTo.isEmpty()) {
            throw new IllegalArgumentException("savePath must not be empty");
        }

        URI uri = Utils.resolveURL(url);
        File saveDir = new File(saveTo);
        Utils.mkdirs(saveDir);

        return new DownloadTask(uri, saveDir.getPath(), null);
    }

    public static DownloadTask create(String url, File saveAs) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url must not be empty");
        }
        if (saveAs == null) {
            throw new IllegalArgumentException("saveAs must not be null");
        }

        URI uri = Utils.resolveURL(url);
        File saveDir = saveAs.getParentFile();
        String filename = saveAs.getName();
        Utils.mkdirs(saveDir);

        return new DownloadTask(uri, saveDir.getPath(), filename);
    }

    public Object attachment() {
        return attachment;
    }

    public boolean isRunning() {
        return RUNNING == state;
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state > RUNNING;
    }

    public void bind(DownloadListener listener) {
        bind(listener, null);
    }

    public void bind(DownloadListener listener, Object attachment) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (state != NEW) {
            throw new IllegalStateException("task was running or done");
        }
        this.listener = listener;
        this.attachment = attachment;
    }

    @Override
    public void onStart(DownloadContext context) {
        state = RUNNING;
        listener.onStart(context);
    }

    @Override
    public void onProgress(DownloadContext context, int bytes, long timeMillis) {
        listener.onProgress(context, bytes, timeMillis);
    }

    @Override
    public void onComplete(DownloadContext context) {
        state = COMPLETED;
        listener.onComplete(context);
    }

    @Override
    public void onCancel(DownloadContext context, int bytesTransferred) {
        state = INTERRUPTED;
        listener.onCancel(context, bytesTransferred);
    }

    @Override
    public void onError(DownloadContext context, Throwable e) throws DownloadException {
        state = EXCEPTIONAL;
        listener.onError(context, e);
    }

    public URI getUrl() {
        return url;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[");
        sb.append(filename != null ? filename : Utils.resolveFilename(url));
        sb.append("](");
        sb.append(url);
        sb.append(")");
        return sb.toString();
    }
}
