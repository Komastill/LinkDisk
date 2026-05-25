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

public class SideNavigation extends JPanel {

    private JButton devicePageButton;
    private JButton transferPageButton;
    private JButton settingsPageButton;

    private static final Color SIDEBAR_BG = new Color(238, 243, 250);
    private static final Color ACTIVE_BG = new Color(219, 232, 252);
    private static final Color INACTIVE_BG = new Color(246, 249, 253);
    private static final Color ACTIVE_BORDER = new Color(67, 126, 202);
    private static final Color INACTIVE_BORDER = new Color(215, 224, 235);
    private static final Color TEXT = new Color(24, 38, 56);

    public SideNavigation(Font font) {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(190, 0));
        setBackground(SIDEBAR_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 18, 24, 18));

        JLabel titleLabel = new JLabel("LinkDisk");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 26f));
        titleLabel.setForeground(TEXT);
        titleLabel.setHorizontalAlignment(JLabel.LEFT);

        add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridLayout(9, 1, 0, 14));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(44, 0, 0, 0));

        devicePageButton = createNavButton("设备连接", font);
        transferPageButton = createNavButton("文件传输", font);
        settingsPageButton = createNavButton("设置", font);

        buttonPanel.add(devicePageButton);
        buttonPanel.add(transferPageButton);
        buttonPanel.add(settingsPageButton);

        add(buttonPanel, BorderLayout.CENTER);

        devicePageButton.addActionListener(e -> setActivePage("device"));
        transferPageButton.addActionListener(e -> setActivePage("transfer"));
        settingsPageButton.addActionListener(e -> setActivePage("settings"));

        setActivePage("device");
    }

    private JButton createNavButton(String text, Font font) {
        JButton button = new JButton(text);

        button.setFont(font.deriveFont(Font.BOLD, 16f));
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(150, 58));

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 4, 1, 1, INACTIVE_BORDER),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)
                )
        );

        button.setBackground(INACTIVE_BG);

        return button;
    }

    private void setButtonActive(JButton button, boolean active) {
        if (active) {
            button.setBackground(ACTIVE_BG);
            button.setForeground(TEXT);
            button.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 4, 1, 1, ACTIVE_BORDER),
                            BorderFactory.createEmptyBorder(0, 10, 0, 10)
                    )
            );
        } else {
            button.setBackground(INACTIVE_BG);
            button.setForeground(TEXT);
            button.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 4, 1, 1, INACTIVE_BORDER),
                            BorderFactory.createEmptyBorder(0, 10, 0, 10)
                    )
            );
        }
    }

    public void setActivePage(String pageName) {
        setButtonActive(devicePageButton, "device".equals(pageName));
        setButtonActive(transferPageButton, "transfer".equals(pageName));
        setButtonActive(settingsPageButton, "settings".equals(pageName));
    }

    public JButton getDevicePageButton() {
        return devicePageButton;
    }

    public JButton getTransferPageButton() {
        return transferPageButton;
    }

    public JButton getSettingsPageButton() {
        return settingsPageButton;
    }
}
