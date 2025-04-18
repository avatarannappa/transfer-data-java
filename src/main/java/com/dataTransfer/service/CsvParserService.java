package com.dataTransfer.service;

import com.dataTransfer.model.DetectionRecord;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CSV文件解析服务类
 */
public class CsvParserService {
    private static final Logger logger = LoggerFactory.getLogger(CsvParserService.class);
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    
    /**
     * 将ISO-8859-1编码的字符串转换为Unicode
     * @param isoString ISO-8859-1编码的字符串
     * @return Unicode编码的字符串
     */
    private String convertToUnicode(String isoString) {
        if (isoString == null) {
            return null;
        }
        try {
            // 先将字符串转换为ISO-8859-1编码的字节数组
            byte[] bytes = isoString.getBytes(ISO_8859_1);
            // 使用GBK解码（因为CSV文件可能是用GBK编码保存的）
            String str = new String(bytes, "GBK");
            if (str.startsWith("◆")) {
                str = str.substring(1);
            }
            return str;
        } catch (UnsupportedEncodingException e) {
            logger.error("编码转换失败: " + e.getMessage(), e);
            return isoString;
        }
    }
    
    /**
     * 解析CSV文件，从指定行开始，最多返回指定数量的记录
     * @param file CSV文件
     * @param startLine 开始行号（从0开始）
     * @param maxRecords 最大记录数量，-1表示不限制
     * @return 检测记录列表
     */
    public List<DetectionRecord> parseFile(File file, long startLine, long maxRecords) {
        List<DetectionRecord> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), ISO_8859_1))) {
            String[] headerLine = reader.readNext(); // 读取表头
            if (headerLine == null) {
                logger.error("CSV文件为空或格式错误");
                return records;
            }

            int panDingColumn = 0;
            for (panDingColumn = 0; panDingColumn < headerLine.length; panDingColumn++) {
                if ("判定".equals(convertToUnicode(headerLine[panDingColumn]))) {
                    break;
                }
            }
            
            String[] nextLine;
            long currentLine = 1; // 已经读取了表头，从1开始
            
            // 跳过行，直到达到startLine
            while (currentLine < startLine && (nextLine = reader.readNext()) != null) {
                currentLine++;
            }
            
            // 读取记录，直到达到maxRecords或文件结束
            int recordCount = 0;
            while ((maxRecords == -1 || recordCount < maxRecords) && (nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0 && !isEmpty(nextLine)) {
                    DetectionRecord record = parseDataLine(headerLine, nextLine, panDingColumn, currentLine);
                    if (record != null) {
                        records.add(record);
                        recordCount++;
                    }
                }
                currentLine++;
            }
        } catch (IOException | CsvValidationException e) {
            logger.error("解析CSV文件失败: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    /**
     * 获取CSV文件中最后一个非空数据行的行号
     * @param file CSV文件
     * @return [最后一个非空数据行的行号，总行号]
     */
    public long[] getLineCount(File file) {
        long lastNonEmptyLine = 0;
        long currentLine = 0;
        long[] result = new long[2];
        result[0] = lastNonEmptyLine;
        result[1] = currentLine;
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), ISO_8859_1))) {
            String[] headerLine = reader.readNext(); // 读取表头
            currentLine = 1; // 已经读取了表头，从1开始
            
            if (headerLine == null) {
                logger.error("CSV文件为空或格式错误");
                return result;
            }
            
            String[] nextLine;
            
            // 读取直到文件末尾
            while ((nextLine = reader.readNext()) != null) {
                currentLine++;
                
                if (nextLine.length > 0 && !isEmpty(nextLine)) {
                    lastNonEmptyLine = currentLine;
                }
            }
        } catch (IOException | CsvValidationException e) {
            logger.error("解析CSV文件失败: " + e.getMessage(), e);
        }
        
        result[0] = lastNonEmptyLine;
        result[1] = currentLine;
        return result;
    }
    
    /**
     * 解析数据行，生成检测记录对象
     * @param header 表头数组
     * @param data 数据数组
     * @param panDingIndex “判定”列的index
     * @param rowId 列id
     * @return 检测记录对象
     */
    private DetectionRecord parseDataLine(String[] header, String[] data, int panDingIndex, long rowId) {
        if (data.length < 3) {
            logger.warn("数据行格式错误，列数不足");
            return null;
        }
        
        
        DetectionRecord record = new DetectionRecord();
        
        // 设置基本字段（假设第一列是二维码，第二列是日期，倒数第二列是判定）
        record.setId(UUID.randomUUID().toString());
        record.setBarcode(convertToUnicode(data[0]));
        record.setDate(convertToUnicode(data[1]));
        
        // 判定结果在倒数第二列
        record.setResult(convertToUnicode(data[panDingIndex]));
        if (!"NG".equals(record.getResult()) && !"OK".equals(record.getResult())) {
            logger.error("error result:", record.getResult());
        }
        
        // 遍历所有其他列，包括倒数第二列，添加到测量数据中
        for (int i = 2; i < data.length - 1; i++) {
            if (i < header.length && data[i] != null && !data[i].isEmpty()) {
                record.addMeasurement(convertToUnicode(header[i]), convertToUnicode(data[i]));
            }
        }
        
        return record;
    }
    
    /**
     * 检查数组是否为空（所有元素都为空字符串）
     * @param line 要检查的数组
     * @return 如果所有元素都为空，则返回true
     */
    private boolean isEmpty(String[] line) {
        for (String s : line) {
            if (s != null && !s.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
} 