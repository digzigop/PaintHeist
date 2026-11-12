import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.RadialGradientPaint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private enum Screen {
        MENU, LEVEL_SELECT, SHOP, CUSTOMIZE, SETTINGS, GALLERY, PLAYING, PAUSED, VICTORY, GAME_OVER
    }

    static final class Assets {
        private final BufferedImage[][] playerSprites;
        private final BufferedImage[][][] guardSprites;
        private final BufferedImage btnPlay;
        private final BufferedImage btnCustomize;
        private final BufferedImage btnSettings;
        private final BufferedImage btnQuit;
        private final BufferedImage btnShop;
        private final BufferedImage framePrimary;
        private final BufferedImage frameSecondary;
        private final BufferedImage frameBadge;
        private final BufferedImage menuBg;

        private Assets(BufferedImage[][] playerSprites, BufferedImage[][][] guardSprites,
                       BufferedImage btnPlay, BufferedImage btnCustomize, BufferedImage btnSettings,
                       BufferedImage btnQuit, BufferedImage btnShop, BufferedImage framePrimary,
                       BufferedImage frameSecondary, BufferedImage frameBadge, BufferedImage menuBg) {
            this.playerSprites = playerSprites;
            this.guardSprites = guardSprites;
            this.btnPlay = btnPlay;
            this.btnCustomize = btnCustomize;
            this.btnSettings = btnSettings;
            this.btnQuit = btnQuit;
            this.btnShop = btnShop;
            this.framePrimary = framePrimary;
            this.frameSecondary = frameSecondary;
            this.frameBadge = frameBadge;
            this.menuBg = menuBg;
        }

        BufferedImage[] getThiefLoadingFrames() {
            return playerSprites[ACTION_ROW_START[WALK_ACTION] + 3];
        }

        BufferedImage[] getGuardLoadingFrames() {
            return guardSprites[1][3];
        }
    }

    private static final int WIDTH = MuseumMap.WIDTH;
    private static final int HEIGHT = MuseumMap.HEIGHT;
    private static final Color GOLD = new Color(231, 190, 105);
    private static final Color INK = new Color(15, 17, 24);
    private static final int SPRITE_COLUMNS = 6;
    private static final int SPRITE_ROWS = 20;
    private static final int SPRITE_STRIP_FRAME_COUNT = 4;
    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 51;
    private static final int GUARD_SPRITE_WIDTH = 32;
    private static final int GUARD_SPRITE_HEIGHT = 64;
    private static final int[] ACTION_ROW_START = {0, 4, 8, 12, 16};
    private static final int IDLE_ACTION = 0;
    private static final int WALK_ACTION = 1;
    private static final int RUN_ACTION = 2;
    private static final int SNEAK_ACTION = 3;
    private static final int VICTORY_NEXT_Y = 335;
    private static final int VICTORY_REPLAY_Y = 395;
    private static final int VICTORY_SELECT_Y = 455;
    private static final Rectangle MENU_PLAY_BOUNDS = new Rectangle(397, 238, 166, 91);
    private static final Rectangle MENU_CUSTOMIZE_BOUNDS = new Rectangle(401, 340, 158, 57);
    private static final Rectangle MENU_SETTINGS_BOUNDS = new Rectangle(401, 403, 158, 57);
    private static final Rectangle MENU_QUIT_BOUNDS = new Rectangle(401, 466, 158, 57);
    private static final Rectangle MENU_SHOP_BOUNDS = new Rectangle(56, 52, 126, 59);
    private static final Rectangle MENU_GALLERY_BOUNDS = new Rectangle(768, 56, 136, 48);

    private final Set<String> pressedKeys = new HashSet<>();
    private final GameLoop gameLoop;
    private final LevelManager levelManager = new LevelManager();
    private final BufferedImage[][] playerSpriteFrames;
    private final BufferedImage[][][] guardSprites;
    private final Assets uiAssets;
    private final Map<BufferedImage, BufferedImage[]> tintedPlayerCache = new HashMap<>();
    private GameWorld world;
    private LevelManager.Result lastResult;
    private Screen screen = Screen.MENU;
    private int selectedLevel;
    private long animationFrame;
    private int playerDirectionRow = 1;
    private int hoverX = -1;
    private int hoverY = -1;
    private String uiToast = "";

    public GamePanel() {
        this(loadAssets());
    }

    GamePanel(Assets assets) {
        uiAssets = assets;
        playerSpriteFrames = assets.playerSprites;
        guardSprites = assets.guardSprites;
        setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(INK);
        installKeys();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                pressedKeys.clear();
            }
        });
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Point p = toCanvasPoint(event.getX(), event.getY());
                if (p != null) handleClick(p.x, p.y);
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                Point p = toCanvasPoint(event.getX(), event.getY());
                hoverX = p == null ? -1 : p.x;
                hoverY = p == null ? -1 : p.y;
                updateCursor();
                if (screen != Screen.PLAYING) repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoverX = -1;
                hoverY = -1;
                setCursor(Cursor.getDefaultCursor());
                if (screen != Screen.PLAYING) repaint();
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        gameLoop = new GameLoop(this::updateGame);
    }

    private Point toCanvasPoint(int rawX, int rawY) {
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        double offsetX = (getWidth() - WIDTH * scale) / 2;
        double offsetY = (getHeight() - HEIGHT * scale) / 2;
        if (rawX < offsetX || rawX >= offsetX + WIDTH * scale
            || rawY < offsetY || rawY >= offsetY + HEIGHT * scale) {
            return null;
        }
        return new Point((int) ((rawX - offsetX) / scale), (int) ((rawY - offsetY) / scale));
    }

    private void updateCursor() {
        boolean clickable = false;
        if (hoverX >= 0 && hoverY >= 0) {
            if (screen == Screen.MENU) {
                clickable = MENU_PLAY_BOUNDS.contains(hoverX, hoverY)
                    || MENU_CUSTOMIZE_BOUNDS.contains(hoverX, hoverY)
                    || MENU_SETTINGS_BOUNDS.contains(hoverX, hoverY)
                    || MENU_QUIT_BOUNDS.contains(hoverX, hoverY)
                    || MENU_SHOP_BOUNDS.contains(hoverX, hoverY)
                    || MENU_GALLERY_BOUNDS.contains(hoverX, hoverY);
            } else if (screen == Screen.LEVEL_SELECT) {
                int level = levelAt(hoverX, hoverY);
                clickable = (level >= 0 && levelManager.isUnlocked(level))
                    || buttonBounds(390, 510).contains(hoverX, hoverY);
            } else if (screen == Screen.SHOP) {
                clickable = shopItemAt(hoverX, hoverY) >= 0 || buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.CUSTOMIZE) {
                clickable = outfitCardAt(hoverX, hoverY) >= 0 || buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.SETTINGS) {
                clickable = settingsRowAt(hoverX, hoverY) >= 0 || buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.GALLERY) {
                clickable = buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.PAUSED) {
                clickable = buttonBounds(390, 330).contains(hoverX, hoverY)
                    || buttonBounds(390, 390).contains(hoverX, hoverY)
                    || buttonBounds(390, 450).contains(hoverX, hoverY);
            } else if (screen == Screen.VICTORY) {
                clickable = buttonBounds(390, VICTORY_NEXT_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, VICTORY_REPLAY_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, VICTORY_SELECT_Y).contains(hoverX, hoverY);
            } else if (screen == Screen.GAME_OVER) {
                clickable = buttonBounds(390, 390).contains(hoverX, hoverY)
                    || buttonBounds(390, 450).contains(hoverX, hoverY);
            }
        }
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    static Assets loadAssets() {
        return new Assets(
            loadPlayerSpriteFrames(),
            loadGuardSprites(),
            loadOptionalUiImage("assets/ui/btn_play.png"),
            loadOptionalUiImage("assets/ui/btn_customize.png"),
            loadOptionalUiImage("assets/ui/btn_settings.png"),
            loadOptionalUiImage("assets/ui/btn_quit.png"),
            loadOptionalUiImage("assets/ui/btn_shop.png"),
            loadOptionalUiImage("assets/ui/frame_primary.png"),
            loadOptionalUiImage("assets/ui/frame_secondary.png"),
            loadOptionalUiImage("assets/ui/frame_badge.png"),
            loadOptionalUiImage("assets/ui/menu_bg.png")
        );
    }

    private static BufferedImage loadOptionalUiImage(String assetPath) {
        try {
            InputStream resource = GamePanel.class.getResourceAsStream("/" + assetPath);
            if (resource != null) {
                try (InputStream input = resource) {
                    return ImageIO.read(input);
                }
            }
            File file = findProjectAssetFrom(new File("").getAbsoluteFile().toPath(), assetPath);
            if (file == null) {
                Path classLocation = Paths.get(GamePanel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
                if (!java.nio.file.Files.isDirectory(classLocation)) {
                    classLocation = classLocation.getParent();
                }
                file = findProjectAssetFrom(classLocation, assetPath);
            }
            return file != null && file.isFile() ? ImageIO.read(file) : null;
        } catch (IOException | URISyntaxException ignored) {
            return null;
        }
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
        bindMove("CONTROL", "sneak", true);
        bindMove("E", "interact", true);
        bindMove("F", "steal", true);
        bindMove("Q", "distract", true);
        bindMove("W", "up", false);
        bindMove("UP", "up", false);
        bindMove("S", "down", false);
        bindMove("DOWN", "down", false);
        bindMove("A", "left", false);
        bindMove("LEFT", "left", false);
        bindMove("D", "right", false);
        bindMove("RIGHT", "right", false);
        bindMove("SHIFT", "sprint", false);
        bindMove("CONTROL", "sneak", false);
        bindMove("E", "interact", false);
        bindMove("F", "steal", false);
        bindMove("Q", "distract", false);
        bindAction("P", "pause", () -> togglePause());
        bindAction("ESCAPE", "escape", () -> handleEscape());
        bindAction("F11", "fullScreen", Main::toggleFullScreen);
        bindAction("M", "mute", () -> {
            SoundEffects.toggleMute();
            levelManager.saveSettings();
            repaint();
        });
        bindAction("ENTER", "enter", () -> {
            if (screen == Screen.MENU) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.LEVEL_SELECT;
                updateCursor();
            } else if (screen == Screen.LEVEL_SELECT) startSelectedLevel();
            else if (screen == Screen.PAUSED) resumeGame();
            else if (screen == Screen.VICTORY || screen == Screen.GAME_OVER) startGame(selectedLevel);
            repaint();
        });
        bindAction("SPACE", "space", () -> {
            if (screen == Screen.MENU) screen = Screen.LEVEL_SELECT;
            else if (screen == Screen.LEVEL_SELECT) startSelectedLevel();
            else if (screen == Screen.PAUSED) resumeGame();
            repaint();
        });
        bindAction("R", "restart", () -> {
            if (screen == Screen.PAUSED || screen == Screen.GAME_OVER || screen == Screen.VICTORY) {
                startGame(selectedLevel);
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
                    if (screen == Screen.LEVEL_SELECT && isDirectional(control)) {
                        moveLevelSelection(levelSelectionDelta(control));
                        return;
                    }
                    if (control.equals("interact") || control.equals("steal") || control.equals("distract")) {
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

    private static boolean isDirectional(String control) {
        return control.equals("up") || control.equals("down")
            || control.equals("left") || control.equals("right");
    }

    private static int levelSelectionDelta(String control) {
        if (control.equals("left")) return -1;
        if (control.equals("right")) return 1;
        if (control.equals("up")) return -2;
        return 2;
    }

    private void worldInputEdge(String control) {
        if (screen != Screen.PLAYING || world == null) return;
        if (control.equals("interact")) world.setInteractPressed(true);
        if (control.equals("steal")) world.setStealPressed(true);
        if (control.equals("distract")) world.setDistractPressed(true);
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
        startGame(selectedLevel);
    }

    private void startGame(int levelIndex) {
        gameLoop.stop();
        pressedKeys.clear();
        selectedLevel = levelIndex;
        lastResult = null;
        world = new GameWorld(levelIndex);
        world.applyUpgrades(
            levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_THROW),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_SWITCH)
        );
        screen = Screen.PLAYING;
        updateCursor();
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    private void resumeGame() {
        if (screen != Screen.PAUSED) return;
        screen = Screen.PLAYING;
        updateCursor();
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    private void togglePause() {
        if (screen == Screen.PLAYING) {
            screen = Screen.PAUSED;
            pressedKeys.clear();
            gameLoop.stop();
            updateCursor();
        } else if (screen == Screen.PAUSED) {
            resumeGame();
        }
        repaint();
    }

    private void handleEscape() {
        if (screen == Screen.PLAYING) togglePause();
        else if (screen == Screen.LEVEL_SELECT || screen == Screen.SHOP || screen == Screen.CUSTOMIZE
            || screen == Screen.SETTINGS || screen == Screen.GALLERY) {
            uiToast = "";
            screen = Screen.MENU;
            updateCursor();
            repaint();
        } else if (screen == Screen.PAUSED || screen == Screen.VICTORY || screen == Screen.GAME_OVER) {
            gameLoop.stop();
            pressedKeys.clear();
            screen = Screen.LEVEL_SELECT;
            updateCursor();
            repaint();
        }
    }

    private void handleClick(int x, int y) {
        if (screen == Screen.MENU) {
            if (MENU_PLAY_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.LEVEL_SELECT;
                updateCursor();
                repaint();
            } else if (MENU_CUSTOMIZE_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.CUSTOMIZE;
                updateCursor();
                repaint();
            } else if (MENU_SETTINGS_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.SETTINGS;
                updateCursor();
                repaint();
            } else if (MENU_QUIT_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                System.exit(0);
            } else if (MENU_SHOP_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.SHOP;
                updateCursor();
                repaint();
            } else if (MENU_GALLERY_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.GALLERY;
                updateCursor();
                repaint();
            }
        } else if (screen == Screen.SHOP) {
            int item = shopItemAt(x, y);
            if (item >= 0) {
                handleShopClick(item);
            } else if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.CUSTOMIZE) {
            int outfit = outfitCardAt(x, y);
            if (outfit >= 0) {
                handleCustomizeClick(outfit);
            } else if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.SETTINGS) {
            int row = settingsRowAt(x, y);
            if (row >= 0) {
                handleSettingsClick(row);
            } else if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.GALLERY) {
            if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.LEVEL_SELECT) {
            int clicked = levelAt(x, y);
            if (clicked >= 0) {
                selectedLevel = clicked;
                if (levelManager.isUnlocked(clicked)) {
                    SoundEffects.play(SoundEffects.Cue.CLICK);
                    startSelectedLevel();
                } else {
                    repaint();
                }
            } else if (buttonBounds(390, 510).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.MENU;
                updateCursor();
                repaint();
            }
        } else if (screen == Screen.PAUSED) {
            if (buttonBounds(390, 330).contains(x, y)) resumeGame();
            else if (buttonBounds(390, 390).contains(x, y)) startGame();
            else if (buttonBounds(390, 450).contains(x, y)) handleEscape();
        } else if (screen == Screen.VICTORY) {
            if (buttonBounds(390, VICTORY_NEXT_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                if (levelManager.isUnlocked(selectedLevel + 1)) startGame(selectedLevel + 1);
                else startGame(selectedLevel);
            } else if (buttonBounds(390, VICTORY_REPLAY_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                startGame(selectedLevel);
            } else if (buttonBounds(390, VICTORY_SELECT_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.LEVEL_SELECT;
                updateCursor();
                repaint();
            }
        } else if (screen == Screen.GAME_OVER) {
            if (buttonBounds(390, 390).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                startGame(selectedLevel);
            } else if (buttonBounds(390, 450).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        }
    }

    private int levelAt(int x, int y) {
        for (int i = 0; i < MuseumLevel.all().size(); i++) {
            java.awt.Rectangle bounds = levelCardBounds(i);
            if (bounds.contains(x, y)) return i;
        }
        return -1;
    }

    private static java.awt.Rectangle levelCardBounds(int index) {
        int column = index % 2;
        int row = index / 2;
        return new java.awt.Rectangle(115 + column * 375, 145 + row * 112, 350, 96);
    }

    private void moveLevelSelection(int amount) {
        if (screen != Screen.LEVEL_SELECT) return;
        int candidate = Math.max(0, Math.min(MuseumLevel.all().size() - 1, selectedLevel + amount));
        if (levelManager.isUnlocked(candidate)) selectedLevel = candidate;
        repaint();
    }

    private void startSelectedLevel() {
        if (levelManager.isUnlocked(selectedLevel)) startGame(selectedLevel);
    }

    private static java.awt.Rectangle buttonBounds(int x, int y) {
        return new java.awt.Rectangle(x, y, 180, 46);
    }

    private void updateGame(double dt) {
        if (screen != Screen.PLAYING || world == null) return;
        animationFrame++;
        GameWorld.Input input = new GameWorld.Input(
            pressedKeys.contains("up"),
            pressedKeys.contains("down"),
            pressedKeys.contains("left"),
            pressedKeys.contains("right"),
            pressedKeys.contains("sprint"),
            pressedKeys.contains("sneak"),
            pressedKeys.contains("interact"),
            pressedKeys.contains("steal"),
            false,
            false,
            false
        );
        if (input.left != input.right || input.up != input.down) {
            double dx = (input.right ? 1 : 0) - (input.left ? 1 : 0);
            double dy = (input.down ? 1 : 0) - (input.up ? 1 : 0);
            playerDirectionRow = getPlayerDirectionRow(dx, dy);
        }
        world.update(dt, input);
        if (world.isWon()) {
            lastResult = levelManager.recordCompletion(world);
            screen = Screen.VICTORY;
            gameLoop.stop();
            pressedKeys.clear();
            updateCursor();
        } else if (world.isGameOver()) {
            screen = Screen.GAME_OVER;
            gameLoop.stop();
            pressedKeys.clear();
            updateCursor();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        double offsetX = (getWidth() - WIDTH * scale) / 2;
        double offsetY = (getHeight() - HEIGHT * scale) / 2;
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (screen == Screen.MENU) drawMenu(g);
        else if (screen == Screen.LEVEL_SELECT) drawLevelSelect(g);
        else if (screen == Screen.SHOP) drawShop(g);
        else if (screen == Screen.CUSTOMIZE) drawCustomize(g);
        else if (screen == Screen.SETTINGS) drawSettings(g);
        else if (screen == Screen.GALLERY) drawGallery(g);
        else {
            if (world != null) drawGame(g, world);
            if (screen == Screen.PAUSED) drawOverlay(g, "HEIST PAUSED", "The guards are still on their rounds.", "CONTINUE", "RESTART HEIST", "LEVEL SELECT");
            else if (screen == Screen.VICTORY) drawVictory(g);
            else if (screen == Screen.GAME_OVER) drawOverlay(g, "HEIST BUSTED", world == null ? "" : world.getEndReason(), "TRY AGAIN", "", "LEVEL SELECT");
        }
        g.dispose();
    }

    private void drawGame(Graphics2D g, GameWorld game) {
        MuseumMap map = game.getMap();
        map.draw(g, game.getPlayerCenterX(), game.getPlayerCenterY());
        drawGuardVision(g, game.getGuards());
        drawCameraVision(g, game.getCameras());
        drawNoiseIndicators(g, game.getNoiseSystem().getEvents());
        for (MuseumMap.Painting painting : map.getPaintings()) {
            drawPainting(g, painting, painting == game.getActivePainting());
        }
        for (MuseumMap.HideSpot spot : map.getHideSpots()) {
            drawHideSpot(g, spot, game.isNear(spot), game.getHidingIn() == spot);
        }
        for (Guard guard : game.getGuards()) drawGuard(g, guard);
        drawPlayer(g, game);
        drawDistractionEffect(g, game);
        if (levelManager.isLightingEnabled()) {
            drawLighting(g, game.getPlayerCenterX(), game.getPlayerCenterY());
        }
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
                java.awt.geom.Point2D.Double endpoint = world.getMap().getVisionEndpoint(
                    guard.centerX(), guard.centerY(), a, radius);
                cone.lineTo(endpoint.x, endpoint.y);
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

    private void drawCameraVision(Graphics2D g, java.util.List<SecurityCamera> cameras) {
        for (SecurityCamera camera : cameras) {
            double angle = camera.getFacingAngle();
            double radius = camera.getRadius();
            Path2D.Double cone = new Path2D.Double();
            cone.moveTo(camera.getX(), camera.getY());
            for (int step = 0; step <= 24; step++) {
                double a = angle - Math.toRadians(34) + Math.toRadians(68) * step / 24.0;
                java.awt.geom.Point2D.Double endpoint = world.getMap().getVisionEndpoint(
                    camera.getX(), camera.getY(), a, radius);
                cone.lineTo(endpoint.x, endpoint.y);
            }
            cone.closePath();
            boolean disabled = world.getMap().areSecuritySystemsDisabled();
            Color tint = disabled ? new Color(90, 206, 132, 22)
                : camera.isDetecting() ? new Color(222, 44, 49, 64)
                : camera.isTracking() ? new Color(238, 181, 79, 45) : new Color(75, 181, 222, 25);
            g.setColor(tint);
            g.fill(cone);
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 110));
            g.draw(cone);
            int x = (int) camera.getX();
            int y = (int) camera.getY();
            g.setColor(new Color(29, 33, 42));
            g.fillRect(x - 7, y - 5, 14, 10);
            g.setColor(camera.isDetecting() ? new Color(250, 69, 64)
                : camera.isTracking() ? new Color(255, 201, 88)
                : disabled ? new Color(108, 204, 129) : new Color(120, 209, 229));
            g.fillOval(x - 3, y - 3, 6, 6);
            if (camera.isTracking()) {
                g.setColor(new Color(32, 34, 39));
                g.fillRect(x - 13, y + 8, 26, 4);
                g.setColor(camera.isDetecting() ? new Color(244, 78, 70) : new Color(247, 198, 93));
                g.fillRect(x - 12, y + 9, (int) (24 * camera.getDetectionProgress()), 2);
            }
        }
    }

    private void drawNoiseIndicators(Graphics2D g, java.util.List<NoiseSystem.NoiseEvent> events) {
        for (NoiseSystem.NoiseEvent event : events) {
            double progress = 1.0 - event.getProgress();
            int radius = 9 + (int) (Math.min(90, event.getRadius() * 0.24) * progress);
            int x = (int) Math.round(event.getX());
            int y = (int) Math.round(event.getY());
            boolean alarm = event.getSource().contains("alarm");
            Color color = alarm ? new Color(255, 88, 75) : new Color(246, 212, 139);
            int alpha = Math.max(12, 125 - (int) (progress * 90));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            if (progress < 0.45) {
                g.fillOval(x - 3, y - 3, 6, 6);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString(event.getSource().toUpperCase(), x + 7, y - 6);
            }
        }
    }

    private void drawPainting(Graphics2D g, MuseumMap.Painting painting, boolean active) {
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
        if (!painting.isStolen()) {
            int pulse = (int) (130 + 95 * Math.sin(animationFrame * 0.12));
            g.setColor(active ? GOLD : new Color(231, 190, 105, pulse));
            g.drawRect(x - 6, y - 6, w + 12, h + 12);
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
        } else if (!painting.isStolen() && painting.isCopied()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 8));
            g.setColor(new Color(119, 232, 160));
            drawCentered(g, "SWAP [F]", x + w / 2, y - 9);
        } else if (!painting.isStolen() && painting.getCopyProgress() > 0) {
            g.setColor(new Color(32, 34, 42));
            g.fillRect(x, y - 10, w, 4);
            g.setColor(new Color(97, 202, 143));
            g.fillRect(x, y - 10, w * painting.getCopyProgress() / 100, 4);
        }
    }

    private void drawDistractionEffect(Graphics2D g, GameWorld game) {
        if (!game.hasDistractionEffect()) return;
        double progress = 1.0 - game.getDistractionEffectProgress();
        int radius = 14 + (int) (progress * 52);
        int x = (int) Math.round(game.getDistractionX());
        int y = (int) Math.round(game.getDistractionY());
        int alpha = Math.max(0, 180 - (int) (progress * 150));
        g.setColor(new Color(246, 213, 143, alpha));
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
        g.setColor(new Color(255, 237, 191, Math.max(0, alpha - 30)));
        g.fillRect(x - 3, y - 3, 6, 6);
    }

    private void drawHideSpot(Graphics2D g, MuseumMap.HideSpot spot, boolean near, boolean occupied) {
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
        g.setColor(occupied ? new Color(119, 232, 160) : near ? GOLD : new Color(95, 83, 74));
        g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(occupied ? new Color(119, 232, 160) : new Color(238, 221, 180));
        g.drawString(occupied ? "HIDDEN" : "HIDE", r.x + 5, r.y + r.height - 4);
    }

    private void drawGuard(Graphics2D g, Guard guard) {
        int x = (int) Math.round(guard.getX());
        int y = (int) Math.round(guard.getY());
        boolean chasing = guard.getState() == Guard.State.CHASE;
        Point facing = guard.getFacing();
        int direction = getGuardDirection(facing.x, facing.y);
        int action = !guard.isMoving() ? 0 : chasing ? 2 : 1;
        int ticksPerFrame = action == 0 ? 12 : action == 2 ? 4 : 7;
        int frameCount = GuardSpriteSheetExtractor.getFrameCount(action, direction);
        int frame = guard.isMoving() || action == 0
            ? (int) (animationFrame / ticksPerFrame % frameCount) : 0;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(x + 2, y + Guard.HEIGHT - 4, Guard.WIDTH - 4, 7);
        g.drawImage(guardSprites[action][direction][frame], x - 4,
            y + Guard.HEIGHT - GUARD_SPRITE_HEIGHT,
            GUARD_SPRITE_WIDTH, GUARD_SPRITE_HEIGHT, null);
        if (guard.getState() == Guard.State.CHASE || guard.getState() == Guard.State.ALERT) {
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(chasing ? new Color(255, 87, 76) : GOLD);
            g.drawString(chasing ? "!" : "?", x + 7, y + Guard.HEIGHT - GUARD_SPRITE_HEIGHT - 3);
        }
    }

    private static int getGuardDirection(double facingX, double facingY) {
        if (Math.abs(facingX) > Math.abs(facingY)) return facingX < 0 ? 2 : 3;
        return facingY < 0 ? 1 : 0;
    }

    private static BufferedImage[][][] loadGuardSprites() {
        try {
            BufferedImage[][][] frames = loadGuardSpriteStrips();
            if (frames != null) return frames;
        } catch (IOException exception) {
            // Fall back to the older generated atlas/sheet pipeline below.
        }

        String[] assetNames = {"idle.png", "walk.png", "run.png"};
        BufferedImage[] atlases = new BufferedImage[assetNames.length];
        try {
            for (int i = 0; i < assetNames.length; i++) {
                String assetPath = "assets/sprites/Guard/" + assetNames[i];
                InputStream resource = GamePanel.class.getResourceAsStream("/" + assetPath);
                if (resource != null) {
                    try (InputStream input = resource) {
                        atlases[i] = ImageIO.read(input);
                    }
                } else {
                    atlases[i] = ImageIO.read(findProjectAsset(assetPath));
                }
                if (atlases[i] == null) throw new IOException("Unsupported guard atlas image: " + assetPath);
            }
            return GuardSpriteSheetExtractor.loadAtlases(atlases);
        } catch (IOException exception) {
            String sheetPath = "assets/sprites/Guard/Pixel Guard Animation Sprite Sheet.png";
            try {
                InputStream resource = GamePanel.class.getResourceAsStream("/" + sheetPath);
                if (resource != null) {
                    try (InputStream input = resource) {
                        BufferedImage sheet = ImageIO.read(input);
                        if (sheet == null) throw new IOException("Unsupported guard sprite sheet image.");
                        return GuardSpriteSheetExtractor.loadFrames(sheet);
                    }
                }
                return GuardSpriteSheetExtractor.loadFrames(findProjectAsset(sheetPath));
            } catch (IOException fallbackException) {
                fallbackException.addSuppressed(exception);
                throw new IllegalStateException("Could not load guard sprites from the generated atlases "
                    + "or the original sprite sheet.", fallbackException);
            }
        }
    }

    private static BufferedImage[][][] loadGuardSpriteStrips() throws IOException {
        String[] directionFiles = {
            "Guard Front Walk.png",
            "Guard Back Walk.png",
            "Guard Left Walk.png",
            "Guard Right Walk.png"
        };

        BufferedImage idleStrip = readGuardSpriteStrip("Guard Idle.png");
        BufferedImage[] walkStrips = new BufferedImage[directionFiles.length];
        for (int i = 0; i < directionFiles.length; i++) {
            walkStrips[i] = readGuardSpriteStrip(directionFiles[i]);
        }

        BufferedImage[][][] frames = new BufferedImage[3][4][4];
        for (int direction = 0; direction < 4; direction++) {
            frames[0][direction] = splitGuardSpriteStrip(idleStrip, 4);
            frames[1][direction] = splitGuardSpriteStrip(walkStrips[direction], 4);
            frames[2][direction] = frames[1][direction].clone();
        }
        return frames;
    }

    private static BufferedImage[] splitGuardSpriteStrip(BufferedImage sheet, int frameCount) throws IOException {
        if (sheet == null) {
            throw new IOException("Guard sprite strip is missing.");
        }
        if (sheet.getWidth() <= 0 || sheet.getHeight() <= 0) {
            throw new IOException("Guard sprite strip dimensions are invalid: "
                + sheet.getWidth() + "x" + sheet.getHeight() + ".");
        }
        int frameWidth = Math.max(1, sheet.getWidth() / frameCount);
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int left = index * frameWidth;
            int right = (index + 1) * frameWidth;
            frames[index] = sheet.getSubimage(left, 0, right - left, sheet.getHeight());
        }
        return frames;
    }

    private static BufferedImage readGuardSpriteStrip(String fileName) throws IOException {
        String assetPath = "assets/sprites/Guard/" + fileName;
        InputStream resource = GamePanel.class.getResourceAsStream("/" + assetPath);
        if (resource != null) {
            try (InputStream input = resource) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) throw new IOException("Unsupported guard sprite image: " + assetPath);
                return image;
            }
        }

        File spriteFile = findProjectAsset(assetPath);
        BufferedImage image = ImageIO.read(spriteFile);
        if (image == null) {
            throw new IOException("Unsupported guard sprite image: " + spriteFile.getAbsolutePath());
        }
        return image;
    }

    private void drawPlayer(Graphics2D g, GameWorld game) {
        int x = (int) Math.round(game.getPlayerCenterX() - SPRITE_WIDTH / 2.0);
        int y = (int) Math.round(game.getPlayerBottomY() - SPRITE_HEIGHT);
        if (game.isHidden()) {
            g.setColor(new Color(10, 12, 16, 120));
            g.fillOval(x + 5, y + SPRITE_HEIGHT - 9, SPRITE_WIDTH - 10, 10);
            return;
        }
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(x + 8, y + SPRITE_HEIGHT - 8, SPRITE_WIDTH - 16, 7);
        BufferedImage frame = getTintedPlayerFrame(getPlayerSpriteFrame(game), levelManager.getSelectedOutfit());
        if (game.isSneaking()) {
            Composite previous = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g.drawImage(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, null);
            g.setComposite(previous);
        } else {
            g.drawImage(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, null);
        }
    }

    private BufferedImage getPlayerSpriteFrame(GameWorld game) {
        int action;
        int ticksPerFrame;
        if (game.isSprinting()) {
            action = RUN_ACTION;
            ticksPerFrame = 4;
        } else if (game.isSneaking()) {
            action = SNEAK_ACTION;
            ticksPerFrame = 8;
        } else if (game.isMoving()) {
            action = WALK_ACTION;
            ticksPerFrame = 6;
        } else {
            action = IDLE_ACTION;
            ticksPerFrame = 8;
        }

        int frame = (int) (animationFrame / ticksPerFrame % SPRITE_STRIP_FRAME_COUNT);
        return playerSpriteFrames[ACTION_ROW_START[action] + playerDirectionRow][frame];
    }

    private static int getPlayerDirectionRow(double facingX, double facingY) {
        if (Math.abs(facingX) > Math.abs(facingY)) return facingX < 0 ? 2 : 3;
        return facingY < 0 ? 1 : 0;
    }

    private static BufferedImage[][] loadPlayerSpriteFrames() {
        try {
            BufferedImage[] idleFrames = splitSpriteStrip(readPlayerSpriteSheet("Idle.png"));
            BufferedImage[] frontWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Front Walk.png"));
            BufferedImage[] backWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Back.png"));
            BufferedImage[] leftWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Left Walk.png", "Left walk.png"));
            BufferedImage[] rightWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Right Walk.png"));

            BufferedImage[][] frames = new BufferedImage[SPRITE_ROWS][SPRITE_COLUMNS];
            BufferedImage[][] directionalWalkFrames = {
                buildAnimationFrames(frontWalkFrames),
                buildAnimationFrames(backWalkFrames),
                buildAnimationFrames(leftWalkFrames),
                buildAnimationFrames(rightWalkFrames)
            };

            for (int direction = 0; direction < 4; direction++) {
                frames[ACTION_ROW_START[IDLE_ACTION] + direction] = buildAnimationFrames(idleFrames);
            }

            for (int action = WALK_ACTION; action < ACTION_ROW_START.length; action++) {
                for (int direction = 0; direction < 4; direction++) {
                    frames[ACTION_ROW_START[action] + direction] = directionalWalkFrames[direction];
                }
            }
            return frames;
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Could not load the thief sprites. Make sure the files in assets/sprites/thief are present "
                    + "(Front Walk.png, Back.png, Left Walk.png, Right Walk.png, Idle.png).",
                exception);
        }
    }

    private static BufferedImage[] splitSpriteStrip(BufferedImage sheet) throws IOException {
        int frameCount = SPRITE_STRIP_FRAME_COUNT;
        if (sheet.getWidth() < frameCount || sheet.getHeight() <= 0) {
            throw new IOException("Unexpected thief sprite strip dimensions: "
                + sheet.getWidth() + "x" + sheet.getHeight() + ".");
        }
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int left = index * sheet.getWidth() / frameCount;
            int right = (index + 1) * sheet.getWidth() / frameCount;
            frames[index] = sheet.getSubimage(left, 0, right - left, sheet.getHeight());
        }
        return frames;
    }

    private static BufferedImage[] buildAnimationFrames(BufferedImage[] frames) {
        BufferedImage[] animationFrames = new BufferedImage[SPRITE_COLUMNS];
        int sourceCount = Math.max(1, frames.length);
        for (int index = 0; index < SPRITE_COLUMNS; index++) {
            animationFrames[index] = frames[Math.min(index, sourceCount - 1)];
        }
        return animationFrames;
    }

    private static BufferedImage readPlayerSpriteSheet(String... candidateNames) throws IOException {
        IOException lastError = null;
        for (String fileName : candidateNames) {
            try {
                return readPlayerSpriteSheetSingle(fileName);
            } catch (IOException exception) {
                lastError = exception;
            }
        }
        throw lastError != null ? lastError : new IOException("No sprite sheet candidates provided.");
    }

    private static BufferedImage readPlayerSpriteSheetSingle(String fileName) throws IOException {
        String assetPath = "assets/sprites/thief/" + fileName;
        File spriteFile = findProjectAssetFrom(new File("").getAbsoluteFile().toPath(), assetPath);
        if (spriteFile == null) {
            try {
                Path classLocation = Paths.get(GamePanel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
                if (!java.nio.file.Files.isDirectory(classLocation)) {
                    classLocation = classLocation.getParent();
                }
                spriteFile = findProjectAssetFrom(classLocation, assetPath);
            } catch (URISyntaxException exception) {
                throw new IOException("Could not locate project sprites from the game classes.", exception);
            }
        }

        BufferedImage image;
        if (spriteFile != null) {
            image = ImageIO.read(spriteFile);
            if (image == null) {
                throw new IOException("Unsupported thief sprite sheet image: " + spriteFile.getAbsolutePath());
            }
        } else {
            String resourcePath = "/" + assetPath;
            try (InputStream resource = GamePanel.class.getResourceAsStream(resourcePath)) {
                if (resource == null) throw new IOException("Game sprite not found: " + assetPath);
                image = ImageIO.read(resource);
                if (image == null) throw new IOException("Unsupported thief sprite sheet image: " + resourcePath);
            }
        }

        BufferedImage argbImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argbImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return argbImage;
    }

    private static File findProjectAsset(String relativePath) throws IOException {
        File asset = findProjectAssetFrom(new File("").getAbsoluteFile().toPath(), relativePath);
        if (asset != null) return asset;

        try {
            Path classLocation = Paths.get(GamePanel.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            if (!java.nio.file.Files.isDirectory(classLocation)) {
                classLocation = classLocation.getParent();
            }
            asset = findProjectAssetFrom(classLocation, relativePath);
            if (asset != null) return asset;
        } catch (URISyntaxException exception) {
            throw new IOException("Could not locate project assets from the game classes.", exception);
        }

        throw new IOException("Game asset not found: " + relativePath
            + " (searched the working directory and game class ancestors).");
    }

    private static File findProjectAssetFrom(Path directory, String relativePath) {
        for (Path current = directory; current != null; current = current.getParent()) {
            File candidate = current.resolve(relativePath).toFile();
            if (candidate.isFile()) return candidate;
        }
        return null;
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
        String objective = game.getStolenCount() == game.getTargetCount()
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
        g.drawString("ALERT / SUSPICION " + game.getSuspicion() + "%", 515, 47);
        g.setColor(new Color(225, 219, 229));
        g.drawString("ART " + game.getStolenCount() + "/" + game.getTargetCount(), 674, 27);
        String alert = game.getAlertStatus();
        g.setColor(alert.equals("CALM") ? new Color(133, 216, 157)
            : alert.equals("CHASE") ? new Color(255, 93, 84) : GOLD);
        g.drawString("SECURITY: " + alert, 674, 47);
        g.setColor(game.getNoiseSystem().getEvents().isEmpty()
            ? new Color(133, 216, 157) : new Color(255, 204, 109));
        g.drawString("SOUNDS " + game.getNoiseSystem().getEvents().size(), 835, 27);
        double distCd = game.getDistractionCooldown();
        g.setColor(distCd <= 0 ? new Color(133, 216, 157) : new Color(202, 193, 205));
        g.drawString(distCd <= 0 ? "Q: READY" : "Q: " + (int) Math.ceil(distCd) + "s", 835, 47);
        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g.setColor(new Color(157, 153, 164));
        g.drawString("WASD MOVE  SHIFT SPRINT  CTRL SNEAK  E INTERACT  F SWAP  Q DISTRACT  P PAUSE  M "
            + (SoundEffects.isMuted() ? "UNMUTE" : "MUTE"), 18, 54);
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
        if (uiAssets.menuBg != null) {
            g.drawImage(uiAssets.menuBg, 0, 0, WIDTH, HEIGHT, null);
            g.setColor(new Color(10, 12, 18, 78));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(new Color(177, 133, 78, 165));
            g.drawRect(22, 22, WIDTH - 44, HEIGHT - 44);
        } else {
            drawBackdrop(g);
            drawMuseumEmblem(g);
        }
        drawImageButton(g, uiAssets.btnShop, MENU_SHOP_BOUNDS, "SHOP", true);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "CREDITS: " + levelManager.getTotalRewards(), MENU_SHOP_BOUNDS.x + MENU_SHOP_BOUNDS.width / 2, MENU_SHOP_BOUNDS.y + MENU_SHOP_BOUNDS.height + 15);
        drawBadgeButton(g, uiAssets.frameBadge, MENU_GALLERY_BOUNDS, "GALLERY");

        g.setFont(new Font("Monospaced", Font.BOLD, 44));
        g.setColor(new Color(12, 10, 16, 190));
        drawCentered(g, "PAINTHEIST", WIDTH / 2 + 2, 95);
        g.setColor(new Color(255, 219, 142));
        drawCentered(g, "PAINTHEIST", WIDTH / 2, 92);

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(12, 10, 16, 190));
        drawCentered(g, "THE QUIETEST HEIST LEAVES THE BEST STORY", WIDTH / 2 + 1, 121);
        g.setColor(new Color(232, 224, 232));
        drawCentered(g, "THE QUIETEST HEIST LEAVES THE BEST STORY", WIDTH / 2, 120);

        drawImageButton(g, uiAssets.btnPlay, MENU_PLAY_BOUNDS, "PLAY", true);
        drawImageButton(g, uiAssets.btnCustomize, MENU_CUSTOMIZE_BOUNDS, "CUSTOMIZE", false);
        drawImageButton(g, uiAssets.btnSettings, MENU_SETTINGS_BOUNDS, "SETTINGS", false);
        drawImageButton(g, uiAssets.btnQuit, MENU_QUIT_BOUNDS, "QUIT", false);

        g.setColor(new Color(10, 12, 18, 175));
        g.fillRoundRect(WIDTH / 2 - 205, 535, 410, 22, 8, 8);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(215, 206, 212));
        drawCentered(g, "F11 TO TOGGLE FULL SCREEN   |   404Tulog Productions", WIDTH / 2, 550);
    }

    private void drawLevelSelect(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 219, 142));
        drawCentered(g, "SELECT YOUR MUSEUM", WIDTH / 2, 96);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(190, 183, 197));
        drawCentered(g, "Complete each heist to unlock the next exhibition.   CREDITS: "
            + levelManager.getTotalRewards(), WIDTH / 2, 120);
        for (int i = 0; i < MuseumLevel.all().size(); i++) {
            MuseumLevel level = MuseumLevel.get(i);
            java.awt.Rectangle card = levelCardBounds(i);
            boolean unlocked = levelManager.isUnlocked(i);
            boolean selected = i == selectedLevel;
            g.setColor(selected ? new Color(65, 53, 49) : new Color(35, 34, 41));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(selected ? GOLD : unlocked ? new Color(127, 105, 78) : new Color(64, 59, 66));
            g.drawRect(card.x, card.y, card.width, card.height);
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(unlocked ? new Color(255, 226, 167) : new Color(134, 129, 141));
            g.drawString((i + 1) + ". " + level.getName(), card.x + 14, card.y + 23);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(unlocked ? new Color(236, 196, 124) : new Color(121, 116, 128));
            g.drawString(unlocked ? level.getDifficulty() : "LOCKED", card.x + 15, card.y + 43);
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(205, 198, 208));
            g.drawString(level.getPaintingCount() + " TARGETS   " + level.getGuards().size() + " GUARDS",
                card.x + 120, card.y + 43);
            g.setColor(new Color(158, 152, 165));
            String challenge = level.getChallenge();
            if (challenge.length() > 49) challenge = challenge.substring(0, 46) + "...";
            g.drawString(challenge, card.x + 15, card.y + 68);
            int bestScore = levelManager.getBestScore(i);
            int bestTime = levelManager.getBestTime(i);
            String best = bestScore == 0 ? "BEST --"
                : "BEST " + bestScore + "   " + bestTime / 60 + ":" + String.format("%02d", bestTime % 60);
            g.setColor(new Color(227, 211, 170));
            g.drawString(best, card.x + 15, card.y + 86);
        }
        drawButton(g, "BACK", 390, 510, false);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(165, 158, 172));
        drawCentered(g, "ARROWS / WASD SELECT   ENTER TO PLAY   ESC TO BACK", WIDTH / 2, 575);
    }

    private Rectangle shopItemBounds(int index) {
        int col = index / 3;
        int row = index % 3;
        return new Rectangle(92 + col * 404, 148 + row * 102, 372, 92);
    }

    private int shopItemAt(int x, int y) {
        for (int i = 0; i < 6; i++) {
            if (shopItemBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleShopClick(int item) {
        SoundEffects.play(SoundEffects.Cue.CLICK);
        if (item == 0) {
            if (levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY)) {
                uiToast = "QUICK FORGER IS ALREADY OWNED.";
            } else if (levelManager.buyUpgrade(LevelManager.UPGRADE_FAST_COPY, 20)) {
                uiToast = "UNLOCKED QUICK FORGER (+30% COPY SPEED)!";
            } else {
                uiToast = "NEED 20 CREDITS FOR QUICK FORGER.";
            }
        } else if (item == 1) {
            if (levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_THROW)) {
                uiToast = "LONG PITCH IS ALREADY OWNED.";
            } else if (levelManager.buyUpgrade(LevelManager.UPGRADE_LONG_THROW, 15)) {
                uiToast = "UNLOCKED LONG PITCH (+35% THROW RANGE)!";
            } else {
                uiToast = "NEED 15 CREDITS FOR LONG PITCH.";
            }
        } else if (item == 2) {
            if (levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_SWITCH)) {
                uiToast = "CIRCUIT JAMMER IS ALREADY OWNED.";
            } else if (levelManager.buyUpgrade(LevelManager.UPGRADE_LONG_SWITCH, 25)) {
                uiToast = "UNLOCKED CIRCUIT JAMMER (+5s SWITCH TIME)!";
            } else {
                uiToast = "NEED 25 CREDITS FOR CIRCUIT JAMMER.";
            }
        } else {
            int outfitIndex = item - 2;
            boolean wasUnlocked = levelManager.isOutfitUnlocked(outfitIndex);
            if (levelManager.selectOrBuyOutfit(outfitIndex)) {
                uiToast = (wasUnlocked ? "EQUIPPED " : "PURCHASED & EQUIPPED ")
                    + LevelManager.OUTFIT_NAMES[outfitIndex].toUpperCase() + "!";
            } else {
                uiToast = "NEED " + LevelManager.OUTFIT_COSTS[outfitIndex] + " CREDITS FOR "
                    + LevelManager.OUTFIT_NAMES[outfitIndex].toUpperCase() + ".";
            }
        }
        updateCursor();
        repaint();
    }

    private void drawShop(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "BLACK MARKET SHOP", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "AVAILABLE HEIST CREDITS: " + levelManager.getTotalRewards(), WIDTH / 2, 118);

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(180, 174, 188));
        g.drawString("HEIST GADGETS", 92, 139);
        g.drawString("THIEF DISGUISES", 496, 139);

        String[] names = {
            "QUICK FORGER", "LONG PITCH", "CIRCUIT JAMMER",
            LevelManager.OUTFIT_NAMES[1].toUpperCase(),
            LevelManager.OUTFIT_NAMES[2].toUpperCase(),
            LevelManager.OUTFIT_NAMES[3].toUpperCase()
        };
        String[] descs = {
            "Copy paintings 30% faster during swaps",
            "Throw coin distractions 35% farther",
            "Laser switches stay offline 5s longer",
            "Ruby coat tint for bold midnight heists",
            "Jade coat tint for stealth specialists",
            "Gold trim coat for master art thieves"
        };
        int[] costs = {
            20, 15, 25,
            LevelManager.OUTFIT_COSTS[1], LevelManager.OUTFIT_COSTS[2], LevelManager.OUTFIT_COSTS[3]
        };
        boolean[] owned = {
            levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_THROW),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_SWITCH),
            levelManager.isOutfitUnlocked(1),
            levelManager.isOutfitUnlocked(2),
            levelManager.isOutfitUnlocked(3)
        };

        for (int i = 0; i < 6; i++) {
            Rectangle card = shopItemBounds(i);
            boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);
            boolean equipped = i >= 3 && levelManager.getSelectedOutfit() == (i - 2);
            g.setColor(equipped ? new Color(68, 56, 46)
                : hovered ? new Color(54, 48, 60) : new Color(34, 32, 41));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(equipped || hovered ? GOLD : owned[i] ? new Color(116, 168, 128) : new Color(102, 87, 74));
            g.drawRect(card.x, card.y, card.width, card.height);

            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(new Color(255, 227, 168));
            g.drawString(names[i], card.x + 14, card.y + 26);

            String status = equipped ? "EQUIPPED" : owned[i] ? "OWNED" : costs[i] + " CR";
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(equipped || owned[i] ? new Color(137, 226, 161) : new Color(240, 198, 113));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(status, card.x + card.width - 14 - fm.stringWidth(status), card.y + 26);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(195, 190, 202));
            g.drawString(descs[i], card.x + 14, card.y + 52);

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(145, 140, 155));
            String hint = i < 3
                ? (owned[i] ? "ACTIVE ON ALL HEISTS" : "CLICK TO BUY FOR " + costs[i] + " CREDITS")
                : (equipped ? "CURRENTLY WORN" : owned[i] ? "CLICK TO EQUIP" : "CLICK TO UNLOCK FOR " + costs[i] + " CREDITS");
            g.drawString(hint, card.x + 14, card.y + 76);
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 224, 146));
            drawCentered(g, uiToast, WIDTH / 2, 480);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private Rectangle outfitCardBounds(int index) {
        int col = index % 2;
        int row = index / 2;
        return new Rectangle(356 + col * 262, 154 + row * 154, 246, 138);
    }

    private int outfitCardAt(int x, int y) {
        for (int i = 0; i < LevelManager.OUTFIT_NAMES.length; i++) {
            if (outfitCardBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleCustomizeClick(int outfit) {
        SoundEffects.play(SoundEffects.Cue.CLICK);
        boolean wasUnlocked = levelManager.isOutfitUnlocked(outfit);
        if (levelManager.selectOrBuyOutfit(outfit)) {
            uiToast = (wasUnlocked ? "EQUIPPED " : "UNLOCKED & EQUIPPED ")
                + LevelManager.OUTFIT_NAMES[outfit].toUpperCase() + "!";
        } else {
            uiToast = "LOCKED: NEED " + LevelManager.OUTFIT_COSTS[outfit] + " CREDITS.";
        }
        updateCursor();
        repaint();
    }

    private void drawCustomize(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "CUSTOMIZE THIEF", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "Choose your disguise.   CREDITS: " + levelManager.getTotalRewards(), WIDTH / 2, 118);

        int selected = levelManager.getSelectedOutfit();
        g.setColor(new Color(32, 30, 39));
        g.fillRect(92, 154, 240, 292);
        g.setColor(GOLD);
        g.drawRect(92, 154, 240, 292);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "LIVE PREVIEW", 212, 180);

        BufferedImage baseFrame = (playerSpriteFrames != null && playerSpriteFrames.length > 0 && playerSpriteFrames[0].length > 0)
            ? playerSpriteFrames[0][0] : null;
        if (baseFrame != null) {
            BufferedImage preview = getTintedPlayerFrame(baseFrame, selected);
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(preview, 152, 200, 120, 156, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(255, 227, 168));
        drawCentered(g, LevelManager.OUTFIT_NAMES[selected].toUpperCase(), 212, 392);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(180, 174, 188));
        drawCentered(g, "ACTIVE DISGUISE", 212, 416);

        Color[] swatches = {
            new Color(74, 104, 154),
            new Color(184, 62, 68),
            new Color(62, 152, 104),
            new Color(214, 170, 74)
        };
        for (int i = 0; i < LevelManager.OUTFIT_NAMES.length; i++) {
            Rectangle card = outfitCardBounds(i);
            boolean unlocked = levelManager.isOutfitUnlocked(i);
            boolean equipped = selected == i;
            boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);
            g.setColor(equipped ? new Color(65, 53, 49) : hovered ? new Color(52, 46, 58) : new Color(35, 34, 41));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(equipped || hovered ? GOLD : unlocked ? new Color(127, 105, 78) : new Color(64, 59, 66));
            g.drawRect(card.x, card.y, card.width, card.height);

            g.setColor(swatches[i]);
            g.fillRect(card.x + 16, card.y + 18, 28, 28);
            g.setColor(new Color(240, 224, 186));
            g.drawRect(card.x + 16, card.y + 18, 28, 28);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(unlocked ? new Color(255, 226, 167) : new Color(148, 142, 154));
            g.drawString(LevelManager.OUTFIT_NAMES[i], card.x + 56, card.y + 37);

            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(equipped ? new Color(137, 226, 161) : unlocked ? new Color(224, 198, 138) : new Color(216, 146, 110));
            String tag = equipped ? "EQUIPPED"
                : unlocked ? "UNLOCKED - CLICK TO EQUIP"
                : "LOCKED - " + LevelManager.OUTFIT_COSTS[i] + " CR";
            g.drawString(tag, card.x + 16, card.y + 82);

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(165, 160, 174));
            g.drawString(i == 0 ? "Signature dark midnight cloak." : "Also available in the Shop.", card.x + 16, card.y + 112);
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 224, 146));
            drawCentered(g, uiToast, WIDTH / 2, 480);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private Rectangle settingsRowBounds(int row) {
        return new Rectangle(210, 152 + row * 74, 540, 58);
    }

    private int settingsRowAt(int x, int y) {
        for (int i = 0; i < 3; i++) {
            if (settingsRowBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleSettingsClick(int row) {
        if (row == 0) {
            SoundEffects.toggleMute();
            levelManager.saveSettings();
            if (!SoundEffects.isMuted()) SoundEffects.play(SoundEffects.Cue.CLICK);
            uiToast = SoundEffects.isMuted() ? "SOUND EFFECTS MUTED." : "SOUND EFFECTS ENABLED.";
        } else if (row == 1) {
            SoundEffects.play(SoundEffects.Cue.CLICK);
            Main.toggleFullScreen();
            uiToast = Main.isFullScreen() ? "SWITCHED TO FULL SCREEN." : "SWITCHED TO WINDOWED MODE.";
        } else if (row == 2) {
            SoundEffects.play(SoundEffects.Cue.CLICK);
            levelManager.setLightingEnabled(!levelManager.isLightingEnabled());
            uiToast = levelManager.isLightingEnabled() ? "GALLERY VIGNETTE ENABLED." : "HIGH VISIBILITY LIGHTING ENABLED.";
        }
        updateCursor();
        repaint();
    }

    private void drawSettings(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "SETTINGS", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "Click any option below to toggle.", WIDTH / 2, 118);

        String[] labels = {"SOUND EFFECTS", "DISPLAY MODE", "GALLERY LIGHTING"};
        String[] values = {
            SoundEffects.isMuted() ? "MUTED" : "ON",
            Main.isFullScreen() ? "FULL SCREEN" : "WINDOWED",
            levelManager.isLightingEnabled() ? "DRAMATIC VIGNETTE" : "HIGH VISIBILITY"
        };
        for (int i = 0; i < 3; i++) {
            Rectangle row = settingsRowBounds(i);
            boolean hovered = hoverX >= 0 && hoverY >= 0 && row.contains(hoverX, hoverY);
            g.setColor(hovered ? new Color(56, 49, 62) : new Color(35, 34, 41));
            g.fillRect(row.x, row.y, row.width, row.height);
            g.setColor(hovered ? GOLD : new Color(127, 105, 78));
            g.drawRect(row.x, row.y, row.width, row.height);

            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(new Color(255, 227, 168));
            g.drawString(labels[i], row.x + 22, row.y + 35);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(137, 226, 161));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(values[i], row.x + row.width - 22 - fm.stringWidth(values[i]), row.y + 35);
        }

        g.setColor(new Color(30, 29, 36));
        g.fillRect(150, 384, 660, 82);
        g.setColor(new Color(92, 79, 64));
        g.drawRect(150, 384, 660, 82);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "CONTROLS", WIDTH / 2, 408);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(205, 198, 208));
        drawCentered(g, "WASD / ARROWS Move   |   SHIFT Sneak   |   R Sprint   |   E Swap / Hide / Switch", WIDTH / 2, 432);
        drawCentered(g, "Q Throw Distraction   |   F Secret Passage   |   P Pause   |   M Mute   |   F11 Fullscreen", WIDTH / 2, 452);

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 224, 146));
            drawCentered(g, uiToast, WIDTH / 2, 488);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private void drawGallery(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "STOLEN MASTERPIECES GALLERY", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "Complete each museum heist to add its works to your private vault.", WIDTH / 2, 118);

        for (int i = 0; i < MuseumLevel.all().size(); i++) {
            MuseumLevel level = MuseumLevel.get(i);
            int y = 142 + i * 68;
            boolean unlocked = levelManager.isUnlocked(i);
            int bestScore = levelManager.getBestScore(i);
            boolean collected = bestScore > 0;
            g.setColor(collected ? new Color(46, 43, 44) : new Color(32, 31, 38));
            g.fillRect(84, y, 792, 58);
            g.setColor(collected ? GOLD : unlocked ? new Color(115, 96, 74) : new Color(64, 59, 66));
            g.drawRect(84, y, 792, 58);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(collected ? new Color(255, 227, 168) : unlocked ? new Color(210, 202, 214) : new Color(134, 129, 141));
            g.drawString((i + 1) + ". " + level.getName(), 100, y + 23);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(collected ? new Color(224, 210, 178) : new Color(148, 143, 156));
            String paintings = String.join("  |  ", level.getPaintingNames());
            if (paintings.length() > 74) paintings = paintings.substring(0, 71) + "...";
            g.drawString(paintings, 100, y + 44);

            int bestTime = levelManager.getBestTime(i);
            String badge = collected
                ? "COLLECTED  |  BEST " + bestScore + "  (" + bestTime / 60 + ":" + String.format("%02d", bestTime % 60) + ")"
                : unlocked ? "UNCLAIMED" : "LOCKED";
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(collected ? new Color(137, 226, 161) : unlocked ? new Color(231, 190, 105) : new Color(134, 129, 141));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(badge, 860 - fm.stringWidth(badge), y + 34);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private void drawVictory(Graphics2D g) {
        g.setColor(new Color(7, 9, 14, 215));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(35, 31, 40));
        g.fillRect(245, 100, 470, 440);
        g.setColor(GOLD);
        g.fillRect(245, 100, 470, 5);
        g.drawRect(245, 100, 470, 440);
        g.setFont(new Font("Monospaced", Font.BOLD, 27));
        g.setColor(new Color(137, 226, 161));
        drawCentered(g, "HEIST COMPLETE", WIDTH / 2, 153);
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, world.getLevel().getName(), WIDTH / 2, 184);
        int stars = lastResult == null ? 0 : lastResult.getStars();
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(GOLD);
        drawCentered(g, starString(stars), WIDTH / 2, 229);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(232, 223, 231));
        drawCentered(g, "SCORE  " + (lastResult == null ? 0 : lastResult.getScore()), WIDTH / 2, 263);
        drawCentered(g, "REWARD  +" + (lastResult == null ? 0 : lastResult.getReward()) + " HEIST CREDITS",
            WIDTH / 2, 285);
        drawCentered(g, "TIME LEFT  " + (lastResult == null ? 0 : lastResult.getTimeRemaining()) + "s"
            + "     ALERTS  " + (lastResult == null ? 0 : lastResult.getDetectionIncidents()), WIDTH / 2, 307);
        String nextLabel = levelManager.isUnlocked(selectedLevel + 1) ? "NEXT MUSEUM" : "REPLAY HEIST";
        drawButton(g, nextLabel, 390, VICTORY_NEXT_Y, true);
        drawButton(g, "REPLAY HEIST", 390, VICTORY_REPLAY_Y, false);
        drawButton(g, "LEVEL SELECT", 390, VICTORY_SELECT_Y, false);
    }

    private static String starString(int stars) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 3; i++) text.append(i < stars ? "* " : "- ");
        return text.toString().trim();
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
        int y = 132;
        g.setColor(new Color(78, 48, 37));
        g.fillRect(x, y, 150, 88);
        g.setColor(new Color(211, 165, 91));
        g.fillRect(x + 6, y + 6, 138, 76);
        g.setColor(new Color(38, 63, 81));
        g.fillRect(x + 13, y + 13, 124, 62);
        g.setColor(new Color(240, 198, 113));
        g.fillRect(x + 26, y + 21, 24, 22);
        g.setColor(new Color(38, 54, 65));
        g.fillRect(x + 16, y + 48, 53, 21);
        g.setColor(new Color(62, 94, 68));
        g.fillRect(x + 66, y + 40, 64, 29);
        g.setColor(new Color(224, 164, 91));
        g.fillRect(x + 83, y + 32, 14, 18);
        g.setColor(new Color(248, 220, 151));
        g.drawRect(x - 2, y - 2, 154, 92);
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

    private BufferedImage getTintedPlayerFrame(BufferedImage src, int outfitIndex) {
        if (src == null || outfitIndex <= 0 || outfitIndex >= LevelManager.OUTFIT_NAMES.length) {
            return src;
        }
        BufferedImage[] variants = tintedPlayerCache.computeIfAbsent(
            src, k -> new BufferedImage[LevelManager.OUTFIT_NAMES.length]);
        if (variants[outfitIndex] != null) {
            return variants[outfitIndex];
        }
        int[] targetRgb = switch (outfitIndex) {
            case 1 -> new int[]{208, 66, 72};   // Crimson Phantom
            case 2 -> new int[]{64, 178, 114};  // Emerald Forger
            default -> new int[]{226, 182, 74}; // Gilded Master
        };
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage tinted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int argb = src.getRGB(px, py);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = (argb >>> 16) & 0xFF;
                int gr = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                boolean skinTone = r > gr + 22 && gr > b + 14 && r > 150;
                if (skinTone) {
                    tinted.setRGB(px, py, argb);
                    continue;
                }
                double lum = (0.299 * r + 0.587 * gr + 0.114 * b) / 255.0;
                double scale = Math.max(0.28, 0.35 + lum * 0.95);
                int nr = Math.min(255, (int) Math.round(r * 0.25 + targetRgb[0] * scale * 0.75));
                int ng = Math.min(255, (int) Math.round(gr * 0.25 + targetRgb[1] * scale * 0.75));
                int nb = Math.min(255, (int) Math.round(b * 0.25 + targetRgb[2] * scale * 0.75));
                tinted.setRGB(px, py, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        variants[outfitIndex] = tinted;
        return tinted;
    }

    private void drawImageButton(Graphics2D g, BufferedImage sprite, Rectangle bounds, String fallbackLabel, boolean highlighted) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        if (sprite != null) {
            int dx = hovered ? bounds.x - 2 : bounds.x;
            int dy = hovered ? bounds.y - 2 : bounds.y;
            int dw = hovered ? bounds.width + 4 : bounds.width;
            int dh = hovered ? bounds.height + 4 : bounds.height;
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(sprite, dx, dy, dw, dh, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            if (hovered) {
                g.setColor(new Color(255, 226, 150, 55));
                g.fillRoundRect(dx + 4, dy + 4, dw - 8, dh - 8, 8, 8);
                g.setColor(new Color(255, 224, 146, 190));
                g.drawRoundRect(dx + 2, dy + 2, dw - 4, dh - 4, 8, 8);
            }
            return;
        }
        g.setColor(highlighted
            ? (hovered ? new Color(145, 96, 54) : new Color(119, 79, 45))
            : (hovered ? new Color(75, 65, 78) : new Color(55, 48, 57)));
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(highlighted || hovered ? new Color(231, 190, 105) : new Color(127, 105, 78));
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(255, 237, 199));
        drawCentered(g, fallbackLabel, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 + 5);
    }

    private void drawBadgeButton(Graphics2D g, BufferedImage frameSprite, Rectangle bounds, String label) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        if (frameSprite != null) {
            int dx = hovered ? bounds.x - 2 : bounds.x;
            int dy = hovered ? bounds.y - 1 : bounds.y;
            int dw = hovered ? bounds.width + 4 : bounds.width;
            int dh = hovered ? bounds.height + 2 : bounds.height;
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(frameSprite, dx, dy, dw, dh, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            if (hovered) {
                g.setColor(new Color(255, 224, 146, 45));
                g.fillRoundRect(dx + 5, dy + 5, dw - 10, dh - 10, 6, 6);
            }
        } else {
            g.setColor(hovered ? new Color(75, 65, 78) : new Color(55, 48, 57));
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.setColor(hovered ? GOLD : new Color(127, 105, 78));
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(hovered ? new Color(255, 244, 212) : new Color(255, 231, 182));
        drawCentered(g, label, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 + 4);
    }

    private void drawButton(Graphics2D g, String label, int x, int y, boolean highlighted) {
        Rectangle bounds = buttonBounds(x, y);
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        BufferedImage frame = highlighted ? uiAssets.framePrimary : uiAssets.frameSecondary;
        if (frame != null) {
            int dx = hovered ? bounds.x - 2 : bounds.x;
            int dy = hovered ? bounds.y - 1 : bounds.y;
            int dw = hovered ? bounds.width + 4 : bounds.width;
            int dh = hovered ? bounds.height + 2 : bounds.height;
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(frame, dx, dy, dw, dh, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            if (hovered) {
                g.setColor(new Color(255, 224, 146, 45));
                g.fillRoundRect(dx + 4, dy + 4, dw - 8, dh - 8, 6, 6);
            }
        } else {
            g.setColor(highlighted
                ? (hovered ? new Color(145, 96, 54) : new Color(119, 79, 45))
                : (hovered ? new Color(75, 65, 78) : new Color(55, 48, 57)));
            g.fillRect(x, y, 180, 46);
            g.setColor(highlighted || hovered ? new Color(231, 190, 105) : new Color(127, 105, 78));
            g.drawRect(x, y, 180, 46);
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(hovered ? new Color(255, 246, 218) : new Color(255, 237, 199));
        drawCentered(g, label, x + 90, y + 28);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
