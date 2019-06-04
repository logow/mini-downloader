package cn.logow.util.download;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

public final class Utils {

    private static final Object DIRECTORY_LOCK = new Object();

    private Utils() {}

    public static boolean isAsciiString(String s) {
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            if ((ch & 0xFF) >= 0x80) {
                return false;
            }
        }
        return true;
    }

    public static String decodeURL(String s, String charset) {
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), charset);
        } catch (Exception e) {
            return null;
        }
    }

    public static URI resolveURL(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid url: " + url);
        }

        if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
            return uri;
        }
        throw new IllegalArgumentException("invalid url: " + url);
    }

    public static String resolveFilename(String url) {
        return resolveFilename(URI.create(url));
    }

    public static String resolveFilename(URI uri) {
        String baseName = FilenameUtils.getBaseName(uri.getPath());
        if (baseName == null || baseName.isEmpty()) {
            return null;
        }

        String query = uri.getQuery();
        if (query != null && query.length() > 0) {
            baseName = baseName + "_" + System.currentTimeMillis();
        }

        String extension = FilenameUtils.getExtension(uri.getPath());
        if (extension != null && extension.length() > 0) {
            return baseName + "." + extension;
        } else {
            return baseName;
        }
    }

    /**
     * 防止并发创建目录时因竞争而失败的问题
     */
    public static void mkdirs(File dir) throws IOException {
        if (dir.isDirectory()) {
            return;
        }
        synchronized (DIRECTORY_LOCK) {
            if (dir.exists()) {
                return;
            }
            if (!dir.mkdirs() && !dir.exists()) {
                throw new IOException("Could not create directory: " + dir);
            }
        }
    }
}
