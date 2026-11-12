import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class NoiseSystem {
    private final List<NoiseEvent> events = new ArrayList<>();

    public void emit(double x, double y, double radius, double duration, String source) {
        if (radius <= 0 || duration <= 0) throw new IllegalArgumentException("Noise radius and duration must be positive.");
        events.add(new NoiseEvent(x, y, radius, duration, source));
    }

    public void update(double dt) {
        Iterator<NoiseEvent> iterator = events.iterator();
        while (iterator.hasNext()) {
            NoiseEvent event = iterator.next();
            event.remainingSeconds -= dt;
            if (event.remainingSeconds <= 0) iterator.remove();
        }
    }

    public NoiseEvent loudestAt(double x, double y) {
        NoiseEvent loudest = null;
        double loudestStrength = 0;
        for (NoiseEvent event : events) {
            double distance = Math.hypot(x - event.x, y - event.y);
            if (distance > event.radius) continue;
            double strength = (1.0 - distance / event.radius) * event.remainingSeconds / event.durationSeconds;
            if (strength > loudestStrength) {
                loudest = event;
                loudestStrength = strength;
            }
        }
        return loudest;
    }

    public List<NoiseEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public static final class NoiseEvent {
        private final double x;
        private final double y;
        private final double radius;
        private final double durationSeconds;
        private final String source;
        private double remainingSeconds;

        private NoiseEvent(double x, double y, double radius, double durationSeconds, String source) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.durationSeconds = durationSeconds;
            this.remainingSeconds = durationSeconds;
            this.source = source;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getRadius() { return radius; }
        public String getSource() { return source; }
        public double getProgress() { return remainingSeconds / durationSeconds; }
    }
}
