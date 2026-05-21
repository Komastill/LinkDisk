package LinkDisk.ui;

import LinkDisk.network.*;
import java.awt.Font;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class MainFrame extends JFrame {

    private JButton selectButton;
    
    private JButton sendButton;
    
    private JButton trustManagerButton;

    private JTextArea logArea;

    private JProgressBar progressBar;

    private JButton openFolderButton;
    
    private java.util.Set<String> connectedDevices = new java.util.HashSet<String>();
    
    private JButton connectButton;
    
    private JList<String> deviceList;

    private DefaultListModel<String> deviceListModel;

    private JButton addIpButton;
    
    private File[] selectedFiles;
    
    private java.util.Map<String, String> deviceNameMap =
            new java.util.HashMap<String, String>();

    private java.util.Map<String, String> devicePlatformMap =
            new java.util.HashMap<String, String>();
    
    private String displayText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(".", "\u2024");
    }

    private String displayIp(String ip) {
        return displayText(ip);
    }

    private String formatFileSize(long size) {
        double value = size;

        if (size < 1024) {
            return size + " B";
        }

        value = value / 1024;
        if (value < 1024) {
            return String.format(java.util.Locale.US, "%.2f KB", value);
        }

        value = value / 1024;
        if (value < 1024) {
            return String.format(java.util.Locale.US, "%.2f MB", value);
        }

        value = value / 1024;
        return String.format(java.util.Locale.US, "%.2f GB", value);
    }
    
    private TransferManager transferManager;

    public MainFrame() {

        setTitle("LinkDisk");

        setSize(1000, 620);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(null);
        Font tempFont = UIManager.getFont("TextField.font");

        if (tempFont == null) {
            tempFont = new Font(Font.DIALOG, Font.PLAIN, 16);
        }

        final Font font = tempFont.deriveFont(Font.PLAIN, 16f);
        
        // 初始化传输管理器
        transferManager = TransferManager.getInstance();

        // 设备列表
        deviceListModel = new DefaultListModel<String>();
        deviceList = new JList<String>(deviceListModel);
        deviceList.setFont(font);
        deviceList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

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
        deviceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane deviceScrollPane = new JScrollPane(deviceList);
        deviceScrollPane.setBounds(30, 30, 430, 170);
        add(deviceScrollPane);

        // 信任设备管理按钮
        trustManagerButton = new JButton("信任设备管理");
        trustManagerButton.setFont(font);
        trustManagerButton.setBounds(560, 230, 180, 45);
        add(trustManagerButton);
        connectButton = new JButton("连接设备");
        connectButton.setFont(font);
        connectButton.setBounds(490, 45, 160, 45);
        add(connectButton);
        addIpButton = new JButton("手动添加IP");
        addIpButton.setFont(font);
        addIpButton.setBounds(490, 115, 160, 45);
        add(addIpButton);
        
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
                            	    logArea.append("设备连接失败或被拒绝：" + displayIp(ip) + "\n");
                            	}
                            }
                        });
                    }
                }).start();
            }
        });
        
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

        openFolderButton = new JButton("打开接收文件夹");
        openFolderButton.setFont(font);
        openFolderButton.setBounds(350, 230, 190, 45);
        add(openFolderButton);
        
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
        
        // 选择文件按钮
        selectButton = new JButton("选择文件");
        selectButton.setFont(font);
        selectButton.setBounds(30, 230, 140, 45);
        add(selectButton);

        // 发送按钮
        sendButton = new JButton("发送文件");
        sendButton.setFont(font);
        sendButton.setBounds(190, 230, 140, 45);
        add(sendButton);

        // 日志区域
        logArea = new JTextArea();
        logArea.setFont(font);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBounds(30, 300, 850, 220);
        add(scrollPane);
        
     // 启动TCP服务器（支持授权）
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
                    public void onFileReceived(
                            String clientIp,
                            String fileName,
                            String savePath,
                            long fileSize
                    ) {

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {

                                logArea.append("\n收到文件\n");
                                logArea.append("来自设备：" + displayIp(clientIp) + "\n");
                                logArea.append("文件名：" + displayText(fileName) + "\n");
                                logArea.append("保存位置：" + displayText(savePath) + "\n");
                                logArea.append("接收大小：" + displayText(formatFileSize(fileSize)) + "\n");                                
                                logArea.append("接收完成\n\n");

                                progressBar.setValue(100);
                            }
                        });
                    }
                }
        );

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setFont(font);
        progressBar.setBounds(30, 540, 850, 25);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        add(progressBar);

        // 选择文件
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFiles = fileChooser.getSelectedFiles();
                    logArea.append("已选择文件：\n");
                    for (File file : selectedFiles) {
                    	logArea.append(
                    	        "  - " +
                    	        displayText(file.getName()) +
                    	        " (" +
                    	        displayText(formatFileSize(file.length())) +
                    	        ")\n"
                    	);                    }
                    logArea.append("\n");
                }
            }
        });
        
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

        // 发送文件
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

                // 检查设备是否已经连接
                for (String ip : selectedIps) {
                    if (!connectedDevices.contains(ip)) {
                        logArea.append(
                                "设备尚未连接，请先点击【连接设备】：" 
                                + displayIp(ip) 
                                + "\n"
                        );
                        return;
                    }
                }

                progressBar.setValue(0);
                logArea.append("开始发送文件...\n");
                
                // 为每个文件创建传输任务
                for (String ip : selectedIps) {
                    for (File file : selectedFiles) {
                        TransferTask task = new TransferTask(ip, file.getName(), file.length(), "upload");
                        transferManager.addTask(task);
                        logArea.append("添加任务：" + displayText(file.getName()) + " -> " + displayIp(ip) + "\n");                    }
                }
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (String ip : selectedIps) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                	logArea.append("发送到设备：" + displayIp(ip) + "\n");                                }
                            });
                            
                            boolean success = TcpClient.sendFiles(
                                    selectedFiles,
                                    ip,
                                    new ProgressListener() {
                                        @Override
                                        public void onProgress(int progress) {
                                            SwingUtilities.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressBar.setValue(progress);
                                                }
                                            });
                                        }
                                    }
                            );
                            
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                	if (success) {
                                	    logArea.append("设备 " + displayIp(ip) + " 文件发送完成\n");
                                	} else {
                                	    logArea.append("设备 " + displayIp(ip) + " 文件发送失败\n");
                                	}                             }
                            });
                        }
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(100);
                                logArea.append("发送流程结束\n");
                            }
                        });
                    }
                }).start();
            }
        });

        // UDP监听（自动发现设备）
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
        
        // 窗口关闭时清理资源
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