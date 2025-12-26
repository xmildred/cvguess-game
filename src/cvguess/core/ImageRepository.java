package cvguess.core;

import cvguess.model.Category;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class ImageRepository {

    public static class ImageItem {
        public final BufferedImage image;
        public final String answer; // normalize edilmiş cevap
        public final String rawName; // dosya adı

        public ImageItem(BufferedImage image, String answer, String rawName) {
            this.image = image;
            this.answer = answer;
            this.rawName = rawName;
        }
    }

    private final File imagesRoot;
    private final Random rnd = new Random();

    public ImageRepository(File imagesRoot) {
        this.imagesRoot = imagesRoot;
    }

    public List<String> listImageNames(Category category) {
        File folder = new File(imagesRoot, category.folderName);
        System.out.println("Listing images in: " + folder.getAbsolutePath());

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") ||
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg"));

        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                names.add(f.getName());
            }
        }
        return names;
    }

    public ImageItem loadImage(Category category, String filename) {
        File folder = new File(imagesRoot, category.folderName);
        File file = new File(folder, filename);

        if (!file.exists() || !file.canRead()) {
            System.err.println("Cannot read file: " + file.getAbsolutePath());
            return null;
        }

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null)
                return null;

            String base = file.getName();
            int dot = base.lastIndexOf('.');
            if (dot > 0)
                base = base.substring(0, dot);

            String answer = normalizeAnswer(base);
            return new ImageItem(img, answer, file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ImageItem randomItem(Category category) {
        // Deprecated adapter
        List<String> all = listImageNames(category);
        if (all.isEmpty())
            return null;
        String randomName = all.get(rnd.nextInt(all.size()));
        return loadImage(category, randomName);
    }

    public static String normalizeAnswer(String s) {
        s = s.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[^a-z0-9çğıöşü\\s_-]", " "); // TR harfleri de kalsın
        s = s.replace('_', ' ').replace('-', ' ');
        s = s.replaceAll("\\s+", " ").trim();

        // İstersen sadece ilk kelimeyi al:
        // String[] parts = s.split(" ");
        // return parts.length > 0 ? parts[0] : s;

        return s;
    }
}
