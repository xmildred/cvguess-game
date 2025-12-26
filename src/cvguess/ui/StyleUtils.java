package cvguess.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class StyleUtils {

    // --- New Year / Holiday Palette 🎄🎅 ---
    public static final Color BG_DARK = new Color(15, 26, 21); // Dark Pine (Background)
    public static final Color BG_PANEL = new Color(25, 44, 35); // Lighter Pine (Surface)
    public static final Color TEXT_MAIN = new Color(240, 248, 255);// Snow White (AliceBlue)
    public static final Color TEXT_MUTED = new Color(144, 164, 174);// Icy Gray

    public static final Color ACCENT_BLUE = new Color(214, 40, 40); // Holiday Red (Primary Action)
    public static final Color ACCENT_GREEN = new Color(42, 157, 143); // Holly Green (Success)
    public static final Color ACCENT_RED = new Color(193, 18, 31); // Deep Red (Error)
    public static final Color ACCENT_YELLOW = new Color(244, 162, 97); // Gold / Gingerbread (Warning)

    // --- Fonts ---
    // Using widely available sans-serif fonts
    public static final Font FONT_TITLE = new Font("Segoe UI Emoji", Font.BOLD, 28);
    public static final Font FONT_HEADER = new Font("Segoe UI Emoji", Font.BOLD, 18);
    public static final Font FONT_NORMAL = new Font("Segoe UI Emoji", Font.PLAIN, 14);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 14);

    /**
     * Applies the base dark theme background to a panel.
     */
    public static void stylePanel(JPanel panel) {
        panel.setBackground(BG_DARK);
    }

    public static void styleCard(JPanel panel) {
        panel.setBackground(BG_PANEL);
    }

    /**
     * Creates a modern styled label.
     */
    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    /**
     * Creates a modern styled text field.
     */
    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_NORMAL);
        tf.setBackground(BG_PANEL);
        tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(ACCENT_BLUE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TEXT_MUTED, 1),
                new EmptyBorder(8, 8, 8, 8)));
        return tf;
    }

    /**
     * Creates a modern, flattened button with hover effects.
     */
    public static JButton createButton(String text, Color baseColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(baseColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(baseColor);
                }

                // Rounded corners
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.setColor(Color.WHITE); // Text always white on colored buttons
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        btn.setFont(FONT_HEADER.deriveFont(14f));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add padding via empty border
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));

        return btn;
    }

    public static JButton createPrimaryButton(String text) {
        return createButton(text, ACCENT_BLUE, ACCENT_BLUE.brighter());
    }

    public static JButton createSecondaryButton(String text) {
        return createButton(text, BG_PANEL.brighter(), BG_PANEL.brighter().brighter());
    }

    /**
     * Styles a JTable for the dark theme.
     */
    public static void styleTable(JTable table) {
        table.setBackground(BG_PANEL);
        table.setForeground(TEXT_MAIN);
        table.setGridColor(BG_DARK);
        table.setRowHeight(30);
        table.setFont(FONT_NORMAL);
        table.setSelectionBackground(ACCENT_BLUE);
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_DARK);
        header.setForeground(ACCENT_BLUE);
        header.setFont(FONT_HEADER.deriveFont(14f));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE));

        // Center alignment wrapper
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, center);
    }
}
