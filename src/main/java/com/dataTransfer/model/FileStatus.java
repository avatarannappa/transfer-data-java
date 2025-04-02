package com.dataTransfer.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 单个文件的状态跟踪
 */
public class FileStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fileHash; // 文件哈希值，用于识别文件变化
    private String fileName; // 文件名
    private String productModel; // 产品型号（从文件名提取）
    private long lastProcessedLine; // 最后处理的行号
    private long totalProcessedLines; // 总共处理的行数
    private long failedLines; // 失败的行数
    private LocalDateTime lastProcessedTime; // 最后处理时间
    private LocalDateTime lastModifiedTime; // 文件最后修改时间
    
    public FileStatus() {
        this.lastProcessedLine = 0;
        this.totalProcessedLines = 0;
        this.failedLines = 0;
        this.lastProcessedTime = LocalDateTime.now();
    }
    
    public FileStatus(String fileName) {
        this();
        this.fileName = fileName;
        // 从文件名中提取产品型号（去掉.CSV后缀）
        if (fileName != null && fileName.toUpperCase().endsWith(".CSV")) {
            this.productModel = fileName.substring(0, fileName.length() - 4);
        } else {
            this.productModel = fileName;
        }
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
        // 更新产品型号
        if (fileName != null && fileName.toUpperCase().endsWith(".CSV")) {
            this.productModel = fileName.substring(0, fileName.length() - 4);
        } else {
            this.productModel = fileName;
        }
    }
    
    public String getProductModel() {
        return productModel;
    }
    
    public void setProductModel(String productModel) {
        this.productModel = productModel;
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
    
    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
    
    public void incrementProcessedLine() {
        this.lastProcessedLine++;
        this.totalProcessedLines++;
        this.lastProcessedTime = LocalDateTime.now();
    }
    
    public void incrementFailedLine() {
        this.failedLines++;
    }
} 