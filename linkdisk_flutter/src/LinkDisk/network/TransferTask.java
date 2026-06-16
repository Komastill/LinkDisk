// TransferTask.java
package LinkDisk.network;

import java.io.Serializable;
import java.util.UUID;

public class TransferTask implements Serializable {
    private String taskId;
    private String targetIp;
    private String fileName;
    private long totalSize;
    private long transferred;
    private String status; // waiting, running, paused, completed, failed, cancelled
    private String type; // upload, download
    private long createTime;
    
    public TransferTask() {
        this.taskId = UUID.randomUUID().toString();
        this.createTime = System.currentTimeMillis();
        this.transferred = 0;
    }
    
    public TransferTask(String targetIp, String fileName, long totalSize, String type) {
        this();
        this.targetIp = targetIp;
        this.fileName = fileName;
        this.totalSize = totalSize;
        this.type = type;
    }
    
    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getTargetIp() { return targetIp; }
    public void setTargetIp(String targetIp) { this.targetIp = targetIp; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    
    public long getTransferred() { return transferred; }
    public void setTransferred(long transferred) { this.transferred = transferred; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public long getCreateTime() { return createTime; }
    
    public int getProgress() {
        if (totalSize == 0) return 0;
        return (int) ((transferred * 100) / totalSize);
    }
}