package cvguess.core;

import cvguess.model.Category;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class ImageRepository {

    public static class ImageItem {
        public final BufferedImage image;
        public final String answer;  // normalize edilmiş cevap
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

    public ImageItem randomItem(Category category) {
        File folder = new File(imagesRoot, category.folderName);
        System.out.println("Looking for images in: " + folder.getAbsolutePath());
        
        File[] files = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") ||
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg"));

        if (files == null || files.length == 0) {
            System.out.println("No images found in " + folder.getAbsolutePath());
            return null;
        }

        System.out.println("Found " + files.length + " images in " + category.folderName);


        // Basit random seç
        File chosen = files[rnd.nextInt(files.length)];
        System.out.println("Selected file: " + chosen.getAbsolutePath());
        System.out.println("File exists: " + chosen.exists() + ", Can read: " + chosen.canRead() + ", Size: " + chosen.length());

        try {
            BufferedImage img = ImageIO.read(chosen);
            if (img == null) {
                System.out.println("ImageIO.read returned NULL for: " + chosen.getAbsolutePath());
                return null;
            }
            System.out.println("Image loaded successfully: " + img.getWidth() + "x" + img.getHeight());

            String base = chosen.getName();
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);

            // Cevap: dosya adının ilk parçası (underscore/dash kırp)
            String answer = normalizeAnswer(base);
            System.out.println("Answer for this image: " + answer);

            return new ImageItem(img, answer, chosen.getName());
        } catch (Exception e) {
            System.out.println("Exception loading image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

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
