import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class MuseumMap {
    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;
    private static final int TILE = 20;
    private final MuseumLevel level;
    private final List<Rectangle> solids = new ArrayList<>();
    private final List<Painting> paintings = new ArrayList<>();
    private final List<HideSpot> hideSpots = new ArrayList<>();
    private final List<Rectangle> cases = new ArrayList<>();
    private final List<Rectangle> benches = new ArrayList<>();
    private final List<Rectangle> statues = new ArrayList<>();
    private final List<Rectangle> lasers = new ArrayList<>();
    private final List<Rectangle> switches = new ArrayList<>();
    private final List<Rectangle> exits = new ArrayList<>();
    private final List<LaserBarrier> laserBarriers = new ArrayList<>();
    private final List<MovableObject> movableObjects = new ArrayList<>();
    private final List<SecretPassage> secretPassages = new ArrayList<>();
    private double securityDisabledSeconds;

    public MuseumMap() {
        this(MuseumLevel.get(0));
    }

    public MuseumMap(MuseumLevel level) {
        this.level = level;
        addWall(0, 65, WIDTH, 25);
        addWall(0, 90, 24, 450);
        addWall(936, 90, 24, 450);
        addWall(0, 540, WIDTH, 60);
        for (Rectangle wall : level.getWalls()) addWall(wall.x, wall.y, wall.width, wall.height);
        copyInto(level.getCases(), cases);
        copyInto(level.getBenches(), benches);
        copyInto(level.getStatues(), statues);
        copyInto(level.getLasers(), lasers);
        copyInto(level.getSwitches(), switches);
        for (LaserBarrier.Data data : level.getLaserPatterns()) laserBarriers.add(data.createBarrier());
        for (MovableObject.Data data : level.getMovableObjects()) movableObjects.add(data.createObject());
        for (SecretPassage.Data data : level.getSecretPassages()) secretPassages.add(data.createPassage());
        copyInto(cases, solids);
        copyInto(benches, solids);
        copyInto(statues, solids);
        for (Point exit : level.getExits()) exits.add(new Rectangle(exit.x, exit.y, 38, 58));
        for (int i = 0; i < level.getPaintingCount(); i++) {
            Point position = level.getPaintingPositions().get(i);
            paintings.add(new Painting(i, position.x, position.y, level.getPaintingNames().get(i)));
        }
        for (MuseumLevel.HideSpotData spot : level.getHideSpots()) {
            hideSpots.add(new HideSpot(spot.getName(), spot.getBounds()));
        }
    }

    private static void copyInto(List<Rectangle> source, List<Rectangle> destination) {
        for (Rectangle rectangle : source) destination.add(new Rectangle(rectangle));
    }

    private void addWall(int x, int y, int width, int height) {
        solids.add(new Rectangle(x, y, width, height));
    }

    public List<Painting> getPaintings() { return Collections.unmodifiableList(paintings); }
    public List<HideSpot> getHideSpots() { return Collections.unmodifiableList(hideSpots); }
    public List<Rectangle> getSolids() { return Collections.unmodifiableList(solids); }
    public List<Rectangle> getExits() { return Collections.unmodifiableList(exits); }
    public Rectangle getExit() { return new Rectangle(exits.get(0)); }
    public MuseumLevel getLevel() { return level; }
    public int getPaintingCount() { return paintings.size(); }
    public List<Rectangle> getLasers() { return Collections.unmodifiableList(lasers); }
    public List<LaserBarrier> getLaserBarriers() { return Collections.unmodifiableList(laserBarriers); }
    public List<MovableObject> getMovableObjects() { return Collections.unmodifiableList(movableObjects); }
    public List<SecretPassage> getSecretPassages() { return Collections.unmodifiableList(secretPassages); }
    public List<Rectangle> getSwitches() { return Collections.unmodifiableList(switches); }
    public boolean areSecuritySystemsDisabled() { return securityDisabledSeconds > 0; }
    public boolean hasSecuritySwitches() { return !switches.isEmpty(); }
    public double getSecurityDisabledSeconds() { return securityDisabledSeconds; }
    public boolean isNearSwitch(double x, double y) {
        for (Rectangle securitySwitch : switches) {
            if (Point.distance(x, y, securitySwitch.getCenterX(), securitySwitch.getCenterY()) <= 48) return true;
        }
        return false;
    }

    public void updateSecurity(double dt) {
        securityDisabledSeconds = Math.max(0, securityDisabledSeconds - dt);
        for (LaserBarrier barrier : laserBarriers) {
            barrier.update(dt, securityDisabledSeconds > 0);
        }
        for (SecretPassage passage : secretPassages) passage.update(dt);
    }

    public boolean activateNearbySwitch(double x, double y) {
        for (Rectangle securitySwitch : switches) {
            if (Point.distance(x, y, securitySwitch.getCenterX(), securitySwitch.getCenterY()) <= 48) {
                securityDisabledSeconds = 12;
                for (LaserBarrier barrier : laserBarriers) barrier.update(0, true);
                return true;
            }
        }
        return false;
    }

    public boolean isBlocked(Rectangle actor) {
        for (Rectangle solid : solids) if (actor.intersects(solid)) return true;
        for (MovableObject object : movableObjects) if (actor.intersects(object.getBounds())) return true;
        for (SecretPassage passage : secretPassages) {
            if (!passage.isDiscovered()
                && (actor.intersects(passage.getFirstEntrance())
                    || actor.intersects(passage.getSecondEntrance()))) return true;
        }
        if (getActiveLaserContact(actor) != null) return true;
        return false;
    }

    public LaserBarrier getActiveLaserContact(Rectangle actor) {
        for (LaserBarrier barrier : laserBarriers) {
            if (barrier.isActive() && actor.intersects(barrier.getBounds())) return barrier;
        }
        return null;
    }

    public LaserBarrier getNearbyLaser(double x, double y) {
        LaserBarrier nearest = null;
        double nearestDistance = 60;
        for (LaserBarrier barrier : laserBarriers) {
            Rectangle bounds = barrier.getBounds();
            double distance = Point.distance(x, y, bounds.getCenterX(), bounds.getCenterY());
            if (distance < nearestDistance) {
                nearest = barrier;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public MovableObject getMovableObjectIntersecting(Rectangle bounds) {
        for (MovableObject object : movableObjects) {
            if (bounds.intersects(object.getBounds())) return object;
        }
        return null;
    }

    public MovableObject getNearbyMovableObject(double x, double y) {
        MovableObject nearest = null;
        double nearestDistance = 52;
        for (MovableObject object : movableObjects) {
            Rectangle bounds = object.getBounds();
            double distance = Point.distance(x, y, bounds.getCenterX(), bounds.getCenterY());
            if (distance < nearestDistance) {
                nearest = object;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public boolean tryPushObject(Rectangle nextActor, double dx, double dy, boolean sprinting) {
        for (MovableObject object : movableObjects) {
            Rectangle objectBounds = object.getBounds();
            if (!nextActor.intersects(objectBounds)) continue;
            if (!object.canMove(sprinting)) return false;

            Rectangle overlap = nextActor.intersection(objectBounds);
            double pushX = 0;
            double pushY = 0;
            if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
                pushX = Math.copySign(overlap.width + 2.0, dx);
            } else if (dy != 0) {
                pushY = Math.copySign(overlap.height + 2.0, dy);
            }
            Rectangle destination = new Rectangle(
                (int) Math.round(objectBounds.x + pushX),
                (int) Math.round(objectBounds.y + pushY),
                objectBounds.width, objectBounds.height);
            if (!canObjectOccupy(object, destination)) return false;
            object.moveBy(pushX, pushY);
            return !nextActor.intersects(object.getBounds());
        }
        return false;
    }

    private boolean canObjectOccupy(MovableObject movingObject, Rectangle destination) {
        for (Rectangle solid : solids) if (destination.intersects(solid)) return false;
        for (MovableObject other : movableObjects) {
            if (other != movingObject && destination.intersects(other.getBounds())) return false;
        }
        for (LaserBarrier barrier : laserBarriers) {
            if (barrier.isActive() && destination.intersects(barrier.getBounds())) return false;
        }
        return true;
    }

    public SecretPassage discoverNearbyPassage(double x, double y) {
        for (SecretPassage passage : secretPassages) {
            if (passage.discover(x, y)) return passage;
        }
        return null;
    }

    public SecretPassage discoverPassageByApproach(double x, double y) {
        for (SecretPassage passage : secretPassages) {
            if (passage.discoverByApproach(x, y)) return passage;
        }
        return null;
    }

    public SecretPassage getNearbyPassage(double x, double y) {
        for (SecretPassage passage : secretPassages) {
            if (passage.isNear(x, y)) return passage;
        }
        return null;
    }

    public Point traversePassage(Rectangle actor) {
        for (SecretPassage passage : secretPassages) {
            Point destination = passage.traverse(actor);
            if (destination != null) return destination;
        }
        return null;
    }

    public boolean canStandAt(double x, double y, int width, int height) {
        return !isBlocked(new Rectangle((int) Math.round(x), (int) Math.round(y), width, height));
    }

    public boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
        Line2D.Double ray = new Line2D.Double(x1, y1, x2, y2);
        for (Rectangle solid : solids) if (ray.intersects(solid)) return false;
        for (MovableObject object : movableObjects) if (ray.intersects(object.getBounds())) return false;
        for (SecretPassage passage : secretPassages) {
            if (!passage.isDiscovered()
                && (ray.intersects(passage.getFirstEntrance()) || ray.intersects(passage.getSecondEntrance()))) {
                return false;
            }
        }
        return true;
    }

    public Point2D.Double getVisionEndpoint(double x, double y, double angle, double radius) {
        double endX = x + Math.cos(angle) * radius;
        double endY = y + Math.sin(angle) * radius;
        if (hasLineOfSight(x, y, endX, endY)) return new Point2D.Double(endX, endY);

        double visible = 0;
        double blocked = radius;
        for (int i = 0; i < 10; i++) {
            double distance = (visible + blocked) / 2.0;
            double testX = x + Math.cos(angle) * distance;
            double testY = y + Math.sin(angle) * distance;
            if (hasLineOfSight(x, y, testX, testY)) visible = distance;
            else blocked = distance;
        }
        double distance = Math.max(0, visible - 2);
        return new Point2D.Double(x + Math.cos(angle) * distance, y + Math.sin(angle) * distance);
    }

    public List<Point> findPath(double fromX, double fromY, double toX, double toY, int actorWidth, int actorHeight) {
        int columns = WIDTH / TILE;
        int rows = HEIGHT / TILE;
        int start = nearestCell(fromX, fromY, actorWidth, actorHeight, columns, rows);
        int goal = nearestCell(toX, toY, actorWidth, actorHeight, columns, rows);
        if (start < 0 || goal < 0) return Collections.emptyList();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(node -> node.score));
        Map<Integer, Double> costs = new HashMap<>();
        Map<Integer, Integer> previous = new HashMap<>();
        Set<Integer> closed = new HashSet<>();
        costs.put(start, 0.0);
        open.add(new Node(start, heuristic(start, goal, columns)));

        int[] stepX = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] stepY = {-1, -1, -1, 0, 0, 1, 1, 1};
        while (!open.isEmpty()) {
            Node current = open.poll();
            if (!closed.add(current.cell)) continue;
            if (current.cell == goal) break;
            int cx = current.cell % columns;
            int cy = current.cell / columns;
            for (int i = 0; i < stepX.length; i++) {
                int nx = cx + stepX[i];
                int ny = cy + stepY[i];
                if (nx < 0 || nx >= columns || ny < 0 || ny >= rows) continue;
                int next = ny * columns + nx;
                if (closed.contains(next) || !isCellWalkable(nx, ny, actorWidth, actorHeight)) continue;
                if (stepX[i] != 0 && stepY[i] != 0
                    && (!isCellWalkable(cx + stepX[i], cy, actorWidth, actorHeight)
                        || !isCellWalkable(cx, cy + stepY[i], actorWidth, actorHeight))) continue;
                double candidate = costs.get(current.cell)
                    + (stepX[i] == 0 || stepY[i] == 0 ? 1.0 : 1.414);
                if (candidate < costs.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    costs.put(next, candidate);
                    previous.put(next, current.cell);
                    open.add(new Node(next, candidate + heuristic(next, goal, columns)));
                }
            }
        }

        if (start != goal && !previous.containsKey(goal)) return Collections.emptyList();
        List<Integer> reversed = new ArrayList<>();
        int cell = goal;
        reversed.add(cell);
        while (cell != start) {
            cell = previous.get(cell);
            reversed.add(cell);
        }
        Collections.reverse(reversed);
        List<Point> path = new ArrayList<>(reversed.size());
        for (int routeCell : reversed) {
            int centerX = routeCell % columns * TILE + TILE / 2;
            int centerY = routeCell / columns * TILE + TILE / 2;
            path.add(new Point(centerX - actorWidth / 2, centerY - actorHeight / 2));
        }
        return path;
    }

    private int nearestCell(double x, double y, int actorWidth, int actorHeight, int columns, int rows) {
        int centerColumn = Math.max(0, Math.min(columns - 1, (int) ((x + actorWidth / 2.0) / TILE)));
        int centerRow = Math.max(0, Math.min(rows - 1, (int) ((y + actorHeight / 2.0) / TILE)));
        for (int radius = 0; radius < Math.max(columns, rows); radius++) {
            for (int cy = centerRow - radius; cy <= centerRow + radius; cy++) {
                for (int cx = centerColumn - radius; cx <= centerColumn + radius; cx++) {
                    if (cx < 0 || cx >= columns || cy < 0 || cy >= rows) continue;
                    if (Math.max(Math.abs(cx - centerColumn), Math.abs(cy - centerRow)) != radius) continue;
                    if (isCellWalkable(cx, cy, actorWidth, actorHeight)) return cy * columns + cx;
                }
            }
        }
        return -1;
    }

    private boolean isCellWalkable(int x, int y, int actorWidth, int actorHeight) {
        int centerX = x * TILE + TILE / 2;
        int centerY = y * TILE + TILE / 2;
        return canStandAt(centerX - actorWidth / 2.0, centerY - actorHeight / 2.0, actorWidth, actorHeight);
    }

    private static double heuristic(int a, int b, int columns) {
        int dx = Math.abs(a % columns - b % columns);
        int dy = Math.abs(a / columns - b / columns);
        return Math.max(dx, dy) + (1.414 - 1.0) * Math.min(dx, dy);
    }

    public void draw(Graphics2D g) {
        draw(g, Double.NaN, Double.NaN);
    }

    public void draw(Graphics2D g, double playerX, double playerY) {
        g.setColor(level.getFloorA().darker());
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int y = 90; y < 540; y += 40) {
            for (int x = 24; x < 936; x += 40) {
                g.setColor(((x / 40 + y / 40) & 1) == 0
                    ? level.getFloorA() : level.getFloorB());
                g.fillRect(x, y, 40, 40);
                g.setColor(new Color(48, 40, 42, 100));
                g.drawRect(x, y, 40, 40);
                g.setColor(new Color(178, 137, 97, 75));
                g.drawLine(x + 4, y + 4, x + 12, y + 4);
            }
        }
        drawRug(g, 350, 285, 250, 86);
        drawRug(g, 53, 425, 184, 64);
        drawCases(g);
        drawBenches(g);
        drawStatues(g);
        drawMovableObjects(g);
        drawSecretPassages(g, playerX, playerY);
        drawWalls(g);
        drawSecurity(g);
    }

    private void drawRug(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(68, 35, 48));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(185, 133, 73));
        g.drawRect(x + 5, y + 5, width - 10, height - 10);
        g.drawRect(x + 10, y + 10, width - 20, height - 20);
        g.setColor(new Color(201, 149, 83));
        for (int px = x + 19; px < x + width - 10; px += 24) g.fillRect(px, y + height / 2, 4, 4);
    }

    private void drawCases(Graphics2D g) {
        for (Rectangle r : cases) {
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect(r.x + 5, r.y + 6, r.width, r.height);
            g.setColor(new Color(73, 47, 37));
            g.fillRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
            g.setColor(new Color(174, 116, 62));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(new Color(112, 157, 157, 155));
            g.fillRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12);
            g.setColor(new Color(237, 204, 139));
            g.fillRect(r.x + 7, r.y + 5, r.width - 14, 3);
            g.setColor(new Color(218, 181, 100));
            g.fillRect(r.x + r.width / 2 - 5, r.y + r.height / 2 - 4, 10, 8);
        }
    }

    private void drawBenches(Graphics2D g) {
        for (Rectangle r : benches) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(r.x + 4, r.y + 6, r.width, r.height);
            g.setColor(new Color(74, 47, 38));
            g.fillRect(r.x, r.y + 4, r.width, r.height - 7);
            g.setColor(new Color(147, 92, 55));
            g.fillRect(r.x + 2, r.y + 2, r.width - 4, r.height - 9);
            g.setColor(new Color(211, 157, 91));
            g.fillRect(r.x + 5, r.y + 5, r.width - 10, 3);
        }
    }

    private void drawStatues(Graphics2D g) {
        for (Rectangle r : statues) {
            g.setColor(new Color(0, 0, 0, 115));
            g.fillOval(r.x - 3, r.y + r.height - 4, r.width + 8, 10);
            g.setColor(new Color(124, 120, 108));
            g.fillRect(r.x + 5, r.y + 12, r.width - 10, r.height - 12);
            g.fillOval(r.x + 3, r.y + 2, r.width - 6, 20);
            g.setColor(new Color(189, 176, 142));
            g.fillRect(r.x + 8, r.y + 6, r.width - 16, 5);
            g.setColor(new Color(81, 77, 73));
            g.fillRect(r.x + 3, r.y + r.height - 5, r.width - 6, 5);
        }
    }

    private void drawMovableObjects(Graphics2D g) {
        for (MovableObject object : movableObjects) {
            Rectangle r = object.getBounds();
            if (object.getName().toLowerCase().contains("plinth")
                || object.getName().toLowerCase().contains("sarcophagus")
                || object.getName().toLowerCase().contains("stand")) {
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(r.x + 4, r.y + r.height - 2, r.width, 7);
                g.setColor(new Color(90, 83, 75));
                g.fillRect(r.x + 3, r.y + 5, r.width - 6, r.height - 5);
                g.setColor(new Color(165, 151, 124));
                g.fillRect(r.x, r.y, r.width, 7);
                g.setColor(new Color(213, 192, 150));
                g.fillRect(r.x + 4, r.y + 2, r.width - 8, 2);
            } else if (object.getName().toLowerCase().contains("cart")
                || object.getName().toLowerCase().contains("trolley")) {
                g.setColor(new Color(41, 43, 47));
                g.fillRect(r.x + 2, r.y + 5, r.width - 4, r.height - 8);
                g.setColor(new Color(151, 105, 61));
                g.fillRect(r.x + 4, r.y + 2, r.width - 8, r.height - 8);
                g.setColor(new Color(219, 176, 105));
                g.drawLine(r.x + 6, r.y + 5, r.x + r.width - 6, r.y + 5);
                g.setColor(new Color(29, 30, 34));
                g.fillOval(r.x + 4, r.y + r.height - 5, 6, 6);
                g.fillOval(r.x + r.width - 10, r.y + r.height - 5, 6, 6);
            } else {
                g.setColor(new Color(0, 0, 0, 95));
                g.fillRect(r.x + 3, r.y + r.height - 1, r.width, 6);
                g.setColor(new Color(102, 62, 42));
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(new Color(162, 108, 61));
                g.fillRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6);
                g.setColor(new Color(221, 175, 105));
                g.drawRect(r.x + 3, r.y + 3, r.width - 7, r.height - 7);
            }
            g.setColor(object.isMovable() ? new Color(227, 192, 121) : new Color(130, 123, 112));
            g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
        }
    }

    private void drawSecretPassages(Graphics2D g, double playerX, double playerY) {
        for (SecretPassage passage : secretPassages) {
            drawPassageEntrance(g, passage.getFirstEntrance(), passage, playerX, playerY);
            drawPassageEntrance(g, passage.getSecondEntrance(), passage, playerX, playerY);
        }
    }

    private void drawPassageEntrance(Graphics2D g, Rectangle entrance, SecretPassage passage,
                                     double playerX, double playerY) {
        double distance = Double.isNaN(playerX) ? Double.MAX_VALUE
            : Point.distance(playerX, playerY, entrance.getCenterX(), entrance.getCenterY());
        boolean discovered = passage.isDiscovered();
        boolean reveal = discovered || distance < 54;
        g.setColor(new Color(0, 0, 0, 85));
        g.fillRect(entrance.x + 2, entrance.y + 3, entrance.width, entrance.height);
        g.setColor(new Color(76, 48, 37));
        g.fillRect(entrance.x, entrance.y, entrance.width, entrance.height);
        g.setColor(new Color(138, 90, 52));
        g.fillRect(entrance.x + 3, entrance.y + 3, entrance.width - 6, entrance.height - 6);
        g.setColor(new Color(202, 158, 96));
        g.drawLine(entrance.x + 7, entrance.y + 5, entrance.x + 7, entrance.y + entrance.height - 5);
        g.drawLine(entrance.x + 14, entrance.y + 5, entrance.x + 14, entrance.y + entrance.height - 5);
        g.drawLine(entrance.x + 21, entrance.y + 5, entrance.x + 21, entrance.y + entrance.height - 5);
        if (reveal) {
            int alpha = discovered ? 180 : 90;
            g.setColor(new Color(238, 198, 112, alpha));
            g.drawRect(entrance.x - 3, entrance.y - 3, entrance.width + 6, entrance.height + 6);
            if (discovered) {
                g.setColor(new Color(107, 212, 159, 170));
                g.drawRect(entrance.x - 5, entrance.y - 5, entrance.width + 10, entrance.height + 10);
                if (passage.getRevealProgress() < 1) {
                    int radius = 5 + (int) (passage.getRevealProgress() * 22);
                    g.setColor(new Color(119, 239, 177,
                        (int) (160 * (1 - passage.getRevealProgress()))));
                    g.drawOval((int) entrance.getCenterX() - radius,
                        (int) entrance.getCenterY() - radius, radius * 2, radius * 2);
                }
                g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 8));
                g.drawString("SECRET", entrance.x - 2, entrance.y - 6);
            }
        }
    }

    private void drawSecurity(Graphics2D g) {
        for (Rectangle securitySwitch : switches) {
            g.setColor(new Color(35, 40, 42));
            g.fillRect(securitySwitch.x, securitySwitch.y, securitySwitch.width, securitySwitch.height);
            g.setColor(new Color(223, 185, 101));
            g.fillRect(securitySwitch.x + 5, securitySwitch.y + 5, securitySwitch.width - 10, 4);
            g.setColor(new Color(118, 196, 123));
            g.fillOval((int) securitySwitch.getCenterX() - 3, (int) securitySwitch.getCenterY(), 6, 6);
        }
        for (LaserBarrier barrier : laserBarriers) {
            Rectangle laser = barrier.getBounds();
            if (barrier.getState() == LaserBarrier.State.DISABLED) {
                g.setColor(new Color(103, 207, 135, 70));
                g.drawRect(laser.x, laser.y, laser.width, laser.height);
                continue;
            }
            if (barrier.getState() == LaserBarrier.State.INACTIVE) {
                g.setColor(new Color(97, 179, 197, 90));
                g.drawRect(laser.x, laser.y, laser.width, laser.height);
                continue;
            }
            if (barrier.getState() == LaserBarrier.State.WARNING) {
                int alpha = (int) (90 + 100 * barrier.getStateProgress());
                g.setColor(new Color(255, 190, 62, alpha));
                g.fillRect(laser.x, laser.y, laser.width, laser.height);
                g.setColor(new Color(255, 225, 125));
                g.drawRect(laser.x - 2, laser.y - 2, laser.width + 4, laser.height + 4);
                g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 8));
                g.drawString("WARNING", laser.x + laser.width + 4, laser.y + 10);
            } else {
                g.setColor(new Color(186, 49, 56, 105));
                g.fillRect(laser.x, laser.y, laser.width, laser.height);
                g.setColor(new Color(255, 75, 76));
                g.fillRect(laser.x + laser.width / 2 - 2, laser.y, 4, laser.height);
                g.setColor(new Color(255, 120, 110, 75));
                g.drawRect(laser.x - 2, laser.y - 2, laser.width + 4, laser.height + 4);
            }
        }
    }

    private void drawWalls(Graphics2D g) {
        for (Rectangle wall : solids) {
            if (cases.contains(wall) || benches.contains(wall) || statues.contains(wall)) continue;
            g.setColor(new Color(31, 28, 35));
            g.fillRect(wall.x, wall.y, wall.width, wall.height);
            g.setColor(new Color(103, 72, 57));
            g.fillRect(wall.x, wall.y, wall.width, Math.min(5, wall.height));
            g.setColor(new Color(61, 48, 48));
            g.drawRect(wall.x, wall.y, wall.width, wall.height);
        }
        for (int i = 0; i < exits.size(); i++) {
            Rectangle exit = exits.get(i);
            g.setColor(new Color(205, 166, 100));
            g.fillRect(exit.x + 5, exit.y, exit.width - 10, 5);
            g.setColor(new Color(64, 39, 42));
            g.fillRect(exit.x + 5, exit.y + 5, exit.width - 10, exit.height - 5);
            g.setColor(new Color(144, 78, 60));
            g.fillRect(exit.x + 10, exit.y + 10, exit.width - 20, exit.height - 10);
            g.setColor(new Color(235, 197, 119));
            g.fillRect(exit.x + exit.width - 13, exit.y + exit.height / 2, 3, 5);
            g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 9));
            g.setColor(new Color(243, 221, 175));
            g.drawString("EXIT", exit.x + 8, exit.y - 5);
        }
    }

    private static final class Node {
        private final int cell;
        private final double score;
        private Node(int cell, double score) { this.cell = cell; this.score = score; }
    }

    public static final class Painting {
        public static final int COUNT = 3;
        public static final int WIDTH = 56;
        public static final int HEIGHT = 34;
        private final int index;
        private final int x;
        private final int y;
        private final String name;
        boolean copied;
        boolean stolen;
        boolean discovered;
        int quality;
        double discoveryTime;
        double nearbyGuardTime;

        private Painting(int index, int x, int y, String name) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.name = name;
        }

        public int getIndex() { return index; }
        public int getX() { return x; }
        public int getY() { return y; }
        public String getName() { return name; }
        public boolean isCopied() { return copied; }
        public boolean isStolen() { return stolen; }
        public boolean isDiscovered() { return discovered; }
        public int getQuality() { return quality; }
        public Rectangle getBounds() { return new Rectangle(x - 3, y - 3, WIDTH + 6, HEIGHT + 6); }
    }

    public static final class HideSpot {
        private final String name;
        private final Rectangle bounds;

        private HideSpot(String name, Rectangle bounds) {
            this.name = name;
            this.bounds = bounds;
        }

        public String getName() { return name; }
        public Rectangle getBounds() { return new Rectangle(bounds); }
    }
}
