package cn.logow.util.download;

public class DownloadContext {

    private DownloadTask task;
    private FileEntity entity;

    DownloadContext(DownloadTask task) {
        this.task = task;
    }

    public DownloadTask getDownloadTask() {
        return task;
    }

    public void setFileEntity(FileEntity entity) {
        this.entity = entity;
    }

    public FileEntity getFileEntity() {
        return entity;
    }
}
