package com.dataTransfer.service;

import com.dataTransfer.model.Config;
import com.dataTransfer.model.TransferStatus;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 持久化服务类，负责配置和状态的保存和加载
 */
public class PersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);
    private static final String CONFIG_FILE = "config.json";
    private static final String STATUS_FILE = "status.json";
    private static final String DATA_DIR = "data";
    
    private final ObjectMapper objectMapper;
    
    public PersistenceService() {
        this.objectMapper = new ObjectMapper();
        // 注册JavaTimeModule以支持Java 8日期/时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 配置忽略未知属性，提高向后兼容性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        createDataDirectory();
    }
    
    /**
     * 保存配置
     * @param config 配置对象
     */
    public void saveConfig(Config config) {
        try {
            File file = new File(DATA_DIR, CONFIG_FILE);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
            logger.info("配置已保存到 {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("保存配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载配置
     * @return 配置对象，如果加载失败则返回新的配置
     */
    public Config loadConfig() {
        File file = new File(DATA_DIR, CONFIG_FILE);
        if (file.exists()) {
            try {
                Config config = objectMapper.readValue(file, Config.class);
                logger.info("配置已从 {} 加载", file.getAbsolutePath());
                return config;
            } catch (IOException e) {
                logger.error("加载配置失败: " + e.getMessage(), e);
            }
        }
        
        // 如果加载失败或文件不存在，返回默认配置
        logger.info("创建新的默认配置");
        return new Config();
    }
    
    /**
     * 保存传输状态
     * @param status 传输状态对象
     */
    public void saveStatus(TransferStatus status) {
        try {
            File file = new File(DATA_DIR, STATUS_FILE);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, status);
            logger.debug("状态已保存到 {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("保存状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载传输状态
     * @return 传输状态对象，如果加载失败则返回新的状态
     */
    public TransferStatus loadStatus() {
        File file = new File(DATA_DIR, STATUS_FILE);
        if (file.exists()) {
            try {
                TransferStatus status = objectMapper.readValue(file, TransferStatus.class);
                logger.info("状态已从 {} 加载", file.getAbsolutePath());
                return status;
            } catch (IOException e) {
                logger.error("加载状态失败: " + e.getMessage(), e);
            }
        }
        
        // 如果加载失败或文件不存在，返回新的状态
        logger.info("创建新的传输状态");
        return new TransferStatus();
    }
    
    /**
     * 计算文件的MD5哈希值
     * @param file 文件
     * @return MD5哈希值的十六进制字符串
     */
    public String calculateFileHash(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    md.update(buffer, 0, read);
                }
            }
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("计算文件哈希值失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 创建数据目录
     */
    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            logger.error("创建数据目录失败: " + e.getMessage(), e);
        }
    }
} 