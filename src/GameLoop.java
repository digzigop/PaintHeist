import javax.swing.Timer;

public final class GameLoop {
    private final Timer timer;

    public GameLoop(Runnable update) {
        timer = new Timer(16, event -> update.run());
        timer.setCoalesce(true);
    }

    public void start() {
        if (!timer.isRunning()) timer.start();
    }

    public void stop() {
        timer.stop();
    }
}