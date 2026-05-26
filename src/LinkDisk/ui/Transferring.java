package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableModel;

public class Transferring extends JPanel {

    public interface FileDropListener {
        void onFilesDropped(List<File> files);
    }

    private FileDropListener fileDropListener;

    private JButton selectButton;
    private JButton sendButton;
    private JButton manageSelectedButton;
    private JButton openFolderButton;
    private JButton clearSelectedButton;
    private JButton cancelTaskButton;
    private JButton clearTaskButton;

    private JTable transferTable;
    private DefaultTableModel transferTableModel;

    private JTextArea selectedFilesArea;

    private JProgressBar progressBar;

    private JTextArea statusArea;
    
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

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JPanel firstButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        firstButtonRow.setOpaque(false);

        JPanel secondButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        secondButtonRow.setOpaque(false);
        secondButtonRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        selectButton = createActionButton("选择文件", font, false, 128);
        sendButton = createActionButton("发送文件", font, true, 128);
        manageSelectedButton = createActionButton("管理已选", font, false, 128);
        openFolderButton = createActionButton("打开接收文件夹", font, false, 170);

        clearSelectedButton = createActionButton("重置选择", font, false, 128);
        cancelTaskButton = createActionButton("取消任务", font, false, 128);
        clearTaskButton = createActionButton("清空任务", font, false, 128);

        firstButtonRow.add(selectButton);
        firstButtonRow.add(sendButton);
        firstButtonRow.add(manageSelectedButton);
        firstButtonRow.add(openFolderButton);

        secondButtonRow.add(clearSelectedButton);
        secondButtonRow.add(cancelTaskButton);
        secondButtonRow.add(clearTaskButton);

        buttonPanel.add(firstButtonRow);
        buttonPanel.add(secondButtonRow);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);

        headerPanel.setPreferredSize(new Dimension(0, 170));
        
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
        selectedFilesArea.setText("暂无已选文件。也可以把文件或文件夹拖到这里。");
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

        FileTransferHandler fileTransferHandler = new FileTransferHandler();

        setTransferHandler(fileTransferHandler);
        tableCard.setTransferHandler(fileTransferHandler);
        selectedFilesArea.setTransferHandler(fileTransferHandler);
        selectedScrollPane.setTransferHandler(fileTransferHandler);
        transferTable.setTransferHandler(fileTransferHandler);
        tableScrollPane.setTransferHandler(fileTransferHandler);

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

        statusArea = new JTextArea();
        statusArea.setFont(font.deriveFont(Font.PLAIN, 14f));
        statusArea.setForeground(SUBTEXT);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(true);
        statusArea.setBackground(Color.WHITE);
        statusArea.setText("请选择文件并连接目标设备。");
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setPreferredSize(new Dimension(420, 70));

        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(statusScrollPane, BorderLayout.EAST);
        
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
            selectedFilesArea.setText("暂无已选文件。也可以把文件或文件夹拖到这里。");
        } else {
            selectedFilesArea.setText(text);
            selectedFilesArea.setCaretPosition(0);
        }
    }

    public void clearSelectedFilesText() {
        selectedFilesArea.setText("暂无已选文件。也可以把文件或文件夹拖到这里。");
    }

    public void setStatusMessage(String message) {
        if (message == null) {
            message = "";
        }

        statusArea.setText(message);
        statusArea.setCaretPosition(0);
    }

    public JButton getSelectButton() {
        return selectButton;
    }

    public JButton getSendButton() {
        return sendButton;
    }
    
    public JButton getManageSelectedButton() {
        return manageSelectedButton;
    }

    public JButton getOpenFolderButton() {
        return openFolderButton;
    }

    public JButton getClearSelectedButton() {
        return clearSelectedButton;
    }

    public JButton getCancelTaskButton() {
        return cancelTaskButton;
    }
    
    public JButton getClearTaskButton() {
        return clearTaskButton;
    }

    public DefaultTableModel getTransferTableModel() {
        return transferTableModel;
    }

    public JTable getTransferTable() {
        return transferTable;
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

    public void setFileDropListener(FileDropListener fileDropListener) {
        this.fileDropListener = fileDropListener;
    }

    private class FileTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {

            if (!canImport(support)) {
                return false;
            }

            try {
                Transferable transferable =
                        support.getTransferable();

                Object data =
                        transferable.getTransferData(DataFlavor.javaFileListFlavor);

                if (data instanceof List) {

                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) data;

                    if (fileDropListener != null) {
                        fileDropListener.onFilesDropped(files);
                    }

                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }
    }
}
