package cn.logow.util.download;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * 下载任务监听器
 */
public interface DownloadListener {

    /**
     * 下载开始
     * @param context
     */
    void onStart(DownloadContext context);

    /**
     * 下载进度
     * @param context
     * @param bytes 下载字节数
     * @param timeMillis 下载耗时
     */
    void onProgress(DownloadContext context, int bytes, long timeMillis);

    /**
     * 下载完成
     * @param context
     */
    void onComplete(DownloadContext context);

    /**
     * 下载中止
     * @param context
     * @param e
     * @throws IOException
     */
    void onAbort(DownloadContext context, InterruptedIOException e) throws IOException;

    /**
     * 下载异常
     * @param context
     * @param e
     * @throws IOException
     */
    void onError(DownloadContext context, IOException e) throws IOException;
}
