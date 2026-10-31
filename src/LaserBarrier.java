import java.awt.Rectangle;

public final class LaserBarrier {
    public enum State { INACTIVE, WARNING, ACTIVE, DISABLED }

    private final String name;
    private final Rectangle bounds;
    private final double periodSeconds;
    private final double warningSeconds;
    private final double activeSeconds;
    private final double phaseOffsetSeconds;
    private double elapsedSeconds;
    private State state = State.INACTIVE;

    public LaserBarrier(String name, Rectangle bounds, double periodSeconds,
                        double warningSeconds, double activeSeconds, double phaseOffsetSeconds) {
        if (periodSeconds <= warningSeconds + activeSeconds || warningSeconds <= 0 || activeSeconds <= 0) {
            throw new IllegalArgumentException("Laser cycle needs inactive, warning, and active time.");
        }
        this.name = name;
        this.bounds = new Rectangle(bounds);
        this.periodSeconds = periodSeconds;
        this.warningSeconds = warningSeconds;
        this.activeSeconds = activeSeconds;
        this.phaseOffsetSeconds = phaseOffsetSeconds;
    }

    public void update(double dt, boolean securityDisabled) {
        if (securityDisabled) {
            if (state != State.DISABLED) elapsedSeconds = 0;
            state = State.DISABLED;
            return;
        }
        elapsedSeconds += dt;

        double phase = (elapsedSeconds + phaseOffsetSeconds) % periodSeconds;
        double warningStart = periodSeconds - warningSeconds - activeSeconds;
        if (phase >= periodSeconds - activeSeconds) {
            state = State.ACTIVE;
        } else if (phase >= warningStart) {
            state = State.WARNING;
        } else {
            state = State.INACTIVE;
        }
    }

    public String getName() { return name; }
    public Rectangle getBounds() { return new Rectangle(bounds); }
    public State getState() { return state; }
    public boolean isActive() { return state == State.ACTIVE; }
    public double getStateProgress() {
        double phase = (elapsedSeconds + phaseOffsetSeconds) % periodSeconds;
        if (state == State.WARNING) {
            double warningStart = periodSeconds - warningSeconds - activeSeconds;
            return Math.max(0, Math.min(1, (phase - warningStart) / warningSeconds));
        }
        if (state == State.ACTIVE) {
            return Math.max(0, Math.min(1, (phase - (periodSeconds - activeSeconds)) / activeSeconds));
        }
        return 0;
    }

    public static final class Data {
        private final String name;
        private final Rectangle bounds;
        private final double periodSeconds;
        private final double warningSeconds;
        private final double activeSeconds;
        private final double phaseOffsetSeconds;

        public Data(String name, Rectangle bounds, double periodSeconds, double warningSeconds,
                    double activeSeconds, double phaseOffsetSeconds) {
            this.name = name;
            this.bounds = new Rectangle(bounds);
            this.periodSeconds = periodSeconds;
            this.warningSeconds = warningSeconds;
            this.activeSeconds = activeSeconds;
            this.phaseOffsetSeconds = phaseOffsetSeconds;
        }

        public LaserBarrier createBarrier() {
            return new LaserBarrier(name, bounds, periodSeconds, warningSeconds, activeSeconds, phaseOffsetSeconds);
        }
    }
}
