package LinkDisk.ui;

import LinkDisk.network.*;

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

    private JList<String> deviceList;

    private DefaultListModel<String> deviceListModel;

    private File[] selectedFiles;
    
    private TransferManager transferManager;

    public MainFrame() {

        setTitle("LinkDisk");

        setSize(600, 500);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(null);
        
        // 初始化传输管理器
        transferManager = TransferManager.getInstance();
        
        // 启动TCP服务器（支持授权）
        TcpServer.startServer(new TcpServer.AuthCallback() {
            @Override
            public boolean onAuthRequest(String ip) {
                // 在UI线程显示授权弹窗
                final boolean[] result = {false};
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            Object[] options = {"允许", "拒绝"};
                            int option = JOptionPane.showOptionDialog(
                                MainFrame.this,
                                "设备 " + ip + " 请求连接，是否允许？\n允许后将可以接收该设备发送的文件",
                                "连接请求",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                            );
                            result[0] = (option == JOptionPane.YES_OPTION);
                            
                            if (result[0]) {
                                logArea.append("已授权设备：" + ip + "\n");
                            } else {
                                logArea.append("拒绝设备连接：" + ip + "\n");
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result[0];
            }
        });

        // 设备列表
        deviceListModel = new DefaultListModel<String>();
        deviceList = new JList<String>(deviceListModel);
        deviceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane deviceScrollPane = new JScrollPane(deviceList);
        deviceScrollPane.setBounds(30, 30, 200, 120);
        add(deviceScrollPane);

        // 信任设备管理按钮
        trustManagerButton = new JButton("信任设备管理");
        trustManagerButton.setBounds(250, 30, 120, 40);
        add(trustManagerButton);

        // 选择文件按钮
        selectButton = new JButton("选择文件");
        selectButton.setBounds(30, 170, 120, 40);
        add(selectButton);

        // 发送按钮
        sendButton = new JButton("发送文件");
        sendButton.setBounds(170, 170, 120, 40);
        add(sendButton);

        // 日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBounds(30, 230, 520, 180);
        add(scrollPane);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setBounds(30, 420, 520, 25);
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
                        logArea.append("  - " + file.getName() + " (" + file.length() + " bytes)\n");
                    }
                    logArea.append("\n");
                }
            }
        });
        
        // 信任设备管理
        trustManagerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                java.util.List<String> trusted = new AuthManager().getAllTrustedDevices();
                if (trusted.isEmpty()) {
                    JOptionPane.showMessageDialog(MainFrame.this, "暂无信任设备", "信任设备管理", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    StringBuilder sb = new StringBuilder("信任设备列表：\n");
                    for (String ip : trusted) {
                        sb.append("  - ").append(ip).append("\n");
                    }
                    sb.append("\n可以在项目目录的 trusted_devices.dat 文件中手动删除");
                    JOptionPane.showMessageDialog(MainFrame.this, sb.toString(), "信任设备管理", JOptionPane.INFORMATION_MESSAGE);
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
                java.util.List<String> selectedIps = deviceList.getSelectedValuesList();
                if (selectedIps.isEmpty()) {
                    logArea.append("请先选择设备\n");
                    return;
                }
                
                progressBar.setValue(0);
                logArea.append("开始发送文件...\n");
                
                // 为每个文件创建传输任务
                for (String ip : selectedIps) {
                    for (File file : selectedFiles) {
                        TransferTask task = new TransferTask(ip, file.getName(), file.length(), "upload");
                        transferManager.addTask(task);
                        logArea.append("添加任务：" + file.getName() + " -> " + ip + "\n");
                    }
                }
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (String ip : selectedIps) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    logArea.append("发送到设备：" + ip + "\n");
                                }
                            });
                            
                            TcpClient.sendFiles(
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
                                    logArea.append("设备 " + ip + " 文件发送完成\n");
                                }
                            });
                        }
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(100);
                                logArea.append("所有文件发送完成\n");
                            }
                        });
                    }
                }).start();
            }
        });

        // UDP监听（自动发现设备）
        UdpListener.startListening(new DeviceFoundListener() {
            @Override
            public void onDeviceFound(String ip) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!deviceListModel.contains(ip)) {
                            deviceListModel.addElement(ip);
                            logArea.append("发现设备：" + ip + "\n");
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
        new MainFrame();
    }
}