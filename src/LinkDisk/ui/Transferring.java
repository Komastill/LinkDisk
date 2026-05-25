package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

public class Transferring extends JPanel {

    private JButton selectButton;
    private JButton sendButton;
    private JButton openFolderButton;
    private JButton clearSelectedButton;
    private JButton clearTaskButton;

    private JTable transferTable;
    private DefaultTableModel transferTableModel;

    private JTextArea selectedFilesArea;

    private JProgressBar progressBar;

    private JLabel statusLabel;

    private static final Color PAGE_BG = new Color(247, 250, 254);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(220, 228, 238);
    private static final Color TEXT = new Color(24, 38, 56);
    private static final Color SUBTEXT = new Color(95, 111, 132);
    private static final Color PRIMARY = new Color(67, 126, 202);

    public Transferring(Font font) {

        setLayout(new BorderLayout(20, 20));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("文件传输");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel("选择文件并发送，实时查看每个任务的传输状态");
        subtitleLabel.setFont(font.deriveFont(Font.PLAIN, 15f));
        subtitleLabel.setForeground(SUBTEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        selectButton = createActionButton("选择文件", font, false, 128);
        sendButton = createActionButton("发送文件", font, true, 128);
        openFolderButton = createActionButton("打开接收文件夹", font, false, 170);
        clearSelectedButton = createActionButton("重置选择", font, false, 128);
        clearTaskButton = createActionButton("清空任务", font, false, 128);

        buttonPanel.add(selectButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(openFolderButton);
        buttonPanel.add(clearSelectedButton);
        buttonPanel.add(clearTaskButton);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        JPanel tableCard = createCardPanel();
        tableCard.setLayout(new BorderLayout(12, 12));

        JPanel topInfoPanel = new JPanel();
        topInfoPanel.setOpaque(false);
        topInfoPanel.setLayout(new BoxLayout(topInfoPanel, BoxLayout.Y_AXIS));

        JLabel selectedTitle = new JLabel("已选文件");
        selectedTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        selectedTitle.setForeground(TEXT);
        selectedTitle.setAlignmentX(LEFT_ALIGNMENT);

        selectedFilesArea = new JTextArea();
        selectedFilesArea.setFont(font.deriveFont(Font.PLAIN, 14f));
        selectedFilesArea.setForeground(SUBTEXT);
        selectedFilesArea.setEditable(false);
        selectedFilesArea.setLineWrap(true);
        selectedFilesArea.setWrapStyleWord(true);
        selectedFilesArea.setText("暂无已选文件。");
        selectedFilesArea.setBackground(new Color(249, 251, 254));
        selectedFilesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JScrollPane selectedScrollPane = new JScrollPane(selectedFilesArea);
        selectedScrollPane.setPreferredSize(new Dimension(0, 88));
        selectedScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tableTitle = new JLabel("传输任务");
        tableTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        tableTitle.setForeground(TEXT);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        tableTitle.setAlignmentX(LEFT_ALIGNMENT);

        topInfoPanel.add(selectedTitle);
        topInfoPanel.add(selectedScrollPane);
        topInfoPanel.add(tableTitle);

        transferTableModel = new DefaultTableModel(
                new Object[]{"方向", "文件名", "对方设备", "大小", "状态", "进度"},
                0
        );

        transferTable = new JTable(transferTableModel);
        transferTable.setFont(font.deriveFont(Font.PLAIN, 14f));
        transferTable.setRowHeight(30);
        transferTable.getTableHeader().setFont(font.deriveFont(Font.BOLD, 14f));

        JScrollPane tableScrollPane = new JScrollPane(transferTable);

        tableCard.add(topInfoPanel, BorderLayout.NORTH);
        tableCard.add(tableScrollPane, BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(16, 0));
        bottomPanel.setOpaque(false);

        progressBar = new JProgressBar();
        progressBar.setFont(font);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 26));

        statusLabel = new JLabel("请选择文件并连接目标设备。");
        statusLabel.setFont(font.deriveFont(Font.PLAIN, 14f));
        statusLabel.setForeground(SUBTEXT);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        statusLabel.setPreferredSize(new Dimension(360, 44));

        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
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

    private JButton createActionButton(String text, Font font, boolean primary, int width) {
        JButton button = new JButton(text);

        button.setFont(font.deriveFont(Font.BOLD, 14f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(width, 40));

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

    public void setSelectedFilesText(String text) {
        if (text == null || text.trim().length() == 0) {
            selectedFilesArea.setText("暂无已选文件。");
        } else {
            selectedFilesArea.setText(text);
            selectedFilesArea.setCaretPosition(0);
        }
    }

    public void clearSelectedFilesText() {
        selectedFilesArea.setText("暂无已选文件。");
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    public JButton getSelectButton() {
        return selectButton;
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public JButton getOpenFolderButton() {
        return openFolderButton;
    }

    public JButton getClearSelectedButton() {
        return clearSelectedButton;
    }

    public JButton getClearTaskButton() {
        return clearTaskButton;
    }

    public DefaultTableModel getTransferTableModel() {
        return transferTableModel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public int addTransferRow(
            String direction,
            String fileName,
            String deviceIp,
            String fileSize,
            String status,
            int progress
    ) {
        int rowIndex = transferTableModel.getRowCount();

        transferTableModel.addRow(
                new Object[]{
                        direction,
                        fileName,
                        deviceIp,
                        fileSize,
                        status,
                        progress + "%"
                }
        );

        return rowIndex;
    }

    public void updateTransferRow(int rowIndex, String status, int progress) {
        if (rowIndex < 0 || rowIndex >= transferTableModel.getRowCount()) {
            return;
        }

        transferTableModel.setValueAt(status, rowIndex, 4);
        transferTableModel.setValueAt(progress + "%", rowIndex, 5);
    }

    public void clearTasks() {
        transferTableModel.setRowCount(0);
        progressBar.setValue(0);
    }

    public void setProgress(int progress) {
        progressBar.setValue(progress);
    }
}
