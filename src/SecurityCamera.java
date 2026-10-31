import java.awt.Point;

public final class SecurityCamera {
    private final String name;
    private final double x;
    private final double y;
    private final double baseAngle;
    private final double sweepRadians;
    private final double radius;
    private final double sweepSpeed;
    private final double detectionSeconds;
    private double elapsed;
    private boolean detecting;
    private boolean newDetection;
    private double exposureSeconds;

    public SecurityCamera(String name, double x, double y, double facingDegrees,
                          double sweepDegrees, double radius, double sweepSpeed) {
        this(name, x, y, facingDegrees, sweepDegrees, radius, sweepSpeed, 0.9);
    }

    public SecurityCamera(String name, double x, double y, double facingDegrees,
                          double sweepDegrees, double radius, double sweepSpeed, double detectionSeconds) {
        if (radius <= 0 || detectionSeconds <= 0) {
            throw new IllegalArgumentException("Camera radius and detection delay must be positive.");
        }
        this.name = name;
        this.x = x;
        this.y = y;
        this.baseAngle = Math.toRadians(facingDegrees);
        this.sweepRadians = Math.toRadians(sweepDegrees);
        this.radius = radius;
        this.sweepSpeed = sweepSpeed;
        this.detectionSeconds = detectionSeconds;
    }

    public void update(double dt, MuseumMap map, double targetX, double targetY,
                       boolean hidden, boolean securityDisabled) {
        newDetection = false;
        elapsed += dt;
        double targetDx = targetX - x;
        double targetDy = targetY - y;
        double distance = Math.hypot(targetDx, targetDy);
        double angle = getFacingAngle();
        double dot = distance < 0.001 ? 1
            : (targetDx * Math.cos(angle) + targetDy * Math.sin(angle)) / distance;
        boolean visible = !hidden && !securityDisabled && distance <= radius
            && dot >= Math.cos(Math.toRadians(34))
            && map.hasLineOfSight(x, y, targetX, targetY);
        if (hidden || securityDisabled) {
            exposureSeconds = 0;
            detecting = false;
        } else if (visible) {
            exposureSeconds = Math.min(detectionSeconds, exposureSeconds + dt);
            if (!detecting && exposureSeconds >= detectionSeconds) {
                detecting = true;
                newDetection = true;
            }
        } else {
            exposureSeconds = Math.max(0, exposureSeconds - dt * 2.0);
            if (exposureSeconds == 0) detecting = false;
        }
    }

    public double getFacingAngle() {
        return baseAngle + Math.sin(elapsed * sweepSpeed) * sweepRadians;
    }

    public Point getPosition() {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public String getName() { return name; }
    public boolean isDetecting() { return detecting; }
    public boolean isTracking() { return exposureSeconds > 0; }
    public boolean hasNewDetection() { return newDetection; }
    public double getDetectionProgress() { return exposureSeconds / detectionSeconds; }
}
