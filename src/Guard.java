import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public final class Guard {
    public enum State { PATROL, INSPECT, ALERT, CHASE, SEARCH, RETURN }

    public static final int WIDTH = 24;
    public static final int HEIGHT = 24;
    private final List<Point> patrolRoute;
    private final int patrolSpeed;
    private final String name;
    private double x;
    private double y;
    private double facingX = 1;
    private double facingY;
    private int patrolIndex;
    private State state = State.PATROL;
    private double stateTimer;
    private double lostSightTimer;
    private double replanTimer;
    private double searchTimer;
    private double searchOffset;
    private double goalX;
    private double goalY;
    private double pathEndX;
    private double pathEndY;
    private boolean pathCalculated;
    private boolean pathReachable;
    private List<Point> path = new ArrayList<>();
    private boolean playerVisible;

    public Guard(String name, double x, double y, int speed, Point... route) {
        this.name = name;
        this.x = x;
        this.y = y;
        patrolSpeed = speed;
        patrolRoute = new ArrayList<>();
        for (Point point : route) patrolRoute.add(new Point(point));
        if (!patrolRoute.isEmpty()) {
            goalX = patrolRoute.get(0).x;
            goalY = patrolRoute.get(0).y;
        }
    }

    public void update(double dt, MuseumMap map, double playerX, double playerY, boolean hidden,
                       boolean sprinting, boolean hasNoise, double noiseX, double noiseY,
                       double noiseRadius) {
        double playerCenterX = playerX + 10;
        double playerCenterY = playerY + 12;
        double distance = Point.distance(centerX(), centerY(), playerCenterX, playerCenterY);
        playerVisible = !hidden && distance <= (sprinting ? 252 : 195)
            && (distance < 45 || inVisionCone(playerCenterX, playerCenterY))
            && map.hasLineOfSight(centerX(), centerY(), playerCenterX, playerCenterY);

        if (playerVisible) {
            state = State.CHASE;
            lostSightTimer = 0;
            goalX = playerX;
            goalY = playerY;
            stateTimer = 0;
            searchTimer = 0;
        } else if (state == State.CHASE) {
            lostSightTimer += dt;
            if (lostSightTimer >= 1.7) {
                state = State.SEARCH;
                searchTimer = 0;
                stateTimer = 0;
            }
        }

        if (!playerVisible && hasNoise && state != State.CHASE && state != State.SEARCH
            && Point.distance(centerX(), centerY(), noiseX, noiseY) <= noiseRadius) {
            goalX = noiseX - WIDTH / 2.0;
            goalY = noiseY - HEIGHT / 2.0;
            state = State.ALERT;
            stateTimer = 0;
        }

        switch (state) {
            case PATROL:
                updatePatrol(dt, map);
                break;
            case ALERT:
                stateTimer += dt;
                if (stateTimer >= 0.8) {
                    state = State.INSPECT;
                    stateTimer = 0;
                }
                break;
            case INSPECT:
                moveTo(dt, map, goalX, goalY, patrolSpeed + 4);
                stateTimer += dt;
                if (stateTimer >= 5 || hasReachedGoal(goalX, goalY)) {
                    state = State.RETURN;
                    stateTimer = 0;
                }
                break;
            case CHASE:
                moveTo(dt, map, goalX, goalY, 138);
                break;
            case SEARCH:
                searchTimer += dt;
                if (searchTimer < 2.5) {
                    moveTo(dt, map, goalX, goalY, patrolSpeed + 10);
                } else if (searchTimer < 5.5) {
                    if (searchTimer - searchOffset > 1.0) {
                        searchOffset = searchTimer;
                        double angle = searchTimer * 2.25;
                        setGoal(goalX + Math.cos(angle) * 55, goalY + Math.sin(angle) * 42);
                    }
                    moveTo(dt, map, goalX, goalY, patrolSpeed);
                } else {
                    state = State.RETURN;
                    stateTimer = 0;
                }
                break;
            case RETURN:
                Point waypoint = patrolRoute.get(patrolIndex);
                moveTo(dt, map, waypoint.x, waypoint.y, patrolSpeed + 4);
                if (hasReachedGoal(waypoint.x, waypoint.y)) {
                    state = State.PATROL;
                    stateTimer = 0;
                }
                break;
        }
    }

    private boolean inVisionCone(double px, double py) {
        double dx = px - centerX();
        double dy = py - centerY();
        double magnitude = Math.hypot(dx, dy);
        if (magnitude < 0.001) return true;
        double dot = (dx * facingX + dy * facingY) / magnitude;
        return dot >= Math.cos(Math.toRadians(58));
    }

    private void updatePatrol(double dt, MuseumMap map) {
        if (patrolRoute.isEmpty()) return;
        Point waypoint = patrolRoute.get(patrolIndex);
        moveTo(dt, map, waypoint.x, waypoint.y, patrolSpeed);
        if (hasReachedGoal(waypoint.x, waypoint.y)) {
            patrolIndex = (patrolIndex + 1) % patrolRoute.size();
            setGoal(patrolRoute.get(patrolIndex).x, patrolRoute.get(patrolIndex).y);
        }
    }

    private void moveTo(double dt, MuseumMap map, double targetX, double targetY, double speed) {
        replanTimer -= dt;
        if (path.isEmpty() || replanTimer <= 0) {
            path = map.findPath(x, y, targetX, targetY, WIDTH, HEIGHT);
            replanTimer = 0.75;
            pathCalculated = true;
            pathReachable = !path.isEmpty();
            if (pathReachable) {
                Point endpoint = path.get(path.size() - 1);
                pathEndX = endpoint.x;
                pathEndY = endpoint.y;
            }
            if (!path.isEmpty() && Point.distance(x, y, path.get(0).x, path.get(0).y) < 8) path.remove(0);
        }
        while (!path.isEmpty() && Point.distance(x, y, path.get(0).x, path.get(0).y) < 5) path.remove(0);
        if (path.isEmpty()) return;
        Point point = path.get(0);
        double dx = point.x - x;
        double dy = point.y - y;
        double distance = Math.hypot(dx, dy);
        if (distance < 0.01) return;
        double step = Math.min(distance, speed * dt);
        double vx = dx / distance * step;
        double vy = dy / distance * step;
        moveAxis(vx, 0, map);
        moveAxis(0, vy, map);
        facingX = dx / distance;
        facingY = dy / distance;
    }

    private void moveAxis(double dx, double dy, MuseumMap map) {
        if (dx == 0 && dy == 0) return;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 3.0));
        double sx = dx / steps;
        double sy = dy / steps;
        for (int i = 0; i < steps; i++) {
            double nextX = x + sx;
            double nextY = y + sy;
            if (map.canStandAt(nextX, nextY, WIDTH, HEIGHT)) {
                x = nextX;
                y = nextY;
            } else {
                path.clear();
                return;
            }
        }
    }

    private boolean atGoal(double targetX, double targetY) {
        return Point.distance(x, y, targetX, targetY) <= 14;
    }

    private boolean hasReachedGoal(double targetX, double targetY) {
        return atGoal(targetX, targetY)
            || (pathCalculated && pathReachable && path.isEmpty()
                && Point.distance(x, y, pathEndX, pathEndY) <= 8);
    }

    private void setGoal(double x, double y) {
        goalX = x;
        goalY = y;
        path.clear();
        pathCalculated = false;
        pathReachable = false;
        replanTimer = 0;
    }

    public void setSearchTarget(double x, double y) {
        setGoal(x, y);
        searchOffset = 0;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double centerX() { return x + WIDTH / 2.0; }
    public double centerY() { return y + HEIGHT / 2.0; }
    public State getState() { return state; }
    public String getName() { return name; }
    public boolean isPlayerVisible() { return playerVisible; }
    public Point getFacing() { return new Point((int) Math.round(facingX * 100), (int) Math.round(facingY * 100)); }
}
