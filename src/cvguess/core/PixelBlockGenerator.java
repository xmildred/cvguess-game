package cvguess.core;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PixelBlockGenerator {

    /**
     * src: işlenmiş (kare + resize + quantize) kaynak
     * gridN: NxN blok sayısı (ama başlangıç 1x2 isteniyor -> özel durum)
     * pixelFactor: pikselasyon için downscale oranı (küçük -> daha detay)
     */
    public BufferedImage generate(BufferedImage src, int gridN, boolean startTwoBlocksMode, int pixelFactor) {
        // 1) Pixelation: downscale -> upscale
        BufferedImage pix = pixelate(src, pixelFactor);

        // 2) Coarse segmentation: blok ortalaması
        if (startTwoBlocksMode) {
            return twoBlockSegmentation(pix); // 1x2
        }
        return gridSegmentation(pix, gridN);
    }

    private BufferedImage pixelate(BufferedImage src, int factor) {
        factor = Math.max(2, factor);
        int w = src.getWidth();
        int h = src.getHeight();

        int dw = Math.max(1, w / factor);
        int dh = Math.max(1, h / factor);

        BufferedImage small = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = small.createGraphics();
        g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g1.drawImage(src, 0, 0, dw, dh, null);
        g1.dispose();

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        // nearest-neighbor hissi için bilerek interpolation kapat
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(small, 0, 0, w, h, null);
        g2.dispose();

        return out;
    }

    private BufferedImage twoBlockSegmentation(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // Sol yarı ve sağ yarı
        fillBlockWithAverage(src, out, 0, 0, w / 2, h);
        fillBlockWithAverage(src, out, w / 2, 0, w - w / 2, h);

        return out;
    }

    private BufferedImage gridSegmentation(BufferedImage src, int n) {
        n = Math.max(2, n);
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int bw = w / n;
        int bh = h / n;

        for (int gy = 0; gy < n; gy++) {
            for (int gx = 0; gx < n; gx++) {
                int x = gx * bw;
                int y = gy * bh;
                int ww = (gx == n - 1) ? (w - x) : bw;
                int hh = (gy == n - 1) ? (h - y) : bh;

                fillBlockWithAverage(src, out, x, y, ww, hh);
            }
        }

        return out;
    }

    private void fillBlockWithAverage(BufferedImage src, BufferedImage out, int x0, int y0, int ww, int hh) {
        long sr = 0, sg = 0, sb = 0;
        int count = 0;

        for (int y = y0; y < y0 + hh; y++) {
            for (int x = x0; x < x0 + ww; x++) {
                int rgb = src.getRGB(x, y);
                sr += (rgb >> 16) & 0xFF;
                sg += (rgb >> 8) & 0xFF;
                sb += rgb & 0xFF;
                count++;
            }
        }

        int r = (int)(sr / count);
        int g = (int)(sg / count);
        int b = (int)(sb / count);
        int avg = (r << 16) | (g << 8) | b;

        for (int y = y0; y < y0 + hh; y++) {
            for (int x = x0; x < x0 + ww; x++) {
                out.setRGB(x, y, avg);
            }
        }
    }
}
