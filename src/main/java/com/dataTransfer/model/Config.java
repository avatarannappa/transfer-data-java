package com.dataTransfer.model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 应用程序配置类
 */
public class Config implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // API类型常量
    public static final String API_TYPE_EQUIPMENT = "equipment";  // 设备运行时数据API
    public static final String API_TYPE_SERIAL = "serial";        // 终检机结果API
    
    private String apiBaseUrl = "http://aaabbb.com"; // API服务器地址
    private String equipmentCode; // 设备代码
    private String lineCode; // 生产线代码
    private String csvFilePath; // CSV文件路径
    private int monitorInterval = 3; // 监控间隔(秒)
    private int maxRetries = 3; // 最大重试次数
    private int retryDelay = 5; // 重试延迟(秒)
    private boolean continueFromLastPosition = true; // 是否从上次位置继续
    private boolean minimizeToTray = false; // 是否最小化到系统托盘
    @JsonAlias({"equipmentApiType"})
    private String apiType = API_TYPE_EQUIPMENT; // API类型，默认为设备运行时数据API
    private boolean monitorAllFiles = true; // 是否监控文件夹中的所有文件
    
    public Config() {
    }
    
    // Getters and Setters
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
    
    public String getEquipmentCode() {
        return equipmentCode;
    }
    
    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }
    
    public String getLineCode() {
        return lineCode;
    }
    
    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }
    
    public String getCsvFilePath() {
        return csvFilePath;
    }
    
    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }
    
    public int getMonitorInterval() {
        return monitorInterval;
    }
    
    public void setMonitorInterval(int monitorInterval) {
        this.monitorInterval = monitorInterval;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public int getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    public boolean isContinueFromLastPosition() {
        return continueFromLastPosition;
    }
    
    public void setContinueFromLastPosition(boolean continueFromLastPosition) {
        this.continueFromLastPosition = continueFromLastPosition;
    }
    
    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }
    
    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }
    
    public String getApiType() {
        return apiType;
    }
    
    public void setApiType(String apiType) {
        this.apiType = apiType;
    }
    
    public boolean isMonitorAllFiles() {
        return monitorAllFiles;
    }
    
    public void setMonitorAllFiles(boolean monitorAllFiles) {
        this.monitorAllFiles = monitorAllFiles;
    }
    
    /**
     * 检查是否使用设备运行时数据API
     * @return 如果使用设备运行时数据API返回true
     */
    public boolean isEquipmentApiType() {
        return API_TYPE_EQUIPMENT.equals(apiType);
    }
    
    /**
     * 检查是否使用终检机结果API
     * @return 如果使用终检机结果API返回true
     */
    public boolean isSerialApiType() {
        return API_TYPE_SERIAL.equals(apiType);
    }
} 