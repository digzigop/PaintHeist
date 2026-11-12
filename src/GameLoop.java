import java.util.function.DoubleConsumer;
import javax.swing.Timer;

/** Fixed-cadence Swing timer loop that passes measured elapsed seconds per tick. */
public final class GameLoop {
    private final Timer timer;
    private long lastNanos;

    public GameLoop(Runnable update) {
        this(dt -> update.run());
    }

    public GameLoop(DoubleConsumer update) {
        timer = new Timer(16, event -> {
            long now = System.nanoTime();
            double dt = lastNanos == 0 ? 1.0 / 60.0 : (now - lastNanos) / 1_000_000_000.0;
            lastNanos = now;
            update.accept(Math.max(0.001, Math.min(dt, 0.05)));
        });
        timer.setCoalesce(true);
    }

    public void start() {
        lastNanos = System.nanoTime();
        if (!timer.isRunning()) timer.start();
    }

    public void stop() {
        timer.stop();
        lastNanos = 0;
    }
}