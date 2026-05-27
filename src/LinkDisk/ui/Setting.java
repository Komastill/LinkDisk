package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

public class Setting extends JPanel {

    private JButton trustManagerButton;
    private JButton chooseReceiveFolderButton;
    private JButton openReceiveFolderButton;

    private JLabel receivePathLabel;

    public Setting(Font font) {

        setLayout(new BorderLayout(20, 20));
        setBackground(UiStyle.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("传输设置");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(UiStyle.TEXT);

        JLabel subtitleLabel = new JLabel("先确认接收位置和可信设备，再进入文件传输页面发送文件");
        subtitleLabel.setFont(font.deriveFont(Font.PLAIN, 15f));
        subtitleLabel.setForeground(UiStyle.SUBTEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        JPanel mainCard = new JPanel(new BorderLayout(18, 18));
        UiStyle.setPanelCardStyle(mainCard);

        JPanel actionPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        actionPanel.setOpaque(false);

        trustManagerButton = createActionButton("管理信任设备", font, 190, 42);
        chooseReceiveFolderButton = createActionButton("选择接收目录", font, 190, 42);
        openReceiveFolderButton = createActionButton("打开接收文件夹", font, 190, 42);

        actionPanel.add(createSettingBlock(
                "信任设备",
                "查看已授权设备，必要时删除信任关系。",
                trustManagerButton,
                font
        ));

        actionPanel.add(createSettingBlock(
                "接收目录",
                "设置接收文件保存位置，避免文件散落。",
                chooseReceiveFolderButton,
                font
        ));

        actionPanel.add(createSettingBlock(
                "接收文件夹",
                "快速打开当前接收目录，查看已收到文件。",
                openReceiveFolderButton,
                font
        ));

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JPanel receivePanel = createReceivePathPanel(font);
        JPanel workflowPanel = createWorkflowPanel(font);

        receivePanel.setAlignmentX(LEFT_ALIGNMENT);
        workflowPanel.setAlignmentX(LEFT_ALIGNMENT);

        centerPanel.add(receivePanel);
        centerPanel.add(javax.swing.Box.createVerticalStrut(16));
        centerPanel.add(workflowPanel);

        mainCard.add(actionPanel, BorderLayout.NORTH);
        mainCard.add(centerPanel, BorderLayout.CENTER);

        add(mainCard, BorderLayout.CENTER);
    }

    private JPanel createSettingBlock(String title, String description, JButton button, Font font) {
        JPanel block = new JPanel(new BorderLayout(0, 12));
        block.setBackground(UiStyle.SOFT_BG);
        block.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyle.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(font.deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(UiStyle.TEXT);

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(font.deriveFont(Font.PLAIN, 13f));
        descLabel.setForeground(UiStyle.SUBTEXT);

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(descLabel, BorderLayout.CENTER);

        block.add(textPanel, BorderLayout.CENTER);
        block.add(button, BorderLayout.SOUTH);

        return block;
    }

    private JPanel createReceivePathPanel(Font font) {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setBackground(new Color(232, 243, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyle.PRIMARY),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JLabel titleLabel = new JLabel("当前接收目录");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 17f));
        titleLabel.setForeground(UiStyle.TEXT);

        receivePathLabel = new JLabel("received_files");
        receivePathLabel.setFont(font.deriveFont(Font.BOLD, 15f));
        receivePathLabel.setForeground(UiStyle.PRIMARY_DARK);

        JLabel hintLabel = new JLabel("收到的文件会保存在这里。需要修改位置时，点击上方“选择接收目录”。");
        hintLabel.setFont(font.deriveFont(Font.PLAIN, 13f));
        hintLabel.setForeground(UiStyle.SUBTEXT);

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(receivePathLabel, BorderLayout.CENTER);
        textPanel.add(hintLabel, BorderLayout.SOUTH);

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createWorkflowPanel(Font font) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyle.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        JLabel title = new JLabel("推荐操作流程");
        title.setFont(font.deriveFont(Font.BOLD, 17f));
        title.setForeground(UiStyle.TEXT);

        JPanel stepsPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        stepsPanel.setOpaque(false);

        stepsPanel.add(createStepCard("1", "连接设备", "在“设备连接”页面发现并连接目标设备。", font));
        stepsPanel.add(createStepCard("2", "确认设置", "确认接收目录，必要时管理信任设备。", font));
        stepsPanel.add(createStepCard("3", "选择并发送", "进入“文件传输”页面，拖拽或选择文件后发送。", font));

        panel.add(title, BorderLayout.NORTH);
        panel.add(stepsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStepCard(String number, String title, String description, Font font) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(UiStyle.SOFT_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyle.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(number + ". " + title);
        titleLabel.setFont(font.deriveFont(Font.BOLD, 14f));
        titleLabel.setForeground(UiStyle.TEXT);

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(font.deriveFont(Font.PLAIN, 12.5f));
        descLabel.setForeground(UiStyle.SUBTEXT);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createActionButton(String text, Font font, int width, int height) {
        return UiStyle.createActionButton(text, font, width, height);
    }

    public JButton getTrustManagerButton() {
        return trustManagerButton;
    }

    public JButton getChooseReceiveFolderButton() {
        return chooseReceiveFolderButton;
    }

    public JButton getOpenReceiveFolderButton() {
        return openReceiveFolderButton;
    }

    public void appendLog(String text) {
        setStatusMessage(text);
    }

    public void clearLog() {
        setStatusMessage("");
    }

    public void setStatusMessage(String message) {
        // 设置页不再显示全局状态提示，保留方法是为了兼容 MainFrame 的状态同步调用。
    }

    public void setReceivePathText(String path) {
        if (path == null || path.trim().length() == 0) {
            path = "未设置";
        }

        receivePathLabel.setText(path);
    }
}
