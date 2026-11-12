import java.awt.Rectangle;

public final class MovableObject {
    private final String name;
    private final int width;
    private final int height;
    private final boolean movable;
    private final boolean requiresSprint;
    private final double noiseRadius;
    private double x;
    private double y;

    public MovableObject(String name, double x, double y, int width, int height,
                         boolean movable, boolean requiresSprint, double noiseRadius) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.movable = movable;
        this.requiresSprint = requiresSprint;
        this.noiseRadius = noiseRadius;
    }

    public String getName() { return name; }
    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }
    public boolean isMovable() { return movable; }
    public boolean canMove(boolean sprinting) { return movable && (!requiresSprint || sprinting); }
    public boolean requiresSprint() { return requiresSprint; }
    public double getNoiseRadius() { return noiseRadius; }

    void moveBy(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public static final class Data {
        private final String name;
        private final Rectangle bounds;
        private final boolean movable;
        private final boolean requiresSprint;
        private final double noiseRadius;

        public Data(String name, Rectangle bounds, boolean movable, boolean requiresSprint, double noiseRadius) {
            this.name = name;
            this.bounds = new Rectangle(bounds);
            this.movable = movable;
            this.requiresSprint = requiresSprint;
            this.noiseRadius = noiseRadius;
        }

        public MovableObject createObject() {
            return new MovableObject(name, bounds.x, bounds.y, bounds.width, bounds.height,
                movable, requiresSprint, noiseRadius);
        }
    }
}
