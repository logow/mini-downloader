package cn.logow.util.download;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BatchDownload implements Iterable<DownloadTask> {

    private List<DownloadTask> taskList = new LinkedList<>();

    public DownloadTask addTask(String url, File saveAs) throws IOException {
        DownloadTask task = DownloadTask.create(url, saveAs);
        taskList.add(task);
        return task;
    }

    public DownloadTask addTask(String url, String saveTo) throws IOException {
        DownloadTask task = DownloadTask.create(url, saveTo);
        taskList.add(task);
        return task;
    }

    public void addTasks(Collection<String> urls, String saveTo) throws IOException {
        addTasks(urls, saveTo, null);
    }

    public void addTasks(Collection<String> urls, String saveTo, DownloadListener listener) throws IOException {
        for (String url : urls) {
            DownloadTask task = addTask(url, saveTo);
            if (listener != null) {
                task.bind(listener);
            }
        }
    }

    public int size() {
        return taskList.size();
    }

    public void clear() {
        taskList.clear();
    }

    @Override
    public Iterator<DownloadTask> iterator() {
        return taskList.iterator();
    }
}
