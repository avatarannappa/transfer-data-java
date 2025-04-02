package com.dataTransfer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 终检机结果请求模型类
 * 对应 /api/UADM/UploadSerialCodeResult 接口
 */
public class SerialCodeResultRequest {
    private String lineCode;
    private String snCode;
    private String result;
    private String fileBase64Data;
    private List<Point> points;
    
    public SerialCodeResultRequest() {
        this.points = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getLineCode() {
        return lineCode;
    }
    
    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }
    
    public String getSnCode() {
        return snCode;
    }
    
    public void setSnCode(String snCode) {
        this.snCode = snCode;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getFileBase64Data() {
        return fileBase64Data;
    }
    
    public void setFileBase64Data(String fileBase64Data) {
        this.fileBase64Data = fileBase64Data;
    }
    
    public List<Point> getPoints() {
        return points;
    }
    
    public void setPoints(List<Point> points) {
        this.points = points;
    }
    
    public void addPoint(String code, String text) {
        this.points.add(new Point(code, text));
    }
    
    /**
     * 点位数据内部类
     */
    public static class Point {
        private String code;
        private String text;
        
        public Point() {
        }
        
        public Point(String code, String text) {
            this.code = code;
            this.text = text;
        }
        
        // Getters and Setters
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
} 