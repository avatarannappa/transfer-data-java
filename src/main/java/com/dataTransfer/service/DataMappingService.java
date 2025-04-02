package com.dataTransfer.service;

import com.dataTransfer.model.DetectionRecord;
import com.dataTransfer.model.EquipmentRuntimeDataRequest;
import com.dataTransfer.model.SerialCodeResultRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;
/**
 * 数据映射服务类，负责将检测记录转换为API请求
 */
public class DataMappingService {
    private static final Logger logger = LoggerFactory.getLogger(DataMappingService.class);

    /**
     * 将检测记录转换为设备运行时数据请求
     * @param record 检测记录
     * @param equipmentCode 设备代码
     * @return 设备运行时数据请求
     */
    public EquipmentRuntimeDataRequest mapToEquipmentRuntimeData(DetectionRecord record, String equipmentCode) {
        EquipmentRuntimeDataRequest request = new EquipmentRuntimeDataRequest();
        request.setEquipmentCode(equipmentCode);
        
        // 添加所有测量数据点位
        for (Map.Entry<String, String> entry : record.getMeasurements().entrySet()) {
            request.addPoint(entry.getKey(), entry.getValue());
        }
        
        logger.debug("映射检测记录到设备运行时数据请求: {}", request);
        return request;
    }
    
    /**
     * 将检测记录转换为终检机结果请求
     * @param record 检测记录
     * @param lineCode 生产线代码
     * @param fileBase64 文件Base64数据（可选）
     * @return 终检机结果请求
     */
    public SerialCodeResultRequest mapToSerialCodeResult(DetectionRecord record, String lineCode, String fileBase64) {
        SerialCodeResultRequest request = new SerialCodeResultRequest();
        request.setLineCode(lineCode);
        request.setSnCode(record.getBarcode());
        request.setResult(record.getResult());
        request.setFileBase64Data(fileBase64);
        
        // 添加所有测量数据点位
        for (Map.Entry<String, String> entry : record.getMeasurements().entrySet()) {
            request.addPoint(entry.getKey(), entry.getValue());
        }
        
        logger.debug("映射检测记录到终检机结果请求: {}", request);
        return request;
    }
    
    /**
     * 从CSV行生成文件Base64数据
     * @param record 检测记录
     * @return Base64编码的字符串
     */
    public String generateFileBase64(DetectionRecord record) {
        // 生成简单的CSV格式字符串
        StringBuilder sb = new StringBuilder();
        sb.append("二维码,日期,结果\n");
        sb.append(record.getBarcode()).append(",")
          .append(record.getDate()).append(",")
          .append(record.getResult()).append("\n");
        
        // 添加所有测量数据
        for (Map.Entry<String, String> entry : record.getMeasurements().entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
        }
        
        // 转换为Base64
        return Base64.getEncoder().encodeToString(sb.toString().getBytes());
    }
} 