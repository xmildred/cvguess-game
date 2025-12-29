package cvguess.ui;

import cvguess.model.Leaderboard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LeaderboardDialog extends JDialog {

    public LeaderboardDialog(JFrame owner, Leaderboard leaderboard) {
        super(owner, "Leaderboard", true);
        setSize(600, 360);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(StyleUtils.BG_DARK);

        String[] cols = { "#", "Name", "Correct", "Difficulty", "Category" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<Leaderboard.Entry> entries = leaderboard.loadAllSorted();
        int rank = 1;
        for (Leaderboard.Entry e : entries) {
            model.addRow(new Object[] { rank++, e.name, e.correct, e.difficulty, e.category });
        }

        JTable table = new JTable(model);
        StyleUtils.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(StyleUtils.BG_PANEL);
        sp.setBorder(BorderFactory.createEmptyBorder());

        JButton close = StyleUtils.createPrimaryButton("CLOSE");
        close.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        StyleUtils.stylePanel(bottom);
        bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
        bottom.add(close);

        setLayout(new BorderLayout());
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}
