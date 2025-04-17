package com.dataTransfer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备运行时数据请求模型类
 * 对应 /api/DC/SyncEquipmentRuntimeData 接口
 */
public class EquipmentRuntimeDataRequest {
    private String id;
    private String equipmentCode;
    private List<Point> points;
    
    public EquipmentRuntimeDataRequest() {
        this.points = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public EquipmentRuntimeDataRequest(String equipmentCode) {
        this.equipmentCode = equipmentCode;
        this.points = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getEquipmentCode() {
        return equipmentCode;
    }
    
    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }
    
    public List<Point> getPoints() {
        return points;
    }
    
    public void setPoints(List<Point> points) {
        this.points = points;
    }
    
    public void addPoint(String pointName, String pointValue) {
        this.points.add(new Point(pointName, pointValue));
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