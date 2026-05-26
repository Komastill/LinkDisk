package LinkDisk.ui;

import LinkDisk.network.*;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import LinkDisk.model.TransferFileItem;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    private SideNavigation sidebarPanel;
    private Device devicePanel;
    private Transferring transferPanel;
    private Setting settingsPanel;

    private JPanel contentPanel;
    private CardLayout cardLayout;

    private TransferFileItem[] selectedItems;
    
    private LinkedHashMap<String, File> selectedRootMap =
            new LinkedHashMap<String, File>();
    
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
    
    private void collectTransferItems(
            File root,
            File current,
            LinkedHashMap<String, TransferFileItem> itemMap
    ) {
        if (current == null || !current.exists()) {
            return;
        }

        if (current.isFile()) {

            String relativePath;

            if (root.isFile()) {
                relativePath = root.getName();
            } else {
                String childPath =
                        root.toPath()
                                .relativize(current.toPath())
                                .toString()
                                .replace(File.separatorChar, '/');

                relativePath = root.getName() + "/" + childPath;
            }

            itemMap.put(
                    current.getAbsolutePath(),
                    new TransferFileItem(current, relativePath)
            );

            return;
        }

        File[] children = current.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {
            collectTransferItems(root, child, itemMap);
        }
    }

    private TransferFileItem[] buildTransferItems(File[] selectedRoots) {

        LinkedHashMap<String, TransferFileItem> itemMap =
                new LinkedHashMap<String, TransferFileItem>();

        if (selectedItems != null) {
            for (TransferFileItem item : selectedItems) {
                itemMap.put(
                        item.getSourceFile().getAbsolutePath(),
                        item
                );
            }
        }

        for (File root : selectedRoots) {
            collectTransferItems(root, root, itemMap);
        }

        return itemMap.values().toArray(new TransferFileItem[0]);
    }

    private int countFilesInRoot(File current) {

        if (current == null || !current.exists()) {
            return 0;
        }

        if (current.isFile()) {
            return 1;
        }

        File[] children = current.listFiles();

        if (children == null) {
            return 0;
        }

        int count = 0;

        for (File child : children) {
            count += countFilesInRoot(child);
        }

        return count;
    }
    
    private boolean isTrustedDevice(String ip) {
        try {
            AuthManager authManager = new AuthManager();
            return authManager.isTrusted(ip);
        } catch (Exception e) {
            return false;
        }
    }

    private String getDeviceDisplayText(String ip) {

        String deviceName = deviceNameMap.get(ip);
        String platform = devicePlatformMap.get(ip);

        if (deviceName == null) {
            deviceName = "未知设备";
        }

        if (platform == null) {
            platform = "未知平台";
        }

        String connectStatus;

        if (connectedDevices.contains(ip)) {
            connectStatus = "已连接";
        } else {
            connectStatus = "未连接";
        }

        String trustStatus;

        if (isTrustedDevice(ip)) {
            trustStatus = "已信任";
        } else {
            trustStatus = "未信任";
        }

        return deviceName +
                "  [" + displayIp(ip) + "]  " +
                platform +
                "  " + connectStatus +
                "  " + trustStatus;
    }

    private void showDeviceStatus(String message) {
        devicePanel.setStatusMessage(message);
        settingsPanel.setStatusMessage(message);
    }

    private void showTransferStatus(String message) {
        transferPanel.setStatusMessage(message);
        settingsPanel.setStatusMessage(message);
    }

    private void showSettingsStatus(String message) {
        settingsPanel.setStatusMessage(message);
    }

    private void updateSelectedFilesDisplay() {

        if (selectedRootMap == null || selectedRootMap.isEmpty()) {
            transferPanel.clearSelectedFilesText();
            return;
        }

        StringBuilder sb = new StringBuilder();

        int index = 1;

        for (File root : selectedRootMap.values()) {

            sb.append(index)
              .append(". ");

            if (root.isDirectory()) {

                int fileCount = countFilesInRoot(root);

                sb.append(displayText(root.getName()))
                  .append("  [文件夹]  共 ")
                  .append(fileCount)
                  .append(" 个文件");

            } else {

                sb.append(displayText(root.getName()))
                  .append("  ")
                  .append(displayText(formatFileSize(root.length())));
            }

            sb.append("\n");

            index++;
        }

        if (selectedItems != null && selectedItems.length > 0) {
            sb.append("\n实际待发送文件数：")
              .append(selectedItems.length);
        }

        transferPanel.setSelectedFilesText(sb.toString());
    }

    private int addTransferRow(
            String direction,
            String fileName,
            String deviceIp,
            long fileSize,
            String status,
            int progress
    ) {
        return transferPanel.addTransferRow(
                direction,
                displayText(fileName),
                displayIp(deviceIp),
                displayText(formatFileSize(fileSize)),
                status,
                progress
        );
    }

    private void updateTransferRow(int rowIndex, String status, int progress) {
        transferPanel.updateTransferRow(rowIndex, status, progress);
    }

    public MainFrame() {

        setTitle("LinkDisk");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Font tempFont = UIManager.getFont("TextField.font");

        if (tempFont == null) {
            tempFont = new Font(Font.DIALOG, Font.PLAIN, 16);
        }

        final Font font = tempFont.deriveFont(Font.PLAIN, 16f);

        transferManager = TransferManager.getInstance();

        sidebarPanel = new SideNavigation(font);

        devicePanel = new Device(
                font,
                new Device.DeviceDisplayProvider() {
                    @Override
                    public String getDeviceDisplayText(String ip) {
                        return MainFrame.this.getDeviceDisplayText(ip);
                    }
                }
        );

        transferPanel = new Transferring(font);

        settingsPanel = new Setting(font);
        settingsPanel.setReceivePathText(AppSettings.getReceiveDir());

        cardLayout = new CardLayout();

        contentPanel = new JPanel(cardLayout);

        contentPanel.add(devicePanel, "device");
        contentPanel.add(transferPanel, "transfer");
        contentPanel.add(settingsPanel, "settings");

        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        bindNavigationActions();

        bindDeviceActions();

        bindTransferActions();

        bindSettingsActions();

        startTcpServer();

        startUdpDiscovery();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                UdpListener.stopListening();
                TcpServer.stopServer();
                transferManager.shutdown();
            }
        });

        setVisible(true);

        showDeviceStatus("LinkDisk 已启动，等待设备发现。");
    }

    private void bindNavigationActions() {

        sidebarPanel.getDevicePageButton().addActionListener(e -> {
            cardLayout.show(contentPanel, "device");
            sidebarPanel.setActivePage("device");
        });

        sidebarPanel.getTransferPageButton().addActionListener(e -> {
            cardLayout.show(contentPanel, "transfer");
            sidebarPanel.setActivePage("transfer");
        });

        sidebarPanel.getSettingsPageButton().addActionListener(e -> {
            cardLayout.show(contentPanel, "settings");
            sidebarPanel.setActivePage("settings");
        });
    }

    private void bindDeviceActions() {

        devicePanel.getConnectButton().addActionListener(e -> connectSelectedDevice());

        devicePanel.getDisconnectButton().addActionListener(e -> disconnectSelectedDevice());

        devicePanel.getAddIpButton().addActionListener(e -> addIpManually());

        devicePanel.getDeleteDeviceButton().addActionListener(e -> deleteSelectedDevice());

        devicePanel.getRefreshDeviceButton().addActionListener(e -> refreshDeviceList());
    }

    private void bindTransferActions() {

        transferPanel.getSelectButton().addActionListener(e -> selectFiles());

        transferPanel.getSendButton().addActionListener(e -> sendFiles());

        transferPanel.getOpenFolderButton().addActionListener(e -> openReceiveFolder());

        transferPanel.getClearSelectedButton().addActionListener(e -> resetSelectedFiles());

        transferPanel.getClearTaskButton().addActionListener(e -> clearTransferTasks());
    }

    private void bindSettingsActions() {

        settingsPanel.getTrustManagerButton().addActionListener(e -> manageTrustedDevices());

        settingsPanel.getChooseReceiveFolderButton().addActionListener(e -> chooseReceiveFolder());

        settingsPanel.getClearLogButton().addActionListener(e -> clearStatusMessage());
    }

    private void connectSelectedDevice() {

        String ip = devicePanel.getSelectedIp();

        if (ip == null) {
            showDeviceStatus("请先在可用设备列表中选择一个设备。");
            return;
        }

        showDeviceStatus("正在连接设备：" + displayIp(ip));

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

                            devicePanel.repaintDeviceList();

                            showDeviceStatus(
                                    "设备连接成功：" +
                                            deviceNameMap.get(ip) +
                                            " [" + displayIp(ip) + "] " +
                                            devicePlatformMap.get(ip)
                            );

                        } else {
                            showDeviceStatus(
                                    "设备连接失败：" +
                                            displayIp(ip) +
                                            "，原因：" +
                                            result.message
                            );
                        }
                    }
                });
            }
        }).start();
    }

    private void disconnectSelectedDevice() {

        String ip = devicePanel.getSelectedIp();

        if (ip == null) {
            showDeviceStatus("请先选择要断开的设备。");
            return;
        }

        if (connectedDevices.contains(ip)) {
            connectedDevices.remove(ip);
            showDeviceStatus("已断开设备：" + displayIp(ip));
        } else {
            showDeviceStatus("该设备当前未连接：" + displayIp(ip));
        }

        devicePanel.repaintDeviceList();
    }

    private void addIpManually() {

        String ip = JOptionPane.showInputDialog(
                MainFrame.this,
                "请输入目标设备 IP 地址：",
                "手动添加 IP",
                JOptionPane.PLAIN_MESSAGE
        );

        if (ip == null) {
            return;
        }

        ip = ip.trim();

        if (ip.length() == 0) {
            showDeviceStatus("IP 地址不能为空。");
            return;
        }

        if (UdpListener.isLocalIp(ip)) {
            deviceNameMap.put(ip, UdpListener.getThisDeviceName() + "（本机）");
            devicePlatformMap.put(ip, UdpListener.getThisPlatform());
        } else {
            deviceNameMap.put(ip, "手动添加设备");
            devicePlatformMap.put(ip, "未知平台");
        }

        DefaultListModel<String> model = devicePanel.getDeviceListModel();

        if (!model.contains(ip)) {
            model.addElement(ip);

            showDeviceStatus(
                    "已手动添加设备：" +
                            deviceNameMap.get(ip) +
                            " [" + displayIp(ip) + "] " +
                            devicePlatformMap.get(ip)
            );
        } else {
            showDeviceStatus("设备已存在：" + displayIp(ip));
        }

        devicePanel.repaintDeviceList();
    }

    private void deleteSelectedDevice() {

        String ip = devicePanel.getSelectedIp();

        if (ip == null) {
            showDeviceStatus("请先选择要删除的设备。");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                MainFrame.this,
                "确定从列表中删除设备 " + displayIp(ip) + " 吗？\n这不会删除信任关系。",
                "删除设备",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {

            DefaultListModel<String> model = devicePanel.getDeviceListModel();

            model.removeElement(ip);

            connectedDevices.remove(ip);
            deviceNameMap.remove(ip);
            devicePlatformMap.remove(ip);

            showDeviceStatus("已从列表删除设备：" + displayIp(ip));

            devicePanel.repaintDeviceList();
        }
    }

    private void refreshDeviceList() {

        int confirm = JOptionPane.showConfirmDialog(
                MainFrame.this,
                "确定刷新设备列表吗？\n当前列表和连接状态会被清空。",
                "刷新设备列表",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {

            devicePanel.getDeviceListModel().clear();

            connectedDevices.clear();
            deviceNameMap.clear();
            devicePlatformMap.clear();

            showDeviceStatus("已刷新设备列表，等待重新发现设备。");
        }
    }

    private void selectFiles() {

        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setMultiSelectionEnabled(true);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int result = fileChooser.showOpenDialog(MainFrame.this);

        if (result == JFileChooser.APPROVE_OPTION) {

        	File[] selectedRoots = fileChooser.getSelectedFiles();

        	for (File root : selectedRoots) {
        	    selectedRootMap.put(root.getAbsolutePath(), root);
        	}

        	selectedItems = buildTransferItems(
        	        selectedRootMap.values().toArray(new File[0])
        	);

        	updateSelectedFilesDisplay();

        	if (selectedItems.length == 0) {
        	    showTransferStatus("未发现可发送文件。空文件夹暂不传输。");
        	} else {
        	    showTransferStatus("当前实际待发送 " + selectedItems.length + " 个文件。");
        	}
        }
    }

    private void sendFiles() {

        if (selectedItems == null || selectedItems.length == 0) {
            showTransferStatus("请先选择文件或文件夹。");
            return;
        }

        java.util.List<String> selectedIps =
                devicePanel.getSelectedIps();

        if (selectedIps.isEmpty()) {
            showTransferStatus("请先回到设备连接页，选择目标设备。");
            return;
        }

        for (String ip : selectedIps) {
            if (!connectedDevices.contains(ip)) {
                showTransferStatus(
                        "设备尚未连接，请先连接设备：" +
                                displayIp(ip)
                );
                return;
            }
        }

        TransferFileItem[] itemsToSend = selectedItems.clone();

        transferPanel.setProgress(0);

        showTransferStatus("开始发送文件，共 " + itemsToSend.length + " 个。");

        Map<String, Integer> rowIndexMap =
                new HashMap<String, Integer>();

        for (String ip : selectedIps) {
            for (int i = 0; i < itemsToSend.length; i++) {

                TransferFileItem item = itemsToSend[i];

                TransferTask task =
                        new TransferTask(
                                ip,
                                item.getRelativePath(),
                                item.getSize(),
                                "upload"
                        );

                transferManager.addTask(task);

                int rowIndex = addTransferRow(
                        "发送",
                        item.getRelativePath(),
                        ip,
                        item.getSize(),
                        "等待",
                        0
                );

                rowIndexMap.put(ip + "|" + i, rowIndex);
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                for (String ip : selectedIps) {

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showTransferStatus("正在发送到设备：" + displayIp(ip));
                        }
                    });

                    TcpClient.SendResult sendResult = TcpClient.sendFiles(
                            itemsToSend,
                            ip,
                            new ProgressListener() {

                                @Override
                                public void onTotalProgress(int progress) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            transferPanel.setProgress(progress);
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
                                public void onFileProgress(
                                        int fileIndex,
                                        String fileName,
                                        int progress
                                ) {
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
                                showTransferStatus("设备 " + displayIp(ip) + " 文件发送完成。");
                            } else {
                                for (int i = 0; i < itemsToSend.length; i++) {
                                    Integer rowIndex = rowIndexMap.get(ip + "|" + i);

                                    if (rowIndex != null) {
                                        updateTransferRow(rowIndex, "失败", 0);
                                    }
                                }

                                showTransferStatus(
                                        "设备 " +
                                                displayIp(ip) +
                                                " 文件发送失败，原因：" +
                                                sendResult.message
                                );
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void openReceiveFolder() {

        try {
            File folder = new File(AppSettings.getReceiveDir());

            if (!folder.exists()) {
                folder.mkdirs();
            }

            java.awt.Desktop.getDesktop().open(folder);

        } catch (Exception ex) {
            ex.printStackTrace();
            showTransferStatus("打开接收文件夹失败。");
        }
    }

    private void resetSelectedFiles() {

        selectedItems = null;

        selectedRootMap.clear();

        transferPanel.setProgress(0);

        transferPanel.clearSelectedFilesText();

        showTransferStatus("已重置当前选择的文件。");
    }

    private void clearTransferTasks() {

        int confirm = JOptionPane.showConfirmDialog(
                MainFrame.this,
                "确定清空传输任务列表吗？",
                "清空任务",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            transferPanel.clearTasks();
            receiveRowIndexMap.clear();
            showTransferStatus("已清空传输任务。");
        }
    }

    private void manageTrustedDevices() {

        AuthManager authManager = new AuthManager();

        java.util.List<String> trusted =
                authManager.getAllTrustedDevices();

        if (trusted.isEmpty()) {

            JOptionPane.showMessageDialog(
                    MainFrame.this,
                    "暂无信任设备",
                    "管理信任设备",
                    JOptionPane.INFORMATION_MESSAGE
            );

            showSettingsStatus("暂无信任设备。");

            return;
        }

        JList<String> trustedList = new JList<String>(
                trusted.toArray(new String[0])
        );

        trustedList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        trustedList.setCellRenderer(new DefaultListCellRenderer() {
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

                if (deviceName == null) {
                    deviceName = "信任设备";
                }

                label.setText(deviceName + " [" + displayIp(ip) + "]");
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(trustedList);
        scrollPane.setPreferredSize(new java.awt.Dimension(360, 180));

        int option = JOptionPane.showConfirmDialog(
                MainFrame.this,
                scrollPane,
                "管理信任设备：选择一个设备后点击确定删除信任",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {

            String selectedIp = trustedList.getSelectedValue();

            if (selectedIp == null) {
                showSettingsStatus("未选择信任设备。");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    MainFrame.this,
                    "确定删除信任设备 " + displayIp(selectedIp) + " 吗？\n下次连接需要重新授权。",
                    "确认删除信任",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {

                authManager.removeTrustedDevice(selectedIp);

                connectedDevices.remove(selectedIp);

                devicePanel.repaintDeviceList();

                showSettingsStatus("已删除信任设备：" + displayIp(selectedIp));
            }
        }
    }


    private void chooseReceiveFolder() {

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("选择接收文件保存目录");

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        File currentFolder = new File(AppSettings.getReceiveDir());

        if (currentFolder.exists()) {
            chooser.setCurrentDirectory(currentFolder);
        }

        int result = chooser.showOpenDialog(MainFrame.this);

        if (result == JFileChooser.APPROVE_OPTION) {

            File selectedFolder = chooser.getSelectedFile();

            AppSettings.setReceiveDir(selectedFolder.getAbsolutePath());

            settingsPanel.setReceivePathText(AppSettings.getReceiveDir());

            showSettingsStatus("接收目录已更新：" + AppSettings.getReceiveDir());
        }
    }

    private void clearStatusMessage() {
        settingsPanel.clearLog();
    }

    private void startTcpServer() {

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
                                        showDeviceStatus("已授权设备：" + displayIp(ip));
                                    } else {
                                        showDeviceStatus("拒绝设备连接：" + displayIp(ip));
                                    }

                                    devicePanel.repaintDeviceList();
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

                                showTransferStatus(
                                        "开始接收文件：" +
                                                displayText(fileName) +
                                                "，来自 " +
                                                displayIp(clientIp)
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

                                showTransferStatus(
                                        "收到文件：" +
                                                displayText(fileName) +
                                                "，大小 " +
                                                displayText(formatFileSize(fileSize))
                                );
                            }
                        });
                    }
                }
        );
    }

    private void startUdpDiscovery() {

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

                        DefaultListModel<String> model = devicePanel.getDeviceListModel();

                        if (!model.contains(ip)) {
                            model.addElement(ip);

                            showDeviceStatus(
                                    "发现设备：" +
                                            deviceName +
                                            " [" + displayIp(ip) + "] " +
                                            platform
                            );
                        } else {
                            devicePanel.repaintDeviceList();
                        }
                    }
                });
            }
        });
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
