package cn.logow.util.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 文件下载器，支持 HTTP/HTTPS 批量下载、异步下载
 *
 * @author hailong.wu
 */
public class Downloader implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    private static final boolean ENABLE_UNSAFE_MODE = false;

    private static final int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1;

    private ExecutorService executor;

    private DownloadEngine downloadEngine;

    public Downloader() {
        executor = initDownloadExecutor();
        downloadEngine = new HttpClientEngine(ENABLE_UNSAFE_MODE);
        logger.info("Downloader initialized");
    }

    private ExecutorService initDownloadExecutor() {
        // 最大支持 MAX_POOL_SIZE 并发下载，超过限制时将使用调用线程串行下载
        return new ThreadPoolExecutor(0, MAX_POOL_SIZE, 1, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "download-thread");
                        thread.setDaemon(true);
                        return thread;
                    }
                }, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 同步下载
     * @param task
     * @return
     * @throws IOException
     */
    public FileEntity execute(DownloadTask task) throws IOException {
        Future<FileEntity> future = submit(task);
        FileEntity fileEntity = null;
        try {
            fileEntity = future.get();
        } catch (Exception e) {
            handleDownloadError(e);
        }
        return fileEntity;
    }

    /**
     * 异步下载
     * @param task 下载任务
     * @return 操作
     */
    public Future<FileEntity> submit(final DownloadTask task) {
        return executor.submit(new Callable<FileEntity>() {
            @Override
            public FileEntity call() throws IOException {
                return downloadEngine.execute(task);
            }
        });
    }

    /**
     * 批量下载，支持并发
     * @param batch 批量下载任务
     * @return 批量下载的文件实体
     * @throws IOException 下载异常
     */
    public List<FileEntity> executeBatch(BatchDownload batch) throws IOException {
        final List<Future<FileEntity>> futures = new ArrayList<>(batch.size());
        for (DownloadTask task : batch) {
            futures.add(submit(task));
        }

        List<FileEntity> results = new ArrayList<>(batch.size());
        try {
            for (Future<FileEntity> future : futures) {
                results.add(future.get());
            }
        } catch (Exception e) {
            handleDownloadError(e);
        }

        return results;
    }

    private void handleDownloadError(Throwable err) throws IOException {
        Throwable cause = err;
        if (err instanceof ExecutionException && err.getCause() != null) {
            cause = err.getCause();
        }
        if (cause instanceof IOException) {
            throw (IOException) cause;
        }
        throw new IllegalStateException("Unexpected error while downloading", cause);
    }

    public FileEntity downloadTo(String url, String saveTo) throws IOException {
        return execute(DownloadTask.create(url, saveTo));
    }

    public FileEntity downloadAs(String url, File saveAs) throws IOException {
        return execute(DownloadTask.create(url, saveAs));
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            downloadEngine.close();
        } catch (Exception e) {
            logger.error("Error closing DownloadEngine", e);
        }
        logger.info("Downloader closed");
    }
}
