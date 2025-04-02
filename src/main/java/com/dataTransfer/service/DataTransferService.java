package com.dataTransfer.service;

import com.dataTransfer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * 数据传输服务类，负责协调各个组件完成数据传输
 */
public class DataTransferService implements FileMonitorService.FileChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(DataTransferService.class);
    
    private final CsvParserService csvParserService;
    private final HttpClientService httpClientService;
    private final DataMappingService dataMappingService;
    private final PersistenceService persistenceService;
    private final FileMonitorService fileMonitorService;
    
    private Config config;
    private TransferStatus status;
    private ScheduledExecutorService scheduler;
    private boolean isInitialized = false;
    
    /**
     * 构造方法
     */
    public DataTransferService() {
        this.csvParserService = new CsvParserService();
        this.dataMappingService = new DataMappingService();
        this.persistenceService = new PersistenceService();
        
        // 加载配置和状态
        this.config = persistenceService.loadConfig();
        this.status = persistenceService.loadStatus();
        
        // 创建HTTP客户端
        this.httpClientService = new HttpClientService(
                config.getMaxRetries(),
                config.getRetryDelay()
        );
        
        // 创建文件监控服务
        this.fileMonitorService = new FileMonitorService(
                config.getCsvFilePath(),
                config.getMonitorInterval(),
                this
        );
    }
    
    /**
     * 初始化服务
     */
    public void initialize() {
        if (isInitialized) {
            return;
        }
        
        // 启动文件监控
        try {
            if (config.getCsvFilePath() != null && !config.getCsvFilePath().isEmpty()) {
                fileMonitorService.start();
            }
        } catch (Exception e) {
            logger.error("启动文件监控失败: " + e.getMessage(), e);
        }
        
        // 创建定时任务，定期保存状态
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                persistenceService.saveStatus(status);
            } catch (Exception e) {
                logger.error("保存状态失败: " + e.getMessage(), e);
            }
        }, 1, 2, TimeUnit.SECONDS);
        
        // 为监控文件变化添加定时任务
        if (config.isMonitorAllFiles()) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!status.isRunning()) {
                        return;
                    }
                    
                    logger.debug("定期检查文件变化...");
                    // 获取目录中所有的CSV文件
                    File[] allFiles = fileMonitorService.getAllCsvFiles();
                    if (allFiles == null || allFiles.length == 0) {
                        return;
                    }
                    
                    // 检查每个文件是否有变化
                    for (File file : allFiles) {
                        String fileHash = persistenceService.calculateFileHash(file);
                        String absolutePath = file.getAbsolutePath();
                        
                        // 获取文件状态，如果不存在则创建新的
                        FileStatus fileStatus = status.getFileStatus(absolutePath);
                        
                        // 如果文件哈希值发生变化，表示文件有更新
                        if (fileStatus.getFileHash() == null || !fileStatus.getFileHash().equals(fileHash)) {
                            logger.info("检测到文件变化: {}", absolutePath);
                            // 设置新的哈希值
                            fileStatus.setFileHash(fileHash);
                            fileStatus.setLastModifiedTime(LocalDateTime.now());
                            
                            // 更新状态
                            status.updateFileStatus(absolutePath, fileStatus);
                            
                            // 处理文件中的新记录
                            processLatestRecord(file);
                        }
                    }
                } catch (Exception e) {
                    logger.error("检查文件变化失败: " + e.getMessage(), e);
                }
            }, 10, config.getMonitorInterval(), TimeUnit.SECONDS);
        }
        
        isInitialized = true;
        logger.info("数据传输服务已初始化");
    }
    
    /**
     * 开始数据传输
     */
    public void start() {
        if (!isInitialized) {
            initialize();
        }
        
        status.setRunning(true);
        
        // 如果配置为从上次位置继续，则尝试处理已有文件
        if (config.isContinueFromLastPosition()) {
            if (config.isMonitorAllFiles() && status.getLastModifiedFileName() != null) {
                File file = new File(status.getLastModifiedFileName());
                if (file.exists()) {
                    processExistingFile(file);
                }
            } else if (status.getFileName() != null) {
                File file = new File(status.getFileName());
                if (file.exists()) {
                    processExistingFile(file);
                }
            }
        }
        
        persistenceService.saveStatus(status);
        logger.info("数据传输服务已启动");
    }
    
    /**
     * 停止数据传输
     */
    public void stop() {
        status.setRunning(false);
        persistenceService.saveStatus(status);
        logger.info("数据传输服务已停止");
    }
    
    /**
     * 关闭服务，释放资源
     */
    public void shutdown() {
        stop();
        
        try {
            fileMonitorService.stop();
        } catch (Exception e) {
            logger.error("停止文件监控失败: " + e.getMessage(), e);
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        httpClientService.close();
        logger.info("数据传输服务已关闭");
    }
    
    /**
     * 更新配置
     * @param newConfig 新的配置
     */
    public void updateConfig(Config newConfig) {
        this.config = newConfig;
        persistenceService.saveConfig(newConfig);
        
        // 更新文件监控
        try {
            fileMonitorService.updateConfig(
                    newConfig.getCsvFilePath(),
                    newConfig.getMonitorInterval()
            );
        } catch (Exception e) {
            logger.error("更新文件监控配置失败: " + e.getMessage(), e);
        }
        
        logger.info("配置已更新");
    }
    
    /**
     * 重置传输状态
     */
    public void resetStatus() {
        this.status = new TransferStatus();
        persistenceService.saveStatus(status);
        logger.info("传输状态已重置");
    }
    
    /**
     * 获取当前状态
     * @return 传输状态
     */
    public TransferStatus getStatus() {
        return status;
    }
    
    /**
     * 获取当前配置
     * @return 配置
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * 处理文件创建事件
     * @param file 新创建的文件
     */
    @Override
    public void onFileCreated(File file) {
        if (!status.isRunning()) {
            return;
        }
        
        logger.info("处理新创建的文件: {}", file.getAbsolutePath());
        if (config.isMonitorAllFiles()) {
            processNewFileMulti(file);
        } else {
            processNewFile(file);
        }
    }
    
    /**
     * 处理文件变化事件
     * @param file 变化的文件
     */
    @Override
    public void onFileChanged(File file) {
        if (!status.isRunning()) {
            return;
        }
        
        logger.info("处理变化的文件: {}", file.getAbsolutePath());
        
        if (config.isMonitorAllFiles()) {
            processExistingFileMulti(file);
        } else {
            // 检查文件是否是当前正在处理的文件
            if (status.getFileName() != null && status.getFileName().equals(file.getAbsolutePath())) {
                processExistingFile(file);
            } else {
                processNewFile(file);
            }
        }
    }
    
    /**
     * 处理文件删除事件
     * @param file 已删除的文件
     */
    @Override
    public void onFileDeleted(File file) {
        if (config.isMonitorAllFiles()) {
            // 从文件状态映射中移除
            String absolutePath = file.getAbsolutePath();
            if (status.getFileStatusMap().containsKey(absolutePath)) {
                status.getFileStatusMap().remove(absolutePath);
                logger.info("从状态映射中移除已删除的文件: {}", absolutePath);
            }
            
            // 如果是当前最近修改的文件，需要重新选择一个最新的文件
            if (status.getLastModifiedFileName() != null && 
                    status.getLastModifiedFileName().equals(absolutePath)) {
                File latestFile = fileMonitorService.getLatestModifiedFile();
                if (latestFile != null) {
                    status.setLastModifiedFileName(latestFile.getAbsolutePath());
                    logger.info("更新最近修改的文件为: {}", latestFile.getAbsolutePath());
                } else {
                    status.setLastModifiedFileName(null);
                }
            }
        } else {
            if (status.getFileName() != null && status.getFileName().equals(file.getAbsolutePath())) {
                logger.warn("当前处理的文件已被删除: {}", file.getAbsolutePath());
                status.setFileName(null);
                status.setFileHash(null);
            }
        }
        
        persistenceService.saveStatus(status);
    }
    
    /**
     * 处理最新修改的文件
     * @param file 最新修改的文件
     */
    @Override
    public void onFileLatestModified(File file) {
        if (!status.isRunning() || !config.isMonitorAllFiles()) {
            return;
        }
        
        logger.info("处理最新修改的文件: {}", file.getAbsolutePath());
        processLatestModifiedFile(file);
    }
    
    /**
     * 处理最新修改的文件
     * @param file 最新修改的文件
     */
    private void processLatestModifiedFile(File file) {
        String absolutePath = file.getAbsolutePath();
        
        // 更新状态中最近修改的文件
        status.updateLastModifiedFile(absolutePath);
        
        // 检查文件是否已在状态映射中
        if (status.getFileStatusMap().containsKey(absolutePath)) {
            processExistingFileMulti(file);
        } else {
            processNewFileMulti(file);
        }
    }
    
    /**
     * 处理新文件（多文件模式）
     * @param file 新文件
     */
    private void processNewFileMulti(File file) {
        String absolutePath = file.getAbsolutePath();
        
        // 创建新的文件状态
        FileStatus fileStatus = new FileStatus(absolutePath);
        fileStatus.setLastModifiedTime(LocalDateTime.now());
        
        // 计算文件哈希值
        String fileHash = persistenceService.calculateFileHash(file);
        fileStatus.setFileHash(fileHash);
        
        // 提取产品型号
        String productModel = fileMonitorService.extractProductModel(file);
        fileStatus.setProductModel(productModel);
        
        // 更新状态
        status.updateFileStatus(absolutePath, fileStatus);
        status.updateLastModifiedFile(absolutePath);
        
        persistenceService.saveStatus(status);
        
        // 发送最后一条记录
        processLatestRecord(file);
    }
    
    /**
     * 处理已存在的文件（多文件模式）
     * @param file 已存在的文件
     */
    private void processExistingFileMulti(File file) {
        String absolutePath = file.getAbsolutePath();
        
        // 检查文件是否在状态映射中
        if (!status.getFileStatusMap().containsKey(absolutePath)) {
            processNewFileMulti(file);
            return;
        }
        
        FileStatus fileStatus = status.getFileStatus(absolutePath);
        
        // 检查文件是否变化
        String fileHash = persistenceService.calculateFileHash(file);
        
        // 如果文件哈希不一致，说明文件已经变化，需要重新处理
        if (!fileHash.equals(fileStatus.getFileHash())) {
            logger.info("文件已变化，检查是否有新记录: {}", absolutePath);
            fileStatus.setFileHash(fileHash);
            fileStatus.setLastModifiedTime(LocalDateTime.now());
            
            // 更新状态
            status.updateFileStatus(absolutePath, fileStatus);
            status.updateLastModifiedFile(absolutePath);
            
            persistenceService.saveStatus(status);
            
            // 发送最后一条记录
            processLatestRecord(file);
        }
    }
    
    /**
     * 处理新文件
     * @param file 新文件
     */
    private void processNewFile(File file) {
        // 计算文件哈希值
        String fileHash = persistenceService.calculateFileHash(file);
        
        // 更新状态
        status.setFileName(file.getAbsolutePath());
        status.setFileHash(fileHash);
        status.setLastProcessedLine(0);
        persistenceService.saveStatus(status);
        
        // 发送最后一条记录
        processLatestRecord(file);
    }
    
    /**
     * 处理已存在的文件（继续处理）
     * @param file 已存在的文件
     */
    private void processExistingFile(File file) {
        // 检查文件是否变化
        String fileHash = persistenceService.calculateFileHash(file);
        
        // 如果文件哈希不一致，说明文件已经变化，需要重新处理
        if (!fileHash.equals(status.getFileHash())) {
            logger.info("文件已变化，检查是否有新记录");
            status.setFileHash(fileHash);
            
            // 发送最后一条记录
            processLatestRecord(file);
        }
    }
    
    /**
     * 处理最新的记录
     * @param file CSV文件
     */
    private void processLatestRecord(File file) {
        String absolutePath = file.getAbsolutePath();
        FileStatus fileStatus = status.getFileStatus(absolutePath);
        
        // 获取文件的总行数
        long totalLines = csvParserService.getLineCount(file);
        
        // 如果文件行数小于等于上次处理的行数，无需处理
        if (totalLines <= fileStatus.getLastProcessedLine()) {
            logger.debug("文件 {} 没有新增记录，最后处理行: {}, 当前总行数: {}", absolutePath, fileStatus.getLastProcessedLine(), totalLines);
            return;
        }
        
        // 计算新增的行数
        long newLines = totalLines - fileStatus.getLastProcessedLine();
        logger.info("文件 {} 有 {} 行新内容需要处理", absolutePath, newLines);
        
        // 设置当前处理的文件名（用于UI显示和状态跟踪）
        status.setFileName(absolutePath);
        
        // 从上次处理的行开始解析新增的记录
        // 注意：如果文件很大，可能需要批量处理
        List<DetectionRecord> newRecords = csvParserService.parseFile(file, fileStatus.getLastProcessedLine(), (int)Math.min(newLines, 100));
        
        if (newRecords.isEmpty()) {
            logger.warn("文件 {} 没有新的有效记录", absolutePath);
            return;
        }
        
        // 处理所有新增记录
        for (DetectionRecord record : newRecords) {
            processRecord(record, file, fileStatus);
        }
        
        // 更新最后处理的行号
        fileStatus.setLastProcessedLine(totalLines);
        status.updateFileStatus(absolutePath, fileStatus);
        
        // 保存状态
        persistenceService.saveStatus(status);
        
        logger.info("文件 {} 处理完成，当前行数: {}", absolutePath, totalLines);
    }
    
    /**
     * 处理单条记录
     * @param record 检测记录
     * @param file 源文件
     * @param fileStatus 文件状态
     */
    private void processRecord(DetectionRecord record, File file, FileStatus fileStatus) {
        if (record == null) {
            logger.warn("无效的记录，跳过处理");
            return;
        }
        
        // 从文件名中提取产品型号
        String productModel = fileMonitorService.extractProductModel(file);
        record.setProductModel(productModel);
        
        String response = null;
        
        // 根据配置的API类型发送不同的请求
        if (config.isEquipmentApiType()) {
            // 发送设备运行时数据
            EquipmentRuntimeDataRequest runtimeRequest = dataMappingService.mapToEquipmentRuntimeData(
                    record, config.getEquipmentCode());
            response = httpClientService.sendPostRequest(
                    config.getApiBaseUrl() + "/api/DC/SyncEquipmentRuntimeData", runtimeRequest);
            
            logger.info("发送设备运行时数据: {}, 产品型号: {}", record.getBarcode(), productModel);
        } else if (config.isSerialApiType()) {
            // 发送终检机结果
            String fileBase64 = dataMappingService.generateFileBase64(record);
            SerialCodeResultRequest resultRequest = dataMappingService.mapToSerialCodeResult(
                    record, config.getLineCode(), fileBase64);
            response = httpClientService.sendPostRequest(
                    config.getApiBaseUrl() + "/api/UADM/UploadSerialCodeResult", resultRequest);
            
            logger.info("发送终检机结果: {}, 产品型号: {}", record.getBarcode(), productModel);
        } else {
            logger.warn("未知的API类型: {}", config.getApiType());
        }
        
        // 更新文件状态
        fileStatus.incrementProcessedLine();
        fileStatus.setLastProcessedTime(LocalDateTime.now());
        
        if (response == null) {
            fileStatus.incrementFailedLine();
        }
        
        // 更新全局统计
        status.incrementProcessedLine();
        if (response == null) {
            status.incrementFailedLine();
        }
        
        logger.debug("处理记录: {} ({}), 产品型号: {}, 结果: {}", 
                record.getBarcode(), record.getResult(), productModel, 
                response != null ? "成功" : "失败");
    }
} 