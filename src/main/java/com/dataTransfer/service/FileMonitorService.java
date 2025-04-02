package com.dataTransfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;

/**
 * 文件监控服务类，用于监控CSV文件的变化
 */
public class FileMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(FileMonitorService.class);
    
    private String directoryPath;
    
    public FileMonitorService(String directoryPath) {
        this.directoryPath = directoryPath;
    }
    
    
    /**
     * 获取目录中所有的CSV文件
     * @return CSV文件数组
     */
    public File[] getAllCsvFiles() {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return new File[0];
        }
        
        File[] csvFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });
        
        return csvFiles != null ? csvFiles : new File[0];
    }
    
    /**
     * 从文件路径中提取产品型号（文件名去掉.csv后缀）
     * @param file CSV文件
     * @return 产品型号
     */
    public String extractProductModel(File file) {
        String fileName = file.getName();
        if (fileName.toLowerCase().endsWith(".csv")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }
} 