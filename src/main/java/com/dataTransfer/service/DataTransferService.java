package com.dataTransfer.service;

import com.dataTransfer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * 数据传输服务类，负责协调各个组件完成数据传输
 */
public class DataTransferService {
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
    
    // 错误回调接口用于通知UI层
    private ErrorCallback errorCallback;
    
    /**
     * 错误回调接口，用于通知UI层出现错误
     */
    public interface ErrorCallback {
        /**
         * 当HTTP请求失败时调用
         * @param errorMessage 错误消息
         * @param record 失败的记录
         */
        void onHttpRequestFailed(String errorMessage, DetectionRecord record);
    }
    
    /**
     * 设置错误回调
     * @param errorCallback 错误回调接口
     */
    public void setErrorCallback(ErrorCallback errorCallback) {
        this.errorCallback = errorCallback;
    }
    
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

                
        // 创建文件管理
        this.fileMonitorService = new FileMonitorService(config.getCsvFilePath());

    }
    
    /**
     * 初始化服务
     */
    public void initialize() {
        if (isInitialized) {
            return;
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
                    // 支持启动后改目录
                    fileMonitorService.setDirectoryPath(config.getCsvFilePath());
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
                            if (!status.isRunning()) {
                                return;
                            }
                            logger.info("检测到文件变化: {}", absolutePath);
                            // 处理文件中的新记录
                            boolean allSuccess = processLatestRecord(file);
                            if (!allSuccess) {
                                logger.warn("文件处理失败: {}", absolutePath);
                                continue;
                            }
                            // 设置新的哈希值
                            fileStatus.setFileHash(fileHash);
                            // 设置文件修改时间
                            Instant instant = Instant.ofEpochMilli(file.lastModified());
                            LocalDateTime fileModifyTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
                            fileStatus.setLastModifiedTime(fileModifyTime);
                            
                            // 更新状态
                            status.updateFileStatus(absolutePath, fileStatus);
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
     * 处理最新的记录
     * @param file CSV文件
     */
    private boolean processLatestRecord(File file) {
        String absolutePath = file.getAbsolutePath();
        FileStatus fileStatus = status.getFileStatus(absolutePath);
        
        // 获取文件的总行数
        long totalLines = csvParserService.getLineCount(file);
        
        // 如果文件行数小于等于上次处理的行数，无需处理
        if (totalLines <= fileStatus.getLastProcessedLine()) {
            logger.debug("文件 {} 没有新增记录，最后处理行: {}, 当前总行数: {}", absolutePath, fileStatus.getLastProcessedLine(), totalLines);
            return true;
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
            return true;
        }
        
        // 处理所有新增记录
        boolean hasHttpError = false;
        for (DetectionRecord record : newRecords) {
            boolean success = processRecord(record, file, fileStatus);
            if (!success) {
                hasHttpError = true;
                break; // HTTP请求失败，停止处理后续记录
            }
        }

        if (hasHttpError) {
            // 没有完全处理
            return false;
        }
        
        // 获取最后一个非空数据行的行号
        long lastNonEmptyLine = csvParserService.getLastNonEmptyLineNumber(file);
        fileStatus.setLastProcessedLine(lastNonEmptyLine);
        fileStatus.setTotalProcessedLines(totalLines);
        
        status.updateFileStatus(absolutePath, fileStatus);
        
        // 保存状态
        persistenceService.saveStatus(status);
        
        logger.info("文件 {} 处理完成，当前行数: {}, 最后非空数据行: {}", absolutePath, totalLines, lastNonEmptyLine);
        return true;
    }
    
    /**
     * 处理单条记录
     * @param record 检测记录
     * @param file 源文件
     * @param fileStatus 文件状态
     * @return 处理是否成功
     */
    private boolean processRecord(DetectionRecord record, File file, FileStatus fileStatus) {
        if (record == null) {
            logger.warn("无效的记录，跳过处理");
            return true; // 跳过无效记录，不算失败
        }
        
        // 从文件名中提取产品型号
        String productModel = fileMonitorService.extractProductModel(file);
        record.setProductModel(productModel);
        
        String response = null;
        String apiType = "未知";
        
        try {
            // 根据配置的API类型发送不同的请求
            if (config.isEquipmentApiType()) {
                // 发送设备运行时数据
                apiType = "设备运行时数据";
                EquipmentRuntimeDataRequest runtimeRequest = dataMappingService.mapToEquipmentRuntimeData(
                        record, config.getEquipmentCode());
                response = httpClientService.sendPostRequest(
                        config.getApiBaseUrl() + "/api/DC/SyncEquipmentRuntimeData", runtimeRequest);
                
                logger.info("发送设备运行时数据: {}, 产品型号: {}", record.getBarcode(), productModel);
            } else if (config.isSerialApiType()) {
                // 发送终检机结果
                apiType = "终检机结果";
                String fileBase64 = dataMappingService.generateFileBase64(record);
                SerialCodeResultRequest resultRequest = dataMappingService.mapToSerialCodeResult(
                        record, config.getLineCode(), fileBase64);
                response = httpClientService.sendPostRequest(
                        config.getApiBaseUrl() + "/api/UADM/UploadSerialCodeResult", resultRequest);
                
                logger.info("发送终检机结果: {}, 产品型号: {}", record.getBarcode(), productModel);
            } else {
                logger.warn("未知的API类型: {}", config.getApiType());
                if (errorCallback != null) {
                    errorCallback.onHttpRequestFailed("未知的API类型: " + config.getApiType(), record);
                }
                return false;
            }
        } catch (Exception e) {
            logger.error("发送请求时发生异常: " + e.getMessage(), e);
            if (errorCallback != null) {
                errorCallback.onHttpRequestFailed("发送请求时发生异常: " + e.getMessage(), record);
            }
            return false;
        }
        
        if (response == null) {
            fileStatus.incrementFailedLine();
            
            // 通知UI层HTTP请求失败
            String errorMsg = "发送" + apiType + "失败，条码: " + record.getBarcode() + 
                              "，产品型号: " + productModel + 
                              "，请检查网络连接和API服务器状态";
            logger.error(errorMsg);
            
            if (errorCallback != null) {
                errorCallback.onHttpRequestFailed(errorMsg, record);
            }
            
            return false;
        }
        
        // 更新文件状态
        // fileStatus.incrementProcessedLine();
        // fileStatus.setLastProcessedTime(LocalDateTime.now());
        // 更新全局统计
        status.incrementProcessedLine();
        
        logger.debug("处理记录: {} ({}), 产品型号: {}, 结果: 成功", 
                record.getBarcode(), record.getResult(), productModel);
        
        return true;
    }
} 