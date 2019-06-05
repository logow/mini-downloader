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
        return resolveFilename(resolveURL(url));
    }

    public static String resolveFilename(URI uri) {
        String path = uri.getPath();
        String baseName = getBaseName(path);
        if (baseName == null) {
            return null;
        }

        String extension = getExtension(path);
        if (extension == null) {
            return baseName;
        }

        return baseName + "." + extension;
    }

    public static String uniqueFilename(String dirPath, String filename) {
        String fn = filename;
        File file =  new File(dirPath, fn);
        if (file.exists()) {
            String baseName = getBaseName(filename);
            String extension = getExtension(filename);
            int num = 1;
            do {
                fn = baseName + "_" + (++num) + (extension != null ? "." + extension : "");
                file = new File(dirPath, fn);
            } while (file.exists());
        }
        return fn;
    }

    public static String getBaseName(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static String getExtension(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        return extension != null && extension.isEmpty() ? null : extension;
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
