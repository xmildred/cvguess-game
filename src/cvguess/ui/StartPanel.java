package cvguess.ui;

import cvguess.model.Category;
import cvguess.model.Difficulty;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StartPanel extends JPanel {

    public interface StartCallback {
        void start(String playerName, Category category, Difficulty difficulty);
    }

    public StartPanel(StartCallback startCallback, Runnable leaderboardCallback) {
        StyleUtils.stylePanel(this); // Dark background
        setLayout(new GridBagLayout());

        // --- Card Panel for Content ---
        JPanel card = new JPanel(new GridBagLayout());
        StyleUtils.styleCard(card);
        card.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Add drop shadow effect simulation (border)
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StyleUtils.ACCENT_BLUE, 1),
                new EmptyBorder(30, 40, 30, 40)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        // Title
        JLabel title = StyleUtils.createLabel("🎄 CV Guess Game 🎅", StyleUtils.FONT_TITLE, StyleUtils.ACCENT_BLUE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, c);

        c.gridy++;
        JLabel subTitle = StyleUtils.createLabel("❄️ Festive Pixel Challenge ❄️", StyleUtils.FONT_NORMAL,
                StyleUtils.TEXT_MUTED);
        subTitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(subTitle, c);

        // Name Input
        c.gridy++;
        c.insets = new Insets(20, 10, 5, 10);
        card.add(StyleUtils.createLabel("YOUR NAME ⛄", StyleUtils.FONT_HEADER.deriveFont(12f), StyleUtils.TEXT_MUTED),
                c);

        c.gridy++;
        c.insets = new Insets(0, 10, 10, 10);
        JTextField nameField = StyleUtils.createTextField(15);
        nameField.setHorizontalAlignment(JTextField.CENTER);
        card.add(nameField, c);

        // Category Selection
        c.gridy++;
        c.insets = new Insets(10, 10, 5, 10);
        card.add(StyleUtils.createLabel("CATEGORY 🎁", StyleUtils.FONT_HEADER.deriveFont(12f), StyleUtils.TEXT_MUTED),
                c);

        c.gridy++;
        c.insets = new Insets(0, 10, 10, 10);
        JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
        categoryBox.setFont(StyleUtils.FONT_NORMAL);
        categoryBox.setBackground(StyleUtils.BG_PANEL);
        categoryBox.setForeground(Color.BLACK); // Swing combos are tricky to style fully without custom UI
        card.add(categoryBox, c);

        // Difficulty Selection
        c.gridy++;
        c.insets = new Insets(10, 10, 5, 10);
        card.add(StyleUtils.createLabel("DIFFICULTY ⚙️", StyleUtils.FONT_HEADER.deriveFont(12f), StyleUtils.TEXT_MUTED),
                c);

        c.gridy++;
        c.insets = new Insets(0, 10, 10, 10);
        JComboBox<Difficulty> difficultyBox = new JComboBox<>(Difficulty.values());
        difficultyBox.setFont(StyleUtils.FONT_NORMAL);
        difficultyBox.setBackground(StyleUtils.BG_PANEL);
        difficultyBox.setForeground(Color.BLACK);
        card.add(difficultyBox, c);

        // Buttons
        c.gridy++;
        c.insets = new Insets(25, 10, 10, 10);
        JButton startBtn = StyleUtils.createPrimaryButton("START GAME");
        card.add(startBtn, c);

        c.gridy++;
        c.insets = new Insets(5, 10, 10, 10);
        JButton lbBtn = StyleUtils.createSecondaryButton("LEADERBOARD");
        card.add(lbBtn, c);

        // Events
        startBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your name!", "Missing Info",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Category cat = (Category) categoryBox.getSelectedItem();
            Difficulty diff = (Difficulty) difficultyBox.getSelectedItem();
            startCallback.start(name, cat, diff);
        });

        lbBtn.addActionListener(e -> leaderboardCallback.run());

        add(card); // Add card to main center

        // --- Info / Footer ---
        // Optional: Add simple footer text outside the card
    }
}
