package LinkDisk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class SideNavigation extends JPanel {

    private JButton devicePageButton;
    private JButton settingsPageButton;
    private JButton transferPageButton;

    private String activePage = "device";

    private Font baseFont;

    private static final Color SIDEBAR_BG = new Color(235, 242, 251);
    private static final Color TEXT = new Color(24, 38, 56);

    private static final Color BUTTON_BG = new Color(248, 251, 255);
    private static final Color BUTTON_HOVER_BG = new Color(236, 246, 255);
    private static final Color BUTTON_ACTIVE_BG = new Color(218, 234, 255);

    private static final Color BORDER = new Color(216, 226, 239);
    private static final Color ACTIVE = new Color(56, 119, 210);

    public SideNavigation(Font font) {

        this.baseFont = font;

        setLayout(new BorderLayout());
        setBackground(SIDEBAR_BG);

        // 左侧栏加宽，避免 LinkDisk 被截断
        setPreferredSize(new Dimension(230, 0));

        setBorder(
                BorderFactory.createMatteBorder(
                        0,
                        0,
                        0,
                        1,
                        new Color(220, 229, 240)
                )
        );

        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(38, 22, 0, 22));

        JLabel brandLabel = new JLabel("LinkDisk");
        brandLabel.setFont(font.deriveFont(Font.BOLD, 32f));
        brandLabel.setForeground(TEXT);
        brandLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel brandLine = new JPanel();
        brandLine.setBackground(ACTIVE);
        brandLine.setMaximumSize(new Dimension(46, 4));
        brandLine.setPreferredSize(new Dimension(46, 4));
        brandLine.setAlignmentX(LEFT_ALIGNMENT);

        devicePageButton = createNavButton("设备连接", "device");
        settingsPageButton = createNavButton("传输设置", "settings");
        transferPageButton = createNavButton("文件传输", "transfer");

        mainPanel.add(brandLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(brandLine);

        mainPanel.add(Box.createVerticalStrut(42));

        mainPanel.add(devicePageButton);
        mainPanel.add(Box.createVerticalStrut(18));

        mainPanel.add(settingsPageButton);
        mainPanel.add(Box.createVerticalStrut(18));

        mainPanel.add(transferPageButton);

        add(mainPanel, BorderLayout.NORTH);

        setActivePage("device");
    }

    private JButton createNavButton(String text, String pageName) {

        JButton button = new JButton(text);

        button.setFont(baseFont.deriveFont(Font.BOLD, 17f));
        button.setForeground(TEXT);
        button.setBackground(BUTTON_BG);

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(true);

        // 按钮恢复大一点，不要小气
        button.setPreferredSize(new Dimension(186, 58));
        button.setMaximumSize(new Dimension(186, 58));
        button.setMinimumSize(new Dimension(186, 58));
        button.setAlignmentX(LEFT_ALIGNMENT);

        applyButtonStyle(button, false, false);

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!pageName.equals(activePage)) {
                    applyButtonStyle(button, false, true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!pageName.equals(activePage)) {
                    applyButtonStyle(button, false, false);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(BUTTON_ACTIVE_BG);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pageName.equals(activePage)) {
                    applyButtonStyle(button, true, false);
                } else {
                    applyButtonStyle(button, false, true);
                }
            }
        });

        return button;
    }

    private void applyButtonStyle(JButton button, boolean active, boolean hover) {

        if (active) {

            button.setBackground(BUTTON_ACTIVE_BG);
            button.setForeground(TEXT);

            button.setBorder(
                    new CompoundBorder(
                            new MatteBorder(0, 6, 0, 1, ACTIVE),
                            new CompoundBorder(
                                    BorderFactory.createMatteBorder(1, 0, 1, 0, ACTIVE),
                                    new EmptyBorder(0, 12, 0, 12)
                            )
                    )
            );

        } else {

            if (hover) {
                button.setBackground(BUTTON_HOVER_BG);
            } else {
                button.setBackground(BUTTON_BG);
            }

            button.setForeground(TEXT);

            button.setBorder(
                    new CompoundBorder(
                            new MatteBorder(0, 6, 0, 1, BORDER),
                            new CompoundBorder(
                                    BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER),
                                    new EmptyBorder(0, 12, 0, 12)
                            )
                    )
            );
        }
    }

    public void setActivePage(String pageName) {

        activePage = pageName;

        applyButtonStyle(devicePageButton, "device".equals(pageName), false);
        applyButtonStyle(settingsPageButton, "settings".equals(pageName), false);
        applyButtonStyle(transferPageButton, "transfer".equals(pageName), false);
    }

    public JButton getDevicePageButton() {
        return devicePageButton;
    }

    public JButton getSettingsPageButton() {
        return settingsPageButton;
    }

    public JButton getTransferPageButton() {
        return transferPageButton;
    }
}