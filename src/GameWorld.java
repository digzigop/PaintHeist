import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class GameWorld {
    private static final int PLAYER_WIDTH = 20;
    private static final int PLAYER_HEIGHT = 24;
    private static final double WALK_SPEED = 132;
    private static final double SPRINT_SPEED = 204;
    private static final int MISSION_SECONDS = 210;

    private final MuseumMap map = new MuseumMap();
    private final List<Guard> guards = new ArrayList<>();
    private double playerX = 55;
    private double playerY = 108;
    private double suspicion;
    private double elapsed;
    private double noiseCooldown;
    private double noiseRemaining;
    private double noiseX;
    private double noiseY;
    private double noiseRadius;
    private int timeLeft = MISSION_SECONDS;
    private int copyProgress;
    private MuseumMap.Painting activePainting;
    private MuseumMap.HideSpot nearbyHideSpot;
    private MuseumMap.HideSpot hidingIn;
    private boolean interactPressed;
    private boolean stealPressed;
    private boolean moving;
    private boolean sprinting;
    private boolean gameOver;
    private boolean won;
    private String endReason = "Security found you in the gallery.";
    private String message = "";
    private double messageTimer;

    public GameWorld() {
        guards.add(new Guard("North Gallery", 180, 117, 78,
            new Point(180, 117), new Point(268, 117), new Point(260, 204), new Point(62, 218)));
        guards.add(new Guard("East Wing", 784, 308, 72,
            new Point(784, 308), new Point(852, 308), new Point(854, 427), new Point(684, 427)));
        guards.add(new Guard("Central Hall", 440, 445, 68,
            new Point(440, 445), new Point(558, 445), new Point(558, 493), new Point(368, 493)));
    }

    public void update(double dt, Input input) {
        if (gameOver || won) return;
        dt = Math.min(dt, 0.05);
        elapsed += dt;
        timeLeft = Math.max(0, MISSION_SECONDS - (int) elapsed);
        if (timeLeft == 0) {
            gameOver = true;
            endReason = "The museum locks down before your escape.";
            return;
        }
        noiseCooldown = Math.max(0, noiseCooldown - dt);
        noiseRemaining = Math.max(0, noiseRemaining - dt);
        messageTimer = Math.max(0, messageTimer - dt);
        if (messageTimer == 0) message = "";

        activePainting = findNearbyPainting();
        nearbyHideSpot = findNearbyHideSpot();
        interactPressed = interactPressed || input.interactPressed;
        stealPressed = stealPressed || input.stealPressed;

        if (hidingIn != null) {
            Rectangle bounds = hidingIn.getBounds();
            playerX = bounds.getCenterX() - PLAYER_WIDTH / 2.0;
            playerY = bounds.getCenterY() - PLAYER_HEIGHT / 2.0;
            moving = false;
            sprinting = false;
            if (interactPressed) {
                hidingIn = null;
                makeNoise(105, 0.45);
                showMessage("Out of hiding. Keep to the shadows.", 2.0);
            }
        } else if (interactPressed && nearbyHideSpot != null) {
            hidingIn = nearbyHideSpot;
            makeNoise(75, 0.25);
            showMessage("Hidden. Guards cannot see you here.", 2.0);
            moving = false;
        } else {
            movePlayer(dt, input);
        }

        activePainting = findNearbyPainting();
        if (activePainting != null && !activePainting.isStolen() && hidingIn == null) {
            if (input.interact) {
                copyProgress = Math.min(100, copyProgress + (int) Math.ceil(dt * 100 / 3.2));
                if (noiseCooldown <= 0) {
                    makeNoise(150, 0.65);
                    noiseCooldown = 1.1;
                }
                if (copyProgress >= 100 && !activePainting.isCopied()) {
                    activePainting.copied = true;
                    activePainting.quality = Math.max(65, Math.min(99, 86 + (int) (12 - suspicion / 9)));
                    showMessage("Copy complete. Press F to swap the original.", 2.5);
                }
            } else if (!activePainting.isCopied()) {
                copyProgress = Math.max(0, copyProgress - (int) Math.ceil(dt * 14));
            }
            if (stealPressed && activePainting.isCopied()) {
                activePainting.stolen = true;
                activePainting.discoveryTime = Math.max(14, 31 - activePainting.quality * 0.12);
                makeNoise(225, 0.9);
                copyProgress = 0;
                showMessage("Original secured. The fake is in place.", 2.8);
                activePainting = null;
            } else if (stealPressed && !activePainting.isCopied()) {
                showMessage("Copy this artwork first (hold E).", 1.8);
            }
        } else if (activePainting != null && activePainting.isStolen() && hidingIn == null && interactPressed) {
            showMessage("Replica inspection: " + activePainting.getQuality()
                + "% match. The original is safely in your bag.", 2.8);
        }

        updateFakeDiscoveries(dt);
        int visibleGuards = 0;
        for (Guard guard : guards) {
            guard.update(dt, map, playerX, playerY, hidingIn != null, sprinting,
                noiseRemaining > 0, noiseX, noiseY, noiseRadius);
            if (guard.isPlayerVisible()) visibleGuards++;
            if (hidingIn == null && playerBounds().intersects(
                new Rectangle((int) guard.getX(), (int) guard.getY(), Guard.WIDTH, Guard.HEIGHT))) {
                gameOver = true;
                endReason = "A guard caught you in the gallery.";
            }
        }
        if (visibleGuards > 0) {
            suspicion = Math.min(100, suspicion + dt * (sprinting ? 34 : 24));
            if (suspicion >= 100) {
                gameOver = true;
                endReason = "Security identified you and sealed the museum.";
            }
        } else {
            suspicion = Math.max(0, suspicion - dt * 5.5);
        }
        if (hidingIn == null && getStolenCount() == MuseumMap.Painting.COUNT
            && playerBounds().intersects(map.getExit())) won = true;
        interactPressed = false;
        stealPressed = false;
    }

    private void movePlayer(double dt, Input input) {
        double dx = (input.right ? 1 : 0) - (input.left ? 1 : 0);
        double dy = (input.down ? 1 : 0) - (input.up ? 1 : 0);
        double length = Math.hypot(dx, dy);
        moving = length > 0;
        sprinting = moving && input.sprint;
        if (length == 0) return;
        double speed = sprinting ? SPRINT_SPEED : WALK_SPEED;
        dx = dx / length * speed * dt;
        dy = dy / length * speed * dt;
        moveAxis(dx, 0);
        moveAxis(0, dy);
        if (sprinting && noiseCooldown <= 0) {
            makeNoise(162, 0.82);
            noiseCooldown = 0.45;
        }
    }

    private void moveAxis(double dx, double dy) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 3.0));
        double sx = dx / steps;
        double sy = dy / steps;
        for (int i = 0; i < steps; i++) {
            double nextX = playerX + sx;
            double nextY = playerY + sy;
            if (map.canStandAt(nextX, nextY, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                playerX = nextX;
                playerY = nextY;
            }
        }
    }

    private void makeNoise(double radius, double strength) {
        noiseX = playerX + PLAYER_WIDTH / 2.0;
        noiseY = playerY + PLAYER_HEIGHT / 2.0;
        noiseRadius = radius;
        noiseRemaining = strength;
    }

    private MuseumMap.Painting findNearbyPainting() {
        MuseumMap.Painting nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MuseumMap.Painting painting : map.getPaintings()) {
            Rectangle bounds = painting.getBounds();
            double x = Math.max(bounds.x, Math.min(playerX + PLAYER_WIDTH / 2.0, bounds.getMaxX()));
            double y = Math.max(bounds.y, Math.min(playerY + PLAYER_HEIGHT / 2.0, bounds.getMaxY()));
            double distance = Point.distance(playerX + PLAYER_WIDTH / 2.0, playerY + PLAYER_HEIGHT / 2.0, x, y);
            if (distance < 42 && distance < nearestDistance) {
                nearest = painting;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private MuseumMap.HideSpot findNearbyHideSpot() {
        for (MuseumMap.HideSpot spot : map.getHideSpots()) {
            Rectangle bounds = spot.getBounds();
            if (bounds.intersects(new Rectangle((int) playerX - 10, (int) playerY - 10,
                PLAYER_WIDTH + 20, PLAYER_HEIGHT + 20))) return spot;
        }
        return null;
    }

    private void updateFakeDiscoveries(double dt) {
        for (MuseumMap.Painting painting : map.getPaintings()) {
            if (!painting.isStolen() || painting.isDiscovered()) continue;
            for (Guard guard : guards) {
                double distance = Point.distance(guard.centerX(), guard.centerY(),
                    painting.getX() + MuseumMap.Painting.WIDTH / 2.0,
                    painting.getY() + MuseumMap.Painting.HEIGHT / 2.0);
                if (distance < 68 && map.hasLineOfSight(guard.centerX(), guard.centerY(),
                    painting.getX() + MuseumMap.Painting.WIDTH / 2.0,
                    painting.getY() + MuseumMap.Painting.HEIGHT / 2.0)) {
                    painting.nearbyGuardTime += dt;
                    if (painting.nearbyGuardTime >= 2.5) {
                        painting.discovered = true;
                        suspicion = Math.min(100, suspicion + 28);
                        showMessage("A guard spotted a fake: " + painting.getName() + ".", 3.2);
                    }
                    break;
                }
            }
            painting.discoveryTime -= dt;
            if (painting.discoveryTime <= 0 && !painting.isDiscovered()) {
                painting.discovered = true;
                suspicion = Math.min(100, suspicion + 18);
                showMessage("Museum staff identified a weak copy.", 3.0);
            }
        }
    }

    private void showMessage(String text, double seconds) {
        message = text;
        messageTimer = seconds;
    }

    public boolean isNear(MuseumMap.HideSpot spot) { return nearbyHideSpot == spot; }
    public void setInteractPressed(boolean pressed) { interactPressed = pressed; }
    public void setStealPressed(boolean pressed) { stealPressed = pressed; }
    public MuseumMap getMap() { return map; }
    public List<Guard> getGuards() { return guards; }
    public double getPlayerX() { return playerX; }
    public double getPlayerY() { return playerY; }
    public double getPlayerCenterX() { return playerX + PLAYER_WIDTH / 2.0; }
    public double getPlayerCenterY() { return playerY + PLAYER_HEIGHT / 2.0; }
    public boolean isMoving() { return moving; }
    public boolean isSprinting() { return sprinting; }
    public boolean isHidden() { return hidingIn != null; }
    public int getSuspicion() { return (int) Math.round(suspicion); }
    public int getTimeLeft() { return timeLeft; }
    public int getCopyProgress() { return activePainting != null && !activePainting.isCopied() ? copyProgress : 0; }

    public int getStolenCount() {
        int count = 0;
        for (MuseumMap.Painting painting : map.getPaintings()) if (painting.isStolen()) count++;
        return count;
    }

    public boolean isWon() { return won; }
    public boolean isGameOver() { return gameOver; }
    public String getEndReason() { return endReason; }

    public String getAlertStatus() {
        String result = "CALM";
        for (Guard guard : guards) {
            if (guard.getState() == Guard.State.CHASE) return "CHASE";
            if (guard.getState() == Guard.State.ALERT || guard.getState() == Guard.State.INSPECT
                || guard.getState() == Guard.State.SEARCH) result = "INVESTIGATING";
        }
        return result;
    }

    public String getPrompt() {
        if (!message.isEmpty()) return message;
        if (hidingIn != null) return "HIDDEN IN " + hidingIn.getName().toUpperCase() + "  -  E TO LEAVE";
        if (nearbyHideSpot != null) return "E  HIDE IN " + nearbyHideSpot.getName().toUpperCase();
        if (activePainting != null) {
            if (activePainting.isStolen()) {
                return "E  INSPECT REPLICA  -  QUALITY " + activePainting.getQuality() + "%";
            }
            if (activePainting.isCopied()) return "F  STEAL " + activePainting.getName().toUpperCase() + "  /  PLACE THE FAKE";
            return "HOLD E  COPY " + activePainting.getName().toUpperCase()
                + (copyProgress > 0 ? "  -  " + copyProgress + "%" : "");
        }
        if (playerBounds().intersects(map.getExit()) && getStolenCount() < MuseumMap.Painting.COUNT) {
            return "EXIT LOCKED  -  SWAP ALL THREE PAINTINGS FIRST";
        }
        return "";
    }

    private Rectangle playerBounds() {
        return new Rectangle((int) Math.round(playerX), (int) Math.round(playerY), PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    public static final class Input {
        public final boolean up, down, left, right, sprint, interact, steal;
        public final boolean interactPressed, stealPressed;
        public Input(boolean up, boolean down, boolean left, boolean right, boolean sprint, boolean interact, boolean steal) {
            this(up, down, left, right, sprint, interact, steal, false, false);
        }
        public Input(boolean up, boolean down, boolean left, boolean right, boolean sprint, boolean interact, boolean steal,
                     boolean interactPressed, boolean stealPressed) {
            this.up = up;
            this.down = down;
            this.left = left;
            this.right = right;
            this.sprint = sprint;
            this.interact = interact;
            this.steal = steal;
            this.interactPressed = interactPressed;
            this.stealPressed = stealPressed;
        }
    }
}
