import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel {

    private static final int WORLD_W = 960;
    private static final int WORLD_H = 600;

    private int playerX = 70;
    private int playerY = 300;
    private final int playerSize = 24;
    private final int playerSpeed = 4;

    private boolean up, down, left, right;
    private boolean copying = false;
    private boolean paintingStolen = false;
    private boolean gameOver = false;
    private boolean won = false;

    private int timeLeft = 120;
    private int copyProgress = 0;
    private int animationTick = 0;

    // Guard settings
    private int guardX = 650;
    private int guardY = 300;
    private final int guardSize = 26;
    private final int guardSpeed = 2;
    private int patrolDirection = 1;
    private boolean guardChasing = false;
    private boolean caught = false;

    private final Rectangle exit = new Rectangle(860, 250, 50, 100);

    private final Rectangle[] paintings = {
        new Rectangle(250, 95, 55, 18),
        new Rectangle(440, 95, 55, 18),
        new Rectangle(630, 95, 55, 18)
    };

    private final ArrayList<Rectangle> obstacles = new ArrayList<>();
    private Rectangle activePainting = null;

    private final Timer gameTimer;
    private final Timer secondTimer;
    private final Random random = new Random(7);

    private final Color ink = new Color(35, 28, 38);
    private final Color floorA = new Color(145, 105, 72);
    private final Color floorB = new Color(132, 93, 65);
    private final Color wall = new Color(49, 39, 45);
    private final Color gold = new Color(235, 190, 91);
    private final Color hud = new Color(28, 24, 36);

    public GamePanel() {
        setPreferredSize(new Dimension(WORLD_W, WORLD_H));
        setBackground(floorA);
        setFocusable(true);

        // Museum walls
        obstacles.add(new Rectangle(0, 0, 960, 40));
        obstacles.add(new Rectangle(0, 40, 30, 520));
        obstacles.add(new Rectangle(930, 40, 30, 520));
        obstacles.add(new Rectangle(0, 560, 960, 40));

        // Display cases
        obstacles.add(new Rectangle(180, 190, 160, 45));
        obstacles.add(new Rectangle(400, 190, 160, 45));
        obstacles.add(new Rectangle(620, 190, 160, 45));
        obstacles.add(new Rectangle(180, 390, 160, 45));
        obstacles.add(new Rectangle(400, 390, 160, 45));
        obstacles.add(new Rectangle(620, 390, 160, 45));

        bindKey("W", "up", () -> up = true);
        bindKey("S", "down", () -> down = true);
        bindKey("A", "left", () -> left = true);
        bindKey("D", "right", () -> right = true);
        bindKey("UP", "upArrow", () -> up = true);
        bindKey("DOWN", "downArrow", () -> down = true);
        bindKey("LEFT", "leftArrow", () -> left = true);
        bindKey("RIGHT", "rightArrow", () -> right = true);

        bindRelease("W", () -> up = false);
        bindRelease("S", () -> down = false);
        bindRelease("A", () -> left = false);
        bindRelease("D", () -> right = false);
        bindRelease("UP", () -> up = false);
        bindRelease("DOWN", () -> down = false);
        bindRelease("LEFT", () -> left = false);
        bindRelease("RIGHT", () -> right = false);

        bindKey("E", "copyPainting", () -> copying = true);
        bindRelease("E", () -> copying = false);
        bindKey("R", "restart", this::restart);

        gameTimer = new Timer(16, e -> updateGame());
        gameTimer.start();

        secondTimer = new Timer(1000, e -> {
            if (!gameOver && !won) {
                timeLeft--;
                if (timeLeft <= 0) {
                    timeLeft = 0;
                    gameOver = true;
                    copying = false;
                }
                repaint();
            }
        });
        secondTimer.start();
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("pressed " + key), name
        );
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void bindRelease(String key, Runnable action) {
        String name = "release" + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("released " + key), name
        );
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void updateGame() {
        if (gameOver || won) {
            repaint();
            return;
        }

        animationTick++;

        int dx = 0;
        int dy = 0;
        if (up) dy -= playerSpeed;
        if (down) dy += playerSpeed;
        if (left) dx -= playerSpeed;
        if (right) dx += playerSpeed;

        // Prevent faster diagonal movement
        if (dx != 0 && dy != 0) {
            dx = (int) Math.round(dx * 0.7071);
            dy = (int) Math.round(dy * 0.7071);
        }

        movePlayer(dx, 0);
        movePlayer(0, dy);

        activePainting = findNearbyPainting();

        // Hold E near any painting to copy it. Progress pauses if interrupted.
        if (activePainting != null && copying && !paintingStolen) {
            copyProgress++;
            if (copyProgress >= 100) {
                copyProgress = 100;
                paintingStolen = true;
                copying = false;
            }
        }

        updateGuard();

        Rectangle player = new Rectangle(playerX, playerY, playerSize, playerSize);
        Rectangle guard = new Rectangle(guardX, guardY, guardSize, guardSize);

        if (player.intersects(guard)) {
            caught = true;
            gameOver = true;
            copying = false;
        }

        if (paintingStolen && player.intersects(exit)) {
            won = true;
            copying = false;
        }

        repaint();
    }

    private void movePlayer(int dx, int dy) {
        if (dx == 0 && dy == 0) return;

        Rectangle next = new Rectangle(
            playerX + dx, playerY + dy, playerSize, playerSize
        );
        for (Rectangle obstacle : obstacles) {
            if (next.intersects(obstacle)) return;
        }
        playerX += dx;
        playerY += dy;
    }

    private void updateGuard() {
        double playerCenterX = playerX + playerSize / 2.0;
        double playerCenterY = playerY + playerSize / 2.0;
        double guardCenterX = guardX + guardSize / 2.0;
        double guardCenterY = guardY + guardSize / 2.0;

        double dx = playerCenterX - guardCenterX;
        double dy = playerCenterY - guardCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        guardChasing = distance < 180;

        if (guardChasing) {
            int moveX = 0;
            int moveY = 0;
            if (Math.abs(dx) > 8) moveX = dx > 0 ? guardSpeed : -guardSpeed;
            if (Math.abs(dy) > 8) moveY = dy > 0 ? guardSpeed : -guardSpeed;
            moveGuard(moveX, 0);
            moveGuard(0, moveY);
        } else {
            int nextX = guardX + patrolDirection * guardSpeed;
            if (nextX < 450 || nextX > 820) {
                patrolDirection *= -1;
            } else {
                moveGuard(patrolDirection * guardSpeed, 0);
            }
        }
    }

    private void moveGuard(int dx, int dy) {
        Rectangle next = new Rectangle(
            guardX + dx, guardY + dy, guardSize, guardSize
        );
        for (Rectangle obstacle : obstacles) {
            if (next.intersects(obstacle)) return;
        }
        guardX += dx;
        guardY += dy;
    }

    private Rectangle findNearbyPainting() {
        Rectangle player = new Rectangle(
            playerX - 12, playerY - 12, playerSize + 24, playerSize + 24
        );
        for (int i = 0; i < paintings.length; i++) {
            if (paintingStolen && i == 0) continue;
            if (player.intersects(paintings[i])) return paintings[i];
        }
        return null;
    }

    private void restart() {
        playerX = 70;
        playerY = 300;
        guardX = 650;
        guardY = 300;
        patrolDirection = 1;
        guardChasing = false;
        caught = false;
        copying = false;
        paintingStolen = false;
        gameOver = false;
        won = false;
        timeLeft = 120;
        copyProgress = 0;
        animationTick = 0;
        up = down = left = right = false;
        activePainting = null;
        repaint();
    }

    private void drawPixelRect(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    private void drawFloor(Graphics2D g) {
        g.setColor(floorA);
        g.fillRect(0, 0, WORLD_W, WORLD_H);

        // Large tiled floor with alternating colors and dark seams
        for (int y = 40; y < 560; y += 40) {
            for (int x = 30; x < 930; x += 40) {
                if (((x / 40) + (y / 40)) % 2 == 0) {
                    g.setColor(floorB);
                    g.fillRect(x, y, 40, 40);
                }
                g.setColor(new Color(91, 62, 51, 110));
                g.drawRect(x, y, 40, 40);
                g.setColor(new Color(178, 137, 93, 70));
                g.drawLine(x + 3, y + 3, x + 12, y + 3);
            }
        }

        // Decorative rug in the central gallery
        g.setColor(new Color(91, 39, 53));
        g.fillRect(350, 250, 260, 100);
        g.setColor(new Color(210, 158, 83));
        g.drawRect(358, 258, 244, 84);
        g.drawRect(365, 265, 230, 70);
        for (int x = 375; x < 595; x += 22) {
            g.fillRect(x, 295, 5, 5);
        }
    }

    private void drawWallsAndCases(Graphics2D g) {
        // Outer walls
        g.setColor(new Color(36, 29, 38));
        g.fillRect(0, 0, 960, 40);
        g.fillRect(0, 40, 30, 520);
        g.fillRect(930, 40, 30, 520);
        g.fillRect(0, 560, 960, 40);

        g.setColor(new Color(93, 65, 62));
        g.fillRect(0, 34, 960, 7);
        g.fillRect(25, 40, 7, 520);
        g.fillRect(928, 40, 7, 520);
        g.fillRect(0, 557, 960, 7);

        // Wall lamps
        for (int x : new int[]{120, 360, 600, 840}) {
            g.setColor(new Color(245, 197, 105, 70));
            g.fillRect(x - 10, 42, 20, 18);
            g.setColor(new Color(255, 220, 133));
            g.fillRect(x - 4, 45, 8, 7);
        }

        // Display cases with pixel shadows, wood edges and glass
        for (Rectangle r : obstacles) {
            if (r.width == 160 && r.height == 45) {
                g.setColor(new Color(40, 29, 34, 100));
                g.fillRect(r.x + 5, r.y + 7, r.width, r.height);
                g.setColor(new Color(65, 39, 37));
                g.fillRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
                g.setColor(new Color(177, 117, 61));
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(new Color(88, 54, 46));
                g.fillRect(r.x + 5, r.y + 5, r.width - 10, r.height - 10);
                g.setColor(new Color(111, 165, 165, 130));
                g.fillRect(r.x + 8, r.y + 7, r.width - 16, r.height - 14);
                g.setColor(new Color(221, 192, 129));
                g.fillRect(r.x + 8, r.y + 6, r.width - 16, 3);
                g.setColor(new Color(245, 218, 151, 120));
                g.drawLine(r.x + 15, r.y + 12, r.x + 45, r.y + 12);
                // Tiny artifact inside the case
                g.setColor(new Color(233, 190, 76));
                g.fillRect(r.x + r.width / 2 - 5, r.y + 16, 10, 10);
                g.setColor(new Color(255, 232, 143));
                g.fillRect(r.x + r.width / 2 - 2, r.y + 18, 4, 4);
            }
        }
    }

    private void drawPainting(Graphics2D g, Rectangle p, int index) {
        boolean removed = paintingStolen && index == 0;
        // Wall shadow and chunky gold frame
        g.setColor(new Color(28, 22, 30, 100));
        g.fillRect(p.x - 7, p.y - 5, p.width + 14, p.height + 12);

        g.setColor(new Color(91, 55, 36));
        g.fillRect(p.x - 5, p.y - 5, p.width + 10, p.height + 10);
        g.setColor(new Color(238, 190, 89));
        g.fillRect(p.x - 3, p.y - 3, p.width + 6, p.height + 6);
        g.setColor(new Color(255, 224, 138));
        g.fillRect(p.x - 3, p.y - 3, p.width + 6, 3);
        g.setColor(new Color(135, 84, 45));
        g.fillRect(p.x - 3, p.y + p.height + 1, p.width + 6, 3);

        if (removed) {
            g.setColor(new Color(75, 60, 59));
            g.fillRect(p.x, p.y, p.width, p.height);
            g.setColor(new Color(45, 36, 40));
            g.fillRect(p.x + 5, p.y + 4, p.width - 10, p.height - 8);
            g.setColor(new Color(200, 95, 80));
            g.fillRect(p.x + 22, p.y + 5, 10, 2);
            return;
        }

        Color sky = index == 0 ? new Color(60, 110, 151)
                  : index == 1 ? new Color(64, 125, 100)
                  : new Color(110, 77, 137);
        g.setColor(sky);
        g.fillRect(p.x, p.y, p.width, p.height);

        // Pixel landscape inside each painting
        g.setColor(new Color(240, 198, 109));
        g.fillRect(p.x + 7 + index * 3, p.y + 3, 8, 8);
        g.setColor(new Color(37, 63, 76));
        g.fillRect(p.x + 2, p.y + 12, 20, 5);
        g.fillRect(p.x + 18, p.y + 9, 16, 8);
        g.setColor(new Color(43, 81, 66));
        g.fillRect(p.x + 30, p.y + 11, 23, 6);
        g.setColor(new Color(226, 160, 90));
        g.fillRect(p.x + 20, p.y + 6, 5, 5);
        g.setColor(new Color(35, 35, 48));
        g.fillRect(p.x + 42, p.y + 3, 8, 6);
    }

    private void drawPlayer(Graphics2D g) {
        int x = playerX;
        int y = playerY;
        int step = ((animationTick / 8) % 2 == 0 && (up || down || left || right)) ? 2 : 0;

        // Shadow
        g.setColor(new Color(26, 21, 31, 100));
        g.fillRect(x + 3, y + 19, 20, 7);

        // Boots
        g.setColor(new Color(43, 32, 42));
        g.fillRect(x + 4, y + 18 + step, 6, 5);
        g.fillRect(x + 15, y + 18 - step, 6, 5);

        // Body / dark outfit
        g.setColor(new Color(38, 43, 65));
        g.fillRect(x + 4, y + 9, 17, 11);
        g.setColor(new Color(58, 75, 113));
        g.fillRect(x + 6, y + 10, 13, 7);
        g.setColor(new Color(228, 175, 83));
        g.fillRect(x + 10, y + 10, 5, 8);

        // Head and face
        g.setColor(new Color(211, 151, 105));
        g.fillRect(x + 7, y + 3, 12, 8);
        g.setColor(new Color(38, 31, 42));
        g.fillRect(x + 6, y + 1, 14, 4);
        g.fillRect(x + 8, y, 10, 3);
        g.setColor(new Color(246, 206, 149));
        g.fillRect(x + 9, y + 5, 7, 3);

        // Mask and eyes
        g.setColor(new Color(37, 31, 43));
        g.fillRect(x + 7, y + 7, 13, 3);
        g.setColor(new Color(255, 229, 173));
        g.fillRect(x + 9, y + 7, 2, 2);
        g.fillRect(x + 16, y + 7, 2, 2);

        // Satchel
        g.setColor(new Color(133, 78, 43));
        g.fillRect(x + 17, y + 12, 5, 6);
        g.setColor(new Color(232, 178, 78));
        g.fillRect(x + 18, y + 13, 3, 2);
    }

    private void drawGuard(Graphics2D g) {
        int x = guardX;
        int y = guardY;
        int step = ((animationTick / 10) % 2 == 0) ? 1 : 0;

        g.setColor(new Color(26, 21, 31, 100));
        g.fillRect(x + 2, y + 21, 23, 7);

        // Boots
        g.setColor(new Color(45, 35, 37));
        g.fillRect(x + 4, y + 21 + step, 7, 5);
        g.fillRect(x + 16, y + 21 - step, 7, 5);

        // Uniform
        g.setColor(guardChasing ? new Color(143, 42, 51) : new Color(47, 83, 69));
        g.fillRect(x + 3, y + 10, 21, 13);
        g.setColor(guardChasing ? new Color(213, 68, 65) : new Color(79, 125, 91));
        g.fillRect(x + 6, y + 11, 15, 8);

        // Belt and badge
        g.setColor(new Color(221, 177, 75));
        g.fillRect(x + 4, y + 18, 19, 3);
        g.fillRect(x + 16, y + 12, 4, 4);

        // Head
        g.setColor(new Color(202, 145, 102));
        g.fillRect(x + 7, y + 3, 13, 9);
        g.setColor(new Color(40, 33, 40));
        g.fillRect(x + 5, y + 1, 17, 5);
        g.fillRect(x + 7, y, 13, 3);
        g.setColor(new Color(255, 221, 160));
        g.fillRect(x + 9, y + 6, 3, 2);
        g.fillRect(x + 16, y + 6, 2, 2);

        if (guardChasing) {
            g.setColor(new Color(255, 224, 116));
            g.fillRect(x + 21, y - 7, 4, 4);
            g.fillRect(x + 22, y - 2, 3, 5);
        }
    }

    private void drawExit(Graphics2D g) {
        // Exit shadow and frame
        g.setColor(new Color(22, 19, 29, 100));
        g.fillRect(exit.x + 5, exit.y + 5, exit.width, exit.height);

        g.setColor(new Color(67, 42, 39));
        g.fillRect(exit.x - 4, exit.y - 4, exit.width + 8, exit.height + 8);
        g.setColor(paintingStolen ? new Color(80, 180, 102) : new Color(148, 65, 62));
        g.fillRect(exit.x, exit.y, exit.width, exit.height);

        g.setColor(new Color(39, 32, 43));
        g.fillRect(exit.x + 7, exit.y + 7, exit.width - 14, exit.height - 14);
        g.setColor(paintingStolen ? new Color(107, 217, 127) : new Color(190, 87, 76));
        g.fillRect(exit.x + 11, exit.y + 11, exit.width - 22, exit.height - 22);

        g.setColor(new Color(245, 203, 104));
        g.fillRect(exit.x + 31, exit.y + 48, 4, 7);

        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString(paintingStolen ? "OPEN" : "LOCKED", exit.x - 1, exit.y - 10);
    }

    private void drawHud(Graphics2D g) {
        // Top panel
        g.setColor(new Color(17, 16, 28, 235));
        g.fillRect(0, 0, WORLD_W, 66);
        g.setColor(new Color(225, 174, 83));
        g.fillRect(0, 63, WORLD_W, 3);

        g.setFont(new Font("Monospaced", Font.BOLD, 21));
        g.setColor(new Color(255, 219, 135));
        g.drawString("PAINTHEIST", 18, 27);

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString("TIME", 20, 48);

        g.setColor(timeLeft <= 20 ? new Color(255, 91, 88) : new Color(135, 226, 166));
        g.drawString(String.format("%03d", timeLeft), 68, 48);

        g.setColor(new Color(220, 216, 223));
        g.drawString(
            paintingStolen ? "OBJECTIVE: ESCAPE TO THE EXIT" : "OBJECTIVE: COPY A PAINTING",
            160, 27
        );

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(196, 190, 204));
        g.drawString("WASD / ARROWS: MOVE", 160, 49);
        g.drawString("HOLD E: COPY", 365, 49);
        g.drawString("R: RESTART", 490, 49);

        // Small status icon
        g.setColor(paintingStolen ? new Color(90, 220, 132) : new Color(225, 174, 83));
        g.fillRect(850, 17, 14, 14);
        g.setColor(Color.WHITE);
        g.drawRect(850, 17, 14, 14);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString(paintingStolen ? "LOOT SECURED" : "NO LOOT", 872, 28);
    }

    private void drawCopyBar(Graphics2D g) {
        if (activePainting == null || paintingStolen || gameOver || won) return;

        int barX = 350;
        int barY = 505;

        g.setColor(new Color(20, 18, 29, 220));
        g.fillRect(barX - 12, barY - 31, 224, 58);
        g.setColor(new Color(236, 190, 96));
        g.drawRect(barX - 12, barY - 31, 224, 58);

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        g.drawString(copying ? "COPYING ARTWORK..." : "HOLD E TO COPY", barX, barY - 12);

        g.setColor(new Color(49, 42, 55));
        g.fillRect(barX, barY, 200, 13);

        g.setColor(new Color(77, 203, 132));
        g.fillRect(barX, barY, copyProgress * 2, 13);

        g.setColor(new Color(255, 225, 151));
        g.drawRect(barX, barY, 200, 13);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(220, 215, 225));
        g.drawString(copyProgress + "%", barX + 170, barY - 12);
    }

    private void drawGameEnd(Graphics2D g) {
        if (!gameOver && !won) return;

        g.setColor(new Color(14, 12, 23, 215));
        g.fillRect(0, 0, getWidth(), getHeight());

        int panelX = 220;
        int panelY = 205;
        int panelW = 520;
        int panelH = 180;

        g.setColor(new Color(45, 34, 49));
        g.fillRect(panelX, panelY, panelW, panelH);
        g.setColor(new Color(230, 181, 91));
        g.fillRect(panelX, panelY, panelW, 5);
        g.drawRect(panelX, panelY, panelW, panelH);

        String message;
        if (won) {
            message = "HEIST COMPLETE!";
        } else if (caught) {
            message = "CAUGHT BY THE GUARD!";
        } else {
            message = "TIME'S UP!";
        }

        g.setFont(new Font("Monospaced", Font.BOLD, 30));
        g.setColor(won ? new Color(106, 235, 145) : new Color(255, 101, 103));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(message, (getWidth() - fm.stringWidth(message)) / 2, 270);

        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(new Color(232, 221, 229));
        String subtitle = won ? "You escaped with the artwork." : "The museum security stopped your heist.";
        fm = g.getFontMetrics();
        g.drawString(subtitle, (getWidth() - fm.stringWidth(subtitle)) / 2, 310);

        g.setColor(new Color(255, 213, 128));
        g.drawString("PRESS R TO PLAY AGAIN", 354, 350);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        drawFloor(g2);
        drawWallsAndCases(g2);

        for (int i = 0; i < paintings.length; i++) {
            drawPainting(g2, paintings[i], i);
        }

        drawExit(g2);
        drawGuard(g2);
        drawPlayer(g2);
        drawHud(g2);
        drawCopyBar(g2);

        if (guardChasing && !gameOver && !won) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 15));
            g2.setColor(new Color(255, 93, 88));
            g2.drawString("!! GUARD ALERT !!", 390, 91);
        }

        drawGameEnd(g2);
        g2.dispose();
    }
}
