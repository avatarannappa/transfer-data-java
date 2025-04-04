package com.dataTransfer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备运行时数据请求模型类
 * 对应 /api/DC/SyncEquipmentRuntimeData 接口
 */
public class EquipmentRuntimeDataRequest {
    private String equipmentCode;
    private List<Point> points;
    
    public EquipmentRuntimeDataRequest() {
        this.points = new ArrayList<>();
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
        private String pointName;
        private String pointValue;
        
        public Point() {
        }
        
        public Point(String pointName, String pointValue) {
            this.pointName = pointName;
            this.pointValue = pointValue;
        }
        
        // Getters and Setters
        public String getPointName() {
            return pointName;
        }
        
        public void setPointName(String pointName) {
            this.pointName = pointName;
        }
        
        public String getPointValue() {
            return pointValue;
        }
        
        public void setPointValue(String pointValue) {
            this.pointValue = pointValue;
        }
    }
} 