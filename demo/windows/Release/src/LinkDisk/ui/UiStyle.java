package LinkDisk.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

public final class UiStyle {

    public static final Color PAGE_BG = new Color(247, 250, 254);
    public static final Color SIDEBAR_BG = new Color(238, 244, 252);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color SOFT_BG = new Color(249, 251, 254);
    public static final Color BORDER = new Color(220, 228, 238);
    public static final Color TEXT = new Color(24, 38, 56);
    public static final Color SUBTEXT = new Color(95, 111, 132);
    public static final Color PRIMARY = new Color(58, 120, 213);
    public static final Color PRIMARY_DARK = new Color(42, 91, 166);
    public static final Color PRIMARY_SOFT = new Color(230, 241, 255);
    public static final Color HOVER_BG = new Color(242, 247, 255);
    public static final Color PRESSED_BG = new Color(219, 234, 254);
    public static final Color SUCCESS = new Color(33, 128, 91);
    public static final Color WARNING = new Color(175, 116, 38);
    public static final Color ACCENT_SOFT = new Color(244, 248, 253);

    private UiStyle() {
    }

    public static JButton createActionButton(String text, Font font, int width, int height) {
        JButton button = new JButton(text);

        button.setUI(new BasicButtonUI());
        button.setFont(font.deriveFont(Font.BOLD, 14f));
        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(width, height));
        button.setMinimumSize(new Dimension(width, height));
        button.setMaximumSize(new Dimension(width, height));
        button.setMargin(new Insets(0, 10, 0, 10));

        applyButtonState(button, false, false);

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    applyButtonState(button, true, false);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    applyButtonState(button, false, false);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    applyButtonState(button, true, true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    applyButtonState(button, button.contains(e.getPoint()), false);
                }
            }
        });

        return button;
    }

    public static JButton createCompactButton(String text, Font font, int width, int height) {
        JButton button = createActionButton(text, font, width, height);
        button.setFont(font.deriveFont(Font.BOLD, 13f));
        return button;
    }


    public static JButton createPrimaryButton(String text, Font font, int width, int height) {
        JButton button = createActionButton(text, font, width, height);
        button.setFont(font.deriveFont(Font.BOLD, 15f));
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 5, 1, 1, PRIMARY_DARK),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(PRIMARY_DARK),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)
                )
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_DARK);
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY);
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(29, 78, 148));
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    if (button.contains(e.getPoint())) {
                        button.setBackground(PRIMARY_DARK);
                    } else {
                        button.setBackground(PRIMARY);
                    }
                    button.setForeground(Color.WHITE);
                }
            }
        });

        return button;
    }

    private static void applyButtonState(JButton button, boolean hover, boolean pressed) {
        if (pressed) {
            button.setBackground(PRESSED_BG);
            button.setForeground(PRIMARY_DARK);
            button.setBorder(createButtonBorder(PRIMARY_DARK, PRIMARY));
        } else if (hover) {
            button.setBackground(HOVER_BG);
            button.setForeground(PRIMARY_DARK);
            button.setBorder(createButtonBorder(PRIMARY, PRIMARY));
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(TEXT);
            button.setBorder(createButtonBorder(new Color(231, 238, 248), BORDER));
        }
    }

    private static Border createButtonBorder(Color leftColor, Color lineColor) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 4, 1, 1, leftColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(lineColor),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)
                )
        );
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        );
    }

    public static Border createSmallCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public static Border createInputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    public static Border createDropNormalBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    public static Border createDropActiveBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 2),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)
        );
    }

    public static void setPanelCardStyle(JComponent component) {
        component.setBackground(CARD_BG);
        component.setBorder(createCardBorder());
    }
}
