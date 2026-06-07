package vue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public class WallCounterView extends JComponent {
    private static final int WIDTH = 144;
    private static final int HEIGHT = 74;

    private final int playerId;
    private final Color accentColor;
    private final BufferedImage wallImage;
    private int wallsLeft;
    private boolean currentPlayer;

    public WallCounterView(int playerId, Color accentColor) {
        this.playerId = playerId;
        this.accentColor = accentColor;
        this.wallImage = ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage("images/themes/forest/wall.png"));
        this.wallsLeft = 0;
        this.currentPlayer = false;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setToolTipText("Murs restants joueur " + playerId);
    }

    public void setWallsLeft(int wallsLeft) {
        this.wallsLeft = wallsLeft;
        repaint();
    }

    public void setCurrentPlayer(boolean currentPlayer) {
        this.currentPlayer = currentPlayer;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();
        int notch = 16;
        Polygon stone = plaqueShape(2, 3, width - 8, height - 8, notch);

        g2.setColor(new Color(0, 0, 0, 85));
        g2.fillPolygon(plaqueShape(6, 8, width - 8, height - 8, notch));

        g2.setPaint(new GradientPaint(0, 0, new Color(96, 111, 55), 0, height, new Color(34, 61, 32)));
        g2.fillPolygon(stone);

        g2.setColor(new Color(150, 183, 74, 74));
        g2.fillOval(-20, -20, 68, 48);
        g2.fillOval(width - 52, height - 34, 66, 42);

        g2.setColor(currentPlayer ? new Color(255, 248, 170) : new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(currentPlayer ? 3 : 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(stone);

        g2.setColor(new Color(255, 246, 188, 42));
        g2.drawLine(20, 10, width - 26, 10);

        drawWallIcon(g2, 12, 25, 60, 38);
        drawPlayerLabel(g2, 16, 21);
        drawCount(g2, width - 54, height / 2 + 16);

        g2.dispose();
    }

    private void drawPlayerLabel(Graphics2D g2, int x, int baseline) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(darken(accentColor, 16));
        g2.fillOval(x - 7, baseline - 16, 34, 23);
        g2.setColor(new Color(255, 244, 177));
        g2.drawString("J" + playerId, x, baseline);
    }

    private void drawCount(Graphics2D g2, int x, int baseline) {
        String text = String.valueOf(wallsLeft);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + (37 - metrics.stringWidth(text)) / 2;

        g2.setColor(new Color(0, 0, 0, 112));
        g2.fillOval(x - 5, baseline - 37, 52, 48);
        g2.setPaint(new GradientPaint(x, baseline - 38, new Color(241, 211, 112), x, baseline + 10, new Color(159, 111, 42)));
        g2.fillOval(x - 7, baseline - 39, 50, 47);
        g2.setColor(new Color(64, 74, 31));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - 7, baseline - 39, 50, 47);
        g2.setColor(new Color(33, 41, 20));
        g2.drawString(text, textX + 2, baseline + 2);
        g2.setColor(new Color(255, 247, 177));
        g2.drawString(text, textX, baseline);
    }

    private void drawWallIcon(Graphics2D g2, int x, int y, int width, int height) {
        if (wallImage != null) {
            g2.drawImage(wallImage, x - 1, y - 5, width + 8, height + 10, null);
            return;
        }

        Color barkTop = brighten(accentColor, 98);
        Color barkBottom = darken(accentColor, 60);
        Color cutFace = new Color(246, 177, 83);
        Color cutRing = new Color(119, 73, 36, 190);
        int cap = height;
        int bodyX = x + cap / 2;
        int bodyWidth = width - cap;

        g2.setColor(new Color(0, 0, 0, 85));
        g2.fillRoundRect(bodyX + 2, y + 4, bodyWidth, height, height, height);
        g2.fillOval(x + 2, y + 4, cap, height);
        g2.fillOval(x + width - cap + 2, y + 4, cap, height);

        g2.setPaint(new GradientPaint(x, y, barkTop, x, y + height, barkBottom));
        g2.fillRoundRect(bodyX, y, bodyWidth, height, height, height);
        g2.fillOval(x, y, cap, height);
        g2.fillOval(x + width - cap, y, cap, height);

        g2.setColor(new Color(255, 255, 255, 190));
        g2.fillRoundRect(bodyX + bodyWidth / 2 - 3, y + 2, 6, height - 4, 6, 6);
        g2.setColor(new Color(255, 236, 86, 220));
        g2.fillOval(bodyX + bodyWidth / 2 - 4, y + height / 2 - 4, 8, 8);

        g2.setColor(cutFace);
        g2.fillOval(x, y, cap, height);
        g2.fillOval(x + width - cap, y, cap, height);

        g2.setColor(cutRing);
        g2.setStroke(new BasicStroke(1));
        drawRing(g2, x, y, cap, height);
        drawRing(g2, x + width - cap, y, cap, height);

        g2.setColor(new Color(255, 235, 165, 115));
        g2.drawLine(bodyX + 3, y + 4, bodyX + bodyWidth - 3, y + 4);
    }

    private Polygon plaqueShape(int x, int y, int width, int height, int notch) {
        Polygon polygon = new Polygon();
        polygon.addPoint(x + notch, y);
        polygon.addPoint(x + width - notch, y);
        polygon.addPoint(x + width, y + notch);
        polygon.addPoint(x + width, y + height - notch);
        polygon.addPoint(x + width - notch, y + height);
        polygon.addPoint(x + notch, y + height);
        polygon.addPoint(x, y + height - notch);
        polygon.addPoint(x, y + notch);
        return polygon;
    }

    private void drawRing(Graphics2D g2, int x, int y, int width, int height) {
        int margin = 4;
        g2.drawOval(x + margin, y + margin, width - margin * 2, height - margin * 2);
    }

    private Color brighten(Color color, int amount) {
        return new Color(
            clamp(color.getRed() + amount),
            clamp(color.getGreen() + amount),
            clamp(color.getBlue() + amount),
            color.getAlpha()
        );
    }

    private Color darken(Color color, int amount) {
        return new Color(
            clamp(color.getRed() - amount),
            clamp(color.getGreen() - amount),
            clamp(color.getBlue() - amount),
            color.getAlpha()
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
