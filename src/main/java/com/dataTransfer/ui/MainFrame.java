package com.dataTransfer.ui;

import com.dataTransfer.model.Config;
import com.dataTransfer.model.DetectionRecord;
import com.dataTransfer.model.FileStatus;
import com.dataTransfer.model.TransferStatus;
import com.dataTransfer.service.DataTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 主界面
 */
public class MainFrame extends JFrame implements DataTransferService.ErrorCallback {
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final DataTransferService dataTransferService;
    private final TrayManager trayManager;
    private final ScheduledExecutorService uiUpdateScheduler;
    
    // 标记是否正在加载配置
    private boolean isLoadingConfig = false;
    
    // UI组件
    private JTextField apiUrlField;
    private JTextField equipmentCodeField;
    private JTextField lineCodeField;
    private JTextField csvPathField;
    private JSpinner monitorIntervalSpinner;
    private JSpinner maxRetriesSpinner;
    private JSpinner retryDelaySpinner;
    private JCheckBox continueFromLastCheckBox;
    private JCheckBox minimizeToTrayCheckBox;
    private JCheckBox monitorAllFilesCheckBox;
    private JRadioButton equipmentApiRadio;
    private JRadioButton serialApiRadio;
    private ButtonGroup apiTypeGroup;
    private JButton browseButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton resetButton;
    private JButton saveConfigButton;
    
    // 状态面板组件
    private JLabel fileNameLabel;
    private JLabel productModelLabel;
    private JLabel lastModifiedFileLabel;
    private JLabel lastProcessedLineLabel;
    private JLabel totalProcessedLinesLabel;
    private JLabel failedLinesLabel;
    private JLabel lastProcessedTimeLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    
    /**
     * 构造方法
     * @param dataTransferService 数据传输服务
     */
    public MainFrame(DataTransferService dataTransferService) {
        this.dataTransferService = dataTransferService;
        
        setTitle("数据传输应用");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        
        // 创建UI
        initUI();
        
        // 加载配置
        loadConfig();
        
        // 注册错误回调
        dataTransferService.setErrorCallback(this);
        
        // 创建托盘管理器
        this.trayManager = new TrayManager(this, this::exitApplication);
        
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (minimizeToTrayCheckBox.isSelected()) {
                    setVisible(false);
                } else {
                    exitApplication();
                }
            }
        });
        
        // 创建UI更新定时器
        uiUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
        uiUpdateScheduler.scheduleAtFixedRate(this::updateStatusUI, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * 初始化UI
     */
    private void initUI() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);
        
        // 创建选项卡面板
        JTabbedPane tabbedPane = new JTabbedPane();
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 创建配置面板
        JPanel configPanel = createConfigPanel();
        tabbedPane.addTab("配置", configPanel);
        
        // 创建状态面板
        JPanel statusPanel = createStatusPanel();
        tabbedPane.addTab("状态", statusPanel);
        
        // 创建控制面板
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建配置面板
     * @return 配置面板
     */
    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("配置"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // API URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("API服务器地址:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        apiUrlField = new JTextField(20);
        configPanel.add(apiUrlField, gbc);
        
        // 设备代码
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        configPanel.add(new JLabel("设备代码:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        equipmentCodeField = new JTextField(20);
        configPanel.add(equipmentCodeField, gbc);
        
        // 生产线代码
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        configPanel.add(new JLabel("生产线代码:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        lineCodeField = new JTextField(20);
        configPanel.add(lineCodeField, gbc);
        
        // CSV文件路径
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        configPanel.add(new JLabel("CSV文件路径:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        csvPathField = new JTextField(20);
        configPanel.add(csvPathField, gbc);
        
        JPanel csvButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        
        browseButton = new JButton("浏览...");
        browseButton.addActionListener(e -> browseForCsvPath());
        csvButtonPanel.add(browseButton);
        
        JButton openFolderButton = new JButton("打开文件夹");
        openFolderButton.addActionListener(e -> openCsvFolder());
        csvButtonPanel.add(openFolderButton);
        
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 0;
        configPanel.add(csvButtonPanel, gbc);
        
        // API类型选择
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        configPanel.add(new JLabel("API类型:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        JPanel apiTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        apiTypeGroup = new ButtonGroup();
        equipmentApiRadio = new JRadioButton("中间数据");
        serialApiRadio = new JRadioButton("结果数据");
        
        apiTypeGroup.add(equipmentApiRadio);
        apiTypeGroup.add(serialApiRadio);
        
        apiTypePanel.add(equipmentApiRadio);
        apiTypePanel.add(serialApiRadio);
        
        configPanel.add(apiTypePanel, gbc);
        
        // 监控间隔
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        configPanel.add(new JLabel("监控间隔(秒):"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        monitorIntervalSpinner = new JSpinner(new SpinnerNumberModel(60, 10, 3600, 10));
        configPanel.add(monitorIntervalSpinner, gbc);
        
        // 最大重试次数
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        configPanel.add(new JLabel("最大重试次数:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.weightx = 1.0;
        maxRetriesSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        configPanel.add(maxRetriesSpinner, gbc);
        
        // 重试延迟
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        configPanel.add(new JLabel("重试延迟(秒):"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.weightx = 1.0;
        retryDelaySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        configPanel.add(retryDelaySpinner, gbc);
        
        // 复选框
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        continueFromLastCheckBox = new JCheckBox("从上次位置继续");
        configPanel.add(continueFromLastCheckBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 9;
        minimizeToTrayCheckBox = new JCheckBox("最小化到系统托盘");
        configPanel.add(minimizeToTrayCheckBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 10;
        monitorAllFilesCheckBox = new JCheckBox("监控全部文件");
        configPanel.add(monitorAllFilesCheckBox, gbc);
        
        return configPanel;
    }
    
    /**
     * 创建状态面板
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("运行状态"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // 当前文件
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel fileNameLabel1 = new JLabel("当前文件:");
        fileNameLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(fileNameLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        fileNameLabel = new JLabel("-");
        // 为文件名标签设置固定高度，但允许水平方向扩展
        fileNameLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(fileNameLabel, gbc);
        
        // 产品型号
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel productModelLabel1 = new JLabel("产品型号:");
        productModelLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(productModelLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        productModelLabel = new JLabel("-");
        productModelLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(productModelLabel, gbc);
        
        // 最后修改文件
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel lastModifiedFileLabel1 = new JLabel("最后修改文件:");
        lastModifiedFileLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(lastModifiedFileLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        lastModifiedFileLabel = new JLabel("-");
        lastModifiedFileLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(lastModifiedFileLabel, gbc);
        
        // 最后处理行
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        JLabel lastProcessedLineLabel1 = new JLabel("最后处理行:");
        lastProcessedLineLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(lastProcessedLineLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        lastProcessedLineLabel = new JLabel("0");
        lastProcessedLineLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(lastProcessedLineLabel, gbc);
        
        // 总处理行数
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        JLabel totalProcessedLinesLabel1 = new JLabel("总处理行数:");
        totalProcessedLinesLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(totalProcessedLinesLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        totalProcessedLinesLabel = new JLabel("0");
        totalProcessedLinesLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(totalProcessedLinesLabel, gbc);
        
        // 失败行数
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        JLabel failedLinesLabel1 = new JLabel("失败行数:");
        failedLinesLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(failedLinesLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        failedLinesLabel = new JLabel("0");
        failedLinesLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(failedLinesLabel, gbc);
        
        // 最后处理时间
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        JLabel lastProcessedTimeLabel1 = new JLabel("最后处理时间:");
        lastProcessedTimeLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(lastProcessedTimeLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.weightx = 1.0;
        lastProcessedTimeLabel = new JLabel("-");
        lastProcessedTimeLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(lastProcessedTimeLabel, gbc);
        
        // 当前状态
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        JLabel statusLabel1 = new JLabel("当前状态:");
        statusLabel1.setPreferredSize(new Dimension(120, 20));
        statusPanel.add(statusLabel1, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.weightx = 1.0;
        statusLabel = new JLabel("未运行");
        statusLabel.setForeground(Color.RED);
        statusLabel.setPreferredSize(new Dimension(350, 20));
        statusPanel.add(statusLabel, gbc);
        
        // 进度条
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(500, 20));
        progressBar.setStringPainted(true);
        progressBar.setString("就绪");
        statusPanel.add(progressBar, gbc);
        
        return statusPanel;
    }
    
    /**
     * 创建控制按钮面板
     * @return 控制按钮面板
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        saveConfigButton = new JButton("保存配置");
        saveConfigButton.addActionListener(e -> saveConfig());
        controlPanel.add(saveConfigButton);

        startButton = new JButton("启动");
        startButton.addActionListener(e -> startService());
        controlPanel.add(startButton);
        
        stopButton = new JButton("停止");
        stopButton.addActionListener(e -> stopService());
        stopButton.setEnabled(false);
        controlPanel.add(stopButton);
        
        resetButton = new JButton("重置状态");
        resetButton.addActionListener(e -> resetStatus());
        controlPanel.add(resetButton);
        
        
        
        return controlPanel;
    }
    
    /**
     * 浏览CSV文件路径
     */
    private void browseForCsvPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("选择CSV文件目录");
        fileChooser.setApproveButtonText("选择此文件夹");
        
        // 添加文件过滤器以强调是选择文件夹
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
            
            @Override
            public String getDescription() {
                return "文件夹";
            }
        });
        
        // 如果已有路径，则设置为初始目录
        String currentPath = csvPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.getParentFile() != null && currentDir.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(currentDir.getParentFile());
            }
        }
        
        try {
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dir = fileChooser.getSelectedFile();
                if (dir != null) {
                    // 确保所选路径是目录
                    if (!dir.exists()) {
                        int option = JOptionPane.showConfirmDialog(
                                this,
                                "所选文件夹不存在，是否创建？",
                                "文件夹不存在",
                                JOptionPane.YES_NO_OPTION);
                        if (option == JOptionPane.YES_OPTION) {
                            if (!dir.mkdirs()) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "无法创建文件夹，请检查权限或手动创建。",
                                        "创建失败",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        } else {
                            return;
                        }
                    } else if (!dir.isDirectory()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "请选择一个文件夹，而不是文件。",
                                "选择错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    csvPathField.setText(dir.getAbsolutePath());
                    logger.info("已选择CSV文件目录: {}", dir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            logger.error("选择文件夹时出错: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(
                    this,
                    "选择文件夹时发生错误: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 打开CSV文件夹
     */
    private void openCsvFolder() {
        String path = csvPathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "请先选择CSV文件路径",
                    "路径为空",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        File dir = new File(path);
        if (!dir.exists()) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "文件夹不存在，是否创建？",
                    "文件夹不存在",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                if (!dir.mkdirs()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "无法创建文件夹，请检查权限或手动创建。",
                            "创建失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                return;
            }
        } else if (!dir.isDirectory()) {
            JOptionPane.showMessageDialog(
                    this,
                    "指定的路径不是文件夹",
                    "路径错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // 尝试打开文件夹
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            } else {
                logger.warn("当前系统不支持打开文件夹操作");
                JOptionPane.showMessageDialog(
                        this,
                        "当前系统不支持打开文件夹功能，请手动打开文件夹：\n" + path,
                        "无法打开",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            logger.error("打开文件夹失败: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(
                    this,
                    "打开文件夹失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 启动服务
     */
    private void startService() {
        // 先保存配置
        // saveConfig();
        
        // 启动服务
        dataTransferService.start();
        
        // 更新UI
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        updateStatusUI();
    }
    
    /**
     * 停止服务
     */
    private void stopService() {
        dataTransferService.stop();
        
        // 更新UI
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatusUI();
    }
    
    /**
     * 重置状态
     */
    private void resetStatus() {
        int option = JOptionPane.showConfirmDialog(
                this,
                "确定要重置传输状态吗？这将清除所有记录的传输进度。",
                "确认重置",
                JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            dataTransferService.resetStatus();
            updateStatusUI();
        }
    }
    
    /**
     * 加载配置到UI
     */
    private void loadConfig() {
        isLoadingConfig = true; // 标记开始加载配置
        
        Config config = dataTransferService.getConfig();
        
        apiUrlField.setText(config.getApiBaseUrl());
        equipmentCodeField.setText(config.getEquipmentCode());
        lineCodeField.setText(config.getLineCode());
        csvPathField.setText(config.getCsvFilePath());
        monitorIntervalSpinner.setValue(config.getMonitorInterval());
        maxRetriesSpinner.setValue(config.getMaxRetries());
        retryDelaySpinner.setValue(config.getRetryDelay());
        continueFromLastCheckBox.setSelected(config.isContinueFromLastPosition());
        minimizeToTrayCheckBox.setSelected(config.isMinimizeToTray());
        monitorAllFilesCheckBox.setSelected(config.isMonitorAllFiles());
        
        // 设置API类型单选按钮
        if (config.isEquipmentApiType()) {
            equipmentApiRadio.setSelected(true);
        } else if (config.isSerialApiType()) {
            serialApiRadio.setSelected(true);
        }
        
        isLoadingConfig = false; // 标记配置加载完成
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        // 验证CSV文件路径
        String csvPath = csvPathField.getText().trim();
        if (csvPath.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "请指定CSV文件路径",
                    "路径错误",
                    JOptionPane.WARNING_MESSAGE);
            csvPathField.requestFocus();
            return;
        }
        
        File csvDir = new File(csvPath);
        if (!csvDir.exists()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "CSV文件路径不存在，是否创建？",
                    "路径不存在",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                if (!csvDir.mkdirs()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "无法创建文件夹，请检查权限或指定其他路径",
                            "创建失败",
                            JOptionPane.ERROR_MESSAGE);
                    csvPathField.requestFocus();
                    return;
                }
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        } else if (!csvDir.isDirectory()) {
            JOptionPane.showMessageDialog(
                    this,
                    "指定的CSV文件路径不是文件夹",
                    "路径错误",
                    JOptionPane.ERROR_MESSAGE);
            csvPathField.requestFocus();
            return;
        }
        
        Config config = new Config();
        
        config.setApiBaseUrl(apiUrlField.getText().trim());
        config.setEquipmentCode(equipmentCodeField.getText().trim());
        config.setLineCode(lineCodeField.getText().trim());
        config.setCsvFilePath(csvPath);
        config.setMonitorInterval((int) monitorIntervalSpinner.getValue());
        config.setMaxRetries((int) maxRetriesSpinner.getValue());
        config.setRetryDelay((int) retryDelaySpinner.getValue());
        config.setContinueFromLastPosition(continueFromLastCheckBox.isSelected());
        config.setMinimizeToTray(minimizeToTrayCheckBox.isSelected());
        config.setMonitorAllFiles(monitorAllFilesCheckBox.isSelected());
        
        // 保存API类型
        if (equipmentApiRadio.isSelected()) {
            config.setApiType(Config.API_TYPE_EQUIPMENT);
        } else if (serialApiRadio.isSelected()) {
            config.setApiType(Config.API_TYPE_SERIAL);
        }
        
        dataTransferService.updateConfig(config);
        
        JOptionPane.showMessageDialog(this, "配置已保存", "保存成功", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 更新状态UI
     */
    private void updateStatusUI() {
        TransferStatus status = dataTransferService.getStatus();
        
        // 文件名 - 使用省略号确保长路径能够在有限空间显示
        String fileName = status.getFileName() != null ? status.getFileName() : "-";
        if (fileName.length() > 50) {
            int prefixLen = 15;
            int suffixLen = 30;
            fileName = fileName.substring(0, prefixLen) + "..." + 
                       fileName.substring(fileName.length() - suffixLen);
        }
        fileNameLabel.setText(fileName);
        fileNameLabel.setToolTipText(status.getFileName()); // 添加工具提示显示完整路径
        
        // 产品型号 - 从当前活动文件的FileStatus中获取
        String productModel = "-";
        FileStatus currentFileStatus = null;
        if (status.getFileName() != null && status.getFileStatusMap().containsKey(status.getFileName())) {
            currentFileStatus = status.getFileStatus(status.getFileName());
            productModel = currentFileStatus.getProductModel() != null ? currentFileStatus.getProductModel() : "-";
        }
        productModelLabel.setText(productModel);
        
        // 最后修改文件 - 使用省略号显示长路径
        String lastModifiedFile = status.getLastModifiedFileName() != null ? status.getLastModifiedFileName() : "-";
        if (lastModifiedFile.length() > 50) {
            int prefixLen = 15;
            int suffixLen = 30;
            lastModifiedFile = lastModifiedFile.substring(0, prefixLen) + "..." + 
                              lastModifiedFile.substring(lastModifiedFile.length() - suffixLen);
        }
        lastModifiedFileLabel.setText(lastModifiedFile);
        lastModifiedFileLabel.setToolTipText(status.getLastModifiedFileName()); // 添加工具提示显示完整路径
        
        // 行数
        lastProcessedLineLabel.setText(String.valueOf(status.getLastProcessedLine()));
        totalProcessedLinesLabel.setText(String.valueOf(status.getTotalProcessedLines()));
        failedLinesLabel.setText(String.valueOf(status.getFailedLines()));
        
        // 最后处理时间
        String lastTime = status.getLastProcessedTime() != null
                ? status.getLastProcessedTime().format(DATE_TIME_FORMATTER)
                : "-";
        lastProcessedTimeLabel.setText(lastTime);
        
        // 状态
        if (status.isRunning()) {
            statusLabel.setText("运行中");
            statusLabel.setForeground(Color.GREEN);
            progressBar.setString("处理中...");
            progressBar.setIndeterminate(true);
        } else {
            statusLabel.setText("未运行");
            statusLabel.setForeground(Color.RED);
            progressBar.setString("就绪");
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
        }
    }
    
    /**
     * 退出应用程序
     */
    private void exitApplication() {
        // 关闭数据传输服务
        if (dataTransferService != null) {
            dataTransferService.shutdown();
        }
        
        // 关闭UI更新调度器
        if (uiUpdateScheduler != null) {
            uiUpdateScheduler.shutdown();
        }
        
        // 移除托盘图标
        if (trayManager != null) {
            trayManager.remove();
        }
        
        logger.info("应用程序已退出");
        System.exit(0);
    }

    /**
     * HTTP请求失败时的回调方法
     * @param errorMessage 错误消息
     * @param record 失败的记录
     */
    @Override
    public void onHttpRequestFailed(String errorMessage, DetectionRecord record) {
        // 使用SwingUtilities.invokeLater确保在EDT线程中执行UI操作
        SwingUtilities.invokeLater(() -> {
            // 更新状态UI
            updateStatusUI();
            
            // 自动停止服务
            if (dataTransferService.getStatus().isRunning()) {
                stopService();
            }
            
            // 显示错误对话框
            JOptionPane.showMessageDialog(
                this,
                String.format("HTTP请求失败：\n%s\n\n条码：%s\n产品型号：%s\n\n后续处理已停止，请检查网络和服务器状态后再继续。",
                              errorMessage,
                              record.getBarcode(),
                              record.getProductModel()),
                "HTTP请求失败",
                JOptionPane.ERROR_MESSAGE
            );
            
        });
    }
}