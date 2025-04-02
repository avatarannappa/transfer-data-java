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
            return new String(bytes, "GBK");
        } catch (UnsupportedEncodingException e) {
            logger.error("编码转换失败: " + e.getMessage(), e);
            return isoString;
        }
    }
    
    /**
     * 解析CSV文件，返回所有检测记录
     * @param file CSV文件
     * @return 检测记录列表
     */
    public List<DetectionRecord> parseFile(File file) {
        List<DetectionRecord> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), ISO_8859_1))) {
            String[] headerLine = reader.readNext(); // 读取表头
            if (headerLine == null) {
                logger.error("CSV文件为空或格式错误");
                return records;
            }
            
            String[] nextLine;
            // 跳过表头后的空行
            do {
                nextLine = reader.readNext();
            } while (nextLine != null && nextLine.length > 0 && isEmpty(nextLine));
            
            // 处理实际的数据行
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0 && !isEmpty(nextLine)) {
                    DetectionRecord record = parseDataLine(headerLine, nextLine);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            logger.error("解析CSV文件失败: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    /**
     * 解析CSV文件，从指定行开始，最多返回指定数量的记录
     * @param file CSV文件
     * @param startLine 开始行号（从0开始）
     * @param maxRecords 最大记录数量，-1表示不限制
     * @return 检测记录列表
     */
    public List<DetectionRecord> parseFile(File file, long startLine, int maxRecords) {
        List<DetectionRecord> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), ISO_8859_1))) {
            String[] headerLine = reader.readNext(); // 读取表头
            if (headerLine == null) {
                logger.error("CSV文件为空或格式错误");
                return records;
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
                    DetectionRecord record = parseDataLine(headerLine, nextLine);
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
     * 解析CSV文件的最后一条记录
     * @param file CSV文件
     * @return 最后一条检测记录，如果没有找到则返回null
     */
    public DetectionRecord parseLastRecord(File file) {
        DetectionRecord lastRecord = null;
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), ISO_8859_1))) {
            String[] headerLine = reader.readNext(); // 读取表头
            if (headerLine == null) {
                logger.error("CSV文件为空或格式错误");
                return null;
            }
            
            String[] nextLine;
            String[] previousLine = null;
            
            // 读取直到文件末尾
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0 && !isEmpty(nextLine)) {
                    previousLine = nextLine;
                }
            }
            
            // 如果找到了最后一条非空行，解析它
            if (previousLine != null) {
                lastRecord = parseDataLine(headerLine, previousLine);
            }
        } catch (IOException | CsvValidationException e) {
            logger.error("解析CSV文件失败: " + e.getMessage(), e);
        }
        
        return lastRecord;
    }
    
    /**
     * 获取CSV文件的总行数
     * @param file CSV文件
     * @return 总行数
     */
    public long getLineCount(File file) {
        long lineCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), ISO_8859_1))) {
            while (reader.readLine() != null) {
                lineCount++;
            }
        } catch (IOException e) {
            logger.error("读取CSV文件失败: " + e.getMessage(), e);
        }
        
        return lineCount;
    }
    
    /**
     * 解析数据行，生成检测记录对象
     * @param header 表头数组
     * @param data 数据数组
     * @return 检测记录对象
     */
    private DetectionRecord parseDataLine(String[] header, String[] data) {
        if (data.length < 3) {
            logger.warn("数据行格式错误，列数不足");
            return null;
        }
        
        DetectionRecord record = new DetectionRecord();
        
        // 设置基本字段（假设第一列是二维码，第二列是日期，倒数第二列是判定）
        record.setBarcode(convertToUnicode(data[0]));
        record.setDate(convertToUnicode(data[1]));
        
        // 判定结果在倒数第二列
        int resultIndex = Math.max(0, data.length - 2);
        record.setResult(convertToUnicode(data[resultIndex]));
        
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