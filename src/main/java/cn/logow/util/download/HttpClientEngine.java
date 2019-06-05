package cn.logow.util.download;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Apache HttpClient 实现的下载引擎
 *
 * @author hailong.wu
 */
public class HttpClientEngine extends DownloadEngine {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientEngine.class);
    // 默认使用 Chrome 浏览器的 UserAgent，兼容性最佳
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    // 所支持的 HTTPS 传输层安全协议
    private static final String[] SSL_PROTOCOLS = {"TLSv1", "TLSv1.1", "TLSv1.2"};
    // 下载响应头
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    // 下载响应头类型
    private static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    // 下载响应头类型
    private static final String CONTENT_DISPOSITION_INLINE = "inline";
    // 默认连接超时
    private static final int DEFAULT_CONN_TIMEOUT = 10000;
    // 默认响应超时
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    // 默认下载重试次数
    private static final int DEFAULT_RETRY_COUNT = 2;

    private CloseableHttpClient httpClient;

    HttpClientEngine(boolean unsafeMode) {
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setConnectTimeout(DEFAULT_CONN_TIMEOUT)
                .setSocketTimeout(DEFAULT_READ_TIMEOUT)
                .build();

        HttpClientBuilder builder = HttpClientBuilder.create()
                .setUserAgent(DEFAULT_USER_AGENT)
                .setMaxConnPerRoute(5)
                .setMaxConnTotal(100)
                .evictIdleConnections(1, TimeUnit.MINUTES)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(new StandardHttpRequestRetryHandler(DEFAULT_RETRY_COUNT, true));
        if (unsafeMode) {
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(
                    unsafeSSLContext(), SSL_PROTOCOLS, null, new UnsafeHttps()));
        }

        httpClient = builder.build();
    }

    @Override
    protected FileEntity doExecute(final DownloadTask task, final DownloadContext context) throws IOException {
        HttpGet req = new HttpGet(task.getUrl());
        return httpClient.execute(req, new ResponseHandler<FileEntity>() {
            @Override
            public FileEntity handleResponse(HttpResponse resp) throws IOException {
                StatusLine statusLine = resp.getStatusLine();
                HttpEntity entity = resp.getEntity();
                if (statusLine.getStatusCode() != 200) {
                    throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                }
                if (entity == null) {
                    throw new ClientProtocolException("HttpResponse contains no content");
                }

                FileEntity fileEntity = initFileEntity(context, resp);
                try (OutputStream dest = new FileOutputStream(fileEntity.getPath())) {
                    transfer(entity.getContent(), dest, context);
                }
                return fileEntity;
            }
        });
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Closing HttpClient threw exception: " + e.getMessage(), e);
        }
    }

    private FileEntity initFileEntity(DownloadContext context, HttpResponse resp) {
        DownloadTask task = context.getDownloadTask();
        String path = null;
        String filename = task.getFilename();
        if (filename == null) {
            filename = resolveFilename(task.getUrl(), resp);
            filename = Utils.uniqueFilename(task.getSavePath(), filename);
            path = new File(task.getSavePath(), filename).getAbsolutePath();
        } else {
            filename = resolveDownloadHeader(resp);
            path = new File(task.getSavePath(), task.getFilename()).getAbsolutePath();
        }

        FileEntity fileEntity = new FileEntity(path, filename, resp.getEntity().getContentLength());
        context.setFileEntity(fileEntity);
        return fileEntity;
    }

    private String resolveFilename(URI uri, HttpResponse resp) {
        String fileName = null;
        // 优先考虑下载响应头
        if (resp != null) {
            fileName = resolveDownloadHeader(resp);
        }
        // 其次解析下载 URL
        if (fileName == null || fileName.isEmpty()) {
            fileName = Utils.resolveFilename(uri);
        }
        // 默认生成随机文件名
        if (fileName == null || fileName.isEmpty()) {
            fileName = "download_" + System.currentTimeMillis();
        }

        return fileName;
    }

    private String resolveDownloadHeader(HttpResponse resp) {
        Header header = resp.getFirstHeader(CONTENT_DISPOSITION);
        if (header == null) {
            return null;
        }

        HeaderElement[] elements = header.getElements();
        if (elements == null || elements.length == 0) {
            return null;
        }

        HeaderElement ele = elements[0];
        if (!CONTENT_DISPOSITION_ATTACHMENT.equalsIgnoreCase(ele.getName())
                || !CONTENT_DISPOSITION_INLINE.equalsIgnoreCase(ele.getName())) {
            return null;
        }

        String rawFilename = null;
        String filename = null;
        NameValuePair filenameParam = ele.getParameterByName("filename*");
        if (filenameParam != null && filenameParam.getValue() != null) {
            String[] tokens = filenameParam.getValue().split("'");
            if (tokens.length == 3) {
                rawFilename = tokens[2];
                filename = Utils.decodeURL(rawFilename, tokens[0]);
            }
        }

        if (filename == null) {
            // back compatibility
            filenameParam = ele.getParameterByName("filename");
            if (filenameParam != null && filenameParam.getValue() != null) {
                rawFilename = filenameParam.getValue();
                if (Utils.isAsciiString(rawFilename)) {
                    filename = Utils.decodeURL(rawFilename, "UTF-8");
                } else {
                    filename = new String(rawFilename.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                }
            }
        }

        return filename != null ? filename : rawFilename;
    }

    private SSLContext unsafeSSLContext() {
        SSLContext context = SSLContexts.createDefault();
        try {
            context.init(null, new TrustManager[]{new UnsafeHttps()}, null);
        } catch (KeyManagementException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return context;
    }

    static class UnsafeHttps implements X509TrustManager, HostnameVerifier {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }
}
