package com.dataTransfer.service;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 文件监控服务类，用于监控CSV文件的变化
 */
public class FileMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(FileMonitorService.class);
    
    private String directoryPath;
    private long interval; // 监控间隔，单位：毫秒
    private long intervalSeconds; // 监控间隔，单位：秒
    private FileAlterationMonitor monitor;
    private final FileChangeListener fileChangeListener;
    
    public FileMonitorService(String directoryPath, long intervalSeconds, FileChangeListener fileChangeListener) {
        this.directoryPath = directoryPath;
        this.intervalSeconds = intervalSeconds;
        this.interval = TimeUnit.SECONDS.toMillis(intervalSeconds);
        this.fileChangeListener = fileChangeListener;
    }
    
    /**
     * 启动文件监控
     * @throws Exception 如果启动失败
     */
    public void start() throws Exception {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            directory.mkdirs();
        }
        
        // 初始时检查已存在的CSV文件，并处理最近修改的文件
        checkExistingFiles(directory);
        
        FileAlterationObserver observer = new FileAlterationObserver(directory);
        observer.addListener(new FileAlterationListener() {
            @Override
            public void onStart(FileAlterationObserver observer) {
                // 监控开始
            }
            
            @Override
            public void onDirectoryCreate(File directory) {
                // 目录创建
            }
            
            @Override
            public void onDirectoryChange(File directory) {
                // 目录修改
            }
            
            @Override
            public void onDirectoryDelete(File directory) {
                // 目录删除
            }
            
            @Override
            public void onFileCreate(File file) {
                // 文件创建
                if (file.getName().toLowerCase().endsWith(".csv")) {
                    logger.info("检测到新文件: {}", file.getAbsolutePath());
                    fileChangeListener.onFileCreated(file);
                }
            }
            
            @Override
            public void onFileChange(File file) {
                // 文件修改
                if (file.getName().toLowerCase().endsWith(".csv")) {
                    logger.info("检测到文件变化: {}", file.getAbsolutePath());
                    fileChangeListener.onFileChanged(file);
                }
            }
            
            @Override
            public void onFileDelete(File file) {
                // 文件删除
                if (file.getName().toLowerCase().endsWith(".csv")) {
                    logger.info("检测到文件删除: {}", file.getAbsolutePath());
                    fileChangeListener.onFileDeleted(file);
                }
            }
            
            @Override
            public void onStop(FileAlterationObserver observer) {
                // 监控结束
            }
        });
        
        monitor = new FileAlterationMonitor(interval);
        monitor.addObserver(observer);
        monitor.start();
        
        logger.info("文件监控服务已启动，监控目录：{}，间隔：{}秒", directoryPath, intervalSeconds);
    }
    
    /**
     * 检查目录中已存在的CSV文件，找出最新修改的文件
     * @param directory 要检查的目录
     */
    private void checkExistingFiles(File directory) {
        File[] csvFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });
        
        if (csvFiles != null && csvFiles.length > 0) {
            // 按最后修改时间排序，获取最新的文件
            Optional<File> latestFile = Arrays.stream(csvFiles)
                    .sorted(Comparator.comparing(File::lastModified).reversed())
                    .findFirst();
                    
            latestFile.ifPresent(file -> {
                logger.info("找到最近修改的CSV文件: {}", file.getAbsolutePath());
                fileChangeListener.onFileLatestModified(file);
            });
        }
    }
    
    /**
     * 获取目录中最新修改的CSV文件
     * @return 最新修改的CSV文件，如果没有则返回null
     */
    public File getLatestModifiedFile() {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }
        
        File[] csvFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });
        
        if (csvFiles == null || csvFiles.length == 0) {
            return null;
        }
        
        // 按最后修改时间排序，获取最新的文件
        return Arrays.stream(csvFiles)
                .sorted(Comparator.comparing(File::lastModified).reversed())
                .findFirst()
                .orElse(null);
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
    
    /**
     * 停止文件监控
     * @throws Exception 如果停止失败
     */
    public void stop() throws Exception {
        if (monitor != null) {
            monitor.stop();
            logger.info("文件监控服务已停止");
        }
    }
    
    /**
     * 更新监控路径和间隔
     * @param directoryPath 新的目录路径
     * @param intervalSeconds 新的监控间隔（秒）
     * @throws Exception 如果更新失败
     */
    public void updateConfig(String directoryPath, long intervalSeconds) throws Exception {
        // 如果监控已经启动，则先停止
        if (monitor != null) {
            stop();
        }
        
        this.directoryPath = directoryPath;
        this.intervalSeconds = intervalSeconds;
        this.interval = TimeUnit.SECONDS.toMillis(intervalSeconds);
        
        // 重新启动监控
        start();
    }
    
    /**
     * 文件变化监听器接口
     */
    public interface FileChangeListener {
        void onFileCreated(File file);
        void onFileChanged(File file);
        void onFileDeleted(File file);
        // 新增方法：处理最近修改的文件
        void onFileLatestModified(File file);
    }
} 