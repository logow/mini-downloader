package cn.logow.util.download;

import java.io.File;

public class FileEntity {

    private final String path;
    private final String filename;
    private final long fileSize;

    FileEntity(String path, String filename, long fileSize) {
        this.path = path;
        this.filename = filename;
        this.fileSize = fileSize;
    }

    public String getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean delete() {
        return new File(path).delete();
    }
}
