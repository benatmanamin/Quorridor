package vue;

import modele.Board;
import modele.GameModel;
import modele.Player;
import modele.Position;
import modele.Wall;
import modele.WallOrientation;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardView extends JPanel {
    private static final int PREFERRED_SIZE = 760;
    private static final int MIN_PADDING = 8;
    private static final int PULSE_TIMER_DELAY_MS = 160;
    private static final int MESSAGE_DURATION_MS = 1800;
    private static final int HINT_DURATION_MS = 5200;
    private static final int OPENING_STORY_DURATION_MS = 5200;
    private final GameModel model;
    private final Timer messageTimer;
    private final Timer hintTimer;
    private final Timer openingStoryTimer;
    private BufferedImage themeBackgroundImage;
    private BufferedImage themeCellImage;
    private BufferedImage themeWallImage;
    private Rectangle themeWallSourceBounds;
    private BufferedImage staticLayer;
    private Theme cachedStaticTheme;
    private int cachedStaticWidth;
    private int cachedStaticHeight;
    private final Theme theme;
    private Wall previewWall;
    private Wall hintWall;
    private Position hintPosition;
    private List<Position> highlightedPositions;
    private boolean paintFullBackground;
    private boolean currentPlayerPulseVisible;
    private boolean openingStoryVisible;
    private String message;

    public BoardView(GameModel model) {
        this.model = model;
        this.theme = Theme.JUNGLE;
        loadThemeAssets();
        this.previewWall = null;
        this.hintWall = null;
        this.hintPosition = null;
        this.highlightedPositions = new ArrayList<>();
        this.paintFullBackground = true;
        this.currentPlayerPulseVisible = true;
        this.openingStoryVisible = true;
        this.message = "";
        this.messageTimer = new Timer(MESSAGE_DURATION_MS, event -> setMessage(""));
        this.messageTimer.setRepeats(false);
        this.hintTimer = new Timer(HINT_DURATION_MS, event -> clearHint());
        this.hintTimer.setRepeats(false);
        this.openingStoryTimer = new Timer(OPENING_STORY_DURATION_MS, event -> {
            openingStoryVisible = false;
            repaintRectangle(boardBounds());
        });
        this.openingStoryTimer.setRepeats(false);
        this.openingStoryTimer.start();

        setPreferredSize(new Dimension(PREFERRED_SIZE, PREFERRED_SIZE));
        setBackground(theme.background);
        setOpaque(true);

        new Timer(PULSE_TIMER_DELAY_MS, event -> {
            boolean nextPulseVisible = (System.currentTimeMillis() / 420) % 2 == 0;
            if (nextPulseVisible == currentPlayerPulseVisible && !model.getState().isGameOver()) {
                return;
            }

            currentPlayerPulseVisible = nextPulseVisible;
            if (model.getState().isGameOver()) {
                repaint();
            } else {
                repaintCurrentPlayerArea();
            }
        }).start();
    }

    public void setPaintFullBackground(boolean paintFullBackground) {
        if (this.paintFullBackground == paintFullBackground) {
            return;
        }

        this.paintFullBackground = paintFullBackground;
        setOpaque(paintFullBackground);
        invalidateStaticLayer();
        repaint();
    }

    public void setMessage(String message) {
        String nextMessage = message == null ? "" : message.trim();
        if (nextMessage.equals(this.message)) {
            if (!nextMessage.isEmpty()) {
                messageTimer.restart();
            }
            return;
        }

        this.message = nextMessage;
        if (this.message.isEmpty()) {
            messageTimer.stop();
        } else {
            messageTimer.restart();
        }
        repaint();
    }

    public void setPreviewWall(Wall previewWall) {
        if (this.previewWall == null ? previewWall == null : this.previewWall.equals(previewWall)) {
            return;
        }

        Wall previousWall = this.previewWall;
        this.previewWall = previewWall;
        repaintWallAreas(previousWall, previewWall);
    }

    public void setHighlightedPositions(List<Position> positions) {
        List<Position> previousPositions = highlightedPositions;
        this.highlightedPositions = new ArrayList<>(positions);
        repaintPositionAreas(previousPositions, highlightedPositions);
    }

    public void clearHighlightedPositions() {
        if (highlightedPositions.isEmpty()) {
            return;
        }

        List<Position> previousPositions = highlightedPositions;
        this.highlightedPositions = Collections.emptyList();
        repaintPositionAreas(previousPositions, highlightedPositions);
    }

    public void showHintPosition(Position position) {
        Position previousPosition = hintPosition;
        Wall previousWall = hintWall;
        hintPosition = position;
        hintWall = null;
        hintTimer.restart();
        repaintPositionAreas(singletonPositionList(previousPosition), singletonPositionList(hintPosition));
        repaintWallAreas(previousWall, hintWall);
    }

    public void showHintWall(Wall wall) {
        Position previousPosition = hintPosition;
        Wall previousWall = hintWall;
        hintPosition = null;
        hintWall = wall;
        hintTimer.restart();
        repaintPositionAreas(singletonPositionList(previousPosition), singletonPositionList(hintPosition));
        repaintWallAreas(previousWall, hintWall);
    }

    public void clearHint() {
        if (hintPosition == null && hintWall == null) {
            return;
        }

        Position previousPosition = hintPosition;
        Wall previousWall = hintWall;
        hintPosition = null;
        hintWall = null;
        hintTimer.stop();
        repaintPositionAreas(singletonPositionList(previousPosition), Collections.emptyList());
        repaintWallAreas(previousWall, null);
    }

    public void repaintDynamicLayer() {
        repaintRectangle(boardBounds());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        installRenderingHints(g2);

        BoardLayout layout = getBoardLayout();
        drawStaticLayer(g2, layout);
        drawHighlightedPositions(g2, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawPreviewWall(g2, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawWalls(g2, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawLastAction(g2, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawHint(g2, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawWinningPath(g2, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawPlayer(g2, model.getState().getPlayer1(), theme.player1Fallback, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawPlayer(g2, model.getState().getPlayer2(), theme.player2Fallback, layout.x, layout.y, layout.cellSize, layout.gapSize);
        drawOpeningStory(g2, layout);
        drawMessage(g2, layout);
        drawGameOverOverlay(g2);

        g2.dispose();
    }

    private void drawStaticLayer(Graphics2D g2, BoardLayout layout) {
        BufferedImage layer = getStaticLayer(layout);
        if (layer != null) {
            g2.drawImage(layer, 0, 0, null);
        }
    }

    private BufferedImage getStaticLayer(BoardLayout layout) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        if (staticLayer == null || cachedStaticTheme != theme || cachedStaticWidth != width || cachedStaticHeight != height) {
            staticLayer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            cachedStaticTheme = theme;
            cachedStaticWidth = width;
            cachedStaticHeight = height;

            Graphics2D staticGraphics = staticLayer.createGraphics();
            installRenderingHints(staticGraphics);
            if (paintFullBackground) {
                drawFullBackground(staticGraphics);
            }
            drawThemeBackground(staticGraphics, layout.x, layout.y, layout.boardSize);
            drawBoard(staticGraphics, layout.x, layout.y, layout.boardSize, layout.cellSize, layout.gapSize);
            drawGoalMarkers(staticGraphics, layout.x, layout.y, layout.cellSize, layout.gapSize);
            staticGraphics.dispose();
        }

        return staticLayer;
    }

    private void installRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private void invalidateStaticLayer() {
        staticLayer = null;
        cachedStaticTheme = null;
        cachedStaticWidth = -1;
        cachedStaticHeight = -1;
    }

    private void repaintCurrentPlayerArea() {
        repaintRectangle(playerBounds(model.getState().getCurrentPlayer()));
    }

    private void repaintWallAreas(Wall firstWall, Wall secondWall) {
        Rectangle dirtyArea = union(wallBounds(firstWall), wallBounds(secondWall));
        repaintRectangle(dirtyArea);
    }

    private void repaintPositionAreas(List<Position> previousPositions, List<Position> nextPositions) {
        Rectangle dirtyArea = null;

        for (Position position : previousPositions) {
            dirtyArea = union(dirtyArea, positionBounds(position));
        }

        for (Position position : nextPositions) {
            dirtyArea = union(dirtyArea, positionBounds(position));
        }

        repaintRectangle(dirtyArea);
    }

    private List<Position> singletonPositionList(Position position) {
        if (position == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(position);
    }

    private Rectangle boardBounds() {
        BoardLayout layout = getBoardLayout();
        int padding = Math.max(28, layout.gapSize * 4);
        return clampRectangle(new Rectangle(
            layout.x - padding,
            layout.y - padding,
            layout.boardSize + padding * 2,
            layout.boardSize + padding * 2
        ));
    }

    private Rectangle playerBounds(Player player) {
        if (player == null) {
            return null;
        }

        return positionBounds(player.getPosition());
    }

    private Rectangle positionBounds(Position position) {
        if (position == null) {
            return null;
        }

        BoardLayout layout = getBoardLayout();
        int step = layout.cellSize + layout.gapSize;
        int padding = Math.max(10, layout.cellSize / 5);
        return clampRectangle(new Rectangle(
            layout.x + position.getCol() * step - padding,
            layout.y + position.getRow() * step - padding,
            layout.cellSize + padding * 2,
            layout.cellSize + padding * 2
        ));
    }

    private Rectangle wallBounds(Wall wall) {
        if (wall == null) {
            return null;
        }

        BoardLayout layout = getBoardLayout();
        int step = layout.cellSize + layout.gapSize;
        int thickness = Math.max(24, layout.gapSize + 16);
        int length = layout.cellSize * 2 + layout.gapSize;
        int x = layout.x + wall.getCol() * step;
        int y = layout.y + wall.getRow() * step;
        int wallX;
        int wallY;
        int wallWidth;
        int wallHeight;

        if (wall.getOrientation() == WallOrientation.HORIZONTAL) {
            wallX = x;
            wallY = y + layout.cellSize + (layout.gapSize - thickness) / 2;
            wallWidth = length;
            wallHeight = thickness;
        } else {
            wallX = x + layout.cellSize + (layout.gapSize - thickness) / 2;
            wallY = y;
            wallWidth = thickness;
            wallHeight = length;
        }

        int padding = Math.max(16, thickness);
        return clampRectangle(new Rectangle(
            wallX - padding,
            wallY - padding,
            wallWidth + padding * 2,
            wallHeight + padding * 2
        ));
    }

    private Rectangle union(Rectangle first, Rectangle second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return first.union(second);
    }

    private Rectangle clampRectangle(Rectangle rectangle) {
        if (rectangle == null) {
            return null;
        }

        int x = Math.max(0, rectangle.x);
        int y = Math.max(0, rectangle.y);
        int maxX = Math.min(getWidth(), rectangle.x + rectangle.width);
        int maxY = Math.min(getHeight(), rectangle.y + rectangle.height);
        return new Rectangle(x, y, Math.max(0, maxX - x), Math.max(0, maxY - y));
    }

    private void repaintRectangle(Rectangle rectangle) {
        if (rectangle == null || rectangle.width <= 0 || rectangle.height <= 0) {
            return;
        }

        repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public Position positionAtPoint(int x, int y) {
        BoardLayout layout = getBoardLayout();
        int step = layout.cellSize + layout.gapSize;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int cellX = layout.x + col * step;
                int cellY = layout.y + row * step;

                if (x >= cellX && x <= cellX + layout.cellSize && y >= cellY && y <= cellY + layout.cellSize) {
                    return new Position(row, col);
                }
            }
        }

        return null;
    }

    public boolean isPointInPawnNeighborhood(int x, int y, Position pawnPosition) {
        if (pawnPosition == null) {
            return false;
        }

        BoardLayout layout = getBoardLayout();
        int minRow = Math.max(0, pawnPosition.getRow() - 1);
        int maxRow = Math.min(Board.SIZE - 1, pawnPosition.getRow() + 1);
        int minCol = Math.max(0, pawnPosition.getCol() - 1);
        int maxCol = Math.min(Board.SIZE - 1, pawnPosition.getCol() + 1);
        Rectangle neighborhood = cellHitBounds(new Position(minRow, minCol), layout)
            .union(cellHitBounds(new Position(maxRow, maxCol), layout));
        return neighborhood.contains(x, y);
    }

    private Rectangle cellHitBounds(Position position, BoardLayout layout) {
        int step = layout.cellSize + layout.gapSize;
        return new Rectangle(
            layout.x + position.getCol() * step,
            layout.y + position.getRow() * step,
            layout.cellSize,
            layout.cellSize
        );
    }

    public Wall wallAtPoint(int x, int y, WallOrientation orientation) {
        BoardLayout layout = getBoardLayout();
        int step = layout.cellSize + layout.gapSize;
        int bestRow = -1;
        int bestCol = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int row = 0; row < Board.WALL_GRID_SIZE; row++) {
            for (int col = 0; col < Board.WALL_GRID_SIZE; col++) {
                int wallX;
                int wallY;

                if (orientation == WallOrientation.HORIZONTAL) {
                    wallX = layout.x + col * step + layout.cellSize;
                    wallY = layout.y + row * step + layout.cellSize + layout.gapSize / 2;
                } else {
                    wallX = layout.x + col * step + layout.cellSize + layout.gapSize / 2;
                    wallY = layout.y + row * step + layout.cellSize;
                }

                int distance = Math.abs(x - wallX) + Math.abs(y - wallY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestRow = row;
                    bestCol = col;
                }
            }
        }

        int maxDistance = Math.max(18, layout.cellSize / 2);
        if (bestDistance > maxDistance) {
            return null;
        }

        return new Wall(bestRow, bestCol, orientation);
    }

    public Wall wallAtPoint(int x, int y) {
        BoardLayout layout = getBoardLayout();
        Wall horizontal = wallAtPoint(x, y, WallOrientation.HORIZONTAL);
        Wall vertical = wallAtPoint(x, y, WallOrientation.VERTICAL);

        if (horizontal == null) {
            return vertical;
        }

        if (vertical == null) {
            return horizontal;
        }

        int horizontalDistance = distanceToWallCenter(x, y, horizontal, layout);
        int verticalDistance = distanceToWallCenter(x, y, vertical, layout);
        return horizontalDistance <= verticalDistance ? horizontal : vertical;
    }

    private int distanceToWallCenter(int x, int y, Wall wall, BoardLayout layout) {
        int step = layout.cellSize + layout.gapSize;
        int wallX;
        int wallY;

        if (wall.getOrientation() == WallOrientation.HORIZONTAL) {
            wallX = layout.x + wall.getCol() * step + layout.cellSize;
            wallY = layout.y + wall.getRow() * step + layout.cellSize + layout.gapSize / 2;
        } else {
            wallX = layout.x + wall.getCol() * step + layout.cellSize + layout.gapSize / 2;
            wallY = layout.y + wall.getRow() * step + layout.cellSize;
        }

        return Math.abs(x - wallX) + Math.abs(y - wallY);
    }

    private void drawFullBackground(Graphics2D g2) {
        drawFullJungleBackground(g2);
    }

    private void drawThemeImageOrGradient(Graphics2D g2, Color fallbackTop, Color fallbackBottom) {
        int width = getWidth();
        int height = getHeight();

        if (themeBackgroundImage != null) {
            drawImageCover(g2, themeBackgroundImage, 0, 0, width, height);
            if (theme.backgroundOverlay != null) {
                g2.setColor(theme.backgroundOverlay);
                g2.fillRect(0, 0, width, height);
            }
            return;
        }

        g2.setPaint(new GradientPaint(0, 0, fallbackTop, width, height, fallbackBottom));
        g2.fillRect(0, 0, width, height);
    }

    private void drawFullJungleBackground(Graphics2D g2) {
        int width = getWidth();
        int height = getHeight();

        drawThemeImageOrGradient(g2, new Color(8, 60, 37), new Color(45, 96, 34));

        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 13; i++) {
            int x = deterministicValue(i, 79, width + 180) - 90;
            int y = deterministicValue(i, 41, Math.max(1, height / 2));
            int length = height / 3 + deterministicValue(i, 29, Math.max(1, height / 3));

            g2.setColor(new Color(19, 88, 47, 125));
            Path2D vine = new Path2D.Double();
            vine.moveTo(x, y);
            vine.curveTo(x + 28, y + length / 3.0, x - 34, y + length * 2 / 3.0, x + 18, y + length);
            g2.draw(vine);

            g2.setColor(new Color(118, 190, 73, 115));
            for (int leaf = 0; leaf < 4; leaf++) {
                int leafY = y + length * (leaf + 1) / 5;
                g2.fillOval(x + ((leaf % 2 == 0) ? 8 : -28), leafY, 34, 14);
            }
        }

        g2.setColor(new Color(215, 255, 154, 38));
        for (int i = 0; i < 9; i++) {
            int x = deterministicValue(i, 47, width + 220) - 110;
            int y = deterministicValue(i, 73, height + 120) - 60;
            int size = 120 + deterministicValue(i, 23, 170);
            g2.fillOval(x, y, size, size / 3);
        }
    }

    private void drawThemeBackground(Graphics2D g2, int boardX, int boardY, int boardSize) {
        drawJungleBackground(g2, boardX, boardY, boardSize);
    }

    private void drawJungleBackground(Graphics2D g2, int boardX, int boardY, int boardSize) {
        g2.setStroke(new BasicStroke(Math.max(3, boardSize / 160), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(207, 255, 126, 95));

        for (int i = 0; i < 6; i++) {
            int x = boardX - boardSize / 10 + i * boardSize / 5;
            Path2D stem = new Path2D.Double();
            stem.moveTo(x, boardY - 18);
            stem.curveTo(x + boardSize / 15.0, boardY + boardSize / 3.0, x - boardSize / 12.0, boardY + boardSize * 2 / 3.0, x + boardSize / 18.0, boardY + boardSize + 26);
            g2.draw(stem);
        }

        g2.setColor(new Color(255, 210, 74, 80));
        g2.fillOval(boardX + boardSize - boardSize / 7, boardY + boardSize / 14, boardSize / 11, boardSize / 11);
        g2.setColor(new Color(255, 118, 67, 95));
        g2.fillOval(boardX + boardSize / 14, boardY + boardSize - boardSize / 6, boardSize / 10, boardSize / 16);
    }

    private int calculateCellSize(int availableSize) {
        double gapRatio = 0.125;
        return (int) (availableSize / (Board.SIZE + (Board.SIZE - 1) * gapRatio));
    }

    private BoardLayout getBoardLayout() {
        int availableSize = Math.min(getWidth(), getHeight()) - MIN_PADDING * 2;
        availableSize = Math.max(availableSize, Board.SIZE);
        int cellSize = calculateCellSize(availableSize);
        int gapSize = Math.max(3, cellSize / 8);
        int boardSize = Board.SIZE * cellSize + (Board.SIZE - 1) * gapSize;
        int boardX = (getWidth() - boardSize) / 2;
        int boardY = (getHeight() - boardSize) / 2;

        return new BoardLayout(boardX, boardY, boardSize, cellSize, gapSize);
    }

    private void drawBoard(Graphics2D g2, int x, int y, int boardSize, int cellSize, int gapSize) {
        g2.setPaint(new GradientPaint(x, y, theme.boardTop, x, y + boardSize, theme.boardBottom));
        g2.fillRoundRect(x - gapSize, y - gapSize, boardSize + gapSize * 2, boardSize + gapSize * 2, gapSize * 2, gapSize * 2);

        g2.setColor(theme.groove);
        g2.fillRect(x, y, boardSize, boardSize);

        int arc = Math.max(8, cellSize / 7);
        g2.setStroke(new BasicStroke(Math.max(1, cellSize / 40)));

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int cellX = x + col * (cellSize + gapSize);
                int cellY = y + row * (cellSize + gapSize);
                drawCreativeCell(g2, row, col, cellX, cellY, cellSize, arc);
            }
        }
    }

    private void drawGoalMarkers(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        int step = cellSize + gapSize;
        drawGoalRow(g2, model.getState().getPlayer1().getGoalRow(), boardX, boardY, cellSize, step, theme.player1Fallback, true);
        drawGoalRow(g2, model.getState().getPlayer2().getGoalRow(), boardX, boardY, cellSize, step, theme.player2Fallback, false);
    }

    private void drawGoalRow(Graphics2D g2, int row, int boardX, int boardY, int cellSize, int step, Color color, boolean topGoal) {
        Graphics2D rowGraphics = (Graphics2D) g2.create();
        rowGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rowGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.48f));

        for (int col = 0; col < Board.SIZE; col++) {
            int x = boardX + col * step;
            int y = boardY + row * step;
            int margin = Math.max(3, cellSize / 13);
            int arc = Math.max(10, cellSize / 6);

            rowGraphics.setColor(withAlpha(color, 85));
            rowGraphics.fillRoundRect(x + margin, y + margin, cellSize - margin * 2, cellSize - margin * 2, arc, arc);
            rowGraphics.setStroke(new BasicStroke(Math.max(2, cellSize / 22), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            rowGraphics.setColor(withAlpha(color, 185));
            rowGraphics.drawRoundRect(x + margin, y + margin, cellSize - margin * 2, cellSize - margin * 2, arc, arc);
        }

        rowGraphics.dispose();

        int markerSize = Math.max(20, cellSize * 3 / 5);
        for (int col = 0; col < Board.SIZE; col++) {
            int centerX = boardX + col * step + cellSize / 2;
            int centerY = boardY + row * step + cellSize / 2;
            drawGoalBanana(g2, centerX, centerY, markerSize, color, topGoal);
        }
    }

    private void drawGoalBanana(Graphics2D g2, int centerX, int centerY, int size, Color color, boolean topGoal) {
        Graphics2D goalGraphics = (Graphics2D) g2.create();
        goalGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        goalGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        drawSingleBanana(goalGraphics, centerX, centerY, size, color, topGoal, 0);
        goalGraphics.dispose();
    }

    private void drawBananaBunch(Graphics2D g2, int centerX, int centerY, int size, Color color, boolean topGoal) {
        int stemSize = Math.max(4, size / 6);
        drawSingleBanana(g2, centerX, centerY - size / 5, size, color, topGoal, -size / 8);
        drawSingleBanana(g2, centerX, centerY + size / 5, size, color, topGoal, size / 8);

        g2.setColor(new Color(71, 49, 21, 225));
        g2.fillOval(centerX - stemSize / 2, centerY - stemSize / 2, stemSize, stemSize);
    }

    private void drawSingleBanana(Graphics2D g2, int centerX, int centerY, int size, Color color, boolean topGoal, int offset) {
        int halfWidth = size / 2;
        int curveHeight = Math.max(8, size / 3);
        int bend = topGoal ? curveHeight : -curveHeight;
        int y = centerY + offset / 3;
        int stroke = Math.max(7, size / 4);

        Path2D curve = new Path2D.Double();
        curve.moveTo(centerX - halfWidth, y);
        curve.curveTo(centerX - size / 4.0, y + bend, centerX + size / 4.0, y + bend, centerX + halfWidth, y);

        g2.setStroke(new BasicStroke(stroke + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(42, 31, 16, 135));
        g2.draw(curve);

        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(withAlpha(shiftColor(color, 38), 235));
        g2.draw(curve);

        Path2D highlight = new Path2D.Double();
        int highlightOffset = topGoal ? -Math.max(2, stroke / 4) : Math.max(2, stroke / 4);
        highlight.moveTo(centerX - halfWidth + stroke / 2.0, y + highlightOffset);
        highlight.curveTo(centerX - size / 4.0, y + bend + highlightOffset, centerX + size / 4.0, y + bend + highlightOffset, centerX + halfWidth - stroke / 2.0, y + highlightOffset);
        g2.setStroke(new BasicStroke(Math.max(2, stroke / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 249, 196, 210));
        g2.draw(highlight);

        g2.setStroke(new BasicStroke(Math.max(1, stroke / 7), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(withAlpha(shiftColor(color, -45), 175));
        g2.draw(curve);

        int tipSize = Math.max(4, size / 9);
        g2.setColor(new Color(67, 43, 19, 215));
        g2.fillOval(centerX - halfWidth - tipSize / 2, y - tipSize / 2, tipSize, tipSize);
        g2.fillOval(centerX + halfWidth - tipSize / 2, y - tipSize / 2, tipSize, tipSize);
    }

    private void drawWalls(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        for (Wall wall : model.getState().getBoard().getWalls()) {
            int owner = model.getState().getBoard().getWallOwner(wall);
            drawSingleWall(g2, wall, boardX, boardY, cellSize, gapSize, wallColorForPlayer(owner));
        }
    }

    private void drawWinningPath(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        if (!model.getState().isGameOver()) {
            return;
        }

        Player winner = model.getState().getWinner();
        List<Position> path = model.getWinningPath();
        if (winner == null || path.size() < 2) {
            return;
        }

        int step = cellSize + gapSize;
        Path2D route = new Path2D.Double();

        for (int index = 0; index < path.size(); index++) {
            Position position = path.get(index);
            int centerX = boardX + position.getCol() * step + cellSize / 2;
            int centerY = boardY + position.getRow() * step + cellSize / 2;

            if (index == 0) {
                route.moveTo(centerX, centerY);
            } else {
                route.lineTo(centerX, centerY);
            }
        }

        Color routeColor = winner.getId() == 1 ? new Color(47, 177, 255) : new Color(255, 99, 82);
        int shadowStroke = Math.max(14, cellSize / 5);
        int routeStroke = Math.max(7, cellSize / 10);
        int lightStroke = Math.max(3, cellSize / 28);

        Graphics2D pathGraphics = (Graphics2D) g2.create();
        pathGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pathGraphics.setStroke(new BasicStroke(shadowStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pathGraphics.setColor(new Color(0, 20, 10, 155));
        pathGraphics.draw(route);

        pathGraphics.setStroke(new BasicStroke(routeStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pathGraphics.setColor(withAlpha(routeColor, 230));
        pathGraphics.draw(route);

        pathGraphics.setStroke(new BasicStroke(lightStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pathGraphics.setColor(new Color(255, 245, 155, 235));
        pathGraphics.draw(route);

        int dotSize = Math.max(8, cellSize / 8);
        int startDotSize = Math.max(dotSize + 3, cellSize / 6);
        for (int index = 0; index < path.size(); index++) {
            Position position = path.get(index);
            int centerX = boardX + position.getCol() * step + cellSize / 2;
            int centerY = boardY + position.getRow() * step + cellSize / 2;
            int size = index == 0 ? startDotSize : dotSize;

            pathGraphics.setColor(new Color(0, 20, 10, 180));
            pathGraphics.fillOval(centerX - size / 2 - 1, centerY - size / 2 - 1, size + 2, size + 2);
            pathGraphics.setColor(index == path.size() - 1 ? new Color(255, 231, 90) : withAlpha(routeColor, 235));
            pathGraphics.fillOval(centerX - size / 2, centerY - size / 2, size, size);
        }

        pathGraphics.dispose();
    }

    private void drawLastAction(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        GameModel.LastAction lastAction = model.getLastAction();
        if (lastAction == null) {
            return;
        }

        if (lastAction.isMove()) {
            drawLastMove(g2, lastAction.getPreviousPosition(), lastAction.getTargetPosition(), boardX, boardY, cellSize, gapSize);
        } else if (lastAction.isWall()) {
            drawLastWallMarker(g2, lastAction.getWall(), boardX, boardY, cellSize, gapSize);
        }
    }

    private void drawLastMove(Graphics2D g2, Position from, Position to, int boardX, int boardY, int cellSize, int gapSize) {
        if (from == null || to == null) {
            return;
        }

        int step = cellSize + gapSize;
        int fromX = boardX + from.getCol() * step + cellSize / 2;
        int fromY = boardY + from.getRow() * step + cellSize / 2;
        int toX = boardX + to.getCol() * step + cellSize / 2;
        int toY = boardY + to.getRow() * step + cellSize / 2;
        int dotSize = Math.max(9, cellSize / 7);

        Graphics2D markerGraphics = (Graphics2D) g2.create();
        markerGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        markerGraphics.setStroke(new BasicStroke(Math.max(8, cellSize / 8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        markerGraphics.setColor(new Color(0, 34, 12, 170));
        markerGraphics.drawLine(fromX, fromY, toX, toY);

        markerGraphics.setStroke(new BasicStroke(Math.max(4, cellSize / 18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        markerGraphics.setColor(new Color(76, 255, 115, 235));
        markerGraphics.drawLine(fromX, fromY, toX, toY);
        drawArrowHead(markerGraphics, fromX, fromY, toX, toY, Math.max(12, cellSize / 5));

        markerGraphics.setColor(new Color(0, 34, 12, 190));
        markerGraphics.fillOval(fromX - dotSize / 2 - 1, fromY - dotSize / 2 - 1, dotSize + 2, dotSize + 2);
        markerGraphics.setColor(new Color(160, 255, 172, 235));
        markerGraphics.fillOval(fromX - dotSize / 2, fromY - dotSize / 2, dotSize, dotSize);
        markerGraphics.dispose();
    }

    private void drawArrowHead(Graphics2D g2, int fromX, int fromY, int toX, int toY, int size) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double wing = Math.PI / 7.0;
        int x1 = toX - (int) Math.round(Math.cos(angle - wing) * size);
        int y1 = toY - (int) Math.round(Math.sin(angle - wing) * size);
        int x2 = toX - (int) Math.round(Math.cos(angle + wing) * size);
        int y2 = toY - (int) Math.round(Math.sin(angle + wing) * size);

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(toX, toY);
        arrow.lineTo(x1, y1);
        arrow.lineTo(x2, y2);
        arrow.closePath();

        g2.setColor(new Color(0, 34, 12, 185));
        g2.fill(arrow);
        g2.setColor(new Color(76, 255, 115, 245));
        g2.draw(arrow);
    }

    private void drawLastWallMarker(Graphics2D g2, Wall wall, int boardX, int boardY, int cellSize, int gapSize) {
        if (wall == null) {
            return;
        }

        int step = cellSize + gapSize;
        int thickness = Math.max(24, gapSize + 16);
        int length = cellSize * 2 + gapSize;
        int x = boardX + wall.getCol() * step;
        int y = boardY + wall.getRow() * step;
        int wallX;
        int wallY;
        int wallWidth;
        int wallHeight;

        if (wall.getOrientation() == WallOrientation.HORIZONTAL) {
            wallX = x;
            wallY = y + cellSize + (gapSize - thickness) / 2;
            wallWidth = length;
            wallHeight = thickness;
        } else {
            wallX = x + cellSize + (gapSize - thickness) / 2;
            wallY = y;
            wallWidth = thickness;
            wallHeight = length;
        }

        Graphics2D markerGraphics = (Graphics2D) g2.create();
        markerGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        markerGraphics.setStroke(new BasicStroke(Math.max(6, thickness / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        markerGraphics.setColor(new Color(0, 34, 12, 160));
        markerGraphics.drawRoundRect(wallX - 4, wallY - 4, wallWidth + 8, wallHeight + 8, thickness, thickness);
        markerGraphics.setStroke(new BasicStroke(Math.max(3, thickness / 8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        markerGraphics.setColor(new Color(76, 255, 115, 240));
        markerGraphics.drawRoundRect(wallX - 2, wallY - 2, wallWidth + 4, wallHeight + 4, thickness, thickness);
        markerGraphics.dispose();
    }

    private void drawHint(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        if (hintPosition != null) {
            drawHintPosition(g2, hintPosition, boardX, boardY, cellSize, gapSize);
        }

        if (hintWall != null) {
            drawHintWall(g2, hintWall, boardX, boardY, cellSize, gapSize);
        }
    }

    private void drawHintPosition(Graphics2D g2, Position position, int boardX, int boardY, int cellSize, int gapSize) {
        int step = cellSize + gapSize;
        int x = boardX + position.getCol() * step;
        int y = boardY + position.getRow() * step;
        int margin = Math.max(4, cellSize / 10);
        int arc = Math.max(16, cellSize / 4);
        long now = System.currentTimeMillis();
        int pulse = (int) Math.round(Math.sin(now / 160.0) * Math.max(3, cellSize / 18));

        Graphics2D hintGraphics = (Graphics2D) g2.create();
        hintGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hintGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.78f));
        hintGraphics.setColor(new Color(18, 42, 9, 150));
        hintGraphics.fillRoundRect(x + margin - 3, y + margin - 1, cellSize - margin * 2 + 6,
            cellSize - margin * 2 + 6, arc, arc);
        hintGraphics.setPaint(new GradientPaint(
            x,
            y,
            new Color(255, 246, 128, 235),
            x,
            y + cellSize,
            new Color(245, 150, 38, 220)
        ));
        hintGraphics.fillRoundRect(x + margin - pulse, y + margin - pulse,
            cellSize - margin * 2 + pulse * 2, cellSize - margin * 2 + pulse * 2, arc, arc);
        hintGraphics.setComposite(AlphaComposite.SrcOver);
        hintGraphics.setStroke(new BasicStroke(Math.max(3, cellSize / 16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        hintGraphics.setColor(new Color(255, 255, 204, 245));
        hintGraphics.drawRoundRect(x + margin - pulse, y + margin - pulse,
            cellSize - margin * 2 + pulse * 2, cellSize - margin * 2 + pulse * 2, arc, arc);
        hintGraphics.dispose();
    }

    private void drawHintWall(Graphics2D g2, Wall wall, int boardX, int boardY, int cellSize, int gapSize) {
        Graphics2D hintGraphics = (Graphics2D) g2.create();
        hintGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        drawSingleWall(hintGraphics, wall, boardX, boardY, cellSize, gapSize, new Color(255, 220, 54));
        drawHintWallOutline(hintGraphics, wall, boardX, boardY, cellSize, gapSize);
        hintGraphics.dispose();
    }

    private void drawHintWallOutline(Graphics2D g2, Wall wall, int boardX, int boardY, int cellSize, int gapSize) {
        int step = cellSize + gapSize;
        int thickness = Math.max(24, gapSize + 16);
        int length = cellSize * 2 + gapSize;
        int x = boardX + wall.getCol() * step;
        int y = boardY + wall.getRow() * step;
        int wallX;
        int wallY;
        int wallWidth;
        int wallHeight;

        if (wall.getOrientation() == WallOrientation.HORIZONTAL) {
            wallX = x;
            wallY = y + cellSize + (gapSize - thickness) / 2;
            wallWidth = length;
            wallHeight = thickness;
        } else {
            wallX = x + cellSize + (gapSize - thickness) / 2;
            wallY = y;
            wallWidth = thickness;
            wallHeight = length;
        }

        int pulse = (int) Math.round((Math.sin(System.currentTimeMillis() / 160.0) + 1.0) * 2);
        g2.setStroke(new BasicStroke(Math.max(6, thickness / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(39, 52, 12, 170));
        g2.drawRoundRect(wallX - 5 - pulse, wallY - 5 - pulse, wallWidth + 10 + pulse * 2,
            wallHeight + 10 + pulse * 2, thickness, thickness);
        g2.setStroke(new BasicStroke(Math.max(3, thickness / 8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 190, 245));
        g2.drawRoundRect(wallX - 2 - pulse, wallY - 2 - pulse, wallWidth + 4 + pulse * 2,
            wallHeight + 4 + pulse * 2, thickness, thickness);
    }

    private void drawHighlightedPositions(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        if (highlightedPositions.isEmpty()) {
            return;
        }

        int step = cellSize + gapSize;
        int arc = Math.max(10, cellSize / 6);
        Graphics2D highlightGraphics = (Graphics2D) g2.create();
        highlightGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.62f));
        Color fill = moveHighlightColorForCurrentPlayer();
        Color border = moveHighlightBorderColorForCurrentPlayer();

        for (Position position : highlightedPositions) {
            int x = boardX + position.getCol() * step;
            int y = boardY + position.getRow() * step;
            int margin = Math.max(5, cellSize / 9);

            highlightGraphics.setColor(fill);
            highlightGraphics.fillRoundRect(x + margin, y + margin, cellSize - margin * 2, cellSize - margin * 2, arc, arc);
            highlightGraphics.setColor(border);
            highlightGraphics.setStroke(new BasicStroke(Math.max(2, cellSize / 18)));
            highlightGraphics.drawRoundRect(x + margin, y + margin, cellSize - margin * 2, cellSize - margin * 2, arc, arc);
        }

        highlightGraphics.dispose();
    }

    private Color moveHighlightColorForCurrentPlayer() {
        if (model.getState().getCurrentPlayerId() == 2) {
            return new Color(255, 64, 72);
        }

        return theme.moveHighlight;
    }

    private Color moveHighlightBorderColorForCurrentPlayer() {
        if (model.getState().getCurrentPlayerId() == 2) {
            return new Color(255, 224, 215);
        }

        return theme.moveHighlightBorder;
    }

    private void drawPreviewWall(Graphics2D g2, int boardX, int boardY, int cellSize, int gapSize) {
        if (previewWall == null) {
            return;
        }

        Graphics2D previewGraphics = (Graphics2D) g2.create();
        previewGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        drawSingleWall(previewGraphics, previewWall, boardX, boardY, cellSize, gapSize, wallColorForPlayer(model.getState().getCurrentPlayerId()));
        previewGraphics.dispose();
    }

    private Color wallColorForPlayer(int playerId) {
        if (playerId == 1) {
            return theme.player1Wall;
        }

        if (playerId == 2) {
            return theme.player2Wall;
        }

        return theme.wall;
    }

    private void drawSingleWall(Graphics2D g2, Wall wall, int boardX, int boardY, int cellSize, int gapSize, Color color) {
        int step = cellSize + gapSize;
        int thickness = Math.max(24, gapSize + 16);
        int length = cellSize * 2 + gapSize;

        int x = boardX + wall.getCol() * step;
        int y = boardY + wall.getRow() * step;
        int wallX;
        int wallY;
        int wallWidth;
        int wallHeight;

        if (wall.getOrientation() == WallOrientation.HORIZONTAL) {
            wallX = x;
            wallY = y + cellSize + (gapSize - thickness) / 2;
            wallWidth = length;
            wallHeight = thickness;
        } else {
            wallX = x + cellSize + (gapSize - thickness) / 2;
            wallY = y;
            wallWidth = thickness;
            wallHeight = length;
        }

        drawThemedWall(g2, wallX, wallY, wallWidth, wallHeight, wall.getOrientation(), color);
    }

    private void drawThemedWall(Graphics2D g2, int x, int y, int width, int height, WallOrientation orientation, Color ownerColor) {
        if (themeWallImage == null || themeWallSourceBounds == null) {
            drawLogWall(g2, x, y, width, height, orientation, ownerColor);
            return;
        }

        int wallLength = orientation == WallOrientation.HORIZONTAL ? width : height;
        int wallThickness = orientation == WallOrientation.HORIZONTAL ? height : width;

        Graphics2D wallGraphics = (Graphics2D) g2.create();
        wallGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        wallGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        wallGraphics.translate(x + width / 2.0, y + height / 2.0);
        if (orientation == WallOrientation.VERTICAL) {
            wallGraphics.rotate(Math.PI / 2.0);
        }

        int drawX = -wallLength / 2;
        int drawY = -wallThickness / 2;
        wallGraphics.drawImage(
            themeWallImage,
            drawX,
            drawY,
            drawX + wallLength,
            drawY + wallThickness,
            themeWallSourceBounds.x,
            themeWallSourceBounds.y,
            themeWallSourceBounds.x + themeWallSourceBounds.width,
            themeWallSourceBounds.y + themeWallSourceBounds.height,
            null
        );
        wallGraphics.dispose();
    }

    private void drawLogWall(Graphics2D g2, int x, int y, int width, int height, WallOrientation orientation, Color ownerColor) {
        int logLength = orientation == WallOrientation.HORIZONTAL ? width : height;
        int logThickness = orientation == WallOrientation.HORIZONTAL ? height : width;

        Graphics2D logGraphics = (Graphics2D) g2.create();
        logGraphics.translate(x + width / 2.0, y + height / 2.0);
        if (orientation == WallOrientation.VERTICAL) {
            logGraphics.rotate(Math.PI / 2.0);
        }

        drawLogBody(logGraphics, -logLength / 2, -logThickness / 2, logLength, logThickness, ownerColor);
        logGraphics.dispose();
    }

    private void drawLogBody(Graphics2D g2, int x, int y, int length, int thickness, Color ownerColor) {
        int capSize = thickness;
        int bodyX = x + capSize / 2;
        int bodyLength = Math.max(thickness, length - capSize);
        int arc = Math.max(10, thickness);

        Color shadow = new Color(0, 0, 0, 105);
        Color barkTop = new Color(154, 94, 45);
        Color barkBottom = new Color(82, 45, 22);
        Color barkLine = new Color(62, 34, 19, 150);
        Color cutFace = new Color(246, 177, 83);
        Color cutRing = new Color(119, 73, 36, 175);
        Color bandColor = withAlpha(ownerColor, 235);
        int glow = Math.max(6, thickness / 4);

        g2.setColor(withAlpha(ownerColor, 145));
        g2.fillRoundRect(bodyX - glow, y - glow, bodyLength + glow * 2, thickness + glow * 2, arc + glow, arc + glow);
        g2.fillOval(x - glow, y - glow, capSize + glow * 2, thickness + glow * 2);
        g2.fillOval(x + length - capSize - glow, y - glow, capSize + glow * 2, thickness + glow * 2);

        g2.setColor(shadow);
        g2.fillRoundRect(bodyX + 5, y + 6, bodyLength, thickness, arc, arc);
        g2.fillOval(x + 5, y + 6, capSize, thickness);
        g2.fillOval(x + length - capSize + 5, y + 6, capSize, thickness);

        g2.setPaint(new GradientPaint(x, y, barkTop, x, y + thickness, barkBottom));
        g2.fillRoundRect(bodyX, y, bodyLength, thickness, arc, arc);

        int bandWidth = Math.max(4, thickness / 5);
        g2.setColor(bandColor);
        g2.fillRoundRect(bodyX + thickness / 2, y + 2, bandWidth, thickness - 4, bandWidth, bandWidth);
        g2.fillRoundRect(bodyX + bodyLength - thickness / 2 - bandWidth, y + 2, bandWidth, thickness - 4, bandWidth, bandWidth);

        g2.setStroke(new BasicStroke(Math.max(2, thickness / 8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int lineY = y + thickness * (i + 1) / 5;
            Path2D grain = new Path2D.Double();
            grain.moveTo(bodyX + thickness / 2.0, lineY);
            grain.curveTo(
                bodyX + bodyLength / 3.0,
                lineY - thickness / 7.0,
                bodyX + bodyLength * 2 / 3.0,
                lineY + thickness / 8.0,
                bodyX + bodyLength - thickness / 3.0,
                lineY
            );
            g2.setColor(i % 2 == 0 ? barkLine : new Color(198, 128, 59, 105));
            g2.draw(grain);
        }

        g2.setColor(cutFace);
        g2.fillOval(x, y, capSize, thickness);
        g2.fillOval(x + length - capSize, y, capSize, thickness);

        g2.setColor(cutRing);
        g2.setStroke(new BasicStroke(Math.max(1, thickness / 12)));
        drawLogRings(g2, x, y, capSize, thickness);
        drawLogRings(g2, x + length - capSize, y, capSize, thickness);

        g2.setColor(new Color(255, 232, 154, 80));
        g2.drawLine(bodyX + thickness / 3, y + Math.max(3, thickness / 5), bodyX + bodyLength - thickness / 3, y + Math.max(3, thickness / 5));

        g2.setColor(theme.wallOutline);
        g2.setStroke(new BasicStroke(Math.max(4, thickness / 7), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawRoundRect(bodyX, y, bodyLength, thickness, arc, arc);
        g2.drawOval(x, y, capSize, thickness);
        g2.drawOval(x + length - capSize, y, capSize, thickness);

        g2.setColor(new Color(255, 246, 164, 165));
        g2.setStroke(new BasicStroke(Math.max(2, thickness / 12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawRoundRect(bodyX + 2, y + 2, bodyLength - 4, thickness - 4, arc, arc);
    }

    private void drawLogRings(Graphics2D g2, int x, int y, int width, int height) {
        int margin = Math.max(3, height / 7);
        g2.drawOval(x + margin, y + margin, width - margin * 2, height - margin * 2);
        int innerMargin = Math.max(margin + 3, height / 3);
        g2.drawOval(x + innerMargin, y + innerMargin, width - innerMargin * 2, height - innerMargin * 2);
    }

    private void drawCreativeCell(Graphics2D g2, int row, int col, int x, int y, int size, int arc) {
        if (themeCellImage != null) {
            g2.drawImage(themeCellImage, x, y, size, size, null);
            if ((row + col) % 2 != 0) {
                g2.setColor(new Color(45, 76, 34, 24));
                g2.fillRoundRect(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4, arc, arc);
            }
            return;
        }

        Color cellColor = shiftColor(theme.cell, ((row + col) % 2 == 0) ? 8 : -5);
        g2.setColor(cellColor);
        g2.fillRoundRect(x, y, size, size, arc, arc);
        drawJungleCell(g2, row, col, x, y, size);

        g2.setColor(theme.cellBorder);
        g2.drawRoundRect(x, y, size, size, arc, arc);
    }

    private void drawJungleCell(Graphics2D g2, int row, int col, int x, int y, int size) {
        g2.setColor(new Color(216, 255, 145, 70));
        int leafX = x + size / 5 + ((row * 11 + col * 7) % Math.max(1, size / 3));
        int leafY = y + size / 6 + ((row * 5 + col * 13) % Math.max(1, size / 3));
        g2.fillOval(leafX, leafY, size / 3, size / 7);
        g2.drawLine(leafX, leafY + size / 14, leafX + size / 3, leafY + size / 14);

        g2.setColor(new Color(20, 78, 38, 65));
        int stemX = x + size / 2;
        g2.drawLine(stemX, y + size / 6, stemX - size / 7, y + size * 5 / 6);

        if ((row + col) % 4 == 0) {
            g2.setColor(new Color(255, 196, 70, 115));
            g2.fillOval(x + size * 2 / 3, y + size / 5, Math.max(4, size / 10), Math.max(4, size / 10));
        }
    }

    private Color shiftColor(Color color, int amount) {
        return new Color(
            clamp(color.getRed() + amount),
            clamp(color.getGreen() + amount),
            clamp(color.getBlue() + amount),
            color.getAlpha()
        );
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int deterministicValue(int index, int multiplier, int modulo) {
        if (modulo <= 0) {
            return 0;
        }

        return Math.abs(index * multiplier * 97 + index * index * 31 + 53) % modulo;
    }

    private void drawPlayer(Graphics2D g2, Player player, Color fallbackColor, int boardX, int boardY, int cellSize, int gapSize) {
        Position position = player.getPosition();
        int cellX = boardX + position.getCol() * (cellSize + gapSize);
        int cellY = boardY + position.getRow() * (cellSize + gapSize);
        int margin = Math.max(2, cellSize / 14);
        int size = cellSize - margin * 2;

        drawPlayerGlow(g2, player, cellX, cellY, cellSize);
        drawMonkeyPawn(g2, player, fallbackColor, cellX + margin, cellY + margin, size);
    }

    private void drawMonkeyPawn(Graphics2D g2, Player player, Color accentColor, int x, int y, int size) {
        int earSize = Math.max(10, size / 3);
        int headSize = Math.max(18, size * 4 / 5);
        int headX = x + (size - headSize) / 2;
        int headY = y + size / 8;
        int earY = headY + headSize / 4;

        Color fur = player.getId() == 1 ? new Color(117, 70, 35) : new Color(92, 56, 36);
        Color furDark = new Color(59, 35, 23);
        Color face = new Color(222, 169, 104);
        Color faceLight = new Color(246, 199, 131);

        g2.setColor(new Color(0, 0, 0, 95));
        g2.fillOval(x + size / 6, y + size * 3 / 4, size * 2 / 3, size / 5);

        g2.setColor(furDark);
        g2.fillOval(headX - earSize / 2 - 2, earY - 2, earSize + 4, earSize + 4);
        g2.fillOval(headX + headSize - earSize / 2 - 2, earY - 2, earSize + 4, earSize + 4);

        g2.setColor(fur);
        g2.fillOval(headX - earSize / 2, earY, earSize, earSize);
        g2.fillOval(headX + headSize - earSize / 2, earY, earSize, earSize);

        g2.setColor(face);
        int innerEar = Math.max(5, earSize / 2);
        g2.fillOval(headX - innerEar / 2, earY + earSize / 4, innerEar, innerEar);
        g2.fillOval(headX + headSize - innerEar / 2, earY + earSize / 4, innerEar, innerEar);

        g2.setColor(furDark);
        g2.fillOval(headX - 2, headY - 2, headSize + 4, headSize + 4);
        g2.setColor(fur);
        g2.fillOval(headX, headY, headSize, headSize);

        int faceWidth = headSize * 2 / 3;
        int faceHeight = headSize / 2;
        int faceX = headX + (headSize - faceWidth) / 2;
        int faceY = headY + headSize / 3;
        g2.setColor(face);
        g2.fillOval(faceX, faceY, faceWidth, faceHeight);
        g2.setColor(faceLight);
        g2.fillOval(faceX + faceWidth / 6, faceY + faceHeight / 4, faceWidth * 2 / 3, faceHeight / 2);

        int eyeSize = Math.max(3, size / 14);
        int eyeY = headY + headSize / 3;
        g2.setColor(new Color(18, 14, 10));
        g2.fillOval(headX + headSize / 3 - eyeSize / 2, eyeY, eyeSize, eyeSize);
        g2.fillOval(headX + headSize * 2 / 3 - eyeSize / 2, eyeY, eyeSize, eyeSize);

        int noseSize = Math.max(4, size / 12);
        g2.fillOval(headX + headSize / 2 - noseSize / 2, faceY + faceHeight / 3, noseSize, noseSize);
        g2.setStroke(new BasicStroke(Math.max(2, size / 24), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(headX + headSize / 2 - size / 8, faceY + faceHeight / 2, size / 4, size / 7, 200, 140);

        int bandHeight = Math.max(5, size / 9);
        g2.setColor(withAlpha(accentColor, 220));
        g2.fillRoundRect(headX + headSize / 5, headY + headSize / 12, headSize * 3 / 5, bandHeight, bandHeight, bandHeight);
        g2.setColor(new Color(255, 255, 255, 150));
        g2.fillOval(headX + headSize / 2 - bandHeight / 4, headY + headSize / 12 + bandHeight / 4, bandHeight / 2, bandHeight / 2);
    }

    private void drawPlayerGlow(Graphics2D g2, Player player, int cellX, int cellY, int cellSize) {
        int glowMargin = Math.max(2, cellSize / 18);
        boolean currentPlayer = player.getId() == model.getState().getCurrentPlayerId();
        int glowAlpha = currentPlayer && currentPlayerPulseVisible ? 185 : theme.pawnGlow.getAlpha();

        g2.setColor(withAlpha(theme.pawnGlow, glowAlpha));
        g2.fillOval(cellX + glowMargin, cellY + glowMargin, cellSize - glowMargin * 2, cellSize - glowMargin * 2);

        if (currentPlayer) {
            Graphics2D pulseGraphics = (Graphics2D) g2.create();
            Color turnRing = currentTurnRingColorForPlayer(player.getId());
            int ringStroke = Math.max(5, cellSize / 13);
            pulseGraphics.setStroke(new BasicStroke(ringStroke + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            pulseGraphics.setColor(currentPlayerPulseVisible ? new Color(255, 255, 255, 230) : new Color(255, 255, 255, 110));
            pulseGraphics.drawOval(cellX + glowMargin - 2, cellY + glowMargin - 2, cellSize - glowMargin * 2 + 4, cellSize - glowMargin * 2 + 4);
            pulseGraphics.setStroke(new BasicStroke(ringStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            pulseGraphics.setColor(currentPlayerPulseVisible ? turnRing : withAlpha(turnRing, 105));
            pulseGraphics.drawOval(cellX + glowMargin, cellY + glowMargin, cellSize - glowMargin * 2, cellSize - glowMargin * 2);
            pulseGraphics.dispose();
        }
    }

    private Color currentTurnRingColorForPlayer(int playerId) {
        if (playerId == 1) {
            return new Color(47, 177, 255, 245);
        }

        return new Color(255, 74, 76, 245);
    }

    private void drawOpeningStory(Graphics2D g2, BoardLayout layout) {
        if (!openingStoryVisible || model.getState().isGameOver() || model.getLastAction() != null) {
            return;
        }

        drawStoryBubble(
            g2,
            model.getState().getPlayer2(),
            layout,
            theme.player2Fallback,
            "Je veux attraper",
            "bananes rouges !",
            false
        );
        drawStoryBubble(
            g2,
            model.getState().getPlayer1(),
            layout,
            theme.player1Fallback,
            "Je veux attraper",
            "bananes bleues !",
            true
        );
    }

    private void drawStoryBubble(Graphics2D g2, Player player, BoardLayout layout, Color color, String firstLine, String secondLine, boolean abovePlayer) {
        Position position = player.getPosition();
        int step = layout.cellSize + layout.gapSize;
        int playerCenterX = layout.x + position.getCol() * step + layout.cellSize / 2;
        int playerCenterY = layout.y + position.getRow() * step + layout.cellSize / 2;

        Graphics2D bubbleGraphics = (Graphics2D) g2.create();
        bubbleGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bubbleGraphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(13, layout.cellSize / 5)));
        FontMetrics metrics = bubbleGraphics.getFontMetrics();

        int iconSize = Math.max(22, layout.cellSize / 3);
        int paddingX = Math.max(12, layout.cellSize / 6);
        int paddingY = Math.max(8, layout.cellSize / 8);
        int textWidth = Math.max(metrics.stringWidth(firstLine), metrics.stringWidth(secondLine));
        int bubbleWidth = Math.min(layout.boardSize - 28, textWidth + iconSize + paddingX * 3);
        int bubbleHeight = metrics.getHeight() * 2 + paddingY * 2;
        int bubbleX = playerCenterX - bubbleWidth / 2;
        int bubbleY = abovePlayer
            ? playerCenterY - layout.cellSize / 2 - bubbleHeight - Math.max(10, layout.cellSize / 6)
            : playerCenterY + layout.cellSize / 2 + Math.max(10, layout.cellSize / 6);

        bubbleX = Math.max(layout.x + 8, Math.min(layout.x + layout.boardSize - bubbleWidth - 8, bubbleX));
        bubbleY = Math.max(layout.y + 8, Math.min(layout.y + layout.boardSize - bubbleHeight - 8, bubbleY));

        int arc = Math.max(18, bubbleHeight / 2);
        bubbleGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.94f));
        bubbleGraphics.setColor(new Color(0, 0, 0, 130));
        bubbleGraphics.fillRoundRect(bubbleX + 4, bubbleY + 5, bubbleWidth, bubbleHeight, arc, arc);
        bubbleGraphics.setColor(new Color(255, 247, 214, 238));
        bubbleGraphics.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, arc, arc);
        bubbleGraphics.setColor(withAlpha(color, 230));
        bubbleGraphics.setStroke(new BasicStroke(Math.max(3, layout.cellSize / 24), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        bubbleGraphics.drawRoundRect(bubbleX + 2, bubbleY + 2, bubbleWidth - 4, bubbleHeight - 4, arc, arc);

        drawBubbleTail(bubbleGraphics, playerCenterX, playerCenterY, bubbleX, bubbleY, bubbleWidth, bubbleHeight, abovePlayer, color);

        int iconX = bubbleX + paddingX;
        int iconY = bubbleY + (bubbleHeight - iconSize) / 2;
        drawBananaBunch(bubbleGraphics, iconX + iconSize / 2, iconY + iconSize / 2, iconSize, color, abovePlayer);

        int textX = iconX + iconSize + paddingX;
        int firstY = bubbleY + paddingY + metrics.getAscent();
        int secondY = firstY + metrics.getHeight();
        bubbleGraphics.setColor(new Color(61, 39, 19));
        bubbleGraphics.drawString(firstLine, textX, firstY);
        bubbleGraphics.drawString(secondLine, textX, secondY);
        bubbleGraphics.dispose();
    }

    private void drawBubbleTail(Graphics2D g2, int playerCenterX, int playerCenterY, int bubbleX, int bubbleY, int bubbleWidth, int bubbleHeight, boolean abovePlayer, Color color) {
        int tailY = abovePlayer ? bubbleY + bubbleHeight : bubbleY;
        int smallSize = 8;
        int mediumSize = 13;
        int largeSize = 18;
        int direction = abovePlayer ? 1 : -1;
        int anchorX = Math.max(bubbleX + 26, Math.min(bubbleX + bubbleWidth - 26, playerCenterX));

        g2.setColor(new Color(0, 0, 0, 88));
        g2.fillOval(anchorX - largeSize / 2 + 2, tailY - largeSize / 2 + direction * 8 + 3, largeSize, largeSize);
        g2.fillOval((anchorX + playerCenterX) / 2 - mediumSize / 2 + 2, (tailY + playerCenterY) / 2 - mediumSize / 2 + 3, mediumSize, mediumSize);
        g2.fillOval(playerCenterX - smallSize / 2 + 2, playerCenterY - smallSize / 2 + 3, smallSize, smallSize);

        g2.setColor(new Color(255, 247, 214, 238));
        g2.fillOval(anchorX - largeSize / 2, tailY - largeSize / 2 + direction * 8, largeSize, largeSize);
        g2.fillOval((anchorX + playerCenterX) / 2 - mediumSize / 2, (tailY + playerCenterY) / 2 - mediumSize / 2, mediumSize, mediumSize);
        g2.fillOval(playerCenterX - smallSize / 2, playerCenterY - smallSize / 2, smallSize, smallSize);
        g2.setColor(withAlpha(color, 210));
        g2.drawOval(anchorX - largeSize / 2, tailY - largeSize / 2 + direction * 8, largeSize, largeSize);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        if (!model.getState().isGameOver()) {
            return;
        }

        Player winner = model.getState().getWinner();
        if (winner == null) {
            return;
        }

        long now = System.currentTimeMillis();
        double pulse = (Math.sin(now / 180.0) + 1.0) / 2.0;
        BoardLayout layout = getBoardLayout();
        String title = "Joueur " + winner.getId() + " a gagne !";
        String subtitle = "Chemin gagnant trace";

        Graphics2D overlayGraphics = (Graphics2D) g2.create();
        overlayGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font titleFont = new Font("SansSerif", Font.BOLD, Math.max(18, layout.cellSize / 3));
        Font subtitleFont = new Font("SansSerif", Font.BOLD, Math.max(12, layout.cellSize / 5));
        overlayGraphics.setFont(titleFont);
        FontMetrics titleMetrics = overlayGraphics.getFontMetrics();
        overlayGraphics.setFont(subtitleFont);
        FontMetrics subtitleMetrics = overlayGraphics.getFontMetrics();

        int iconSize = Math.max(28, layout.cellSize / 2);
        int iconGap = Math.max(10, layout.cellSize / 7);
        int paddingX = Math.max(18, layout.cellSize / 4);
        int paddingY = Math.max(9, layout.cellSize / 8);
        int textWidth = Math.max(titleMetrics.stringWidth(title), subtitleMetrics.stringWidth(subtitle));
        int desiredWidth = textWidth + iconSize + iconGap + paddingX * 2;
        int maxPanelWidth = Math.max(180, layout.boardSize - 24);
        int panelWidth = Math.min(maxPanelWidth, desiredWidth);
        int panelHeight = Math.max(iconSize + paddingY * 2, titleMetrics.getHeight() + subtitleMetrics.getHeight() + paddingY * 2);
        int panelX = layout.x + (layout.boardSize - panelWidth) / 2;
        int panelY = layout.y + Math.max(8, layout.cellSize / 7);
        int arc = Math.max(18, panelHeight / 2);
        int contentX = panelX + paddingX;
        int iconX = contentX;
        int iconY = panelY + (panelHeight - iconSize) / 2 + (int) (pulse * 2);
        int textX = iconX + iconSize + iconGap;
        int titleY = panelY + paddingY + titleMetrics.getAscent();
        int subtitleY = titleY + subtitleMetrics.getHeight();

        overlayGraphics.setColor(new Color(0, 0, 0, 130));
        overlayGraphics.fillRoundRect(panelX + 4, panelY + 5, panelWidth, panelHeight, arc, arc);
        overlayGraphics.setPaint(new GradientPaint(panelX, panelY, new Color(121, 82, 52, 245), panelX, panelY + panelHeight, new Color(67, 42, 27, 245)));
        overlayGraphics.fillRoundRect(panelX, panelY, panelWidth, panelHeight, arc, arc);
        overlayGraphics.setColor(new Color(255, 229, 144, 225));
        overlayGraphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        overlayGraphics.drawRoundRect(panelX + 2, panelY + 2, panelWidth - 4, panelHeight - 4, arc, arc);

        drawMonkeyPawn(overlayGraphics, winner, wallColorForPlayer(winner.getId()), iconX, iconY, iconSize);

        overlayGraphics.setFont(titleFont);
        overlayGraphics.setColor(new Color(63, 38, 22));
        overlayGraphics.drawString(title, textX + 2, titleY + 2);
        overlayGraphics.setColor(new Color(255, 226, 95));
        overlayGraphics.drawString(title, textX, titleY);

        overlayGraphics.setFont(subtitleFont);
        overlayGraphics.setColor(new Color(60, 36, 20));
        overlayGraphics.drawString(subtitle, textX + 1, subtitleY + 1);
        overlayGraphics.setColor(new Color(255, 242, 190));
        overlayGraphics.drawString(subtitle, textX, subtitleY);

        overlayGraphics.dispose();
    }

    private void drawMessage(Graphics2D g2, BoardLayout layout) {
        if (message.isEmpty() || model.getState().isGameOver()) {
            return;
        }

        Graphics2D messageGraphics = (Graphics2D) g2.create();
        messageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        messageGraphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(15, layout.cellSize / 4)));
        FontMetrics metrics = messageGraphics.getFontMetrics();

        int paddingX = Math.max(14, layout.cellSize / 5);
        int paddingY = Math.max(8, layout.cellSize / 8);
        int textWidth = metrics.stringWidth(message);
        int bubbleWidth = Math.min(layout.boardSize - 36, textWidth + paddingX * 2);
        int bubbleHeight = metrics.getHeight() + paddingY * 2;
        int bubbleX = layout.x + (layout.boardSize - bubbleWidth) / 2;
        int bubbleY = layout.y + Math.max(12, layout.cellSize / 4);
        int arc = Math.max(14, bubbleHeight / 2);

        messageGraphics.setColor(new Color(0, 0, 0, 128));
        messageGraphics.fillRoundRect(bubbleX + 3, bubbleY + 4, bubbleWidth, bubbleHeight, arc, arc);
        messageGraphics.setColor(new Color(86, 54, 36, 225));
        messageGraphics.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, arc, arc);
        messageGraphics.setColor(new Color(255, 220, 122, 220));
        messageGraphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        messageGraphics.drawRoundRect(bubbleX + 1, bubbleY + 1, bubbleWidth - 3, bubbleHeight - 3, arc, arc);

        int textX = bubbleX + (bubbleWidth - textWidth) / 2;
        int textY = bubbleY + paddingY + metrics.getAscent();
        messageGraphics.setColor(new Color(52, 28, 16));
        messageGraphics.drawString(message, textX + 1, textY + 1);
        messageGraphics.setColor(new Color(255, 246, 205));
        messageGraphics.drawString(message, textX, textY);
        messageGraphics.dispose();
    }


    private void drawImageCover(Graphics2D g2, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.max((double) width / image.getWidth(), (double) height / image.getHeight());
        int drawWidth = (int) Math.ceil(image.getWidth() * scale);
        int drawHeight = (int) Math.ceil(image.getHeight() * scale);
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
    }

    private void loadThemeAssets() {
        this.themeBackgroundImage = loadImage(theme.backgroundPath);
        this.themeCellImage = loadImage(theme.cellImagePath);
        this.themeWallImage = makeLightEdgeBackgroundTransparent(loadImage(theme.wallImagePath));
        this.themeWallSourceBounds = findVisibleWallBounds(themeWallImage);
    }

    private BufferedImage loadImage(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        File file = findImageFile(path);
        if (!file.exists()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                System.out.println("Format d'image non reconnu : " + file.getAbsolutePath());
            }
            return image;
        } catch (IOException e) {
            System.out.println("Impossible de charger l'image : " + file.getAbsolutePath());
            return null;
        }
    }

    private File findImageFile(String path) {
        File directFile = new File(path);
        if (directFile.exists()) {
            return directFile;
        }

        File workingDirectory = new File(System.getProperty("user.dir"));
        File found = findImageFileFromBase(workingDirectory, path);
        if (found.exists()) {
            return found;
        }

        try {
            File codeRoot = new File(BoardView.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            found = findImageFileFromBase(codeRoot, path);
            if (found.exists()) {
                return found;
            }
        } catch (Exception ignored) {
        }

        return directFile;
    }

    private File findImageFileFromBase(File base, String path) {
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

    private Rectangle findVisibleWallBounds(BufferedImage image) {
        if (image == null) {
            return null;
        }

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >>> 24) & 0xff;
                if (alpha < 16) {
                    continue;
                }

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private BufferedImage makeLightEdgeBackgroundTransparent(BufferedImage image) {
        if (image == null) {
            return null;
        }

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
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, 0);
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, height - 1);
        }

        for (int y = 0; y < height; y++) {
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, 0, y);
            tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, width - 1, y);
        }

        while (head < tail) {
            int x = queueX[head];
            int y = queueY[head];
            head++;
            transparent.setRGB(x, y, 0);

            if (x > 0) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x - 1, y);
            }
            if (x + 1 < width) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x + 1, y);
            }
            if (y > 0) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, y - 1);
            }
            if (y + 1 < height) {
                tail = enqueueBackgroundPixel(transparent, visited, queueX, queueY, tail, x, y + 1);
            }
        }

        return transparent;
    }

    private int enqueueBackgroundPixel(BufferedImage image, boolean[] visited, int[] queueX, int[] queueY, int tail, int x, int y) {
        int width = image.getWidth();
        int index = y * width + x;
        if (visited[index]) {
            return tail;
        }

        int rgb = image.getRGB(x, y);
        int alpha = (rgb >>> 24) & 0xff;
        if (alpha < 16 || isLightNeutralBackground(rgb)) {
            visited[index] = true;
            queueX[tail] = x;
            queueY[tail] = y;
            return tail + 1;
        }

        return tail;
    }

    private boolean isLightNeutralBackground(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return max > 218 && max - min < 22;
    }

    private enum Theme {
        JUNGLE(
            new Color(18, 48, 30),
            new Color(48, 104, 54, 185),
            new Color(22, 67, 37, 205),
            new Color(8, 44, 25, 145),
            new Color(85, 148, 67, 194),
            new Color(172, 220, 108, 140),
            new Color(255, 236, 115, 95),
            new Color(216, 255, 147, 145),
            new Color(35, 105, 201),
            new Color(255, 126, 69),
            "images/themes/forest/BACKGROUND.png",
            "images/robin/cell_default.png",
            "images/themes/forest/wall.png",
            new Color(4, 24, 13, 35)
        );

        private final String backgroundPath;
        private final String cellImagePath;
        private final String wallImagePath;
        private final Color backgroundOverlay;
        private final Color background;
        private final Color boardTop;
        private final Color boardBottom;
        private final Color groove;
        private final Color cell;
        private final Color cellBorder;
        private final Color pawnGlow;
        private final Color player1Fallback;
        private final Color player2Fallback;
        private final Color player1Wall;
        private final Color player2Wall;
        private final Color wall;
        private final Color wallOutline;
        private final Color moveHighlight;
        private final Color moveHighlightBorder;

        Theme(Color background, Color boardTop, Color boardBottom, Color groove,
                Color cell, Color cellBorder, Color pawnGlow, Color goalMark,
                Color player1Fallback, Color player2Fallback, String backgroundPath,
                String cellImagePath, String wallImagePath, Color backgroundOverlay) {
            this.backgroundPath = backgroundPath;
            this.cellImagePath = cellImagePath;
            this.wallImagePath = wallImagePath;
            this.backgroundOverlay = backgroundOverlay;
            this.background = background;
            this.boardTop = boardTop;
            this.boardBottom = boardBottom;
            this.groove = groove;
            this.cell = cell;
            this.cellBorder = cellBorder;
            this.pawnGlow = pawnGlow;

            this.player1Fallback = player1Fallback;
            this.player2Fallback = player2Fallback;
            this.player1Wall = new Color(41, 126, 216);
            this.player2Wall = new Color(255, 126, 69);

            this.wall = new Color(104, 66, 26);

            this.wallOutline = new Color(25, 68, 28);

            this.moveHighlight = new Color(45, 145, 255);
            this.moveHighlightBorder = new Color(215, 240, 255);
        }
    }

    private static class BoardLayout {
        private final int x;
        private final int y;
        private final int boardSize;
        private final int cellSize;
        private final int gapSize;

        private BoardLayout(int x, int y, int boardSize, int cellSize, int gapSize) {
            this.x = x;
            this.y = y;
            this.boardSize = boardSize;
            this.cellSize = cellSize;
            this.gapSize = gapSize;
        }
    }
}
