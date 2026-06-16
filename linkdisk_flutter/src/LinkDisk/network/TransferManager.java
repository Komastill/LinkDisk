// TransferManager.java
package LinkDisk.network;

import java.util.*;
import java.util.concurrent.*;

public class TransferManager {
    private static TransferManager instance;
    private ExecutorService executor;
    private Queue<TransferTask> waitingQueue;
    private List<TransferTask> activeTasks;
    private List<TransferTask> allTasks;
    private TransferListener listener;
    
    public interface TransferListener {
        void onProgress(TransferTask task, int progress);
        void onStatusChange(TransferTask task);
    }
    
    private TransferManager() {
        executor = Executors.newFixedThreadPool(3);
        waitingQueue = new LinkedList<>();
        activeTasks = new ArrayList<>();
        allTasks = new ArrayList<>();
    }
    
    public static synchronized TransferManager getInstance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }
    
    public void setListener(TransferListener listener) {
        this.listener = listener;
    }
    
    public void addTask(TransferTask task) {
        allTasks.add(task);
        if (activeTasks.size() < 3) {
            startTask(task);
        } else {
            waitingQueue.offer(task);
            task.setStatus("waiting");
            if (listener != null) {
                listener.onStatusChange(task);
            }
        }
    }
    
    private void startTask(TransferTask task) {
        activeTasks.add(task);
        executor.submit(() -> {
            task.setStatus("running");
            if (listener != null) {
                listener.onStatusChange(task);
            }
            
            // 实际传输逻辑会由 TcpClient 调用 updateProgress
            // 这里只是占位
            
            task.setStatus("completed");
            activeTasks.remove(task);
            if (listener != null) {
                listener.onStatusChange(task);
            }
            processNextTask();
        });
    }
    
    public void updateProgress(String taskId, long transferred, long total) {
        for (TransferTask task : allTasks) {
            if (task.getTaskId().equals(taskId)) {
                task.setTransferred(transferred);
                if (listener != null) {
                    int progress = (int) ((transferred * 100) / total);
                    listener.onProgress(task, progress);
                }
                break;
            }
        }
    }
    
    private void processNextTask() {
        if (!waitingQueue.isEmpty() && activeTasks.size() < 3) {
            startTask(waitingQueue.poll());
        }
    }
    
    public void pauseTask(String taskId) {
        // 简化实现，实际需要更复杂的暂停逻辑
        for (TransferTask task : allTasks) {
            if (task.getTaskId().equals(taskId) && "running".equals(task.getStatus())) {
                task.setStatus("paused");
                if (listener != null) {
                    listener.onStatusChange(task);
                }
                break;
            }
        }
    }
    
    public void resumeTask(String taskId) {
        for (TransferTask task : allTasks) {
            if (task.getTaskId().equals(taskId) && "paused".equals(task.getStatus())) {
                task.setStatus("waiting");
                waitingQueue.offer(task);
                if (listener != null) {
                    listener.onStatusChange(task);
                }
                processNextTask();
                break;
            }
        }
    }
    
    public void cancelTask(String taskId) {
        for (TransferTask task : allTasks) {
            if (task.getTaskId().equals(taskId)) {
                task.setStatus("cancelled");
                waitingQueue.remove(task);
                activeTasks.remove(task);
                if (listener != null) {
                    listener.onStatusChange(task);
                }
                processNextTask();
                break;
            }
        }
    }
    
    public List<TransferTask> getAllTasks() {
        return new ArrayList<>(allTasks);
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}