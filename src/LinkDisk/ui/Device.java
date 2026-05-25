package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private JButton deleteDeviceButton;
    private JButton refreshDeviceButton;

    private JLabel statusLabel;

    private static final Color PAGE_BG = new Color(247, 250, 254);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(220, 228, 238);
    private static final Color TEXT = new Color(24, 38, 56);
    private static final Color SUBTEXT = new Color(95, 111, 132);
    private static final Color PRIMARY = new Color(67, 126, 202);

    public Device(Font font, DeviceDisplayProvider displayProvider) {

        setLayout(new BorderLayout(20, 20));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("设备连接");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel("发现局域网设备，建立连接并管理设备状态");
        subtitleLabel.setFont(font.deriveFont(Font.PLAIN, 15f));
        subtitleLabel.setForeground(SUBTEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 0));
        mainPanel.setOpaque(false);

        JPanel listCard = createCardPanel();
        listCard.setLayout(new BorderLayout(12, 12));

        JLabel listTitle = new JLabel("可用设备");
        listTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        listTitle.setForeground(TEXT);

        deviceListModel = new DefaultListModel<String>();
        deviceList = new JList<String>(deviceListModel);
        deviceList.setFont(font.deriveFont(Font.BOLD, 15f));
        deviceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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

                if (isSelected) {
                    label.setBackground(new Color(229, 240, 255));
                    label.setForeground(TEXT);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(TEXT);
                }

                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(deviceList);
        scrollPane.setPreferredSize(new Dimension(600, 360));

        listCard.add(listTitle, BorderLayout.NORTH);
        listCard.add(scrollPane, BorderLayout.CENTER);

        JPanel actionCard = createCardPanel();
        actionCard.setLayout(new BorderLayout(0, 16));
        actionCard.setPreferredSize(new Dimension(300, 0));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridLayout(5, 1, 0, 14));

        connectButton = createActionButton("连接设备", font, true);
        disconnectButton = createActionButton("断开设备", font, false);
        addIpButton = createActionButton("手动添加", font, false);
        deleteDeviceButton = createActionButton("删除设备", font, false);
        refreshDeviceButton = createActionButton("刷新列表", font, false);

        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(addIpButton);
        buttonPanel.add(deleteDeviceButton);
        buttonPanel.add(refreshDeviceButton);

        statusLabel = new JLabel("请选择设备，或手动添加 IP。");
        statusLabel.setFont(font.deriveFont(Font.PLAIN, 14f));
        statusLabel.setForeground(SUBTEXT);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(249, 251, 254));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        actionCard.add(buttonPanel, BorderLayout.NORTH);
        actionCard.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(listCard, BorderLayout.CENTER);
        mainPanel.add(actionCard, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        return panel;
    }

    private JButton createActionButton(String text, Font font, boolean primary) {
        JButton button = new JButton(text);

        button.setFont(font.deriveFont(Font.BOLD, 15f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(250, 48));

        if (primary) {
            button.setBackground(new Color(231, 241, 255));
            button.setForeground(TEXT);
            button.setBorder(BorderFactory.createLineBorder(PRIMARY));
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(TEXT);
            button.setBorder(BorderFactory.createLineBorder(BORDER));
        }

        return button;
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(message);
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
