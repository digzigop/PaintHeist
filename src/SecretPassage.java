import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecretPassage {
    private final String name;
    private final Rectangle firstEntrance;
    private final Rectangle secondEntrance;
    private boolean discovered;
    private double revealSeconds;

    public SecretPassage(String name, Rectangle firstEntrance, Rectangle secondEntrance) {
        this.name = name;
        this.firstEntrance = new Rectangle(firstEntrance);
        this.secondEntrance = new Rectangle(secondEntrance);
    }

    public String getName() { return name; }
    public boolean isDiscovered() { return discovered; }
    public double getRevealProgress() { return Math.min(1, revealSeconds / 0.8); }

    public boolean isNear(double x, double y) {
        return distanceTo(firstEntrance, x, y) <= 48 || distanceTo(secondEntrance, x, y) <= 48;
    }

    public boolean discover(double x, double y) {
        if (!isNear(x, y) || discovered) return false;
        reveal();
        return true;
    }

    public boolean discoverByApproach(double x, double y) {
        if (discovered || distanceTo(firstEntrance, x, y) > 28
            && distanceTo(secondEntrance, x, y) > 28) return false;
        reveal();
        return true;
    }

    private void reveal() {
        discovered = true;
        revealSeconds = 0;
    }

    public void update(double dt) {
        revealSeconds = Math.min(0.8, revealSeconds + dt);
    }

    public Point traverse(Rectangle actorBounds) {
        List<Point> candidates = traverseCandidates(actorBounds);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /** Returns candidate landing centers on the opposite end of the passage. */
    public List<Point> traverseCandidates(Rectangle actorBounds) {
        if (!discovered) return Collections.emptyList();
        if (actorBounds.intersects(firstEntrance)) {
            return landingPoints(firstEntrance, secondEntrance);
        }
        if (actorBounds.intersects(secondEntrance)) {
            return landingPoints(secondEntrance, firstEntrance);
        }
        return Collections.emptyList();
    }

    private static List<Point> landingPoints(Rectangle from, Rectangle to) {
        double dx = to.getCenterX() - from.getCenterX();
        double dy = to.getCenterY() - from.getCenterY();
        double distance = Math.hypot(dx, dy);
        double ux = distance < 0.001 ? 1.0 : dx / distance;
        double uy = distance < 0.001 ? 0.0 : dy / distance;
        double[][] offsets = {
            {ux * 30, uy * 30},
            {-uy * 30, ux * 30},
            {uy * 30, -ux * 30},
            {0, 30},
            {0, -30},
            {30, 0},
            {-30, 0}
        };
        List<Point> points = new ArrayList<>(offsets.length);
        for (double[] offset : offsets) {
            points.add(new Point(
                (int) Math.round(to.getCenterX() + offset[0]),
                (int) Math.round(to.getCenterY() + offset[1])));
        }
        return points;
    }

    public Rectangle getFirstEntrance() { return new Rectangle(firstEntrance); }
    public Rectangle getSecondEntrance() { return new Rectangle(secondEntrance); }

    private static double distanceTo(Rectangle bounds, double x, double y) {
        double nearestX = Math.max(bounds.x, Math.min(x, bounds.getMaxX()));
        double nearestY = Math.max(bounds.y, Math.min(y, bounds.getMaxY()));
        return Math.hypot(x - nearestX, y - nearestY);
    }

    public static final class Data {
        private final String name;
        private final Rectangle firstEntrance;
        private final Rectangle secondEntrance;

        public Data(String name, Rectangle firstEntrance, Rectangle secondEntrance) {
            this.name = name;
            this.firstEntrance = new Rectangle(firstEntrance);
            this.secondEntrance = new Rectangle(secondEntrance);
        }

        public SecretPassage createPassage() {
            return new SecretPassage(name, firstEntrance, secondEntrance);
        }
    }
}
