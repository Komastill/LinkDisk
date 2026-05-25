package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Setting extends JPanel {

    private JButton trustManagerButton;
    private JButton clearLogButton;

    private JLabel statusLabel;

    private static final Color PAGE_BG = new Color(247, 250, 254);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(220, 228, 238);
    private static final Color TEXT = new Color(24, 38, 56);
    private static final Color SUBTEXT = new Color(95, 111, 132);
    private static final Color PRIMARY = new Color(67, 126, 202);

    public Setting(Font font) {

        setLayout(new BorderLayout(20, 20));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("设置");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel("管理信任设备和系统状态");
        subtitleLabel.setFont(font.deriveFont(Font.PLAIN, 15f));
        subtitleLabel.setForeground(SUBTEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(16, 16));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)
        ));

        JLabel sectionTitle = new JLabel("安全与信任");
        sectionTitle.setFont(font.deriveFont(Font.BOLD, 18f));
        sectionTitle.setForeground(TEXT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonPanel.setOpaque(false);

        trustManagerButton = createActionButton("管理信任设备", font, true, 170);
        clearLogButton = createActionButton("清空状态提示", font, false, 170);

        buttonPanel.add(trustManagerButton);
        buttonPanel.add(clearLogButton);

        statusLabel = new JLabel("系统状态将在这里显示。");
        statusLabel.setFont(font.deriveFont(Font.PLAIN, 14f));
        statusLabel.setForeground(SUBTEXT);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(249, 251, 254));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        statusLabel.setPreferredSize(new Dimension(0, 48));

        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        top.add(sectionTitle, BorderLayout.NORTH);
        top.add(buttonPanel, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(statusLabel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    private JButton createActionButton(String text, Font font, boolean primary, int width) {
        JButton button = new JButton(text);
        button.setFont(font.deriveFont(Font.BOLD, 14f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(width, 42));

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

    public JButton getTrustManagerButton() { return trustManagerButton; }
    public JButton getClearLogButton() { return clearLogButton; }

    public void appendLog(String text) { setStatusMessage(text); }

    public void clearLog() { setStatusMessage("状态提示已清空。"); }

    public void setStatusMessage(String message) {
        if (message == null) message = "";
        message = message.replace("\n", "  ");
        statusLabel.setText(message);
    }
}
