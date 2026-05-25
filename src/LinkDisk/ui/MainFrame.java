package LinkDisk.ui;

import LinkDisk.network.*;

import java.awt.Font;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    private JButton selectButton;
    private JButton sendButton;
    private JButton trustManagerButton;

    private JButton clearTaskButton;
    private JButton clearLogButton;
    private JButton clearSelectedButton;

    private JButton openFolderButton;
    private JButton connectButton;
    private JButton addIpButton;

    private JTextArea logArea;

    private JTable transferTable;
    private DefaultTableModel transferTableModel;

    private JProgressBar progressBar;

    private JList<String> deviceList;
    private DefaultListModel<String> deviceListModel;

    private File[] selectedFiles;

    private TransferManager transferManager;

    private Set<String> connectedDevices = new HashSet<String>();

    private Map<String, Integer> receiveRowIndexMap =
            new HashMap<String, Integer>();

    private Map<String, String> deviceNameMap =
            new HashMap<String, String>();

    private Map<String, String> devicePlatformMap =
            new HashMap<String, String>();

    private String displayText(String text) {
        if (text == null) {
            return "";
        }

        // 用特殊点号绕开 macOS Swing 部分字体不显示英文点号的问题
        return text.replace(".", "\u2024");
    }

    private String displayIp(String ip) {
        return displayText(ip);
    }

    private String formatFileSize(long size) {
        double value = size;

        if (size < 1000) {
            return size + " B";
        }

        value = value / 1000;
        if (value < 1000) {
            return String.format(java.util.Locale.US, "%.2f KB", value);
        }

        value = value / 1000;
        if (value < 1000) {
            return String.format(java.util.Locale.US, "%.2f MB", value);
        }

        value = value / 1000;
        return String.format(java.util.Locale.US, "%.2f GB", value);
    }

    private int addTransferRow(
            String direction,
            String fileName,
            String deviceIp,
            long fileSize,
            String status,
            int progress
    ) {
        int rowIndex = transferTableModel.getRowCount();

        transferTableModel.addRow(
                new Object[]{
                        direction,
                        displayText(fileName),
                        displayIp(deviceIp),
                        displayText(formatFileSize(fileSize)),
                        status,
                        progress + "%"
                }
        );

        return rowIndex;
    }

    private void updateTransferRow(int rowIndex, String status, int progress) {
        if (rowIndex < 0 || rowIndex >= transferTableModel.getRowCount()) {
            return;
        }

        transferTableModel.setValueAt(status, rowIndex, 4);
        transferTableModel.setValueAt(progress + "%", rowIndex, 5);
    }

    public MainFrame() {

        setTitle("LinkDisk");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        Font tempFont = UIManager.getFont("TextField.font");

        if (tempFont == null) {
            tempFont = new Font(Font.DIALOG, Font.PLAIN, 16);
        }

        final Font font = tempFont.deriveFont(Font.PLAIN, 16f);

        transferManager = TransferManager.getInstance();

        // =========================
        // 设备列表
        // =========================
        deviceListModel = new DefaultListModel<String>();

        deviceList = new JList<String>(deviceListModel);
        deviceList.setFont(font);
        deviceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        deviceList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );

                String ip = String.valueOf(value);

                String deviceName = deviceNameMap.get(ip);
                String platform = devicePlatformMap.get(ip);

                if (deviceName == null) {
                    deviceName = "未知设备";
                }

                if (platform == null) {
                    platform = "未知平台";
                }

                String status;

                if (connectedDevices.contains(ip)) {
                    status = "已连接";
                } else {
                    status = "未连接";
                }

                label.setText(
                        deviceName + "  [" + displayIp(ip) + "]  " + platform + "  " + status
                );

                label.setFont(font);

                return label;
            }
        });

        JScrollPane deviceScrollPane = new JScrollPane(deviceList);
        deviceScrollPane.setBounds(30, 30, 430, 170);
        add(deviceScrollPane);

        // =========================
        // 顶部设备操作按钮
        // =========================
        connectButton = new JButton("连接设备");
        connectButton.setFont(font);
        connectButton.setBounds(490, 45, 160, 45);
        add(connectButton);

        addIpButton = new JButton("手动添加IP");
        addIpButton.setFont(font);
        addIpButton.setBounds(490, 115, 160, 45);
        add(addIpButton);

        clearTaskButton = new JButton("清空任务");
        clearTaskButton.setFont(font);
        clearTaskButton.setBounds(670, 45, 140, 45);
        add(clearTaskButton);

        clearLogButton = new JButton("清空日志");
        clearLogButton.setFont(font);
        clearLogButton.setBounds(670, 115, 140, 45);
        add(clearLogButton);

        // =========================
        // 文件操作按钮
        // =========================
        selectButton = new JButton("选择文件");
        selectButton.setFont(font);
        selectButton.setBounds(30, 230, 140, 45);
        add(selectButton);

        sendButton = new JButton("发送文件");
        sendButton.setFont(font);
        sendButton.setBounds(190, 230, 140, 45);
        add(sendButton);

        openFolderButton = new JButton("打开接收文件夹");
        openFolderButton.setFont(font);
        openFolderButton.setBounds(350, 230, 190, 45);
        add(openFolderButton);

        trustManagerButton = new JButton("信任设备管理");
        trustManagerButton.setFont(font);
        trustManagerButton.setBounds(560, 230, 180, 45);
        add(trustManagerButton);

        clearSelectedButton = new JButton("重置选择");
        clearSelectedButton.setFont(font);
        clearSelectedButton.setBounds(760, 230, 140, 45);
        add(clearSelectedButton);

        // =========================
        // 传输任务表格
        // =========================
        transferTableModel = new DefaultTableModel(
                new Object[]{"方向", "文件名", "对方设备", "大小", "状态", "进度"},
                0
        );

        transferTable = new JTable(transferTableModel);
        transferTable.setFont(font);
        transferTable.setRowHeight(28);
        transferTable.getTableHeader().setFont(font);

        JScrollPane tableScrollPane = new JScrollPane(transferTable);
        tableScrollPane.setBounds(30, 300, 850, 120);
        add(tableScrollPane);

        // =========================
        // 日志区域
        // =========================
        logArea = new JTextArea();
        logArea.setFont(font);
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBounds(30, 440, 850, 130);
        add(scrollPane);

        // =========================
        // 进度条
        // =========================
        progressBar = new JProgressBar();
        progressBar.setFont(font);
        progressBar.setBounds(30, 590, 850, 25);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        add(progressBar);

        // =========================
        // 启动 TCP 服务器
        // =========================
        TcpServer.startServer(
                new TcpServer.AuthCallback() {
                    @Override
                    public boolean onAuthRequest(String ip) {

                        final boolean[] result = {false};

                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {

                                    Object[] options = {"允许", "拒绝"};

                                    int option = JOptionPane.showOptionDialog(
                                            MainFrame.this,
                                            "设备 " + displayIp(ip) + " 请求连接，是否允许？\n允许后将可以接收该设备发送的文件",
                                            "连接请求",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE,
                                            null,
                                            options,
                                            options[0]
                                    );

                                    result[0] = (option == JOptionPane.YES_OPTION);

                                    if (result[0]) {
                                        logArea.append("已授权设备：" + displayIp(ip) + "\n");
                                    } else {
                                        logArea.append("拒绝设备连接：" + displayIp(ip) + "\n");
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return result[0];
                    }
                },

                new TcpServer.ReceiveCallback() {

                    @Override
                    public void onFileReceiveStart(
                            String clientIp,
                            String fileName,
                            String savePath,
                            long fileSize
                    ) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {

                                int rowIndex = addTransferRow(
                                        "接收",
                                        fileName,
                                        clientIp,
                                        fileSize,
                                        "接收中",
                                        0
                                );

                                receiveRowIndexMap.put(clientIp + "|" + savePath, rowIndex);

                                logArea.append(
                                        "开始接收文件：" +
                                                displayText(fileName) +
                                                "，来自 " +
                                                displayIp(clientIp) +
                                                "\n"
                                );
                            }
                        });
                    }

                    @Override
                    public void onFileReceiveProgress(
                            String clientIp,
                            String fileName,
                            String savePath,
                            long fileSize,
                            long receivedBytes,
                            int progress
                    ) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {

                                Integer rowIndex =
                                        receiveRowIndexMap.get(clientIp + "|" + savePath);

                                if (rowIndex != null) {
                                    updateTransferRow(rowIndex, "接收中", progress);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFileReceived(
                            String clientIp,
                            String fileName,
                            String savePath,
                            long fileSize
                    ) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {

                                Integer rowIndex =
                                        receiveRowIndexMap.get(clientIp + "|" + savePath);

                                if (rowIndex != null) {
                                    updateTransferRow(rowIndex, "完成", 100);
                                }

                                logArea.append("\n收到文件\n");
                                logArea.append("来自设备：" + displayIp(clientIp) + "\n");
                                logArea.append("文件名：" + displayText(fileName) + "\n");
                                logArea.append("保存位置：" + displayText(savePath) + "\n");
                                logArea.append("接收大小：" + displayText(formatFileSize(fileSize)) + "\n");
                                logArea.append("接收完成\n\n");
                            }
                        });
                    }
                }
        );

        // =========================
        // 连接设备
        // =========================
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String ip = deviceList.getSelectedValue();

                if (ip == null) {
                    logArea.append("请先选择要连接的设备\n");
                    return;
                }

                logArea.append("正在连接设备：" + displayIp(ip) + "\n");

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        TcpClient.ConnectResult result =
                                TcpClient.connectDevice(ip);

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {

                                if (result.success) {

                                    connectedDevices.add(ip);

                                    if (result.deviceName != null && result.deviceName.length() > 0) {
                                        deviceNameMap.put(ip, result.deviceName);
                                    }

                                    if (result.platform != null && result.platform.length() > 0) {
                                        devicePlatformMap.put(ip, result.platform);
                                    }

                                    deviceList.repaint();

                                    logArea.append(
                                            "设备连接成功：" +
                                                    deviceNameMap.get(ip) +
                                                    " [" + displayIp(ip) + "] " +
                                                    devicePlatformMap.get(ip) +
                                                    "\n"
                                    );

                                } else {
                                    logArea.append(
                                            "设备连接失败：" +
                                                    displayIp(ip) +
                                                    "，原因：" +
                                                    result.message +
                                                    "\n"
                                    );
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // =========================
        // 手动添加 IP
        // =========================
        addIpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String ip = JOptionPane.showInputDialog(
                        MainFrame.this,
                        "请输入目标设备 IP 地址：",
                        "手动添加IP",
                        JOptionPane.PLAIN_MESSAGE
                );

                if (ip == null) {
                    return;
                }

                ip = ip.trim();

                if (ip.length() == 0) {
                    logArea.append("IP 地址不能为空\n");
                    return;
                }

                if (UdpListener.isLocalIp(ip)) {
                    deviceNameMap.put(ip, UdpListener.getThisDeviceName() + "（本机）");
                    devicePlatformMap.put(ip, UdpListener.getThisPlatform());
                } else {
                    deviceNameMap.put(ip, "手动添加设备");
                    devicePlatformMap.put(ip, "未知平台");
                }

                if (!deviceListModel.contains(ip)) {
                    deviceListModel.addElement(ip);

                    logArea.append(
                            "已手动添加设备：" +
                                    deviceNameMap.get(ip) +
                                    " [" + displayIp(ip) + "] " +
                                    devicePlatformMap.get(ip) +
                                    "\n"
                    );
                } else {
                    logArea.append("设备已存在：" + displayIp(ip) + "\n");
                }

                deviceList.repaint();
            }
        });

        // =========================
        // 清空任务
        // =========================
        clearTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int confirm = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "确定清空传输任务列表吗？",
                        "清空任务",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {

                    transferTableModel.setRowCount(0);

                    receiveRowIndexMap.clear();

                    progressBar.setValue(0);

                    logArea.append("已清空传输任务\n");
                }
            }
        });

        // =========================
        // 清空日志
        // =========================
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int confirm = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "确定清空日志吗？",
                        "清空日志",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    logArea.setText("");
                }
            }
        });

        // =========================
        // 重置选择
        // =========================
        clearSelectedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                selectedFiles = null;

                progressBar.setValue(0);

                logArea.append("已重置当前选择的文件\n");
            }
        });

        // =========================
        // 打开接收文件夹
        // =========================
        openFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File folder = new File("received_files");

                    if (!folder.exists()) {
                        folder.mkdirs();
                    }

                    java.awt.Desktop.getDesktop().open(folder);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    logArea.append("打开接收文件夹失败\n");
                }
            }
        });

        // =========================
        // 选择文件
        // =========================
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fileChooser = new JFileChooser();

                fileChooser.setMultiSelectionEnabled(true);

                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int result = fileChooser.showOpenDialog(MainFrame.this);

                if (result == JFileChooser.APPROVE_OPTION) {

                    File[] newFiles = fileChooser.getSelectedFiles();

                    LinkedHashMap<String, File> fileMap =
                            new LinkedHashMap<String, File>();

                    if (selectedFiles != null) {
                        for (File file : selectedFiles) {
                            fileMap.put(file.getAbsolutePath(), file);
                        }
                    }

                    for (File file : newFiles) {
                        fileMap.put(file.getAbsolutePath(), file);
                    }

                    selectedFiles = fileMap.values().toArray(new File[0]);

                    logArea.append("当前已选择文件：\n");

                    for (File file : selectedFiles) {
                        logArea.append(
                                "  - " +
                                        displayText(file.getName()) +
                                        " (" +
                                        displayText(formatFileSize(file.length())) +
                                        ")\n"
                        );
                    }

                    logArea.append("\n");
                }
            }
        });

        // =========================
        // 信任设备管理
        // =========================
        trustManagerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                AuthManager authManager = new AuthManager();

                java.util.List<String> trusted =
                        authManager.getAllTrustedDevices();

                if (trusted.isEmpty()) {

                    JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "暂无信任设备",
                            "信任设备管理",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    return;
                }

                String selectedIp = (String) JOptionPane.showInputDialog(
                        MainFrame.this,
                        "请选择要删除的信任设备：",
                        "信任设备管理",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        trusted.toArray(),
                        trusted.get(0)
                );

                if (selectedIp != null) {

                    int confirm = JOptionPane.showConfirmDialog(
                            MainFrame.this,
                            "确定删除信任设备 " + selectedIp + " 吗？\n下次连接需要重新授权。",
                            "确认删除",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (confirm == JOptionPane.YES_OPTION) {

                        authManager.removeTrustedDevice(selectedIp);

                        connectedDevices.remove(selectedIp);

                        deviceList.repaint();

                        logArea.append(
                                "已删除信任设备：" + displayIp(selectedIp) + "\n"
                        );

                        JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "删除成功",
                                "提示",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            }
        });

        // =========================
        // 发送文件
        // =========================
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (selectedFiles == null || selectedFiles.length == 0) {
                    logArea.append("请先选择文件\n");
                    return;
                }

                java.util.List<String> selectedIps =
                        deviceList.getSelectedValuesList();

                if (selectedIps.isEmpty()) {
                    logArea.append("请先选择设备\n");
                    return;
                }

                for (String ip : selectedIps) {
                    if (!connectedDevices.contains(ip)) {
                        logArea.append(
                                "设备尚未连接，请先点击【连接设备】：" +
                                        displayIp(ip) +
                                        "\n"
                        );
                        return;
                    }
                }

                File[] filesToSend = selectedFiles.clone();

                progressBar.setValue(0);

                logArea.append("开始发送文件...\n");

                Map<String, Integer> rowIndexMap =
                        new HashMap<String, Integer>();

                for (String ip : selectedIps) {
                    for (int i = 0; i < filesToSend.length; i++) {

                        File file = filesToSend[i];

                        TransferTask task =
                                new TransferTask(ip, file.getName(), file.length(), "upload");

                        transferManager.addTask(task);

                        int rowIndex = addTransferRow(
                                "发送",
                                file.getName(),
                                ip,
                                file.length(),
                                "等待",
                                0
                        );

                        rowIndexMap.put(ip + "|" + i, rowIndex);

                        logArea.append(
                                "添加任务：" +
                                        displayText(file.getName()) +
                                        " -> " +
                                        displayIp(ip) +
                                        "\n"
                        );
                    }
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        for (String ip : selectedIps) {

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    logArea.append("发送到设备：" + displayIp(ip) + "\n");
                                }
                            });

                            TcpClient.SendResult sendResult = TcpClient.sendFiles(
                                    filesToSend,
                                    ip,
                                    new ProgressListener() {

                                        @Override
                                        public void onTotalProgress(int progress) {
                                            SwingUtilities.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressBar.setValue(progress);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFileStart(int fileIndex, String fileName) {
                                            SwingUtilities.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Integer rowIndex =
                                                            rowIndexMap.get(ip + "|" + fileIndex);

                                                    if (rowIndex != null) {
                                                        updateTransferRow(rowIndex, "传输中", 0);
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFileProgress(int fileIndex, String fileName, int progress) {
                                            SwingUtilities.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Integer rowIndex =
                                                            rowIndexMap.get(ip + "|" + fileIndex);

                                                    if (rowIndex != null) {
                                                        updateTransferRow(rowIndex, "传输中", progress);
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFileComplete(int fileIndex, String fileName) {
                                            SwingUtilities.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Integer rowIndex =
                                                            rowIndexMap.get(ip + "|" + fileIndex);

                                                    if (rowIndex != null) {
                                                        updateTransferRow(rowIndex, "完成", 100);
                                                    }
                                                }
                                            });
                                        }
                                    }
                            );

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {

                                    if (sendResult.success) {
                                        logArea.append("设备 " + displayIp(ip) + " 文件发送完成\n");
                                    } else {
                                        for (int i = 0; i < filesToSend.length; i++) {
                                            Integer rowIndex = rowIndexMap.get(ip + "|" + i);

                                            if (rowIndex != null) {
                                                updateTransferRow(rowIndex, "失败", 0);
                                            }
                                        }

                                        logArea.append(
                                                "设备 " +
                                                        displayIp(ip) +
                                                        " 文件发送失败，原因：" +
                                                        sendResult.message +
                                                        "\n"
                                        );
                                    }
                                }
                            });
                        }

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                logArea.append("发送流程结束\n");
                            }
                        });
                    }
                }).start();
            }
        });

        // =========================
        // UDP 自动发现
        // =========================
        UdpListener.startListening(new DeviceFoundListener() {
            @Override
            public void onDeviceFound(String ip, String deviceName, String platform) {

                System.out.println(
                        "UI收到设备 = [" +
                                deviceName + "] [" +
                                platform + "] [" +
                                ip + "]"
                );

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {

                        deviceNameMap.put(ip, deviceName);
                        devicePlatformMap.put(ip, platform);

                        if (!deviceListModel.contains(ip)) {
                            deviceListModel.addElement(ip);

                            logArea.append(
                                    "发现设备：" +
                                            deviceName +
                                            " [" + displayIp(ip) + "] " +
                                            platform +
                                            "\n"
                            );
                        } else {
                            deviceList.repaint();
                        }
                    }
                });
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                UdpListener.stopListening();
                TcpServer.stopServer();
                transferManager.shutdown();
            }
        });

        setVisible(true);

        logArea.append("LinkDisk 已启动\n");
        logArea.append("等待设备发现...\n");
        logArea.append("提示：设备会自动互相发现，选择文件和目标设备即可发送\n\n");
    }

    public static void main(String[] args) {

        System.setProperty("sun.java2d.metal", "false");
        System.setProperty("sun.java2d.opengl", "false");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame();
            }
        });
    }
}
