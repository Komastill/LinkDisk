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
import javax.swing.Box;
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
    private JButton clearSelectedButton;
    private JButton cancelTaskButton;
    private JButton clearTaskButton;

    private JTable transferTable;
    private DefaultTableModel transferTableModel;

    private JTextArea selectedFilesArea;
    private JScrollPane selectedScrollPane;
    private JLabel dropHintLabel;

    private JProgressBar progressBar;

    private JTextArea statusArea;
    
    private static final Color PAGE_BG = UiStyle.PAGE_BG;
    private static final Color CARD_BG = UiStyle.CARD_BG;
    private static final Color BORDER = UiStyle.BORDER;
    private static final Color TEXT = UiStyle.TEXT;
    private static final Color SUBTEXT = UiStyle.SUBTEXT;

    public Transferring(Font font) {

        setLayout(new BorderLayout(20, 20));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("文件传输");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel("选择或拖拽文件，确认待发送列表后再开始传输");
        subtitleLabel.setFont(font.deriveFont(Font.PLAIN, 15f));
        subtitleLabel.setForeground(SUBTEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        selectButton = createActionButton("选择文件", font, 128);
        manageSelectedButton = createActionButton("管理待发送", font, 140);
        clearSelectedButton = createActionButton("重置选择", font, 128);
        cancelTaskButton = createActionButton("取消任务", font, 128);
        clearTaskButton = createActionButton("清空任务", font, 128);

        buttonPanel.add(selectButton);
        buttonPanel.add(manageSelectedButton);
        buttonPanel.add(clearSelectedButton);
        buttonPanel.add(cancelTaskButton);
        buttonPanel.add(clearTaskButton);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);

        headerPanel.setPreferredSize(new Dimension(0, 120));
        
        add(headerPanel, BorderLayout.NORTH);

        JPanel tableCard = createCardPanel();
        tableCard.setLayout(new BorderLayout(12, 12));

        JPanel topInfoPanel = new JPanel();
        topInfoPanel.setOpaque(false);
        topInfoPanel.setLayout(new BoxLayout(topInfoPanel, BoxLayout.Y_AXIS));

        JPanel selectedHeaderPanel = new JPanel(new BorderLayout(12, 0));
        selectedHeaderPanel.setOpaque(false);
        selectedHeaderPanel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel selectedTitle = new JLabel("待发送文件");
        selectedTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        selectedTitle.setForeground(TEXT);

        sendButton = UiStyle.createPrimaryButton("发送文件", font, 180, 46);
        selectedHeaderPanel.add(selectedTitle, BorderLayout.WEST);
        selectedHeaderPanel.add(sendButton, BorderLayout.EAST);

        dropHintLabel = new JLabel("拖拽文件或文件夹到下方区域，确认列表后点击右侧“发送文件”。");
        dropHintLabel.setFont(font.deriveFont(Font.PLAIN, 13f));
        dropHintLabel.setForeground(SUBTEXT);
        dropHintLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        dropHintLabel.setAlignmentX(LEFT_ALIGNMENT);

        selectedFilesArea = new JTextArea();
        selectedFilesArea.setFont(font.deriveFont(Font.PLAIN, 14f));
        selectedFilesArea.setForeground(SUBTEXT);
        selectedFilesArea.setEditable(false);
        selectedFilesArea.setLineWrap(true);
        selectedFilesArea.setWrapStyleWord(true);
        selectedFilesArea.setText("暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。");
        selectedFilesArea.setBackground(UiStyle.SOFT_BG);
        selectedFilesArea.setBorder(UiStyle.createDropNormalBorder());

        selectedScrollPane = new JScrollPane(selectedFilesArea);
        selectedScrollPane.setPreferredSize(new Dimension(0, 90));
        selectedScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tableTitle = new JLabel("传输任务");
        tableTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        tableTitle.setForeground(TEXT);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        tableTitle.setAlignmentX(LEFT_ALIGNMENT);

        topInfoPanel.add(selectedHeaderPanel);
        topInfoPanel.add(dropHintLabel);
        topInfoPanel.add(selectedScrollPane);
        topInfoPanel.add(tableTitle);

        transferTableModel = new DefaultTableModel(
                new Object[]{"方向", "文件名", "对方设备", "大小", "状态", "进度"},
                0
        );

        transferTable = new JTable(transferTableModel);
        transferTable.setFont(font.deriveFont(Font.PLAIN, 14f));
        transferTable.setRowHeight(30);
        transferTable.setGridColor(new Color(235, 241, 248));
        transferTable.setSelectionBackground(UiStyle.PRIMARY_SOFT);
        transferTable.setSelectionForeground(UiStyle.TEXT);
        transferTable.getTableHeader().setFont(font.deriveFont(Font.BOLD, 14f));
        transferTable.getTableHeader().setBackground(new Color(244, 248, 253));
        transferTable.getTableHeader().setForeground(UiStyle.TEXT);

        JScrollPane tableScrollPane = new JScrollPane(transferTable);

        FileTransferHandler fileTransferHandler = new FileTransferHandler();

        selectedFilesArea.setTransferHandler(fileTransferHandler);
        selectedScrollPane.setTransferHandler(fileTransferHandler);

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

        JPanel statusPanel = new JPanel(new BorderLayout(0, 6));
        statusPanel.setOpaque(false);
        statusPanel.setPreferredSize(new Dimension(440, 88));

        JLabel statusTitle = new JLabel("操作提示");
        statusTitle.setFont(font.deriveFont(Font.BOLD, 13f));
        statusTitle.setForeground(UiStyle.PRIMARY_DARK);

        statusArea = new JTextArea();
        statusArea.setFont(font.deriveFont(Font.BOLD, 14f));
        statusArea.setForeground(UiStyle.TEXT);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(true);
        statusArea.setBackground(new Color(244, 248, 253));
        statusArea.setText("请选择文件并连接目标设备。");
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204, 221, 244)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setPreferredSize(new Dimension(440, 62));
        statusScrollPane.setMinimumSize(new Dimension(440, 62));
        statusScrollPane.setMaximumSize(new Dimension(440, 62));
        statusScrollPane.setBorder(null);

        statusPanel.add(statusTitle, BorderLayout.NORTH);
        statusPanel.add(statusScrollPane, BorderLayout.CENTER);

        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.EAST);
        
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

    private JButton createActionButton(String text, Font font, int width) {
        return UiStyle.createActionButton(text, font, width, 40);
    }

    private String textBeforeDrag;

    private void setDropHighlight(boolean active) {
        if (selectedFilesArea == null) {
            return;
        }

        if (active) {
            selectedFilesArea.setBackground(new Color(232, 243, 255));
            selectedFilesArea.setBorder(UiStyle.createDropActiveBorder());

            if (dropHintLabel != null) {
                dropHintLabel.setText("已识别拖拽文件，松开鼠标即可加入待发送列表。");
                dropHintLabel.setForeground(UiStyle.PRIMARY_DARK);
            }

            String currentText = selectedFilesArea.getText();

            if (textBeforeDrag == null) {
                textBeforeDrag = currentText;
            }

            if (currentText == null
                    || currentText.startsWith("暂无待发送文件")
                    || currentText.startsWith("松开鼠标")) {
                selectedFilesArea.setText("松开鼠标即可添加文件或文件夹...\n\n提示：只有拖到这个待发送文件区域才会加入发送列表。");
            }
        } else {
            selectedFilesArea.setBackground(UiStyle.SOFT_BG);
            selectedFilesArea.setBorder(UiStyle.createDropNormalBorder());

            if (dropHintLabel != null) {
                dropHintLabel.setText("拖拽文件或文件夹到下方区域，确认列表后点击右侧“发送文件”。");
                dropHintLabel.setForeground(SUBTEXT);
            }

            String currentText = selectedFilesArea.getText();

            if (currentText == null || currentText.startsWith("松开鼠标")) {
                if (textBeforeDrag != null && textBeforeDrag.trim().length() > 0) {
                    selectedFilesArea.setText(textBeforeDrag);
                } else {
                    selectedFilesArea.setText("暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。");
                }
            }

            textBeforeDrag = null;
        }
    }

    public void setSelectedFilesText(String text) {
        textBeforeDrag = null;

        if (text == null || text.trim().length() == 0) {
            selectedFilesArea.setText("暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。");
        } else {
            selectedFilesArea.setText(text);
            selectedFilesArea.setCaretPosition(0);
        }
    }

    public void clearSelectedFilesText() {
        textBeforeDrag = null;
        selectedFilesArea.setText("暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。");
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
            boolean supported = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            setDropHighlight(supported);
            return supported;
        }

        @Override
        public boolean importData(TransferSupport support) {

            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                setDropHighlight(false);
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

                    setDropHighlight(false);
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            setDropHighlight(false);
            return false;
        }
    }
}
