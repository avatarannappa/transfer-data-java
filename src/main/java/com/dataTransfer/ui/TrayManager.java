package com.dataTransfer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * 系统托盘管理类
 */
public class TrayManager {
    private static final Logger logger = LoggerFactory.getLogger(TrayManager.class);
    
    private SystemTray tray;
    private TrayIcon trayIcon;
    private final JFrame mainFrame;
    private final Runnable exitAction;
    
    public TrayManager(JFrame mainFrame, Runnable exitAction) {
        this.mainFrame = mainFrame;
        this.exitAction = exitAction;
        initialize();
    }
    
    /**
     * 初始化系统托盘
     */
    private void initialize() {
        if (!SystemTray.isSupported()) {
            logger.warn("系统不支持托盘功能");
            return;
        }
        
        tray = SystemTray.getSystemTray();
        
        // 加载图标
        Image iconImage = createDefaultIcon();
        
        // 创建弹出菜单
        PopupMenu popup = createPopupMenu();
        
        // 创建托盘图标
        trayIcon = new TrayIcon(iconImage, "数据传输应用", popup);
        trayIcon.setImageAutoSize(true);
        
        // 添加双击事件：显示主窗口
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showMainFrame();
                }
            }
        });
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            logger.error("无法将图标添加到系统托盘: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建弹出菜单
     * @return 弹出菜单
     */
    private PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        // 打开主窗口菜单项
        MenuItem openItem = new MenuItem("打开主窗口");
        openItem.addActionListener(e -> showMainFrame());
        popup.add(openItem);
        
        // 分隔线
        popup.addSeparator();
        
        // 退出菜单项
        MenuItem exitItem = new MenuItem("退出");
        exitItem.addActionListener(e -> exitAction.run());
        popup.add(exitItem);
        
        return popup;
    }
    
    /**
     * 显示主窗口
     */
    private void showMainFrame() {
        if (mainFrame != null) {
            mainFrame.setVisible(true);
            mainFrame.setState(Frame.NORMAL);
            mainFrame.toFront();
        }
    }
    
    /**
     * 创建默认图标
     * @return 图标图像
     */
    private Image createDefaultIcon() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 填充背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, size, size);
        
        // 绘制图标
        g2d.setColor(Color.BLUE);
        g2d.fillOval(2, 2, size - 4, size - 4);
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 显示托盘通知
     * @param title 标题
     * @param message 消息内容
     * @param messageType 消息类型
     */
    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }
    
    /**
     * 移除系统托盘图标
     */
    public void remove() {
        if (tray != null && trayIcon != null) {
            tray.remove(trayIcon);
        }
    }
} 