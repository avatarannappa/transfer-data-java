package com.dataTransfer.model;

import java.util.LinkedHashMap;

/**
 * 检测记录类，表示CSV文件中的一行数据
 */
public class DetectionRecord {
    private String barcode; // 二维码
    private String date; // 日期
    private String result; // 判定结果
    private LinkedHashMap<String, String> measurements; // 测量数据，key为列名，value为值
    private String productModel; // 产品型号，从文件名获取
    
    public DetectionRecord() {
        this.measurements = new LinkedHashMap<>();
    }
    
    // Getters and Setters
    public String getBarcode() {
        return barcode;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public LinkedHashMap<String, String> getMeasurements() {
        return measurements;
    }
    
    public void setMeasurements(LinkedHashMap<String, String> measurements) {
        this.measurements = measurements;
    }
    
    public String getProductModel() {
        return productModel;
    }
    
    public void setProductModel(String productModel) {
        this.productModel = productModel;
    }
    
    public void addMeasurement(String name, String value) {
        this.measurements.put(name, value);
    }
    
    /**
     * 生成该记录的唯一标识
     * @return UUID字符串
     */
    public String generateUUID() {
        return barcode + "_" + date; // 使用二维码和日期组合作为唯一标识
    }
    
    @Override
    public String toString() {
        return "DetectionRecord{" +
                "barcode='" + barcode + '\'' +
                ", date='" + date + '\'' +
                ", result='" + result + '\'' +
                ", productModel='" + productModel + '\'' +
                ", measurements.size=" + measurements.size() +
                '}';
    }
} 