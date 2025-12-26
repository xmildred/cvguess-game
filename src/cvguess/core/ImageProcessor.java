package cvguess.core;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageProcessor {

    // Görüntüyü merkezden kare crop yapıp hedef boyuta ölçekler
    public BufferedImage centerCropAndResize(BufferedImage src, int targetSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h);

        int x = (w - side) / 2;
        int y = (h - side) / 2;

        BufferedImage cropped = src.getSubimage(x, y, side, side);

        BufferedImage out = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, targetSize, targetSize, null);
        g.dispose();
        return out;
    }

    // Basit renk quantization: her kanalı belirli seviyeye yuvarlar (K-means değil
    // ama “kümeleme benzeri”)
    public BufferedImage quantizeColors(BufferedImage src, int levelsPerChannel) {
        // ... (existing code kept same, just context)
        if (levelsPerChannel < 2)
            levelsPerChannel = 2;
        int step = 256 / levelsPerChannel;

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = (r / step) * step;
                g = (g / step) * step;
                b = (b / step) * step;

                int q = (r << 16) | (g << 8) | b;
                out.setRGB(x, y, q);
            }
        }
        return out;
    }

    // --- New Algorithms for Game Modes ---

    // Medium Mode: Box Blur (approximation of Gaussian for speed)
    public BufferedImage applyBlur(BufferedImage src, int radius) {
        if (radius < 1)
            return src;
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // Simple box blur passes (horizontal + vertical)
        // Note: Full Gaussian kernel calculation is heavy, simple averaging is enough
        // for game mechanic.
        int[] pixels = src.getRGB(0, 0, w, h, null, 0, w);
        int[] result = new int[w * h];

        // This is a naive O(R*W*H) implementation. For game UI it's fine for small
        // images (300x300).
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                long r = 0, g = 0, b = 0;
                int count = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    int ny = y + ky;
                    if (ny < 0 || ny >= h)
                        continue;
                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = x + kx;
                        if (nx < 0 || nx >= w)
                            continue;

                        int rgb = pixels[ny * w + nx];
                        r += (rgb >> 16) & 0xFF;
                        g += (rgb >> 8) & 0xFF;
                        b += rgb & 0xFF;
                        count++;
                    }
                }
                int ar = (int) (r / count);
                int ag = (int) (g / count);
                int ab = (int) (b / count);
                result[y * w + x] = (ar << 16) | (ag << 8) | ab;
            }
        }

        out.setRGB(0, 0, w, h, result, 0, w);
        return out;
    }

    // Hard Mode: Grayscale + Thresholding
    // level: 0..255 (threshold point). Or maybe "amount of detail" logic?
    // Let's interpret 'threshold' as: pixels brighter than T are shown, others
    // black?
    // OR: pixels close to a certain band are shown?
    // User said: "Hard mode thresholding ile olsun".
    // Game logic: Start with high threshold (almost black) -> lower threshold (more
    // detail) or mix with grayscale.
    // Let's do: Converts to Grayscale, then if (gray < threshold) BLACK else WHITE.
    // As game progresses, we add more "gray levels" or just reveal the image?
    // Let's implement generic: Grayscale + Binary Threshold.
    public BufferedImage applyThreshold(BufferedImage src, int threshold) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Luminance
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                int val = (gray > threshold) ? 0xFFFFFF : 0x000000;
                out.setRGB(x, y, (val == 0xFFFFFF) ? 0xFFFFFFFF : 0xFF000000); // White or Black
            }
        }
        return out;
    }

    // Helper to mix threshold with real image based on progress?
    // Maybe better: Threshold normally produces binary image. To "reveal" it, maybe
    // we move T?
    // Actually, "Thresholding" removes info.
    // Let's keep this simple tool here, logic will be in Controller.

    // Extreme Mode: Edge Detection (Sobel)
    public BufferedImage applyEdgeDetection(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int[][] gx = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
        int[][] gy = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int valX = 0;
                int valY = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = src.getRGB(x + kx, y + ky);
                        int gray = (int) ((rgb >> 16) & 0xFF); // approximate using Red channel or avg
                        // Better use luminance
                        // int r = (rgb >> 16) & 0xFF; int g = ...
                        // For speed just use Green channel as proxy for luminance usually works
                        int gPos = (rgb >> 8) & 0xFF;

                        valX += gPos * gx[ky + 1][kx + 1];
                        valY += gPos * gy[ky + 1][kx + 1];
                    }
                }

                int magnitude = (int) Math.sqrt(valX * valX + valY * valY);
                if (magnitude > 255)
                    magnitude = 255;
                if (magnitude < 0)
                    magnitude = 0;

                // Invert for "pencil sketch" look (Black on White) or keep White on Black?
                // White edges on Black background is standard Sobel.
                int c = (magnitude << 16) | (magnitude << 8) | magnitude;
                out.setRGB(x, y, c);
            }
        }
        return out;
    }
}
