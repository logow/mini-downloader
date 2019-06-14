package cn.logow.util.download;

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
     * 下载取消
     * @param context
     * @param bytesTransferred
     */
    void onCancel(DownloadContext context, int bytesTransferred);

    /**
     * 下载异常
     * @param context
     * @param e
     * @throws DownloadException
     */
    void onError(DownloadContext context, Throwable e) throws DownloadException;
}
