package vue;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

class ImageIconButton extends JButton {
    private static final double NORMAL_SCALE = 0.88;
    private static final double HOVER_SCALE = 1.00;

    private final BufferedImage normalImage;
    private final BufferedImage hoverImage;
    private final BufferedImage selectedImage;
    private final BufferedImage selectedHoverImage;

    ImageIconButton(BufferedImage normalImage, BufferedImage hoverImage) {
        this(normalImage, hoverImage, normalImage, hoverImage);
    }

    ImageIconButton(BufferedImage normalImage, BufferedImage hoverImage,
            BufferedImage selectedImage, BufferedImage selectedHoverImage) {
        this.normalImage = normalImage;
        this.hoverImage = hoverImage;
        this.selectedImage = selectedImage;
        this.selectedHoverImage = selectedHoverImage;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (isSelected() && isEnabled()) {
            int size = Math.min(getWidth(), getHeight()) - 8;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2.setColor(new Color(255, 231, 110, 95));
            g2.fillOval(x, y, size, size);
            g2.setColor(new Color(255, 249, 183, 125));
            g2.drawOval(x + 2, y + 2, size - 4, size - 4);
        }

        boolean hover = getModel().isRollover() || getModel().isPressed();
        BufferedImage image = selectedImage();
        if (hover) {
            image = selectedHoverImage();
        }
        if (image != null) {
            if (!isEnabled()) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
            }

            double scale = hover ? HOVER_SCALE : NORMAL_SCALE;
            double fitScale = Math.min(
                (getWidth() * scale) / image.getWidth(),
                (getHeight() * scale) / image.getHeight()
            );
            int iconWidth = (int) Math.round(image.getWidth() * fitScale);
            int iconHeight = (int) Math.round(image.getHeight() * fitScale);
            int x = (getWidth() - iconWidth) / 2;
            int y = (getHeight() - iconHeight) / 2 + (getModel().isPressed() ? 2 : 0);
            g2.drawImage(image, x, y, iconWidth, iconHeight, null);
        }
        g2.dispose();
    }

    private BufferedImage selectedImage() {
        return isSelected() ? selectedImage : normalImage;
    }

    private BufferedImage selectedHoverImage() {
        return isSelected() ? selectedHoverImage : hoverImage;
    }
}
