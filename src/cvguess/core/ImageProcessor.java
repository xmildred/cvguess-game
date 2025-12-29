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

    // Medium Mode: Gaussian Blur (Matches Slide 3 & 4)
    // Previously was Box Blur (Average), now updated to true Gaussian.
    public BufferedImage applyBlur(BufferedImage src, int radius) {
        if (radius < 1)
            return src;
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // 1. Gaussian Kernel Oluşturma
        // Sigma değeri genellikle yarıçapın 1/3'ü alınır (kural olarak)
        double sigma = Math.max(1.0, radius / 3.0);
        int size = 2 * radius + 1;
        double[] kernel = new double[size * size];
        double sum = 0.0;

        for (int ky = -radius; ky <= radius; ky++) {
            for (int kx = -radius; kx <= radius; kx++) {
                double distanceSq = kx * kx + ky * ky;
                // Gaussian Formülü: G(x,y) = e^-( (x^2 + y^2) / (2 * sigma^2) )
                // Sabit katsayıyı (1/2pi*sigma^2) ihmal edebiliriz çünkü sonda normalizasyon
                // yapacağız.
                double weight = Math.exp(-distanceSq / (2 * sigma * sigma));

                int index = (ky + radius) * size + (kx + radius);
                kernel[index] = weight;
                sum += weight;
            }
        }

        // 2. Kerneli Normalize Et (Toplamları 1 olsun ki parlaklık değişmesin)
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        int[] pixels = src.getRGB(0, 0, w, h, null, 0, w);
        int[] result = new int[w * h];

        // 3. Konvolüsyon (Convolution)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double rAcc = 0, gAcc = 0, bAcc = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    int ny = y + ky;
                    if (ny < 0 || ny >= h)
                        continue; // Sınır kontrolü (Zero padding benzeri, işlem yapma)

                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = x + kx;
                        if (nx < 0 || nx >= w)
                            continue;

                        int rgb = pixels[ny * w + nx];
                        double weight = kernel[(ky + radius) * size + (kx + radius)];

                        rAcc += ((rgb >> 16) & 0xFF) * weight;
                        gAcc += ((rgb >> 8) & 0xFF) * weight;
                        bAcc += (rgb & 0xFF) * weight;
                    }
                }

                int r = Math.min(255, (int) rAcc);
                int g = Math.min(255, (int) gAcc);
                int b = Math.min(255, (int) bAcc);

                result[y * w + x] = (r << 16) | (g << 8) | b;
            }
        }

        out.setRGB(0, 0, w, h, result, 0, w);
        return out;
    }

    // Medium Mode: Grayscale + Thresholding
    public BufferedImage applyThreshold(BufferedImage src, int threshold) {
        // image'ın boyutlarını aldık
        int w = src.getWidth();
        int h = src.getHeight();
        // yeni bir image oluşturduk
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) { // tüm pikselleri gezmek için döngü oluşturduk
            for (int x = 0; x < w; x++) {
                // 1. Pikselin RGB değerlerini al
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF; // kırmızı channel
                int g = (rgb >> 8) & 0xFF; // yeşil channel
                int b = rgb & 0xFF; // mavi channel

                // 2. Gri Tonlamaya Çevir
                // İnsan gözü yeşile daha duyarlı olduğu için katsayılar farklıdır (0.587 G,
                // 0.299 R, 0.114 B)
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b); // gri tonu hesapla

                // 3. Eşikleme (Thresholding)
                // Eğer gri tonu eşik değerinden büyükse BEYAZ (0xFFFFFF),
                // küçük veya eşitse SİYAH (0x000000) yap.
                int val = (gray > threshold) ? 0xFFFFFF : 0x000000; // threshold
                out.setRGB(x, y, (val == 0xFFFFFF) ? 0xFFFFFFFF : 0xFF000000); // boş image'ı dolduruk
            }
        }
        return out;
    }

    // Easy Mode: Edge Detection (Sobel)
    public BufferedImage applyEdgeDetection(BufferedImage src) {
        // image'ın boyutlarını aldık
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); // boş image oluşturduk

        int[][] gx = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } }; // sobel yatay matrix
        int[][] gy = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } }; // sobel dikey matrix

        for (int y = 1; y < h - 1; y++) { // tüm pikselleri gezmek için döngü oluşturduk. kenar pixel'lerini atladık(3x3
                                          // matrix için)
            for (int x = 1; x < w - 1; x++) {
                // xiy etrafındaki 3x3 komşularını kx, ky ile -1 den +1 e kadar dolaşıyoruz
                int valX = 0;
                int valY = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = src.getRGB(x + kx, y + ky); // komşu pixel'lerin rgb değerini alıyoruz
                        int gPos = (rgb >> 8) & 0xFF; // komşu pixel'lerin g (green channel)değeri.

                        valX += gPos * gx[ky + 1][kx + 1]; // x'ler için komşu pixel'lerin g değeri ile sobel matrix'ini
                                                           // çarpıyoruz
                        valY += gPos * gy[ky + 1][kx + 1]; // y'ler için komşu pixel'lerin g değeri ile sobel matrix'ini
                                                           // çarpıyoruz
                    }
                }

                int magnitude = (int) Math.sqrt(valX * valX + valY * valY); // x ve y değerlerini kullanarak total
                                                                            // magnitude hesaplıyoruz
                // magnitude değerini 0-255 arası bir değer olarak ayarlıyoruz.
                if (magnitude > 255)
                    magnitude = 255;
                if (magnitude < 0)
                    magnitude = 0;

                // Bulduğumuz magnitude değerini R, G, B kanalına yazarak gri tonlamalı bir
                // piksel oluşturuyoruz ve çıktı resmine kaydediyoruz
                int c = (magnitude << 16) | (magnitude << 8) | magnitude;
                out.setRGB(x, y, c); // renklerin aniden değiştiği yerler parlak beyaz (kenar), değişmediği düz
                                     // alanlar siyah olur.
            }
        }
        return out;
    }
}
