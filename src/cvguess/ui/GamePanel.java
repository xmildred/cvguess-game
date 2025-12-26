package cvguess.ui;

import cvguess.core.GameController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {

    private final GameController controller;
    private final Runnable backCallback;
    private final Runnable leaderboardCallback;

    private final JLabel timeLabel = StyleUtils.createLabel("Time ⏰: 60", StyleUtils.FONT_TITLE,
            StyleUtils.ACCENT_YELLOW);
    private final JLabel scoreLabel = StyleUtils.createLabel("Score 🎁: 0", StyleUtils.FONT_TITLE,
            StyleUtils.ACCENT_GREEN);
    private final JLabel hintLabel = StyleUtils.createLabel(" ", StyleUtils.FONT_HEADER, StyleUtils.ACCENT_BLUE); // Message
                                                                                                                  // label

    private final ImageCanvas canvas = new ImageCanvas();
    private final JTextField guessField = StyleUtils.createTextField(20);

    private Timer timer;

    public GamePanel(GameController controller, Runnable backCallback, Runnable leaderboardCallback) {
        this.controller = controller;
        this.backCallback = backCallback;
        this.leaderboardCallback = leaderboardCallback;

        StyleUtils.stylePanel(this);
        setLayout(new BorderLayout(10, 10));

        add(buildTop(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
    }

    public void start() {
        refreshUIFromState();
        guessField.setText("");
        guessField.requestFocusInWindow();
        hintLabel.setText(" ");

        if (timer != null)
            timer.stop();
        timer = new Timer(1000, e -> {
            controller.tickOneSecond();
            if (controller.isGameOver()) {
                endGame();
            } else {
                refreshUIFromState();
            }
        });
        timer.start();
    }

    private JPanel buildTop() {
        JPanel p = new JPanel(new BorderLayout());
        StyleUtils.stylePanel(p);
        p.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Left: Stats
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        stats.setOpaque(false);
        stats.add(timeLabel);
        stats.add(scoreLabel);

        // Right: Menu Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton lbBtn = StyleUtils.createSecondaryButton("Leaderboard 📜");
        lbBtn.setFont(StyleUtils.FONT_NORMAL);
        lbBtn.addActionListener(e -> leaderboardCallback.run());

        JButton backBtn = StyleUtils.createSecondaryButton("Exit 🚪");
        backBtn.setFont(StyleUtils.FONT_NORMAL);
        backBtn.setForeground(StyleUtils.ACCENT_RED);
        backBtn.addActionListener(e -> {
            if (timer != null)
                timer.stop();
            backCallback.run();
        });

        buttons.add(lbBtn);
        buttons.add(backBtn);

        p.add(stats, BorderLayout.WEST);
        p.add(buttons, BorderLayout.EAST);
        return p;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new GridBagLayout());
        StyleUtils.stylePanel(p);
        p.setBorder(new EmptyBorder(15, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 10, 0, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JButton submitBtn = StyleUtils.createPrimaryButton("GUESS 🎅");
        submitBtn.addActionListener(e -> submitGuess());

        guessField.addActionListener(e -> submitGuess());

        // Label
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        p.add(StyleUtils.createLabel("What am I ? 🦌:", StyleUtils.FONT_HEADER, StyleUtils.TEXT_MAIN), c);

        // Input
        c.gridx = 1;
        c.weightx = 1;
        p.add(guessField, c);

        // Button
        c.gridx = 2;
        c.weightx = 0;
        p.add(submitBtn, c);

        // Message
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 0, 10);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(hintLabel, c);

        return p;
    }

    private void submitGuess() {
        if (controller.isGameOver())
            return;

        String guess = guessField.getText().trim();
        guessField.setText("");

        GameController.GuessResult r = controller.submitGuess(guess);
        if (r == GameController.GuessResult.CORRECT) {
            hintLabel.setForeground(StyleUtils.ACCENT_GREEN);
            hintLabel.setText("CORRECT! 🎄 +3s (Next Gift)");
        } else if (r == GameController.GuessResult.WRONG) {
            hintLabel.setForeground(StyleUtils.ACCENT_RED);
            hintLabel.setText("WRONG! 🧊 -2s (Melting...)");
        } else {
            endGame();
            return;
        }

        if (controller.isGameOver()) {
            endGame();
        } else {
            refreshUIFromState();
        }
    }

    private void refreshUIFromState() {
        GameController.RoundState st = controller.getCurrentState();
        timeLabel.setText("Time ⏰: " + st.timeLeft);
        if (st.timeLeft <= 10)
            timeLabel.setForeground(StyleUtils.ACCENT_RED);
        else
            timeLabel.setForeground(StyleUtils.ACCENT_YELLOW);

        scoreLabel.setText("Score 🎁: " + st.correctCount);
        canvas.setImage(st.shownImage);
    }

    private void endGame() {
        if (timer != null)
            timer.stop();
        controller.finalizeAndSave();

        int correct = controller.getPlayer().getCorrectCount();
        int t = controller.getTimeLeft();

        BufferedImage finalImg = controller.getCurrentImage();
        String answer = controller.getCurrentAnswer();

        ImageIcon icon = null;
        if (finalImg != null) {
            int maxDim = 300;
            int w = finalImg.getWidth();
            int h = finalImg.getHeight();
            double scale = Math.min((double) maxDim / w, (double) maxDim / h);
            int nw = (int) (w * scale);
            int nh = (int) (h * scale);
            Image scaled = finalImg.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);
        }

        JOptionPane.showMessageDialog(this,
                "GAME OVER!\n" +
                        "Player: " + controller.getPlayer().getName() + "\n" +
                        "Score: " + correct + "\n" +
                        "Correct Answer: " + answer.toUpperCase(),
                "Game Over",
                JOptionPane.INFORMATION_MESSAGE,
                icon);

        backCallback.run();
    }

    private static class ImageCanvas extends JPanel {
        private BufferedImage image;

        public ImageCanvas() {
            setBackground(StyleUtils.BG_PANEL); // Matches card bg
            setBorder(BorderFactory.createLineBorder(StyleUtils.BG_DARK, 5));
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null)
                return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int w = getWidth();
            int h = getHeight();

            int side = Math.min(w, h) - 40; // padding
            int x = (w - side) / 2;
            int y = (h - side) / 2;

            g2.setColor(Color.BLACK);
            g2.fillRect(x - 2, y - 2, side + 4, side + 4); // Frame

            g2.drawImage(image, x, y, side, side, null);
        }
    }
}
