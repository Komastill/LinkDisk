package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

public class Device extends JPanel {

    public interface DeviceDisplayProvider {
        String getDeviceDisplayText(String ip);
    }

    private DefaultListModel<String> deviceListModel;
    private JList<String> deviceList;

    private JButton connectButton;
    private JButton disconnectButton;
    private JButton addIpButton;
    private JButton copyLocalIpButton;
    private JButton deleteDeviceButton;
    private JButton refreshDeviceButton;

    private JTextArea localInfoArea;
    private JTextArea statusArea;

    public Device(Font font, DeviceDisplayProvider displayProvider) {

        setLayout(new BorderLayout(20, 20));
        setBackground(UiStyle.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("设备连接");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(UiStyle.TEXT);

        JLabel subtitleLabel = new JLabel("发现局域网设备，查看本机网络信息，建立可信连接");
        subtitleLabel.setFont(font.deriveFont(Font.PLAIN, 15f));
        subtitleLabel.setForeground(UiStyle.SUBTEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 0));
        mainPanel.setOpaque(false);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 16));
        leftPanel.setOpaque(false);

        JPanel localCard = createCardPanel();
        localCard.setLayout(new BorderLayout(14, 10));
        localCard.setPreferredSize(new Dimension(0, 142));

        JPanel localHeader = new JPanel(new BorderLayout());
        localHeader.setOpaque(false);

        JLabel localTitle = new JLabel("本机信息");
        localTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        localTitle.setForeground(UiStyle.TEXT);

        copyLocalIpButton = UiStyle.createCompactButton("复制 IP", font, 94, 34);

        localHeader.add(localTitle, BorderLayout.WEST);
        localHeader.add(copyLocalIpButton, BorderLayout.EAST);

        localInfoArea = new JTextArea();
        localInfoArea.setFont(font.deriveFont(Font.PLAIN, 14f));
        localInfoArea.setForeground(UiStyle.SUBTEXT);
        localInfoArea.setEditable(false);
        localInfoArea.setOpaque(false);
        localInfoArea.setLineWrap(true);
        localInfoArea.setWrapStyleWord(true);
        localInfoArea.setText("设备名：读取中...\n平台：读取中...\n本机 IP：读取中...");

        localCard.add(localHeader, BorderLayout.NORTH);
        localCard.add(localInfoArea, BorderLayout.CENTER);

        JPanel listCard = createCardPanel();
        listCard.setLayout(new BorderLayout(12, 12));

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);

        JLabel listTitle = new JLabel("可用设备");
        listTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        listTitle.setForeground(UiStyle.TEXT);

        JLabel listTip = new JLabel("同一局域网内运行 LinkDisk 的设备会显示在这里");
        listTip.setFont(font.deriveFont(Font.PLAIN, 13f));
        listTip.setForeground(UiStyle.SUBTEXT);

        listHeader.add(listTitle, BorderLayout.NORTH);
        listHeader.add(listTip, BorderLayout.SOUTH);

        deviceListModel = new DefaultListModel<String>();
        deviceList = new JList<String>(deviceListModel);
        deviceList.setFont(font.deriveFont(Font.BOLD, 15f));
        deviceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        deviceList.setBackground(UiStyle.SOFT_BG);
        deviceList.setFixedCellHeight(48);
        deviceList.setPrototypeCellValue(
                "MacBook-Air-7.local（本机） [192.168.100.100] macOS 已连接 已信任"
        );

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
                label.setText(displayProvider.getDeviceDisplayText(ip));
                label.setFont(font.deriveFont(Font.BOLD, 15f));
                label.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                label.setOpaque(true);

                if (isSelected) {
                    label.setBackground(UiStyle.PRIMARY_SOFT);
                    label.setForeground(UiStyle.TEXT);
                } else {
                    label.setBackground(UiStyle.SOFT_BG);
                    label.setForeground(UiStyle.TEXT);
                }

                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(deviceList);
        scrollPane.setPreferredSize(new Dimension(620, 330));
        scrollPane.setBorder(BorderFactory.createLineBorder(UiStyle.BORDER));

        listCard.add(listHeader, BorderLayout.NORTH);
        listCard.add(scrollPane, BorderLayout.CENTER);

        leftPanel.add(localCard, BorderLayout.NORTH);
        leftPanel.add(listCard, BorderLayout.CENTER);

        JPanel actionCard = createCardPanel();
        actionCard.setLayout(new BorderLayout());
        actionCard.setPreferredSize(new Dimension(270, 0));

        JPanel actionContent = new JPanel();
        actionContent.setOpaque(false);
        actionContent.setLayout(new BoxLayout(actionContent, BoxLayout.Y_AXIS));

        JLabel actionTitle = new JLabel("设备操作");
        actionTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        actionTitle.setForeground(UiStyle.TEXT);
        actionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(230, 260));

        JPanel buttonColumn = new JPanel();
        buttonColumn.setOpaque(false);
        buttonColumn.setLayout(new BoxLayout(buttonColumn, BoxLayout.Y_AXIS));

        connectButton = createActionButton("连接设备", font);
        disconnectButton = createActionButton("断开设备", font);
        addIpButton = createActionButton("手动添加", font);
        deleteDeviceButton = createActionButton("删除设备", font);
        refreshDeviceButton = createActionButton("刷新列表", font);

        addButtonWithGap(buttonColumn, connectButton, 0);
        addButtonWithGap(buttonColumn, disconnectButton, 10);
        addButtonWithGap(buttonColumn, addIpButton, 10);
        addButtonWithGap(buttonColumn, deleteDeviceButton, 10);
        addButtonWithGap(buttonColumn, refreshDeviceButton, 10);

        buttonPanel.add(buttonColumn);

        JLabel statusTitle = new JLabel("状态提示");
        statusTitle.setFont(font.deriveFont(Font.BOLD, 13f));
        statusTitle.setForeground(UiStyle.PRIMARY_DARK);
        statusTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusArea = new JTextArea();
        statusArea.setFont(font.deriveFont(Font.BOLD, 14f));
        statusArea.setForeground(UiStyle.TEXT);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(true);
        statusArea.setBackground(new Color(244, 248, 253));
        statusArea.setText("LinkDisk 已启动，等待设备发现。");
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204, 221, 244)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setPreferredSize(new Dimension(218, 90));
        statusScrollPane.setMinimumSize(new Dimension(218, 90));
        statusScrollPane.setMaximumSize(new Dimension(218, 90));
        statusScrollPane.setBorder(null);
        statusScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        actionContent.add(actionTitle);
        actionContent.add(Box.createVerticalStrut(18));
        actionContent.add(buttonPanel);
        actionContent.add(Box.createVerticalStrut(22));
        actionContent.add(statusTitle);
        actionContent.add(Box.createVerticalStrut(8));
        actionContent.add(statusScrollPane);

        actionCard.add(actionContent, BorderLayout.NORTH);

        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(actionCard, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void addButtonWithGap(JPanel panel, JButton button, int gap) {
        if (gap > 0) {
            panel.add(Box.createVerticalStrut(gap));
        }

        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(button);
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        UiStyle.setPanelCardStyle(panel);
        return panel;
    }

    private JButton createActionButton(String text, Font font) {
        return UiStyle.createActionButton(text, font, 190, 44);
    }

    public void setLocalInfoText(String text) {
        if (text == null || text.trim().length() == 0) {
            text = "设备信息暂不可用。";
        }

        localInfoArea.setText(text);
        localInfoArea.setCaretPosition(0);
    }

    public void setStatusMessage(String message) {
        if (message == null) {
            message = "";
        }

        statusArea.setText(message);
        statusArea.setCaretPosition(0);
    }

    public DefaultListModel<String> getDeviceListModel() {
        return deviceListModel;
    }

    public JList<String> getDeviceList() {
        return deviceList;
    }

    public JButton getConnectButton() {
        return connectButton;
    }

    public JButton getDisconnectButton() {
        return disconnectButton;
    }

    public JButton getAddIpButton() {
        return addIpButton;
    }

    public JButton getCopyLocalIpButton() {
        return copyLocalIpButton;
    }

    public JButton getDeleteDeviceButton() {
        return deleteDeviceButton;
    }

    public JButton getRefreshDeviceButton() {
        return refreshDeviceButton;
    }

    public String getSelectedIp() {
        return deviceList.getSelectedValue();
    }

    public java.util.List<String> getSelectedIps() {
        return deviceList.getSelectedValuesList();
    }

    public void repaintDeviceList() {
        deviceList.repaint();
    }
}
