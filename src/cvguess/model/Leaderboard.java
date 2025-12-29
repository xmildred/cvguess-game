package cvguess.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Leaderboard {
    public static class Entry {
        public final String name;
        public final int correct;
        public final String difficulty;
        public final String category;

        public Entry(String name, int correct, String difficulty, String category) {
            this.name = name;
            this.correct = correct;
            this.difficulty = difficulty;
            this.category = category;
        }
    }

    private final Path filePath;

    public Leaderboard() {
        this.filePath = Paths.get(System.getProperty("user.home"), "leaderboard.csv");
    }

    public synchronized void add(String name, int correct, String difficulty, String category) {
        try {
            boolean exists = Files.exists(filePath);
            try (BufferedWriter bw = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (!exists) {
                    bw.write("name,correct,difficulty,category");
                    bw.newLine();
                }
                bw.write(escape(name) + "," + correct + "," + escape(difficulty) + "," + escape(category));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Entry> loadAllSorted() {
        List<Entry> list = new ArrayList<>();
        if (!Files.exists(filePath))
            return list;

        try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {
                String[] parts = splitCsv(line);
                if (parts.length < 2)
                    continue;

                String name = unescape(parts[0]);
                int correct = parseIntSafe(parts[1]);
                String diff = "-";
                String cat = "-";

                if (parts.length >= 3) {
                    String p3 = unescape(parts[2]);
                    // Check if it looks like the old TimeLeft (integer)
                    if (isInteger(p3)) {
                        // It is likely old legacy data (TimeLeft)
                        // We can mark difficulty as Unknown or "-"
                        diff = "-";
                    } else {
                        // It is likely the new Difficulty string
                        diff = p3;
                    }
                }

                if (parts.length >= 4) {
                    cat = unescape(parts[3]);
                }

                list.add(new Entry(name, correct, diff, cat));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Score: sort by correct count descending
        list.sort((a, b) -> Integer.compare(b.correct, a.correct));
        return list;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Simple CSV escape
    private String escape(String s) {
        if (s == null)
            return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String unescape(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
