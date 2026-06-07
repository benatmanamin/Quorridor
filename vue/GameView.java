package vue;

import ia.AiDifficulty;
import modele.GameModel;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GameView extends JPanel {
    private static final String BACKGROUND_PATH = "images/themes/forest/BACKGROUND.png";
    private static final Color BACKGROUND_COLOR = new Color(18, 48, 30);
    private static final Color BACKGROUND_OVERLAY = new Color(4, 24, 13, 35);

    private final BoardView boardView;
    private final BufferedImage backgroundImage;
    private final RulesOverlay rulesOverlay;
    private final JButton menuButton;
    private final JButton restartButton;
    private final JButton undoButton;
    private final JButton redoButton;
    private final JButton saveButton;
    private final JButton hintButton;
    private final JButton aiDifficultyButton;
    private final JButton soundButton;
    private final JButton musicButton;
    private final JButton rulesButton;
    private final WallCounterView player1WallCounter;
    private final WallCounterView player2WallCounter;
    private final HintCounterView player1HintCounter;
    private final HintCounterView player2HintCounter;
    private Component player1HintGap;
    private Component player2HintGap;
    private Component aiDifficultyGap;

    public GameView(GameModel model) {
        this.backgroundImage = ImageUtils.loadImage(BACKGROUND_PATH);
        this.boardView = new BoardView(model);
        this.boardView.setPaintFullBackground(false);
        this.rulesOverlay = new RulesOverlay();
        this.menuButton = ButtonSupport.createHomeIconButton();
        this.restartButton = ButtonSupport.createRestartButton();
        this.undoButton = ButtonSupport.createUndoButton();
        this.redoButton = ButtonSupport.createRedoButton();
        this.saveButton = ButtonSupport.createSaveButton();
        this.hintButton = ButtonSupport.createHintButton();
        this.aiDifficultyButton = ButtonSupport.createAiDifficultyButton();
        this.soundButton = ButtonSupport.createSoundButton();
        this.musicButton = ButtonSupport.createMusicButton();
        this.rulesButton = ButtonSupport.createRulesButton();
        this.player1WallCounter = new WallCounterView(1, new Color(41, 126, 216));
        this.player2WallCounter = new WallCounterView(2, new Color(255, 126, 69));
        this.player1HintCounter = new HintCounterView(1, new Color(41, 126, 216));
        this.player2HintCounter = new HintCounterView(2, new Color(255, 126, 69));
        this.aiDifficultyGap = null;
        setSoundEnabled(false);
        this.rulesButton.addActionListener(event -> showRulesMessage());

        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        add(createGameLayer(createGameArea()), BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (backgroundImage != null) {
            ImageUtils.drawImageCover(g2, backgroundImage, 0, 0, getWidth(), getHeight());
            g2.setColor(BACKGROUND_OVERLAY);
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g2.setPaint(new GradientPaint(0, 0, BACKGROUND_COLOR.brighter(), getWidth(), getHeight(), BACKGROUND_COLOR.darker()));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.dispose();
    }

    public BoardView getBoardView() {
        return boardView;
    }

    public void addMenuListener(ActionListener listener) {
        menuButton.addActionListener(listener);
    }

    public void addRestartListener(ActionListener listener) {
        restartButton.addActionListener(listener);
    }

    public void addUndoListener(ActionListener listener) {
        undoButton.addActionListener(listener);
    }

    public void addRedoListener(ActionListener listener) {
        redoButton.addActionListener(listener);
    }

    public void addSaveListener(ActionListener listener) {
        saveButton.addActionListener(listener);
    }

    public void addHintListener(ActionListener listener) {
        hintButton.addActionListener(listener);
    }

    public void addAiDifficultyListener(ActionListener listener) {
        aiDifficultyButton.addActionListener(listener);
    }

    public void addSoundListener(ActionListener listener) {
        soundButton.addActionListener(listener);
    }

    public void addMusicListener(ActionListener listener) {
        musicButton.addActionListener(listener);
    }

    public void setUndoEnabled(boolean enabled) {
        undoButton.setEnabled(enabled);
        undoButton.setToolTipText(enabled ? "Annuler" : "Aucun coup a annuler");
    }

    public void setRedoEnabled(boolean enabled) {
        redoButton.setEnabled(enabled);
        redoButton.setToolTipText(enabled ? "Refaire" : "Aucun coup a refaire");
    }

    public void setSaveEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        saveButton.setToolTipText(enabled ? "Sauvegarder" : "Sauvegarde indisponible");
    }

    public void setHintEnabled(boolean enabled, int hintsLeft) {
        hintButton.setEnabled(enabled);
        hintButton.setToolTipText(enabled ? "Indice (" + hintsLeft + " restants)" : "Aucun indice disponible");
    }

    public void setAiDifficultyControlsVisible(boolean visible) {
        aiDifficultyButton.setVisible(visible);
        if (aiDifficultyGap != null) {
            aiDifficultyGap.setVisible(visible);
        }
        if (!visible) {
            aiDifficultyButton.setEnabled(false);
        }
        revalidate();
        repaint();
    }

    public void setAiDifficultyEnabled(boolean enabled) {
        aiDifficultyButton.setEnabled(enabled);
        aiDifficultyButton.setToolTipText(enabled ? "Changer le niveau IA" : "Niveau IA indisponible");
    }

    public void setAiDifficultyText(String title, String... details) {
        StringBuilder text = new StringBuilder("<html><center>");
        text.append(escapeHtml(title == null || title.trim().isEmpty() ? "Niveau IA" : title.trim()));

        if (details != null) {
            for (String detail : details) {
                if (detail != null && !detail.trim().isEmpty()) {
                    text.append("<br>");
                    text.append(escapeHtml(detail.trim()));
                }
            }
        }

        text.append("</center></html>");
        aiDifficultyButton.setText(text.toString());
        revalidate();
        repaint();
    }

    public void setSoundEnabled(boolean enabled) {
        soundButton.setSelected(enabled);
        soundButton.setToolTipText(enabled ? "Couper les effets" : "Activer les effets");
    }

    public void setMusicEnabled(boolean enabled) {
        musicButton.setSelected(enabled);
        musicButton.setToolTipText(enabled ? "Couper la musique" : "Activer la musique");
    }

    public void setHintControlsVisible(boolean player1HintsVisible, boolean player2HintsVisible) {
        boolean hintButtonVisible = player1HintsVisible || player2HintsVisible;
        hintButton.setVisible(hintButtonVisible);
        if (!hintButtonVisible) {
            hintButton.setEnabled(false);
        }

        player1HintCounter.setVisible(player1HintsVisible);
        player2HintCounter.setVisible(player2HintsVisible);
        if (player1HintGap != null) {
            player1HintGap.setVisible(player1HintsVisible);
        }
        if (player2HintGap != null) {
            player2HintGap.setVisible(player2HintsVisible);
        }

        revalidate();
        repaint();
    }

    public void setStatusText(String text) {
        boardView.setMessage(text);
    }

    public void updateWallCounters(int player1Walls, int player2Walls, int currentPlayerId) {
        player1WallCounter.setWallsLeft(player1Walls);
        player2WallCounter.setWallsLeft(player2Walls);
        player1WallCounter.setCurrentPlayer(currentPlayerId == 1);
        player2WallCounter.setCurrentPlayer(currentPlayerId == 2);
    }

    public void updateHintCounters(int player1Hints, int player2Hints, int currentPlayerId) {
        player1HintCounter.setHintsLeft(player1Hints);
        player2HintCounter.setHintsLeft(player2Hints);
        player1HintCounter.setCurrentPlayer(currentPlayerId == 1);
        player2HintCounter.setCurrentPlayer(currentPlayerId == 2);
    }

    public void requestBoardFocusLater() {
        SwingUtilities.invokeLater(boardView::requestFocusInWindow);
    }

    public AiDifficulty[] chooseAiDifficulties(boolean player1Ai, boolean player2Ai,
            AiDifficulty player1Difficulty, AiDifficulty player2Difficulty) {
        JPanel panel = new JPanel(new GridBagLayout());
        JComboBox<AiDifficulty> player1Selector = player1Ai ? createAiDifficultySelector(player1Difficulty) : null;
        JComboBox<AiDifficulty> player2Selector = player2Ai ? createAiDifficultySelector(player2Difficulty) : null;
        int row = 0;

        if (player1Ai) {
            addDifficultyRow(panel, row++, "IA joueur 1", player1Selector);
        }
        if (player2Ai) {
            addDifficultyRow(panel, row++, "IA joueur 2", player2Selector);
        }

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Niveau IA",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        return new AiDifficulty[] {
            player1Selector == null ? player1Difficulty : (AiDifficulty) player1Selector.getSelectedItem(),
            player2Selector == null ? player2Difficulty : (AiDifficulty) player2Selector.getSelectedItem()
        };
    }

    private JComboBox<AiDifficulty> createAiDifficultySelector(AiDifficulty selectedDifficulty) {
        JComboBox<AiDifficulty> selector = new JComboBox<>(AiDifficulty.values());
        selector.setSelectedItem(selectedDifficulty == null ? AiDifficulty.INTERMEDIATE : selectedDifficulty);
        selector.setFocusable(false);
        selector.setFont(new Font("SansSerif", Font.BOLD, 14));
        return selector;
    }

    private void addDifficultyRow(JPanel panel, int row, String label, JComboBox<AiDifficulty> selector) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 14);
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 0);
        panel.add(selector, constraints);
    }

    private void showRulesMessage() {
        rulesOverlay.setVisible(true);
        rulesOverlay.repaint();
    }

    private JComponent createGameLayer(JComponent gameArea) {
        JLayeredPane layer = new JLayeredPane() {
            @Override
            public void doLayout() {
                gameArea.setBounds(0, 0, getWidth(), getHeight());
                rulesOverlay.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        layer.setOpaque(false);
        layer.add(gameArea, JLayeredPane.DEFAULT_LAYER);
        layer.add(rulesOverlay, JLayeredPane.POPUP_LAYER);
        return layer;
    }

    private JPanel createGameArea() {
        JPanel gameArea = new JPanel(new BorderLayout(12, 0));
        gameArea.setOpaque(false);
        gameArea.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        gameArea.add(createControlRail(), BorderLayout.WEST);
        gameArea.add(boardView, BorderLayout.CENTER);
        gameArea.add(createInfoRail(), BorderLayout.EAST);
        return gameArea;
    }

    private JPanel createControlRail() {
        JPanel rail = createSideRail(104);
        JPanel stack = createRailStack();

        addRailComponent(stack, menuButton);
        stack.add(Box.createVerticalStrut(10));
        addRailComponent(stack, restartButton);
        stack.add(Box.createVerticalStrut(10));
        addRailComponent(stack, undoButton);
        stack.add(Box.createVerticalStrut(10));
        addRailComponent(stack, redoButton);
        stack.add(Box.createVerticalStrut(10));
        addRailComponent(stack, saveButton);
        stack.add(Box.createVerticalStrut(10));
        addRailComponent(stack, hintButton);
        stack.add(Box.createVerticalStrut(10));
        addRailComponent(stack, soundButton);

        centerInRail(rail, stack);
        return rail;
    }

    private JPanel createInfoRail() {
        JPanel rail = createSideRail(254);
        JPanel stack = createRailStack();

        addRailComponent(stack, createPlayerResourcePanel(player2WallCounter, player2HintCounter));
        stack.add(Box.createVerticalStrut(16));
        addRailComponent(stack, createPlayerResourcePanel(player1WallCounter, player1HintCounter));
        aiDifficultyGap = Box.createVerticalStrut(18);
        stack.add(aiDifficultyGap);
        addRailComponent(stack, aiDifficultyButton);

        GridBagConstraints stackConstraints = new GridBagConstraints();
        stackConstraints.gridx = 0;
        stackConstraints.gridy = 0;
        stackConstraints.weighty = 1.0;
        stackConstraints.anchor = GridBagConstraints.CENTER;
        rail.add(stack, stackConstraints);

        GridBagConstraints rulesConstraints = new GridBagConstraints();
        rulesConstraints.gridx = 0;
        rulesConstraints.gridy = 1;
        rulesConstraints.anchor = GridBagConstraints.SOUTHEAST;
        rulesConstraints.insets = new Insets(0, 0, 8, 0);
        rail.add(createBottomInfoButtons(), rulesConstraints);
        return rail;
    }

    private JPanel createBottomInfoButtons() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        addRailComponent(panel, musicButton);
        panel.add(Box.createVerticalStrut(8));
        addRailComponent(panel, rulesButton);
        return panel;
    }

    private JPanel createPlayerResourcePanel(WallCounterView wallCounter, HintCounterView hintCounter) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(wallCounter);
        Component hintGap = Box.createHorizontalStrut(8);
        panel.add(hintGap);
        panel.add(hintCounter);
        if (hintCounter == player1HintCounter) {
            player1HintGap = hintGap;
        } else if (hintCounter == player2HintCounter) {
            player2HintGap = hintGap;
        }
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private JPanel createSideRail(int width) {
        JPanel rail = new JPanel(new GridBagLayout());
        rail.setOpaque(false);
        rail.setPreferredSize(new Dimension(width, 1));
        rail.setMinimumSize(new Dimension(width, 1));
        rail.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return rail;
    }

    private JPanel createRailStack() {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        return stack;
    }

    private void addRailComponent(JPanel stack, JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(component);
    }

    private void centerInRail(JPanel rail, JPanel stack) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        rail.add(stack, constraints);
    }

    private String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private class RulesOverlay extends JComponent {
        private final String[] rules = {
            "Objectif : atteindre la ligne opposee avant l'autre joueur.",
            "A ton tour : deplace ton pion ou pose un mur.",
            "Deplacement : avance d'une case horizontalement ou verticalement.",
            "Si l'adversaire est devant et que le passage est libre, saute par-dessus.",
            "Murs : un mur bloque un passage entre deux cases.",
            "Un mur ne peut pas chevaucher un autre mur.",
            "Il faut toujours laisser au moins un chemin aux deux joueurs.",
            "Le premier joueur qui atteint sa ligne d'arrivee gagne."
        };

        private RulesOverlay() {
            setOpaque(false);
            setVisible(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    setVisible(false);
                    requestBoardFocusLater();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2.setColor(new Color(3, 12, 8, 132));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int panelWidth = Math.min(getWidth() - 120, 640);
            int panelHeight = 360;
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;
            int arc = 30;

            g2.setColor(new Color(0, 0, 0, 125));
            g2.fillRoundRect(panelX + 7, panelY + 9, panelWidth, panelHeight, arc, arc);
            g2.setPaint(new GradientPaint(panelX, panelY, new Color(245, 213, 119), panelX, panelY + panelHeight, new Color(111, 82, 39)));
            g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, arc, arc);
            g2.setColor(new Color(47, 72, 55));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, arc, arc);

            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            FontMetrics titleMetrics = g2.getFontMetrics();
            String title = "Regles du Quoridor";
            int titleX = panelX + (panelWidth - titleMetrics.stringWidth(title)) / 2;
            int titleY = panelY + 50;
            g2.setColor(new Color(43, 35, 19, 150));
            g2.drawString(title, titleX + 2, titleY + 3);
            g2.setColor(new Color(37, 67, 39));
            g2.drawString(title, titleX, titleY);

            g2.setFont(new Font("SansSerif", Font.BOLD, 17));
            FontMetrics metrics = g2.getFontMetrics();
            int textX = panelX + 38;
            int textY = panelY + 88;
            int lineHeight = metrics.getHeight() + 5;
            g2.setColor(new Color(39, 31, 18));
            for (String line : rules) {
                g2.drawString(line, textX, textY);
                textY += lineHeight;
            }

            String closeText = "Clique pour fermer";
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics closeMetrics = g2.getFontMetrics();
            int closeX = panelX + (panelWidth - closeMetrics.stringWidth(closeText)) / 2;
            int closeY = panelY + panelHeight - 28;
            g2.setColor(new Color(255, 248, 199, 210));
            g2.drawString(closeText, closeX, closeY);

            g2.dispose();
        }
    }
}
