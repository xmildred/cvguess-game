package cvguess.ui;

import cvguess.core.GameController;
import cvguess.core.ImageRepository;
import cvguess.model.Leaderboard;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainFrame extends JFrame {

    private final CardLayout card = new CardLayout();
    private final JPanel root = new JPanel(card);

    private final Leaderboard leaderboard = new Leaderboard();
    private GameController controller;

    private StartPanel startPanel;
    private GamePanel gamePanel;

    public MainFrame() {
        super("CV Guess Game (Swing) - Pixel Blocks");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Dark theme background
        StyleUtils.stylePanel(root);
        getContentPane().setBackground(StyleUtils.BG_DARK);

        // İlk iş: images root seçtir
        File imagesRoot = new File("images");
        if (!imagesRoot.exists() || !imagesRoot.isDirectory()) {
            imagesRoot = chooseImagesRoot();
        }

        if (imagesRoot == null || !imagesRoot.exists()) {
            JOptionPane.showMessageDialog(this, "Görsel klasörü bulunamadı/seçilmedi. Program kapanıyor.");
            System.exit(0);
        }

        System.out.println("Images root: " + imagesRoot.getAbsolutePath());

        controller = new GameController(new ImageRepository(imagesRoot), leaderboard);

        startPanel = new StartPanel(this::onStartGame, this::onShowLeaderboard);
        gamePanel = new GamePanel(controller, this::onBackToStart, this::onShowLeaderboard);

        root.add(startPanel, "START");
        root.add(gamePanel, "GAME");

        setContentPane(root);
        card.show(root, "START");
    }

    private File chooseImagesRoot() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Images root klasörünü seç (altında animal/film/game/logo olmalı)");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = fc.showOpenDialog(this);
        if (r != JFileChooser.APPROVE_OPTION)
            return null;
        return fc.getSelectedFile();
    }

    private void onStartGame(String playerName, cvguess.model.Category category, cvguess.model.Difficulty difficulty) {
        try {
            controller.setDifficulty(difficulty);
            controller.startNewGame(playerName, category);
            // gamePanel.start() removed? No wait, logic was:
            // controller.startNewGame initializes state
            // gamePanel needs to refresh? gamePanel uses controller.
            // In original code: gamePanel.start() was called. Let's keep it if it exists or
            // check if I removed it?
            // Checking view_file output: gamePanel.start() exists in line 70.
            // Wait, I am editing MainFrame.java.
            // GamePanel *might* need updates but primarily it pulls from controller.
            // If GamePanel.start() starts a timer, we need that.
            // I will assume gamePanel has .start() or .initGame().
            // Looking at file content: line 70 says `gamePanel.start();`

            // Wait, previous file view showed `gamePanel.start()`.
            gamePanel.start();
            card.show(root, "GAME");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage(), "Başlatma Hatası",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void onBackToStart() {
        card.show(root, "START");
    }

    private void onShowLeaderboard() {
        LeaderboardDialog dlg = new LeaderboardDialog(this, leaderboard);
        dlg.setVisible(true);
    }
}
