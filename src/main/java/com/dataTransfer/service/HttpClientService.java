package com.dataTransfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端服务类，负责发送HTTP请求
 */
public class HttpClientService {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
    private static final int CONNECTION_TIMEOUT = 10000; // 连接超时时间（毫秒）
    private static final int SOCKET_TIMEOUT = 30000; // 数据传输超时时间（毫秒）
    
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final int maxRetries;
    private final int retryDelay;
    
    /**
     * 构造方法
     * @param maxRetries 最大重试次数
     * @param retryDelay 重试延迟（秒）
     */
    public HttpClientService(int maxRetries, int retryDelay) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        
        // 创建并配置ObjectMapper
        this.objectMapper = new ObjectMapper();
        // 添加对Java 8日期时间类型的支持
        this.objectMapper.registerModule(new JavaTimeModule());
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("HTTP客户端服务已初始化，最大重试次数: {}, 重试延迟: {}秒", maxRetries, retryDelay);
    }
    
    /**
     * 发送HTTP POST请求，带重试机制
     * @param url 请求URL
     * @param requestBody 请求体对象
     * @return 响应字符串，如果请求失败则返回null
     */
    public String sendPostRequest(String url, Object requestBody) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return sendPostRequestInternal(url, requestBody);
            } catch (Exception e) {
                attempts++;
                logger.error("HTTP请求失败 (尝试 {}/{}): {}", attempts, maxRetries, e.getMessage());
                
                if (attempts < maxRetries) {
                    // 等待一段时间后重试
                    try {
                        TimeUnit.SECONDS.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("HTTP请求失败，已达到最大重试次数: {}", maxRetries);
        return null;
    }
    
    /**
     * 内部发送HTTP POST请求方法
     * @param url 请求URL
     * @param requestBody 请求体对象
     * @return 响应字符串
     * @throws IOException 如果发送请求失败
     */
    private String sendPostRequestInternal(String url, Object requestBody) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        
        // 将请求体对象转换为JSON字符串
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        // 明确使用UTF-8编码创建StringEntity
        StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset("UTF-8"));
        httpPost.setEntity(entity);
        
        // 添加请求头，明确指定UTF-8编码
        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpPost.setHeader("Accept", "application/json; charset=UTF-8");
        
        logger.debug("发送HTTP请求 URL: {}, Body: {}", url, jsonBody);
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity, "UTF-8") : null;
            
            logger.debug("收到HTTP响应 StatusCode: {}, Body: {}", statusCode, responseBody);
            
            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                throw new IOException("HTTP请求失败，状态码: " + statusCode + ", 响应: " + responseBody);
            }
        }
    }
    
    /**
     * 关闭HTTP客户端资源
     */
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("关闭HTTP客户端失败: " + e.getMessage(), e);
        }
    }
} 