package vue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

final class ImageUtils {
    private ImageUtils() {
    }

    static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(findImageFile(path));
        } catch (IOException exception) {
            return null;
        }
    }

    private static File findImageFile(String path) {
        File directFile = new File(path);
        if (directFile.exists()) {
            return directFile;
        }

        File found = findImageFileFromBase(new File(System.getProperty("user.dir")), path);
        if (found.exists()) {
            return found;
        }

        try {
            File codeRoot = new File(ImageUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            found = findImageFileFromBase(codeRoot, path);
            if (found.exists()) {
                return found;
            }
        } catch (Exception ignored) {
        }

        return directFile;
    }

    private static File findImageFileFromBase(File base, String path) {
        File current = base;
        while (current != null) {
            File candidate = new File(current, path);
            if (candidate.exists()) {
                return candidate;
            }

            candidate = new File(new File(current, "prog66"), path);
            if (candidate.exists()) {
                return candidate;
            }

            candidate = new File(new File(new File(current, "prog6"), "prog66"), path);
            if (candidate.exists()) {
                return candidate;
            }

            current = current.getParentFile();
        }

        return new File(path);
    }

    static void drawImageCover(Graphics2D g2, BufferedImage image, int x, int y, int width, int height) {
        if (image == null || width <= 0 || height <= 0) {
            return;
        }

        double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawWidth = (int) Math.ceil(image.getWidth() * scale);
        int drawHeight = (int) Math.ceil(image.getHeight() * scale);
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
    }

    static BufferedImage makeLightEdgeBackgroundTransparent(BufferedImage image) {
        if (image == null) {
            return null;
        }

        return makeEdgeBackgroundTransparent(image, true);
    }

    static BufferedImage makeDarkEdgeBackgroundTransparent(BufferedImage image) {
        if (image == null) {
            return null;
        }

        return makeEdgeBackgroundTransparent(image, false);
    }

    private static BufferedImage makeEdgeBackgroundTransparent(BufferedImage image, boolean lightBackground) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage transparent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = transparent.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        boolean[] visited = new boolean[width * height];
        int[] queueX = new int[width * height];
        int[] queueY = new int[width * height];
        int head = 0;
        int tail = 0;

        for (int x = 0; x < width; x++) {
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, 0, lightBackground);
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, height - 1, lightBackground);
        }

        for (int y = 0; y < height; y++) {
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, 0, y, lightBackground);
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, width - 1, y, lightBackground);
        }

        while (head < tail) {
            int x = queueX[head];
            int y = queueY[head];
            head++;
            transparent.setRGB(x, y, 0);

            if (x > 0) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x - 1, y, lightBackground);
            }
            if (x + 1 < width) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x + 1, y, lightBackground);
            }
            if (y > 0) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, y - 1, lightBackground);
            }
            if (y + 1 < height) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, y + 1, lightBackground);
            }
        }

        return transparent;
    }

    private static int enqueueBackgroundPixel(BufferedImage image, boolean[] visited, int[] queueX, int[] queueY,
            int tail, int x, int y, boolean lightBackground) {
        int width = image.getWidth();
        int index = y * width + x;
        if (visited[index]) {
            return tail;
        }

        int rgb = image.getRGB(x, y);
        int alpha = (rgb >>> 24) & 0xff;
        boolean backgroundPixel = lightBackground ? isLightNeutralBackground(rgb) : isDarkNeutralBackground(rgb);
        if (alpha < 16 || backgroundPixel) {
            visited[index] = true;
            queueX[tail] = x;
            queueY[tail] = y;
            return tail + 1;
        }

        return tail;
    }

    private static boolean isLightNeutralBackground(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return max > 218 && max - min < 22;
    }

    private static boolean isDarkNeutralBackground(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        return max < 28;
    }
}
