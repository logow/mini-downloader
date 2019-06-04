package cn.logow.util.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * 下载引擎，实现文件下载协议
 *
 * @author logow
 */
public abstract class DownloadEngine implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DownloadEngine.class);

    // 默认文件IO缓冲区大小
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    // 默认更新下载进度的间隔
    private static final long DEFAULT_PROGRESS_TICK_MILLIS = 100L;

    public FileEntity execute(DownloadTask task) throws IOException {
        DownloadContext context = new DownloadContext(task);
        logger.debug("Starting download {}", task);
        task.onStart(context);

        FileEntity fileEntity = null;
        try {
            fileEntity = doExecute(task, context);
        } catch (IOException e) {
            if (InterruptedException.class.equals(e.getClass())) {
                logger.warn("Aborting download {}", task, e);
                task.onAbort(context, (InterruptedIOException) e);
            }
            logger.error("Error download {}", task, e);
            task.onError(context, e);
        }

        if (fileEntity != null) {
            task.onComplete(context);
            logger.info("Complete download {} to '{}'", task, fileEntity.getPath());
        }

        return fileEntity;
    }

    protected abstract FileEntity doExecute(DownloadTask task, DownloadContext context) throws IOException;

    protected int transfer(InputStream src, OutputStream dest, DownloadContext context) throws IOException {
        int n = 0;
        int count = 0;
        int transferred = 0;
        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
        long currentTime = System.currentTimeMillis();
        long nextTickTime = currentTime + DEFAULT_PROGRESS_TICK_MILLIS;

        while(!Thread.interrupted() && n != -1) {
            while (currentTime < nextTickTime && (n = src.read(buf)) != -1) {
                dest.write(buf, 0, n);
                count += n;
                currentTime = System.currentTimeMillis();
            }

            long timeMillis = currentTime - nextTickTime + DEFAULT_PROGRESS_TICK_MILLIS;
            context.getDownloadTask().onProgress(context, count, timeMillis);
            nextTickTime = currentTime + DEFAULT_PROGRESS_TICK_MILLIS;
            transferred += count;
            count = 0;
        }

        if (n != -1) {
            InterruptedIOException ioe = new InterruptedIOException("download interrupted");
            ioe.bytesTransferred = transferred;
            throw ioe;
        }

        return transferred;
    }
}
