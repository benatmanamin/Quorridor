package vue;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.SwingConstants;

final class ButtonSupport {
    private static final String CONTROL_BUTTONS_PATH = "images/robin/tout.png";

    private ButtonSupport() {
    }

    static JButton createHomeIconButton() {
        return createControlButton(0, 0, "Menu");
    }

    static JButton createRestartButton() {
        return createControlButton(1, 0, "Recommencer");
    }

    static JButton createUndoButton() {
        return createControlButton(0, 1, "Annuler");
    }

    static JButton createRedoButton() {
        return createImageControlButton("images/themes/forest/game_buttons/redo.png", "Refaire");
    }

    static JButton createSaveButton() {
        return createImageControlButton("images/themes/forest/game_buttons/sauvegarde.png", "Sauvegarder");
    }

    static JButton createHintButton() {
        return createImageControlButton("images/themes/forest/game_buttons/hint.png", "Indice");
    }

    static JButton createAiDifficultyButton() {
        JButton button = new JButton("Niveau IA");
        button.setFocusPainted(false);
        button.setToolTipText("Changer le niveau IA");
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(new Color(37, 67, 39));
        button.setBackground(new Color(232, 196, 92));
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(new Color(92, 84, 38), 2));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(4, 8, 4, 8));
        setButtonSize(button, new Dimension(214, 58));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static JButton createSoundButton() {
        BufferedImage enabledImage = loadControlButtonImage(1, 1);
        BufferedImage disabledImage = trimTransparentBounds(
            ImageUtils.makeDarkEdgeBackgroundTransparent(ImageUtils.loadImage("images/themes/forest/game_buttons/soundoff.png"))
        );
        JButton button = new ImageIconButton(disabledImage, disabledImage, enabledImage, enabledImage);
        button.setFocusPainted(false);
        button.setToolTipText("Activer les effets");
        setButtonSize(button, new Dimension(76, 76));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static JButton createMusicButton() {
        BufferedImage enabledImage = trimTransparentBounds(
            ImageUtils.makeDarkEdgeBackgroundTransparent(ImageUtils.loadImage("images/themes/forest/game_buttons/musique.png"))
        );
        BufferedImage disabledImage = trimTransparentBounds(
            ImageUtils.makeDarkEdgeBackgroundTransparent(ImageUtils.loadImage("images/themes/forest/game_buttons/musiqueoff.png"))
        );
        JButton button = new ImageIconButton(disabledImage, disabledImage, enabledImage, enabledImage);
        button.setFocusPainted(false);
        button.setToolTipText("Activer la musique");
        setButtonSize(button, new Dimension(76, 76));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static JButton createRulesButton() {
        BufferedImage image = trimTransparentBounds(
            ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage("images/themes/forest/QUEST.png"))
        );
        JButton button = new ImageIconButton(image, image);
        button.setFocusPainted(false);
        button.setToolTipText("Regles du jeu");
        setButtonSize(button, new Dimension(86, 86));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton createControlButton(int column, int row, String tooltip) {
        BufferedImage image = loadControlButtonImage(column, row);
        JButton button = new ImageIconButton(image, image);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        setButtonSize(button, new Dimension(76, 76));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton createImageControlButton(String imagePath, String tooltip) {
        BufferedImage image = trimTransparentBounds(
            ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage(imagePath))
        );
        JButton button = new ImageIconButton(image, image);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        setButtonSize(button, new Dimension(76, 76));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static void setButtonSize(AbstractButton button, Dimension size) {
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    private static BufferedImage loadControlButtonImage(int column, int row) {
        BufferedImage sheet = ImageUtils.loadImage(CONTROL_BUTTONS_PATH);
        if (sheet == null) {
            return null;
        }

        int cellWidth = sheet.getWidth() / 2;
        int cellHeight = sheet.getHeight() / 2;
        BufferedImage cell = new BufferedImage(cellWidth, cellHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cell.createGraphics();
        graphics.drawImage(
            sheet,
            0,
            0,
            cellWidth,
            cellHeight,
            column * cellWidth,
            row * cellHeight,
            (column + 1) * cellWidth,
            (row + 1) * cellHeight,
            null
        );
        graphics.dispose();

        return trimTransparentBounds(ImageUtils.makeLightEdgeBackgroundTransparent(cell));
    }

    private static BufferedImage trimTransparentBounds(BufferedImage image) {
        if (image == null) {
            return null;
        }

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > 16) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        int contentWidth = maxX - minX + 1;
        int contentHeight = maxY - minY + 1;
        int size = Math.max(contentWidth, contentHeight);
        BufferedImage trimmed = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = trimmed.createGraphics();
        graphics.drawImage(
            image,
            (size - contentWidth) / 2,
            (size - contentHeight) / 2,
            (size - contentWidth) / 2 + contentWidth,
            (size - contentHeight) / 2 + contentHeight,
            minX,
            minY,
            maxX + 1,
            maxY + 1,
            null
        );
        graphics.dispose();
        return trimmed;
    }
}
