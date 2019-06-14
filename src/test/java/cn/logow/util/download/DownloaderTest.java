package cn.logow.util.download;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class DownloaderTest {

    private String saveDir = "/tmp/test_downloader";
    private Downloader downloader;

    @Before
    public void setUp() {
        downloader = new Downloader();
    }

    @After
    public void tearDown() {
        downloader.close();
    }

    @Test
    public void testSyncDownload() throws IOException {
        String url = urlForSyncDownload();
        DownloadTask task = DownloadTask.create(url, saveDir);
        FileEntity fileEntity = downloader.execute(task);
        File file = new File(fileEntity.getPath());
        Assert.assertTrue(file.exists());
        Assert.assertEquals(file.getName(), fileEntity.getFilename());
        file.delete();
    }

    @Test
    public void testAsyncDownload() throws Exception {
        String url = urlForAsyncDownload();
        File file = new File(saveDir, Utils.resolveFilename(url));
        DownloadTask task = DownloadTask.create(url, file);
        final CountDownLatch latch = new CountDownLatch(1);
        task.bind(new DefaultDownloadListener() {
            @Override
            public void onProgress(DownloadContext context, int bytes, long timeMillis) {
                float rate = (bytes / 1024.0f) / (timeMillis / 1000.0f);
                System.out.printf("bytes=%d, time=%d, rate=%.2f KB/s%n", bytes, timeMillis, rate);
            }
            @Override
            public void onComplete(DownloadContext context) {
                latch.countDown();
            }
        });
        downloader.submit(task);
        Assert.assertTrue(!file.exists());
        latch.await();
        Assert.assertTrue(file.exists());
        file.delete();
    }

    @Test
    public void testBatchDownload() throws IOException {
        List<String> urls = urlsForBatchDownload();
        BatchDownload batch = new BatchDownload();
        batch.addTasks(urls, saveDir);
        List<FileEntity> entities = downloader.executeBatch(batch);
        Assert.assertEquals(urls.size(), entities.size());
        for (FileEntity entity : entities) {
            File file = new File(entity.getPath());
            Assert.assertTrue(file.exists());
            file.delete();
        }
    }

    @Test
    public void testDownloadHeader() throws IOException {
        List<String> urls = urlsForDownloadHeader();
        BatchDownload batch = new BatchDownload();
        batch.addTasks(urls, saveDir);
        List<FileEntity> entities = downloader.executeBatch(batch);
        String filename = entities.get(0).getFilename();
        for (FileEntity fe : entities) {
            Assert.assertEquals(filename, fe.getFilename());
            fe.delete();
        }
    }

    @Test
    public void testDownloadCancel() throws Exception {
        DownloadTask task = DownloadTask.create(urlForDownloadInterrupt(), saveDir);
        final CountDownLatch latch = new CountDownLatch(1);
        task.bind(new DefaultDownloadListener() {
            @Override
            public void onCancel(DownloadContext context, int bytesTransferred) {
                latch.countDown();
            }
        });
        Future<FileEntity> future = downloader.submit(task);
        Thread.sleep(1000);
        future.cancel(true);
        latch.await();
        Assert.assertTrue(task.isCancelled());
    }

    @Test(expected = IOException.class)
    public void testDownloadError() throws IOException {
        downloader.downloadTo(urlForDownloadError(), saveDir);
    }

    private String urlForSyncDownload() {
        return "http://central.maven.org/maven2/commons-io/commons-io/2.6/commons-io-2.6.jar";
    }

    private String urlForAsyncDownload() {
        return "http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar";
    }

    private String urlForDownloadInterrupt() {
        return "http://mirror.bit.edu.cn/apache/tomcat/tomcat-8/v8.5.41/bin/apache-tomcat-8.5.41.zip";
    }

    private String urlForDownloadError() {
        return "http://mirror.bit.edu.cn/apache/tomcat/tomcat-8/v8.5.41/bin/apache-tomcat-8.5.zip";
    }

    private List<String> urlsForBatchDownload() {
        return Arrays.asList(
                "http://central.maven.org/maven2/commons-io/commons-io/2.6/commons-io-2.6.jar",
                "http://central.maven.org/maven2/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar",
                "http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar"
        );
    }

    /**
     * 特别感谢作者提供实验链接，以下URL来自 <a>https://blog.lyz810.com/article/2016/11/download-response-header-setting/</a>
     */
    private List<String> urlsForDownloadHeader() {
        String url1 = "https://demo.lyz810.com/downloadHeader/?fntype=ascii&output=old";
        String url2 = "https://demo.lyz810.com/downloadHeader/?fntype=ascii&output=old&urlencode=1";
        String url3 = "https://demo.lyz810.com/downloadHeader/?output=old";
        String url4 = "https://demo.lyz810.com/downloadHeader/?output=old&urlencode=1";
        String url5 = "https://demo.lyz810.com/downloadHeader/?output=new&urlencode=1";
        String url6 = "https://demo.lyz810.com/downloadHeader/?urlencode=1";
        return Arrays.asList(url1, url2, url3, url4, url5, url6);
    }
}
