import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
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
    private final List<Rectangle> solids = new ArrayList<>();
    private final List<Painting> paintings = new ArrayList<>();
    private final List<HideSpot> hideSpots = new ArrayList<>();
    private final List<Rectangle> cases = new ArrayList<>();
    private final List<Rectangle> benches = new ArrayList<>();
    private final Rectangle exit = new Rectangle(42, 474, 38, 58);

    public MuseumMap() {
        addWall(0, 65, WIDTH, 25);
        addWall(0, 90, 24, 450);
        addWall(936, 90, 24, 450);
        addWall(0, 540, WIDTH, 60);

        addWall(24, 248, 138, 18);
        addWall(207, 248, 235, 18);
        addWall(505, 248, 230, 18);
        addWall(785, 248, 151, 18);

        addWall(310, 90, 18, 72);
        addWall(310, 207, 18, 41);
        addWall(310, 266, 18, 111);
        addWall(310, 425, 18, 115);
        addWall(632, 90, 18, 72);
        addWall(632, 207, 18, 41);
        addWall(632, 266, 18, 111);
        addWall(632, 425, 18, 115);

        cases.add(new Rectangle(93, 173, 100, 39));
        cases.add(new Rectangle(386, 168, 111, 40));
        cases.add(new Rectangle(725, 171, 112, 40));
        cases.add(new Rectangle(125, 346, 112, 40));
        cases.add(new Rectangle(421, 347, 110, 40));
        cases.add(new Rectangle(718, 346, 111, 40));
        benches.add(new Rectangle(44, 294, 52, 23));
        benches.add(new Rectangle(852, 287, 48, 22));
        benches.add(new Rectangle(526, 455, 56, 22));
        solids.addAll(cases);
        solids.addAll(benches);

        paintings.add(new Painting(0, 112, 112, "Moonlit Orchard"));
        paintings.add(new Painting(1, 451, 112, "The Green Hour"));
        paintings.add(new Painting(2, 779, 112, "Velvet Coast"));

        hideSpots.add(new HideSpot("Gallery wardrobe", new Rectangle(250, 316, 44, 52)));
        hideSpots.add(new HideSpot("Restoration cabinet", new Rectangle(662, 432, 46, 52)));
        hideSpots.add(new HideSpot("Security locker", new Rectangle(852, 451, 48, 54)));
    }

    private void addWall(int x, int y, int width, int height) {
        solids.add(new Rectangle(x, y, width, height));
    }

    public List<Painting> getPaintings() { return Collections.unmodifiableList(paintings); }
    public List<HideSpot> getHideSpots() { return Collections.unmodifiableList(hideSpots); }
    public List<Rectangle> getSolids() { return Collections.unmodifiableList(solids); }
    public Rectangle getExit() { return new Rectangle(exit); }

    public boolean isBlocked(Rectangle actor) {
        for (Rectangle solid : solids) if (actor.intersects(solid)) return true;
        return false;
    }

    public boolean canStandAt(double x, double y, int width, int height) {
        return !isBlocked(new Rectangle((int) Math.round(x), (int) Math.round(y), width, height));
    }

    public boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
        Line2D.Double ray = new Line2D.Double(x1, y1, x2, y2);
        for (Rectangle solid : solids) if (ray.intersects(solid)) return false;
        return true;
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
        g.setColor(new Color(93, 66, 54));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int y = 90; y < 540; y += 40) {
            for (int x = 24; x < 936; x += 40) {
                g.setColor(((x / 40 + y / 40) & 1) == 0
                    ? new Color(125, 91, 70) : new Color(114, 81, 65));
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
        drawWalls(g);
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

    private void drawWalls(Graphics2D g) {
        for (Rectangle wall : solids) {
            if (cases.contains(wall) || benches.contains(wall)) continue;
            g.setColor(new Color(31, 28, 35));
            g.fillRect(wall.x, wall.y, wall.width, wall.height);
            g.setColor(new Color(103, 72, 57));
            g.fillRect(wall.x, wall.y, wall.width, Math.min(5, wall.height));
            g.setColor(new Color(61, 48, 48));
            g.drawRect(wall.x, wall.y, wall.width, wall.height);
        }
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
