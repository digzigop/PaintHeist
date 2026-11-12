import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class GameWorld {
    private static final int PLAYER_WIDTH = 20;
    private static final int PLAYER_HEIGHT = 24;
    private static final double WALK_SPEED = 132;
    private static final double SPRINT_SPEED = 204;
    private final MuseumMap map;
    private final List<Guard> guards = new ArrayList<>();
    private final List<SecurityCamera> cameras = new ArrayList<>();
    private final NoiseSystem noiseSystem = new NoiseSystem();
    private final int missionSeconds;
    private double playerX;
    private double playerY;
    private double suspicion;
    private double elapsed;
    private double noiseCooldown;
    private double objectNoiseCooldown;
    private double laserAlarmCooldown;
    private double distractionCooldown;
    private double distractionEffectTimer;
    private double distractionX;
    private double distractionY;
    private double facingX;
    private double facingY = -1;
    private int timeLeft;
    private int copyProgress;
    private MuseumMap.Painting activePainting;
    private MuseumMap.HideSpot nearbyHideSpot;
    private MovableObject nearbyMovableObject;
    private SecretPassage nearbySecretPassage;
    private MuseumMap.HideSpot hidingIn;
    private boolean interactPressed;
    private boolean stealPressed;
    private boolean distractPressed;
    private boolean moving;
    private boolean sprinting;
    private boolean sneaking;
    private boolean gameOver;
    private boolean won;
    private int detectionIncidents;
    private String endReason = "Security found you in the gallery.";
    private String message = "";
    private double messageTimer;

    public GameWorld() {
        this(0);
    }

    public GameWorld(int levelIndex) {
        MuseumLevel level = MuseumLevel.get(levelIndex);
        map = new MuseumMap(level);
        missionSeconds = level.getMissionSeconds();
        timeLeft = missionSeconds;
        Point start = level.getPlayerStart();
        playerX = start.x;
        playerY = start.y;
        for (MuseumLevel.GuardRoute route : level.getGuards()) {
            Point position = route.getStart();
            guards.add(new Guard(route.getName(), position.x, position.y, route.getSpeed(),
                route.getWaypoints().toArray(new Point[0])));
        }
        for (MuseumLevel.CameraData data : level.getCameras()) {
            cameras.add(new SecurityCamera(data.getName(), data.getX(), data.getY(), data.getFacing(),
                data.getSweep(), data.getRadius(), data.getSpeed(), data.getDetectionSeconds()));
        }
    }

    public void update(double dt, Input input) {
        if (gameOver || won) return;
        dt = Math.min(dt, 0.05);
        elapsed += dt;
        timeLeft = Math.max(0, missionSeconds - (int) elapsed);
        if (timeLeft == 0) {
            gameOver = true;
            endReason = "The museum locks down before your escape.";
            return;
        }
        noiseCooldown = Math.max(0, noiseCooldown - dt);
        objectNoiseCooldown = Math.max(0, objectNoiseCooldown - dt);
        laserAlarmCooldown = Math.max(0, laserAlarmCooldown - dt);
        distractionCooldown = Math.max(0, distractionCooldown - dt);
        distractionEffectTimer = Math.max(0, distractionEffectTimer - dt);
        noiseSystem.update(dt);
        messageTimer = Math.max(0, messageTimer - dt);
        if (messageTimer == 0) message = "";

        map.updateSecurity(dt);
        activePainting = findNearbyPainting();
        nearbyHideSpot = findNearbyHideSpot();
        nearbyMovableObject = map.getNearbyMovableObject(getPlayerCenterX(), getPlayerCenterY());
        nearbySecretPassage = map.getNearbyPassage(getPlayerCenterX(), getPlayerCenterY());
        SecretPassage approachedPassage = map.discoverPassageByApproach(
            getPlayerCenterX(), getPlayerCenterY());
        if (approachedPassage != null) {
            makeNoise(75, 0.25);
            showMessage("Hidden passage discovered: " + approachedPassage.getName() + ".", 2.8);
        }
        interactPressed = interactPressed || input.interactPressed;
        stealPressed = stealPressed || input.stealPressed;
        distractPressed = distractPressed || input.distractPressed;

        if (hidingIn != null) {
            Rectangle bounds = hidingIn.getBounds();
            playerX = bounds.getCenterX() - PLAYER_WIDTH / 2.0;
            playerY = bounds.getCenterY() - PLAYER_HEIGHT / 2.0;
            moving = false;
            sprinting = false;
            sneaking = false;
            if (interactPressed) {
                hidingIn = null;
                makeNoise(105, 0.45);
                showMessage("Out of hiding. Keep to the shadows.", 2.0);
            }
        } else if (interactPressed && nearbySecretPassage != null
            && !nearbySecretPassage.isDiscovered()) {
            SecretPassage discovered = map.discoverNearbyPassage(getPlayerCenterX(), getPlayerCenterY());
            if (discovered != null) {
                makeNoise(115, 0.5);
                showMessage("Hidden passage discovered: " + discovered.getName() + ".", 2.8);
            }
        } else if (interactPressed && nearbyHideSpot != null) {
            hidingIn = nearbyHideSpot;
            makeNoise(75, 0.25);
            showMessage("Hidden. Guards cannot see you here.", 2.0);
            moving = false;
        } else {
            if (interactPressed && map.activateNearbySwitch(getPlayerCenterX(), getPlayerCenterY())) {
                makeNoise(95, 0.4);
                showMessage("Security disabled for 12 seconds.", 2.2);
            }
            movePlayer(dt, input);
        }
        Point passageExit = hidingIn == null ? map.traversePassage(playerBounds()) : null;
        if (passageExit != null) {
            double exitX = passageExit.x - PLAYER_WIDTH / 2.0;
            double exitY = passageExit.y - PLAYER_HEIGHT / 2.0;
            if (map.canStandAt(exitX, exitY, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                playerX = exitX;
                playerY = exitY;
                makeNoise(85, 0.3);
                showMessage("You slipped through the hidden passage.", 1.8);
            } else {
                showMessage("The far side of the passage is obstructed.", 1.8);
            }
        }

        boolean cameraDetecting = false;
        for (SecurityCamera camera : cameras) {
            camera.update(dt, map, getPlayerCenterX(), getPlayerCenterY(),
                hidingIn != null, map.areSecuritySystemsDisabled());
            cameraDetecting |= camera.isDetecting();
            if (camera.hasNewDetection()) {
                detectionIncidents++;
                suspicion = Math.min(100, suspicion + 10);
                makeNoiseAt(camera.getX(), camera.getY(), 360, 1.0, "camera alarm");
                showMessage("SECURITY CAMERA ALERT! Break line of sight or disable security.", 3.2);
            }
        }

        if (distractPressed && hidingIn == null) {
            throwDistraction();
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
            Guard.State previousState = guard.getState();
            NoiseSystem.NoiseEvent noise = noiseSystem.loudestAt(guard.centerX(), guard.centerY());
            guard.update(dt, map, playerX, playerY, hidingIn != null, sprinting,
                noise != null, noise == null ? 0 : noise.getX(),
                noise == null ? 0 : noise.getY(), noise == null ? 0 : noise.getRadius());
            if (previousState != Guard.State.CHASE && guard.getState() == Guard.State.CHASE) {
                detectionIncidents++;
            }
            if (guard.isPlayerVisible()) visibleGuards++;
            if (hidingIn == null && playerBounds().intersects(
                new Rectangle((int) guard.getX(), (int) guard.getY(), Guard.WIDTH, Guard.HEIGHT))) {
                gameOver = true;
                endReason = "A guard caught you in the gallery.";
            }
        }
        LaserBarrier activeLaser = map.getActiveLaserContact(playerBounds());
        if (activeLaser != null) triggerLaserAlarm(activeLaser);
        if (visibleGuards > 0 || cameraDetecting) {
            suspicion = Math.min(100, suspicion + dt * (sprinting ? 34 : 24));
            if (suspicion >= 100) {
                gameOver = true;
                endReason = "Security identified you and sealed the museum.";
            }
        } else {
            suspicion = Math.max(0, suspicion - dt * 5.5);
        }
        if (hidingIn == null && getStolenCount() == map.getPaintingCount() && isAtAnyExit()) won = true;
        interactPressed = false;
        stealPressed = false;
        distractPressed = false;
    }

    private void movePlayer(double dt, Input input) {
        double dx = (input.right ? 1 : 0) - (input.left ? 1 : 0);
        double dy = (input.down ? 1 : 0) - (input.up ? 1 : 0);
        double length = Math.hypot(dx, dy);
        moving = length > 0;
        sprinting = moving && input.sprint;
        sneaking = moving && input.sneak && !sprinting;
        if (length == 0) return;
        facingX = dx / length;
        facingY = dy / length;
        double speed = sprinting ? SPRINT_SPEED : sneaking ? WALK_SPEED * 0.62 : WALK_SPEED;
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
            } else {
                Rectangle nextBounds = new Rectangle((int) Math.round(nextX), (int) Math.round(nextY),
                    PLAYER_WIDTH, PLAYER_HEIGHT);
                MovableObject object = map.getMovableObjectIntersecting(nextBounds);
                if (object != null && map.tryPushObject(nextBounds, sx, sy, sprinting)
                    && map.canStandAt(nextX, nextY, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                    playerX = nextX;
                    playerY = nextY;
                    if (objectNoiseCooldown <= 0 && object.getNoiseRadius() > 0) {
                        Rectangle bounds = object.getBounds();
                        makeNoiseAt(bounds.getCenterX(), bounds.getCenterY(),
                            object.getNoiseRadius(), 0.75, object.getName());
                        objectNoiseCooldown = 0.7;
                    }
                } else {
                    LaserBarrier barrier = map.getActiveLaserContact(nextBounds);
                    if (barrier != null) triggerLaserAlarm(barrier);
                }
            }
        }
    }

    private void makeNoise(double radius, double strength) {
        makeNoiseAt(getPlayerCenterX(), getPlayerCenterY(), radius, strength, "player");
    }

    private void makeNoiseAt(double x, double y, double radius, double strength, String source) {
        noiseSystem.emit(x, y, radius, strength, source);
    }

    private void triggerLaserAlarm(LaserBarrier barrier) {
        if (laserAlarmCooldown > 0) return;
        Rectangle bounds = barrier.getBounds();
        makeNoiseAt(bounds.getCenterX(), bounds.getCenterY(), 390, 1.2, "laser alarm");
        suspicion = Math.min(100, suspicion + 18);
        detectionIncidents++;
        laserAlarmCooldown = 1.25;
        showMessage("LASER ALARM! Wait for the barrier to power down.", 2.8);
    }

    private void throwDistraction() {
        if (distractionCooldown > 0) {
            showMessage("No distraction ready for " + (int) Math.ceil(distractionCooldown) + "s.", 1.5);
            return;
        }
        distractionX = Math.max(35, Math.min(MuseumMap.WIDTH - 35,
            getPlayerCenterX() + facingX * 190));
        distractionY = Math.max(105, Math.min(MuseumMap.HEIGHT - 35,
            getPlayerCenterY() + facingY * 190));
        makeNoiseAt(distractionX, distractionY, 360, 1.1, "thrown distraction");
        distractionEffectTimer = 1.2;
        distractionCooldown = 5.0;
        showMessage("Distraction thrown. Guards will investigate the sound.", 2.2);
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
    public void setDistractPressed(boolean pressed) { distractPressed = pressed; }
    public MuseumMap getMap() { return map; }
    public List<Guard> getGuards() { return guards; }
    public List<SecurityCamera> getCameras() { return cameras; }
    public double getPlayerX() { return playerX; }
    public double getPlayerY() { return playerY; }
    public double getPlayerCenterX() { return playerX + PLAYER_WIDTH / 2.0; }
    public double getPlayerCenterY() { return playerY + PLAYER_HEIGHT / 2.0; }
    public double getPlayerBottomY() { return playerY + PLAYER_HEIGHT; }
    public double getPlayerFacingX() { return facingX; }
    public double getPlayerFacingY() { return facingY; }
    public boolean isMoving() { return moving; }
    public boolean isSprinting() { return sprinting; }
    public boolean isSneaking() { return sneaking; }
    public boolean isHidden() { return hidingIn != null; }
    public boolean hasDistractionEffect() { return distractionEffectTimer > 0; }
    public double getDistractionX() { return distractionX; }
    public double getDistractionY() { return distractionY; }
    public double getDistractionEffectProgress() { return distractionEffectTimer / 1.2; }
    public double getDistractionCooldown() { return distractionCooldown; }
    public NoiseSystem getNoiseSystem() { return noiseSystem; }
    public int getSuspicion() { return (int) Math.round(suspicion); }
    public int getTimeLeft() { return timeLeft; }
    public int getCopyProgress() { return activePainting != null && !activePainting.isCopied() ? copyProgress : 0; }

    public int getStolenCount() {
        int count = 0;
        for (MuseumMap.Painting painting : map.getPaintings()) if (painting.isStolen()) count++;
        return count;
    }

    private boolean isAtAnyExit() {
        for (Rectangle exit : map.getExits()) {
            if (playerBounds().intersects(exit)) return true;
        }
        return false;
    }

    public int getLevelIndex() { return map.getLevel().getId(); }
    public int getDetectionIncidents() { return detectionIncidents; }
    public int getElapsedSeconds() { return (int) elapsed; }
    public MuseumLevel getLevel() { return map.getLevel(); }
    public int getTargetCount() { return map.getPaintingCount(); }
    public int getScorePreview() {
        return timeLeft * 15 + getStolenCount() * 400 + Math.max(0, 100 - getSuspicion()) * 5
            + (detectionIncidents == 0 ? 500 : 0);
    }

    public boolean isWon() { return won; }
    public boolean isGameOver() { return gameOver; }
    public String getEndReason() { return endReason; }

    public String getAlertStatus() {
        String result = "CALM";
        for (SecurityCamera camera : cameras) {
            if (camera.isDetecting()) return "CAMERA ALERT";
            if (camera.isTracking()) result = "CAMERA TRACKING";
        }
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
        if (nearbySecretPassage != null) {
            if (nearbySecretPassage.isDiscovered()) return "HIDDEN ROUTE: " + nearbySecretPassage.getName().toUpperCase()
                + "  -  WALK INTO THE BOOKCASE";
            return "E  SEARCH THE SUSPICIOUS BOOKCASE";
        }
        if (activePainting != null) {
            if (activePainting.isStolen()) {
                return "E  INSPECT REPLICA  -  QUALITY " + activePainting.getQuality() + "%";
            }
            if (activePainting.isCopied()) return "F  STEAL " + activePainting.getName().toUpperCase() + "  /  PLACE THE FAKE";
            return "HOLD E  COPY " + activePainting.getName().toUpperCase()
                + (copyProgress > 0 ? "  -  " + copyProgress + "%" : "");
        }
        if (isAtAnyExit() && getStolenCount() < map.getPaintingCount()) {
            return "EXIT LOCKED  -  SWAP ALL " + map.getPaintingCount() + " PAINTINGS FIRST";
        }
        if (map.isNearSwitch(getPlayerCenterX(), getPlayerCenterY())) {
            return map.areSecuritySystemsDisabled()
                ? "SECURITY DISABLED  -  " + (int) Math.ceil(map.getSecurityDisabledSeconds()) + "s"
                : "E  DISABLE LASERS AND CAMERAS FOR 12 SECONDS";
        }
        LaserBarrier nearbyLaser = map.getNearbyLaser(getPlayerCenterX(), getPlayerCenterY());
        if (nearbyLaser != null) {
            if (nearbyLaser.getState() == LaserBarrier.State.WARNING) {
                return nearbyLaser.getName().toUpperCase() + "  -  WARNING";
            }
            if (nearbyLaser.getState() == LaserBarrier.State.ACTIVE) {
                return nearbyLaser.getName().toUpperCase() + "  -  ACTIVE";
            }
            if (nearbyLaser.getState() == LaserBarrier.State.DISABLED) {
                return nearbyLaser.getName().toUpperCase() + "  -  DISABLED";
            }
            return nearbyLaser.getName().toUpperCase() + "  -  INACTIVE";
        }
        if (nearbyMovableObject != null) {
            if (!nearbyMovableObject.isMovable()) {
                return nearbyMovableObject.getName().toUpperCase() + "  -  TOO HEAVY TO MOVE";
            }
            if (nearbyMovableObject.requiresSprint()) {
                return "SPRINT INTO " + nearbyMovableObject.getName().toUpperCase() + " TO PUSH";
            }
            return "WALK INTO " + nearbyMovableObject.getName().toUpperCase() + " TO PUSH";
        }
        if (distractionCooldown > 0) {
            return "Q  DISTRACTION RECHARGING  -  " + (int) Math.ceil(distractionCooldown) + "s";
        }
        return "Q  THROW DISTRACTION";
    }

    private Rectangle playerBounds() {
        return new Rectangle((int) Math.round(playerX), (int) Math.round(playerY), PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    public static final class Input {
        public final boolean up, down, left, right, sprint, sneak, interact, steal;
        public final boolean interactPressed, stealPressed, distractPressed;
        public Input(boolean up, boolean down, boolean left, boolean right, boolean sprint, boolean interact, boolean steal) {
            this(up, down, left, right, sprint, interact, steal, false, false, false);
        }
        public Input(boolean up, boolean down, boolean left, boolean right, boolean sprint, boolean interact, boolean steal,
                     boolean interactPressed, boolean stealPressed) {
            this(up, down, left, right, sprint, interact, steal, interactPressed, stealPressed, false);
        }
        public Input(boolean up, boolean down, boolean left, boolean right, boolean sprint, boolean interact, boolean steal,
                     boolean interactPressed, boolean stealPressed, boolean distractPressed) {
            this(up, down, left, right, sprint, false, interact, steal,
                interactPressed, stealPressed, distractPressed);
        }
        public Input(boolean up, boolean down, boolean left, boolean right, boolean sprint, boolean sneak,
                     boolean interact, boolean steal, boolean interactPressed, boolean stealPressed,
                     boolean distractPressed) {
            this.up = up;
            this.down = down;
            this.left = left;
            this.right = right;
            this.sprint = sprint;
            this.sneak = sneak;
            this.interact = interact;
            this.steal = steal;
            this.interactPressed = interactPressed;
            this.stealPressed = stealPressed;
            this.distractPressed = distractPressed;
        }
    }
}
