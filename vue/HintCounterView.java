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
import javax.swing.JComponent;

public class HintCounterView extends JComponent {
    private static final int WIDTH = 94;
    private static final int HEIGHT = 74;

    
    private final Color accentColor;
    private int hintsLeft;
    private boolean currentPlayer;

    public HintCounterView(int playerId, Color accentColor) {
        
        this.accentColor = accentColor;
        this.hintsLeft = 2;
        this.currentPlayer = false;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setToolTipText("Indices restants joueur " + playerId);
    }

    public void setHintsLeft(int hintsLeft) {
        this.hintsLeft = Math.max(0, hintsLeft);
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
        int notch = 15;
        Polygon stone = plaqueShape(2, 3, width - 8, height - 8, notch);

        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillPolygon(plaqueShape(6, 8, width - 8, height - 8, notch));
        g2.setPaint(new GradientPaint(0, 0, new Color(92, 105, 53), 0, height, new Color(35, 57, 31)));
        g2.fillPolygon(stone);

        g2.setColor(new Color(255, 246, 188, 40));
        g2.drawLine(18, 10, width - 24, 10);
        g2.setColor(new Color(153, 190, 78, 60));
        g2.fillOval(-16, -18, 54, 42);
        g2.fillOval(width - 42, height - 30, 54, 38);

        g2.setColor(currentPlayer ? new Color(255, 248, 170) : new Color(255, 255, 255, 155));
        g2.setStroke(new BasicStroke(currentPlayer ? 3f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(stone);

        drawBulb(g2, 13, 17, 44);
        drawCountBubble(g2, width - 43, height / 2 + 16);
        g2.dispose();
    }

    private void drawBulb(Graphics2D g2, int x, int y, int size) {
        int centerX = x + size / 2;
        int topY = y + 5;
        int bulbSize = size - 12;

        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 235, 120, 125));
        g2.drawLine(centerX, y - 1, centerX, y + 6);
        g2.drawLine(x + 5, y + 9, x + 12, y + 15);
        g2.drawLine(x + size - 5, y + 9, x + size - 12, y + 15);

        g2.setColor(new Color(0, 0, 0, 105));
        g2.fillOval(x + 8, topY + 4, bulbSize, bulbSize);
        g2.setPaint(new GradientPaint(x, topY, new Color(255, 239, 126), x, topY + bulbSize, new Color(183, 123, 35)));
        g2.fillOval(x + 6, topY, bulbSize, bulbSize);
        g2.setColor(darken(accentColor, 50));
        g2.drawOval(x + 6, topY, bulbSize, bulbSize);

        int baseX = centerX - 10;
        int baseY = topY + bulbSize - 2;
        g2.setPaint(new GradientPaint(baseX, baseY, new Color(180, 120, 42), baseX, baseY + 15, new Color(77, 55, 24)));
        g2.fillRoundRect(baseX, baseY, 20, 14, 7, 7);
        g2.setColor(new Color(255, 221, 118, 185));
        g2.drawLine(baseX + 4, baseY + 5, baseX + 16, baseY + 5);
        g2.drawLine(baseX + 5, baseY + 10, baseX + 15, baseY + 10);
    }

    private void drawCountBubble(Graphics2D g2, int x, int baseline) {
        String text = String.valueOf(hintsLeft);
        int size = 42;
        int y = baseline - 35;

        g2.setColor(new Color(0, 0, 0, 125));
        g2.fillOval(x + 3, y + 4, size, size - 2);
        g2.setPaint(new GradientPaint(x, y, new Color(241, 211, 112), x, y + size, new Color(159, 111, 42)));
        g2.fillOval(x, y, size, size - 2);
        g2.setColor(new Color(64, 74, 31));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x, y, size, size - 2);

        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + (size - metrics.stringWidth(text)) / 2;
        int textY = y + ((size - 2) - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setColor(new Color(33, 41, 20));
        g2.drawString(text, textX + 2, textY + 2);
        g2.setColor(new Color(255, 247, 177));
        g2.drawString(text, textX, textY);
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
