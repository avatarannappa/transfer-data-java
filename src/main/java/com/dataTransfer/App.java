package com.dataTransfer;

import com.dataTransfer.service.DataTransferService;
import com.dataTransfer.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * 应用程序主类
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    /**
     * 应用程序入口点
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置Swing外观为系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("无法设置系统外观: " + e.getMessage());
        }
        
        // 在事件调度线程中启动UI
        EventQueue.invokeLater(() -> {
            try {
                // 创建数据传输服务
                DataTransferService dataTransferService = new DataTransferService();
                
                // 创建并显示主窗口
                MainFrame mainFrame = new MainFrame(dataTransferService);
                mainFrame.setVisible(true);
                
                logger.info("应用程序已启动");
            } catch (Exception e) {
                logger.error("应用程序启动失败: " + e.getMessage(), e);
                JOptionPane.showMessageDialog(
                        null,
                        "应用程序启动失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
} 