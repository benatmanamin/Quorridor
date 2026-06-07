package vue;

import ia.AiDifficulty;

import java.awt.CardLayout;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class InterfaceGraphique {
    public static final int SAVE_ACTION_CANCEL = -1;
    public static final int SAVE_ACTION_LOAD = 0;
    public static final int SAVE_ACTION_DELETE = 1;

    private static final String MENU_CARD = "menu";
    private static final String MODE_CARD = "mode";
    private static final String DIFFICULTY_CARD = "difficulty";
    private static final String GAME_CARD = "game";

    private final JFrame frame;
    private final JPanel rootPanel;
    private final CardLayout cardLayout;
    private final JButton playButton;
    private final JButton quitButton;
    private final JButton playerVsPlayerButton;
    private final JButton playerVsAiButton;
    private final JButton aiVsAiButton;
    private final JButton loadSavedButton;
    private final JButton modeBackButton;
    private final JButton beginnerButton;
    private final JButton amateurButton;
    private final JButton professionalButton;
    private final JButton difficultyBackButton;
    private final JButton menuMusicButton;
    private final JButton modeMusicButton;
    private final JButton difficultyMusicButton;
    private GameView gameView;

    public InterfaceGraphique() {
        this.frame = new JFrame("Quoridor");
        this.cardLayout = new CardLayout();
        this.rootPanel = new JPanel(cardLayout);

        this.playButton = createHomeButton("images/themes/forest/button_play.png", "PLAY");
        this.quitButton = createHomeButton("images/themes/forest/button_quit.png", "QUIT");
        this.playerVsPlayerButton = createImageMenuButton("images/themes/forest/mode_buttons/playervsplayer.png", "PLAYER VS PLAYER");
        this.playerVsAiButton = createImageMenuButton("images/themes/forest/mode_buttons/playervsai.png", "PLAYER VS AI");
        this.aiVsAiButton = createImageMenuButton("images/themes/forest/mode_buttons/aivsai.png", "AI VS AI");
        this.loadSavedButton = createImageMenuButton("images/themes/forest/mode_buttons/reprendreunepartie.png", "REPRENDRE UNE PARTIE");
        this.modeBackButton = createImageMenuButton("images/themes/forest/mode_buttons/return.png", "RETURN");
        this.beginnerButton = createGameMenuButton("DEBUTANT");
        this.amateurButton = createGameMenuButton("INTERMEDIAIRE");
        this.professionalButton = createGameMenuButton("AVANCE");
        this.difficultyBackButton = createGameMenuButton("RETURN");
        this.menuMusicButton = ButtonSupport.createMusicButton();
        this.modeMusicButton = ButtonSupport.createMusicButton();
        this.difficultyMusicButton = ButtonSupport.createMusicButton();

        rootPanel.add(createMenuPanel(), MENU_CARD);
        rootPanel.add(createModePanel(), MODE_CARD);
        rootPanel.add(createDifficultyPanel(), DIFFICULTY_CARD);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(rootPanel);
        frame.setPreferredSize(new Dimension(1280, 720));
        frame.setMinimumSize(new Dimension(980, 552));
    }

    public void afficher() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void afficherMenu() {
        cardLayout.show(rootPanel, MENU_CARD);
    }

    public void afficherMode() {
        cardLayout.show(rootPanel, MODE_CARD);
    }

    public void afficherDifficulte() {
        cardLayout.show(rootPanel, DIFFICULTY_CARD);
    }

    public void afficherJeu(GameView nextGameView) {
        if (gameView != null) {
            rootPanel.remove(gameView);
        }

        gameView = nextGameView;
        rootPanel.add(gameView, GAME_CARD);
        cardLayout.show(rootPanel, GAME_CARD);
        frame.revalidate();
        frame.repaint();
        gameView.requestBoardFocusLater();
    }

    public void fermer() {
        frame.dispose();
    }

    public void onNouvellePartie(ActionListener listener) {
        playButton.addActionListener(listener);
    }

    public void onQuitter(ActionListener listener) {
        quitButton.addActionListener(listener);
    }

    public void onModePlayerVsPlayer(ActionListener listener) {
        playerVsPlayerButton.addActionListener(listener);
    }

    public void onModePlayerVsAi(ActionListener listener) {
        playerVsAiButton.addActionListener(listener);
    }

    public void onModeAiVsAi(ActionListener listener) {
        aiVsAiButton.addActionListener(listener);
    }

    public void onReprendrePartie(ActionListener listener) {
        loadSavedButton.addActionListener(listener);
    }

    public void onRetourMode(ActionListener listener) {
        modeBackButton.addActionListener(listener);
    }

    public void onNiveauBeginner(ActionListener listener) {
        beginnerButton.addActionListener(listener);
    }

    public void onNiveauAmateur(ActionListener listener) {
        amateurButton.addActionListener(listener);
    }

    public void onNiveauProfessional(ActionListener listener) {
        professionalButton.addActionListener(listener);
    }

    public void onRetourDifficulte(ActionListener listener) {
        difficultyBackButton.addActionListener(listener);
    }

    public void onMusic(ActionListener listener) {
        menuMusicButton.addActionListener(listener);
        modeMusicButton.addActionListener(listener);
        difficultyMusicButton.addActionListener(listener);
    }

    public void setMusicEnabled(boolean enabled) {
        menuMusicButton.setSelected(enabled);
        modeMusicButton.setSelected(enabled);
        difficultyMusicButton.setSelected(enabled);
        setMusicTooltip(menuMusicButton, enabled);
        setMusicTooltip(modeMusicButton, enabled);
        setMusicTooltip(difficultyMusicButton, enabled);
        if (gameView != null) {
            gameView.setMusicEnabled(enabled);
        }
    }

    public Object choisirDansListe(String titre, String message, Object[] choix) {
        if (choix == null || choix.length == 0) {
            return null;
        }

        return JOptionPane.showInputDialog(
            frame,
            message,
            titre,
            JOptionPane.PLAIN_MESSAGE,
            null,
            choix,
            choix[0]
        );
    }

    public AiDifficulty[] choisirDifficultesAiVsAi(AiDifficulty defaultPlayer1, AiDifficulty defaultPlayer2) {
        JComboBox<AiDifficulty> player1Selector = createAiDifficultySelector(defaultPlayer1);
        JComboBox<AiDifficulty> player2Selector = createAiDifficultySelector(defaultPlayer2);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 10, 14);
        panel.add(new JLabel("IA joueur 1"), constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 10, 0);
        panel.add(player1Selector, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 14);
        panel.add(new JLabel("IA joueur 2"), constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(player2Selector, constraints);

        int result = JOptionPane.showConfirmDialog(
            frame,
            panel,
            "AI vs AI",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        return new AiDifficulty[] {
            (AiDifficulty) player1Selector.getSelectedItem(),
            (AiDifficulty) player2Selector.getSelectedItem()
        };
    }

    public int choisirActionSauvegarde(Object sauvegarde) {
        Object[] options = {"Reprendre", "Supprimer", "Annuler"};
        int action = JOptionPane.showOptionDialog(
            frame,
            "Que veux-tu faire avec cette sauvegarde ?\n" + sauvegarde,
            "Sauvegarde",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        if (action == 0) {
            return SAVE_ACTION_LOAD;
        }
        if (action == 1) {
            return SAVE_ACTION_DELETE;
        }
        return SAVE_ACTION_CANCEL;
    }

    public boolean confirmerSuppressionSauvegarde(Object sauvegarde) {
        int result = JOptionPane.showConfirmDialog(
            frame,
            "Supprimer definitivement cette sauvegarde ?\n" + sauvegarde,
            "Supprimer une sauvegarde",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    public void afficherMessage(String titre, String message, int messageType) {
        JOptionPane.showMessageDialog(frame, message, titre, messageType);
    }

    private JComponent createMenuPanel() {
        return createMenuScreen(
            "Deux joueurs, des murs, une ligne d'arrivee.",
            true,
            menuMusicButton,
            playButton,
            quitButton
        );
    }

    private JComponent createModePanel() {
        return createMenuScreen(
            "Choisis le mode de jeu.",
            false,
            modeMusicButton,
            playerVsPlayerButton,
            playerVsAiButton,
            aiVsAiButton,
            loadSavedButton,
            modeBackButton
        );
    }

    private JComponent createDifficultyPanel() {
        return createMenuScreen(
            "Choisis le niveau de l'IA.",
            false,
            difficultyMusicButton,
            beginnerButton,
            amateurButton,
            professionalButton,
            difficultyBackButton
        );
    }

    private JComponent createMenuScreen(String subtitle, boolean homeScreen, JButton musicButton, JButton... buttons) {
        JPanel menuPanel = new ThemedMenuPanel(homeScreen);
        menuPanel.setLayout(new GridBagLayout());
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                menuPanel.setBounds(0, 0, getWidth(), getHeight());
                int margin = 24;
                int buttonWidth = musicButton.getPreferredSize().width;
                int buttonHeight = musicButton.getPreferredSize().height;
                musicButton.setBounds(
                    Math.max(margin, getWidth() - buttonWidth - margin),
                    Math.max(margin, getHeight() - buttonHeight - 18),
                    buttonWidth,
                    buttonHeight
                );
            }
        };

        JPanel menuBox = new JPanel(new GridBagLayout());
        menuBox.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        int startRow = 0;
        if (!homeScreen) {
            JComponent titleLabel = new ImageTitle("images/themes/forest/quoridor.png");

            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setForeground(new Color(255, 239, 174));
            subtitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

            constraints.insets = new Insets(10, 20, 10, 20);
            constraints.gridy = 0;
            menuBox.add(titleLabel, constraints);
            constraints.gridy = 1;
            constraints.insets = new Insets(2, 20, 26, 20);
            menuBox.add(subtitleLabel, constraints);
            startRow = 2;
        }

        constraints.insets = homeScreen ? new Insets(4, 90, 4, 90) : new Insets(2, 110, 2, 110);
        for (int index = 0; index < buttons.length; index++) {
            constraints.gridy = index + startRow;
            menuBox.add(buttons[index], constraints);
        }

        GridBagConstraints boxConstraints = new GridBagConstraints();
        boxConstraints.gridx = 0;
        boxConstraints.gridy = 1;
        boxConstraints.gridwidth = 2;
        boxConstraints.weightx = 1.0;
        boxConstraints.weighty = 1.0;
        boxConstraints.anchor = homeScreen ? GridBagConstraints.SOUTH : GridBagConstraints.CENTER;
        boxConstraints.insets = homeScreen ? new Insets(0, 0, 46, 0) : new Insets(0, 0, 0, 0);
        menuPanel.add(menuBox, boxConstraints);
        layeredPane.add(menuPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(musicButton, JLayeredPane.PALETTE_LAYER);
        return layeredPane;
    }

    private void setMusicTooltip(JButton button, boolean enabled) {
        button.setToolTipText(enabled ? "Couper la musique" : "Activer la musique");
    }

    private JButton createHomeButton(String imagePath, String tooltip) {
        JButton button = new ThemedHomeButton(tooltip, ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage(imagePath)));
        Dimension size = new Dimension(350, 164);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(tooltip);
        return button;
    }

    private JButton createGameMenuButton(String text) {
        JButton button = new ThemedGameMenuButton(
            text,
            ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage("images/themes/forest/button_play.png"))
        );
        Dimension size = new Dimension(318, 92);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 20));
        return button;
    }

    private JButton createImageMenuButton(String imagePath, String tooltip) {
        JButton button = new DirectImageButton(ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage(imagePath)));
        Dimension size = new Dimension(286, 92);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(tooltip);
        return button;
    }

    private JComboBox<AiDifficulty> createAiDifficultySelector(AiDifficulty selectedDifficulty) {
        JComboBox<AiDifficulty> selector = new JComboBox<>(AiDifficulty.values());
        selector.setSelectedItem(selectedDifficulty == null ? AiDifficulty.INTERMEDIATE : selectedDifficulty);
        selector.setFocusable(false);
        selector.setFont(new Font("SansSerif", Font.BOLD, 14));
        selector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                java.awt.Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AiDifficulty) {
                    setText(((AiDifficulty) value).getLabel());
                }
                return component;
            }
        });
        return selector;
    }

    private String menuBackgroundPath(boolean homeScreen) {
        return homeScreen ? "images/themes/forest/menu_home.png" : "images/themes/forest/menu_background.png";
    }

    private class ThemedMenuPanel extends JPanel {
        private final boolean homeScreen;

        private ThemedMenuPanel(boolean homeScreen) {
            this.homeScreen = homeScreen;
            setBackground(new Color(23, 43, 31));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            BufferedImage background = ImageUtils.loadImage(menuBackgroundPath(homeScreen));
            ImageUtils.drawImageCover(g2, background, 0, 0, getWidth(), getHeight());
            if (!homeScreen) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
                g2.setColor(new Color(8, 17, 16));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    private class ImageTitle extends JComponent {
        private final BufferedImage image;

        private ImageTitle(String imagePath) {
            this.image = ImageUtils.makeLightEdgeBackgroundTransparent(ImageUtils.loadImage(imagePath));
            setPreferredSize(new Dimension(640, 178));
            setMinimumSize(new Dimension(420, 118));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (image == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            double scale = Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;
            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            g2.dispose();
        }
    }

    private class DirectImageButton extends JButton {
        private final BufferedImage normalImage;
        private final BufferedImage hoverImage;

        private DirectImageButton(BufferedImage image) {
            this.normalImage = image;
            this.hoverImage = createHighlightedImage(image);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (normalImage == null) {
                super.paintComponent(graphics);
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            BufferedImage image = getModel().isRollover() || getModel().isPressed() ? hoverImage : normalImage;
            int offset = getModel().isPressed() ? 2 : 0;
            g2.drawImage(image, 0, offset, getWidth(), getHeight(), null);
            g2.dispose();
        }

        private BufferedImage createHighlightedImage(BufferedImage source) {
            if (source == null) {
                return null;
            }

            BufferedImage highlighted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    int red = Math.min(255, ((argb >> 16) & 0xff) + 28);
                    int green = Math.min(255, ((argb >> 8) & 0xff) + 28);
                    int blue = Math.min(255, (argb & 0xff) + 18);
                    highlighted.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
                }
            }
            return highlighted;
        }
    }

    private class ThemedGameMenuButton extends JButton {
        private final BufferedImage normalImage;
        private final BufferedImage hoverImage;

        private ThemedGameMenuButton(String text, BufferedImage image) {
            super(text);
            this.normalImage = image;
            this.hoverImage = createHighlightedImage(normalImage);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (normalImage == null) {
                super.paintComponent(graphics);
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed();
            int offset = pressed ? 2 : 0;
            BufferedImage image = hover || pressed ? hoverImage : normalImage;

            if (hover || pressed) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
                g2.drawImage(hoverImage, -8, offset - 5, width + 16, height + 10, null);
                g2.setComposite(AlphaComposite.SrcOver);
            }

            g2.drawImage(image, 0, offset, width, height, null);
            drawTextPlate(g2, width, height, offset, hover || pressed);
            g2.setComposite(AlphaComposite.SrcOver);
            drawCenteredButtonText(g2, width, height, offset);
            g2.dispose();
        }

        private void drawTextPlate(Graphics2D g2, int width, int height, int offset, boolean hover) {
            int plateWidth = Math.max(170, width - 116);
            int plateHeight = Math.max(42, height - 42);
            int plateX = (width - plateWidth) / 2;
            int plateY = (height - plateHeight) / 2 + offset;
            int notch = Math.max(10, plateHeight / 5);

            java.awt.Polygon plate = buttonShape(plateX, plateY, plateWidth, plateHeight, notch);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.94f));
            g2.setPaint(new java.awt.GradientPaint(
                plateX,
                plateY,
                hover ? new Color(244, 216, 126) : new Color(223, 190, 101),
                plateX,
                plateY + plateHeight,
                hover ? new Color(174, 129, 47) : new Color(143, 101, 37)
            ));
            g2.fillPolygon(plate);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setStroke(new java.awt.BasicStroke(2.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(hover ? new Color(255, 244, 160) : new Color(75, 70, 37));
            g2.drawPolygon(plate);
            g2.setColor(new Color(255, 248, 181, hover ? 155 : 105));
            g2.drawLine(plateX + notch, plateY + 8, plateX + plateWidth - notch, plateY + 8);
        }

        private void drawCenteredButtonText(Graphics2D g2, int width, int height, int offset) {
            String label = getText();
            int fontSize = getFont().getSize();
            Font font = getFont().deriveFont(Font.BOLD, (float) fontSize);
            g2.setFont(font);
            java.awt.FontMetrics metrics = g2.getFontMetrics();
            while (fontSize > 12 && metrics.stringWidth(label) > width - 44) {
                fontSize--;
                font = getFont().deriveFont(Font.BOLD, (float) fontSize);
                g2.setFont(font);
                metrics = g2.getFontMetrics();
            }

            int textX = (width - metrics.stringWidth(label)) / 2;
            int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent() + offset;
            g2.setColor(new Color(15, 34, 13, 190));
            g2.drawString(label, textX + 2, textY + 3);
            g2.setColor(new Color(48, 106, 30));
            g2.drawString(label, textX, textY);
        }

        private java.awt.Polygon buttonShape(int x, int y, int width, int height, int notch) {
            java.awt.Polygon polygon = new java.awt.Polygon();
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

        private BufferedImage createHighlightedImage(BufferedImage source) {
            if (source == null) {
                return null;
            }

            BufferedImage highlighted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    int red = Math.min(255, ((argb >> 16) & 0xff) + 30);
                    int green = Math.min(255, ((argb >> 8) & 0xff) + 34);
                    int blue = Math.min(255, (argb & 0xff) + 18);
                    highlighted.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
                }
            }
            return highlighted;
        }
    }

    private class ThemedHomeButton extends JButton {
        private final BufferedImage normalImage;
        private final BufferedImage hoverImage;

        private ThemedHomeButton(String text, BufferedImage image) {
            super(text);
            this.normalImage = image;
            this.hoverImage = createHighlightedImage(image);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (normalImage == null) {
                super.paintComponent(graphics);
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed();
            BufferedImage image = hover || pressed ? hoverImage : normalImage;

            double scale = Math.min((getWidth() - 10) / (double) image.getWidth(), (getHeight() - 8) / (double) image.getHeight());
            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2 + (pressed ? 2 : 0);

            if (hover || pressed) {
                int glowWidth = Math.min(getWidth(), drawWidth + 18);
                int glowHeight = Math.min(getHeight(), drawHeight + 12);
                int glowX = (getWidth() - glowWidth) / 2;
                int glowY = (getHeight() - glowHeight) / 2 + (pressed ? 2 : 0);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
                g2.drawImage(hoverImage, glowX, glowY, glowWidth, glowHeight, null);
                g2.setComposite(AlphaComposite.SrcOver);
            }

            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            g2.dispose();
        }

        private BufferedImage createHighlightedImage(BufferedImage source) {
            if (source == null) {
                return null;
            }

            BufferedImage highlighted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    if (alpha == 0) {
                        highlighted.setRGB(x, y, 0);
                        continue;
                    }

                    int red = Math.min(255, ((argb >> 16) & 0xff) + 38);
                    int green = Math.min(255, ((argb >> 8) & 0xff) + 42);
                    int blue = Math.min(255, (argb & 0xff) + 18);
                    int boostedAlpha = Math.min(255, alpha + 18);
                    highlighted.setRGB(x, y, (boostedAlpha << 24) | (red << 16) | (green << 8) | blue);
                }
            }

            return highlighted;
        }
    }

}
