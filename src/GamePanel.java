import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.RadialGradientPaint;
import java.util.HashSet;
import java.util.Set;

public class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private enum Screen { MENU, PLAYING, PAUSED, VICTORY, GAME_OVER }

    private static final int WIDTH = MuseumMap.WIDTH;
    private static final int HEIGHT = MuseumMap.HEIGHT;
    private static final Color GOLD = new Color(231, 190, 105);
    private static final Color INK = new Color(15, 17, 24);

    private final Set<String> pressedKeys = new HashSet<>();
    private final GameLoop gameLoop;
    private GameWorld world;
    private Screen screen = Screen.MENU;
    private long animationFrame;

    public GamePanel() {
        setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(INK);
        installKeys();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleClick(event.getX(), event.getY());
            }
        });
        gameLoop = new GameLoop(this::updateGame);
    }

    private void installKeys() {
        bindMove("W", "up", true);
        bindMove("UP", "up", true);
        bindMove("S", "down", true);
        bindMove("DOWN", "down", true);
        bindMove("A", "left", true);
        bindMove("LEFT", "left", true);
        bindMove("D", "right", true);
        bindMove("RIGHT", "right", true);
        bindMove("SHIFT", "sprint", true);
        bindMove("E", "interact", true);
        bindMove("F", "steal", true);
        bindMove("W", "up", false);
        bindMove("UP", "up", false);
        bindMove("S", "down", false);
        bindMove("DOWN", "down", false);
        bindMove("A", "left", false);
        bindMove("LEFT", "left", false);
        bindMove("D", "right", false);
        bindMove("RIGHT", "right", false);
        bindMove("SHIFT", "sprint", false);
        bindMove("E", "interact", false);
        bindMove("F", "steal", false);
        bindAction("P", "pause", () -> togglePause());
        bindAction("ESCAPE", "escape", () -> handleEscape());
        bindAction("ENTER", "enter", () -> {
            if (screen == Screen.MENU) startGame();
            else if (screen == Screen.PAUSED) resumeGame();
            else if (screen == Screen.VICTORY || screen == Screen.GAME_OVER) startGame();
        });
        bindAction("SPACE", "space", () -> {
            if (screen == Screen.MENU) startGame();
            else if (screen == Screen.PAUSED) resumeGame();
        });
        bindAction("R", "restart", () -> {
            if (screen == Screen.PAUSED || screen == Screen.GAME_OVER || screen == Screen.VICTORY) {
                startGame();
            }
        });
    }

    private void bindMove(String key, String control, boolean down) {
        String id = control + (down ? "Down" : "Up") + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke((down ? "pressed " : "released ") + key), id
        );
        getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (down) {
                    if (control.equals("interact") || control.equals("steal")) {
                        if (!pressedKeys.contains(control)) {
                            worldInputEdge(control);
                        }
                    }
                    pressedKeys.add(control);
                } else {
                    pressedKeys.remove(control);
                }
            }
        });
    }

    private void worldInputEdge(String control) {
        if (world == null) return;
        if (control.equals("interact")) world.setInteractPressed(true);
        if (control.equals("steal")) world.setStealPressed(true);
    }

    private void bindAction(String key, String id, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void startGame() {
        gameLoop.stop();
        pressedKeys.clear();
        world = new GameWorld();
        screen = Screen.PLAYING;
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    private void resumeGame() {
        if (screen != Screen.PAUSED) return;
        screen = Screen.PLAYING;
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    private void togglePause() {
        if (screen == Screen.PLAYING) {
            screen = Screen.PAUSED;
            pressedKeys.clear();
            gameLoop.stop();
        } else if (screen == Screen.PAUSED) {
            resumeGame();
        }
        repaint();
    }

    private void handleEscape() {
        if (screen == Screen.PLAYING) togglePause();
        else if (screen == Screen.PAUSED || screen == Screen.VICTORY || screen == Screen.GAME_OVER) {
            gameLoop.stop();
            pressedKeys.clear();
            screen = Screen.MENU;
            repaint();
        }
    }

    private void handleClick(int x, int y) {
        if (screen == Screen.MENU) {
            if (buttonBounds(390, 350).contains(x, y)) startGame();
        } else if (screen == Screen.PAUSED) {
            if (buttonBounds(390, 330).contains(x, y)) resumeGame();
            else if (buttonBounds(390, 390).contains(x, y)) startGame();
            else if (buttonBounds(390, 450).contains(x, y)) handleEscape();
        } else if (screen == Screen.VICTORY || screen == Screen.GAME_OVER) {
            if (buttonBounds(390, 390).contains(x, y)) startGame();
            else if (buttonBounds(390, 450).contains(x, y)) handleEscape();
        }
    }

    private static java.awt.Rectangle buttonBounds(int x, int y) {
        return new java.awt.Rectangle(x, y, 180, 46);
    }

    private void updateGame() {
        if (screen != Screen.PLAYING || world == null) return;
        animationFrame++;
        GameWorld.Input input = new GameWorld.Input(
            pressedKeys.contains("up"),
            pressedKeys.contains("down"),
            pressedKeys.contains("left"),
            pressedKeys.contains("right"),
            pressedKeys.contains("sprint"),
            pressedKeys.contains("interact"),
            pressedKeys.contains("steal")
        );
        world.update(1.0 / 60.0, input);
        if (world.isWon()) {
            screen = Screen.VICTORY;
            gameLoop.stop();
            pressedKeys.clear();
        } else if (world.isGameOver()) {
            screen = Screen.GAME_OVER;
            gameLoop.stop();
            pressedKeys.clear();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (screen == Screen.MENU) drawMenu(g);
        else {
            if (world != null) drawGame(g, world);
            if (screen == Screen.PAUSED) drawOverlay(g, "HEIST PAUSED", "The guards are still on their rounds.", "CONTINUE", "RESTART HEIST", "MAIN MENU");
            else if (screen == Screen.VICTORY) drawOverlay(g, "HEIST COMPLETE", "Every original is safe. You made it out.", "PLAY AGAIN", "", "MAIN MENU");
            else if (screen == Screen.GAME_OVER) drawOverlay(g, "HEIST BUSTED", world == null ? "" : world.getEndReason(), "TRY AGAIN", "", "MAIN MENU");
        }
        g.dispose();
    }

    private void drawGame(Graphics2D g, GameWorld game) {
        MuseumMap map = game.getMap();
        map.draw(g);
        drawGuardVision(g, game.getGuards());
        for (MuseumMap.Painting painting : map.getPaintings()) drawPainting(g, painting);
        for (MuseumMap.HideSpot spot : map.getHideSpots()) drawHideSpot(g, spot, game.isNear(spot));
        for (Guard guard : game.getGuards()) drawGuard(g, guard);
        drawPlayer(g, game);
        drawLighting(g, game.getPlayerCenterX(), game.getPlayerCenterY());
        drawHud(g, game);
        drawPrompt(g, game);
    }

    private void drawGuardVision(Graphics2D g, java.util.List<Guard> guards) {
        for (Guard guard : guards) {
            Point facing = guard.getFacing();
            double angle = Math.atan2(facing.y, facing.x);
            double radius = guard.getState() == Guard.State.CHASE ? 230 : 190;
            Path2D.Double cone = new Path2D.Double();
            cone.moveTo(guard.centerX(), guard.centerY());
            for (int step = 0; step <= 24; step++) {
                double a = angle - Math.toRadians(58) + Math.toRadians(116) * step / 24.0;
                double x = guard.centerX() + Math.cos(a) * radius;
                double y = guard.centerY() + Math.sin(a) * radius;
                if (step == 0) cone.lineTo(x, y);
                else cone.lineTo(x, y);
            }
            cone.closePath();
            Color tint = guard.getState() == Guard.State.CHASE
                ? new Color(210, 54, 59, 52)
                : new Color(238, 191, 101, 28);
            g.setColor(tint);
            g.fill(cone);
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 95));
            g.draw(cone);
        }
    }

    private void drawPainting(Graphics2D g, MuseumMap.Painting painting) {
        int x = painting.getX();
        int y = painting.getY();
        int w = MuseumMap.Painting.WIDTH;
        int h = MuseumMap.Painting.HEIGHT;
        g.setColor(new Color(0, 0, 0, 95));
        g.fillRect(x - 5, y - 4, w + 10, h + 11);
        g.setColor(new Color(92, 54, 37));
        g.fillRect(x - 4, y - 4, w + 8, h + 8);
        g.setColor(new Color(205, 157, 83));
        g.fillRect(x - 2, y - 2, w + 4, h + 4);
        g.setColor(new Color(251, 218, 144));
        g.fillRect(x - 2, y - 2, w + 4, 3);
        if (painting.isStolen()) {
        }
        Color[] skies = {
            new Color(45, 93, 124), new Color(60, 106, 86), new Color(91, 67, 117)
        };
        Color sky = skies[painting.getIndex() % skies.length];
        if (painting.isStolen()) sky = sky.brighter();
        g.setColor(sky);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(244, 198, 111));
        int sunOffset = painting.isStolen() ? 3 : 0;
        g.fillRect(x + 7 + sunOffset, y + 5, 9, 9);
        g.setColor(new Color(36, 61, 75));
        g.fillRect(x + 2, y + 17, 18, 8);
        g.fillRect(x + 18, y + 12, 16, 13);
        g.setColor(new Color(53, 89, 64));
        g.fillRect(x + 30, y + 15, 22, 10);
        g.setColor(new Color(230, 160, 92));
        g.fillRect(x + 23, y + 8, 6, 6);
        g.setColor(new Color(40, 41, 52));
        g.fillRect(x + 42, y + 5, 8, 6);
        if (painting.isStolen() && painting.getQuality() < 90) {
            g.setColor(new Color(244, 205, 157));
            g.fillRect(x + 47, y + 26, 2, 2);
        }
        if (painting.isStolen() && painting.isDiscovered()) {
            g.setColor(new Color(183, 63, 54));
            g.fillRect(x + w - 9, y + 2, 6, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.BOLD, 7));
            g.drawString("!", x + w - 8, y + 9);
        }
    }

    private void drawHideSpot(Graphics2D g, MuseumMap.HideSpot spot, boolean near) {
        java.awt.Rectangle r = spot.getBounds();
        g.setColor(new Color(32, 27, 35));
        g.fillRect(r.x + 3, r.y + 5, r.width, r.height);
        g.setColor(new Color(87, 56, 45));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(new Color(153, 101, 61));
        g.fillRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6);
        g.setColor(new Color(62, 47, 43));
        g.fillRect(r.x + 7, r.y + 9, r.width - 14, r.height - 15);
        g.setColor(new Color(218, 176, 106));
        g.fillRect(r.x + 4, r.y + 5, r.width - 8, 3);
        g.setColor(near ? GOLD : new Color(95, 83, 74));
        g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(new Color(238, 221, 180));
        g.drawString("HIDE", r.x + 8, r.y + r.height - 4);
    }

    private void drawGuard(Graphics2D g, Guard guard) {
        int x = (int) Math.round(guard.getX());
        int y = (int) Math.round(guard.getY());
        boolean chasing = guard.getState() == Guard.State.CHASE;
        Color shirt = chasing ? new Color(147, 50, 57) : new Color(49, 83, 69);
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(x + 1, y + 17, 23, 8);
        g.setColor(new Color(40, 34, 39));
        g.fillRect(x + 4, y + 17, 6, 6);
        g.fillRect(x + 15, y + 17, 6, 6);
        g.setColor(shirt);
        g.fillRect(x + 3, y + 7, 18, 13);
        g.setColor(chasing ? new Color(214, 78, 71) : new Color(83, 130, 93));
        g.fillRect(x + 6, y + 9, 12, 7);
        g.setColor(new Color(219, 177, 82));
        g.fillRect(x + 4, y + 16, 16, 2);
        g.setColor(new Color(202, 151, 111));
        g.fillRect(x + 6, y + 1, 13, 9);
        g.setColor(new Color(43, 36, 43));
        g.fillRect(x + 4, y, 17, 4);
        g.setColor(new Color(244, 221, 172));
        g.fillRect(x + 9, y + 5, 2, 2);
        g.fillRect(x + 15, y + 5, 2, 2);
        if (guard.getState() == Guard.State.CHASE || guard.getState() == Guard.State.ALERT) {
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(chasing ? new Color(255, 87, 76) : GOLD);
            g.drawString(chasing ? "!" : "?", x + 7, y - 5);
        }
    }

    private void drawPlayer(Graphics2D g, GameWorld game) {
        int x = (int) Math.round(game.getPlayerX());
        int y = (int) Math.round(game.getPlayerY());
        boolean walking = game.isMoving();
        int step = walking && animationFrame / 8 % 2 == 0 ? 2 : 0;
        if (game.isHidden()) {
            g.setColor(new Color(10, 12, 16, 120));
            g.fillOval(x - 7, y - 5, 34, 34);
            return;
        }
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(x + 1, y + 18, 23, 8);
        g.setColor(new Color(39, 31, 41));
        g.fillRect(x + 4, y + 18 + step, 6, 5);
        g.fillRect(x + 15, y + 18 - step, 6, 5);
        g.setColor(new Color(39, 45, 69));
        g.fillRect(x + 3, y + 9, 18, 12);
        g.setColor(new Color(68, 87, 127));
        g.fillRect(x + 6, y + 10, 12, 7);
        g.setColor(new Color(222, 172, 82));
        g.fillRect(x + 10, y + 10, 5, 8);
        g.setColor(new Color(210, 151, 106));
        g.fillRect(x + 6, y + 2, 13, 9);
        g.setColor(new Color(36, 30, 42));
        g.fillRect(x + 5, y, 15, 4);
        g.fillRect(x + 6, y + 7, 14, 3);
        g.setColor(new Color(255, 225, 168));
        g.fillRect(x + 9, y + 7, 2, 2);
        g.fillRect(x + 16, y + 7, 2, 2);
        g.setColor(new Color(139, 82, 45));
        g.fillRect(x + 17, y + 13, 5, 6);
    }

    private void drawLighting(Graphics2D g, double x, double y) {
        java.awt.Rectangle playArea = new java.awt.Rectangle(0, 66, WIDTH, HEIGHT - 66);
        g.setColor(new Color(8, 10, 18, 68));
        g.fill(playArea);
        float radius = 475f;
        g.setPaint(new RadialGradientPaint(
            new Point((int) x, (int) y), radius,
            new float[]{0f, 0.32f, 0.72f, 1f},
            new Color[]{
                new Color(8, 10, 18, 0),
                new Color(8, 10, 18, 10),
                new Color(8, 10, 18, 65),
                new Color(8, 10, 18, 145)
            }
        ));
        g.fill(playArea);
    }

    private void drawHud(Graphics2D g, GameWorld game) {
        g.setColor(new Color(14, 16, 23));
        g.fillRect(0, 0, WIDTH, 66);
        g.setColor(GOLD);
        g.fillRect(0, 63, WIDTH, 3);
        g.setFont(new Font("Monospaced", Font.BOLD, 20));
        g.setColor(new Color(255, 222, 154));
        g.drawString("PAINTHEIST", 18, 27);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(203, 198, 207));
        g.drawString("OBJECTIVE", 165, 19);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        String objective = game.getStolenCount() == MuseumMap.Painting.COUNT
            ? "Reach the marked exit" : "Copy, then steal every marked painting";
        g.drawString(objective, 165, 38);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(game.getTimeLeft() < 30 ? new Color(255, 104, 93) : new Color(198, 230, 200));
        g.drawString(String.format("%03d:%02d", game.getTimeLeft() / 60, game.getTimeLeft() % 60), 435, 29);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(202, 193, 205));
        g.drawString("TIME", 435, 48);
        g.setColor(new Color(53, 48, 57));
        g.fillRect(515, 20, 118, 12);
        g.setColor(game.getSuspicion() >= 70 ? new Color(228, 75, 70) : new Color(224, 159, 78));
        g.fillRect(515, 20, (int) (114 * game.getSuspicion() / 100.0), 8);
        g.setColor(new Color(225, 218, 221));
        g.drawRect(514, 19, 119, 13);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString("SUSPICION " + game.getSuspicion() + "%", 515, 47);
        g.setColor(new Color(225, 219, 229));
        g.drawString("ART " + game.getStolenCount() + "/" + MuseumMap.Painting.COUNT, 674, 27);
        String alert = game.getAlertStatus();
        g.setColor(alert.equals("CALM") ? new Color(133, 216, 157)
            : alert.equals("CHASE") ? new Color(255, 93, 84) : GOLD);
        g.drawString("SECURITY: " + alert, 674, 47);
        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g.setColor(new Color(157, 153, 164));
        g.drawString("WASD MOVE  SHIFT SPRINT  E INTERACT  F STEAL  P PAUSE", 18, 54);
    }

    private void drawPrompt(Graphics2D g, GameWorld game) {
        String prompt = game.getPrompt();
        if (prompt.isEmpty()) return;
        int w = 470;
        int x = (WIDTH - w) / 2;
        int y = HEIGHT - 51;
        g.setColor(new Color(14, 15, 22, 235));
        g.fillRect(x, y, w, 34);
        g.setColor(new Color(184, 145, 82));
        g.drawRect(x, y, w, 34);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(246, 232, 199));
        drawCentered(g, prompt, WIDTH / 2, y + 22);
        if (game.getCopyProgress() > 0 && game.getCopyProgress() < 100) {
            g.setColor(new Color(42, 40, 46));
            g.fillRect(x + 10, y + 27, w - 20, 3);
            g.setColor(new Color(97, 202, 143));
            g.fillRect(x + 10, y + 27, (w - 20) * game.getCopyProgress() / 100, 3);
        }
    }

    private void drawMenu(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 42));
        g.setColor(new Color(255, 219, 142));
        drawCentered(g, "PAINTHEIST", WIDTH / 2, 155);
        g.setFont(new Font("Monospaced", Font.PLAIN, 15));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "THE QUIETEST HEIST LEAVES THE BEST STORY", WIDTH / 2, 190);
        drawMuseumEmblem(g);
        drawButton(g, "BEGIN THE HEIST", 390, 350, true);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(171, 166, 177));
        drawCentered(g, "WASD / ARROWS MOVE   SHIFT SPRINT   E COPY / HIDE   F SWAP ART", WIDTH / 2, 442);
        drawCentered(g, "Copy every masterpiece, leave a convincing fake, and reach the exit.", WIDTH / 2, 465);
        g.setColor(new Color(120, 113, 127));
        drawCentered(g, "404 TULOG PRODUCTIONS", WIDTH / 2, 547);
    }

    private void drawBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(39, 33, 42), WIDTH, HEIGHT, new Color(9, 11, 17)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(112, 82, 54));
        g.fillRect(30, 30, WIDTH - 60, HEIGHT - 60);
        g.setColor(new Color(25, 24, 31));
        g.fillRect(36, 36, WIDTH - 72, HEIGHT - 72);
        g.setColor(new Color(177, 133, 78));
        g.drawRect(43, 43, WIDTH - 86, HEIGHT - 86);
        g.setColor(new Color(54, 47, 54));
        for (int x = 70; x < WIDTH - 80; x += 56) {
            g.fillRect(x, 58, 4, HEIGHT - 116);
        }
    }

    private void drawMuseumEmblem(Graphics2D g) {
        int x = 405;
        int y = 222;
        g.setColor(new Color(78, 48, 37));
        g.fillRect(x, y, 150, 94);
        g.setColor(new Color(211, 165, 91));
        g.fillRect(x + 6, y + 6, 138, 82);
        g.setColor(new Color(38, 63, 81));
        g.fillRect(x + 13, y + 13, 124, 68);
        g.setColor(new Color(240, 198, 113));
        g.fillRect(x + 26, y + 23, 24, 24);
        g.setColor(new Color(38, 54, 65));
        g.fillRect(x + 16, y + 53, 53, 22);
        g.setColor(new Color(62, 94, 68));
        g.fillRect(x + 66, y + 44, 64, 31);
        g.setColor(new Color(224, 164, 91));
        g.fillRect(x + 83, y + 35, 14, 19);
        g.setColor(new Color(248, 220, 151));
        g.drawRect(x - 2, y - 2, 154, 98);
    }

    private void drawOverlay(Graphics2D g, String title, String subtitle, String first, String second, String third) {
        g.setColor(new Color(7, 9, 14, 205));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(35, 31, 40));
        g.fillRect(260, 145, 440, 360);
        g.setColor(GOLD);
        g.fillRect(260, 145, 440, 5);
        g.drawRect(260, 145, 440, 360);
        g.setFont(new Font("Monospaced", Font.BOLD, 27));
        g.setColor(screen == Screen.VICTORY ? new Color(137, 226, 161)
            : screen == Screen.GAME_OVER ? new Color(246, 108, 95) : new Color(255, 220, 144));
        drawCentered(g, title, WIDTH / 2, 220);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(219, 211, 221));
        drawCentered(g, subtitle, WIDTH / 2, 255);
        int firstY = screen == Screen.PAUSED ? 330 : 390;
        if (!first.isEmpty()) drawButton(g, first, 390, firstY, true);
        if (!second.isEmpty()) drawButton(g, second, 390, 390, false);
        drawButton(g, third, 390, 450, false);
        if (screen == Screen.PAUSED) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(157, 151, 164));
            drawCentered(g, "P TO RESUME   R TO RESTART   ESC FOR MENU", WIDTH / 2, 490);
        }
    }

    private void drawButton(Graphics2D g, String label, int x, int y, boolean highlighted) {
        g.setColor(highlighted ? new Color(119, 79, 45) : new Color(55, 48, 57));
        g.fillRect(x, y, 180, 46);
        g.setColor(highlighted ? new Color(231, 190, 105) : new Color(127, 105, 78));
        g.drawRect(x, y, 180, 46);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(255, 237, 199));
        drawCentered(g, label, x + 90, y + 28);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
