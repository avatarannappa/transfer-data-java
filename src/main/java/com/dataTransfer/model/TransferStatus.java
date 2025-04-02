package com.dataTransfer.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据传输状态类，用于记录传输进度和状态
 */
public class TransferStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fileHash; // 文件哈希值，用于识别文件变化
    private String fileName; // 当前处理的文件名
    private long lastProcessedLine; // 最后处理的行号
    private long totalProcessedLines; // 总共处理的行数
    private long failedLines; // 失败的行数
    private LocalDateTime lastProcessedTime; // 最后处理时间
    private boolean isRunning; // 是否正在运行
    
    // 存储多个文件的状态
    private Map<String, FileStatus> fileStatusMap = new HashMap<>();
    private String lastModifiedFileName; // 最近修改的文件名
    
    public TransferStatus() {
        this.lastProcessedLine = 0;
        this.totalProcessedLines = 0;
        this.failedLines = 0;
        this.lastProcessedTime = LocalDateTime.now();
        this.isRunning = false;
    }
    
    // Getters and Setters
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public long getLastProcessedLine() {
        return lastProcessedLine;
    }
    
    public void setLastProcessedLine(long lastProcessedLine) {
        this.lastProcessedLine = lastProcessedLine;
    }
    
    public long getTotalProcessedLines() {
        return totalProcessedLines;
    }
    
    public void setTotalProcessedLines(long totalProcessedLines) {
        this.totalProcessedLines = totalProcessedLines;
    }
    
    public long getFailedLines() {
        return failedLines;
    }
    
    public void setFailedLines(long failedLines) {
        this.failedLines = failedLines;
    }
    
    public LocalDateTime getLastProcessedTime() {
        return lastProcessedTime;
    }
    
    public void setLastProcessedTime(LocalDateTime lastProcessedTime) {
        this.lastProcessedTime = lastProcessedTime;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setRunning(boolean running) {
        isRunning = running;
    }
    
    public Map<String, FileStatus> getFileStatusMap() {
        return fileStatusMap;
    }
    
    public void setFileStatusMap(Map<String, FileStatus> fileStatusMap) {
        this.fileStatusMap = fileStatusMap;
    }
    
    public String getLastModifiedFileName() {
        return lastModifiedFileName;
    }
    
    public void setLastModifiedFileName(String lastModifiedFileName) {
        this.lastModifiedFileName = lastModifiedFileName;
    }
    
    /**
     * 获取指定文件的状态，如果不存在则创建
     * @param fileName 文件名
     * @return 文件状态对象
     */
    public FileStatus getFileStatus(String fileName) {
        if (!fileStatusMap.containsKey(fileName)) {
            fileStatusMap.put(fileName, new FileStatus(fileName));
        }
        return fileStatusMap.get(fileName);
    }
    
    /**
     * 更新指定文件的状态
     * @param fileName 文件名
     * @param fileStatus 文件状态
     */
    public void updateFileStatus(String fileName, FileStatus fileStatus) {
        fileStatusMap.put(fileName, fileStatus);
    }
    
    /**
     * 更新最近修改的文件
     * @param fileName 文件名
     */
    public void updateLastModifiedFile(String fileName) {
        this.lastModifiedFileName = fileName;
        // 同时更新当前活动文件
        this.fileName = fileName;
        
        // 更新全局计数器（可选，取决于UI是否需要显示全局统计）
        long totalLines = 0;
        long totalFailedLines = 0;
        LocalDateTime latestTime = null;
        
        for (FileStatus status : fileStatusMap.values()) {
            totalLines += status.getTotalProcessedLines();
            totalFailedLines += status.getFailedLines();
            
            if (latestTime == null || (status.getLastProcessedTime() != null && 
                    status.getLastProcessedTime().isAfter(latestTime))) {
                latestTime = status.getLastProcessedTime();
            }
        }
        
        this.totalProcessedLines = totalLines;
        this.failedLines = totalFailedLines;
        if (latestTime != null) {
            this.lastProcessedTime = latestTime;
        }
    }
    
    public void incrementProcessedLine() {
        this.lastProcessedLine++;
        this.totalProcessedLines++;
        this.lastProcessedTime = LocalDateTime.now();
        
        // 如果存在当前文件状态，同时更新它
        if (fileName != null && fileStatusMap.containsKey(fileName)) {
            fileStatusMap.get(fileName).incrementProcessedLine();
        }
    }
    
    public void incrementFailedLine() {
        this.failedLines++;
        
        // 如果存在当前文件状态，同时更新它
        if (fileName != null && fileStatusMap.containsKey(fileName)) {
            fileStatusMap.get(fileName).incrementFailedLine();
        }
    }
} 