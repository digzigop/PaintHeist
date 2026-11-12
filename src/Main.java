
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

public class Main {
    private static JFrame window;
    private static Rectangle windowedBounds;
    private static boolean fullScreen;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            window = new JFrame("PaintHeist");

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(true);

            StudioSplashPanel splashScreen = new StudioSplashPanel(() -> {
                LoadingPanel loadingScreen = new LoadingPanel(assets -> {
                    try {
                        window.setContentPane(new GamePanel(assets));
                        window.revalidate();
                        window.repaint();
                        window.requestFocus();
                    } catch (RuntimeException exception) {
                        showStartupError(exception);
                    }
                });
                window.setContentPane(loadingScreen);
                window.revalidate();
                window.repaint();

                SwingWorker<GamePanel.Assets, Void> assetLoader =
                    new SwingWorker<GamePanel.Assets, Void>() {
                        @Override
                        protected GamePanel.Assets doInBackground() {
                            return GamePanel.loadAssets();
                        }

                        @Override
                        protected void done() {
                            try {
                                loadingScreen.setAssets(get());
                            } catch (InterruptedException exception) {
                                Thread.currentThread().interrupt();
                                showStartupError(exception);
                            } catch (ExecutionException exception) {
                                showStartupError(exception.getCause());
                            }
                        }
                    };
                assetLoader.execute();
            });

            window.setContentPane(splashScreen);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    private static void showStartupError(Throwable error) {
        error.printStackTrace();
        StringBuilder details = new StringBuilder(error.toString());
        Throwable cause = error.getCause();
        while (cause != null) {
            details.append("\nCaused by: ").append(cause);
            for (Throwable suppressed : cause.getSuppressed()) {
                details.append("\nSuppressed: ").append(suppressed);
            }
            cause = cause.getCause();
        }
        JOptionPane.showMessageDialog(window,
            "The game could not finish loading:\n" + details,
            "PaintHeist startup error", JOptionPane.ERROR_MESSAGE);
    }

    static void toggleFullScreen() {
        if (window == null) return;

        if (fullScreen) {
            window.dispose();
            window.setUndecorated(false);
            window.setResizable(true);
            window.setBounds(windowedBounds);
            window.setVisible(true);
            fullScreen = false;
        } else {
            windowedBounds = window.getBounds();
            Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            window.dispose();
            window.setUndecorated(true);
            window.setResizable(false);
            window.setBounds(screenBounds);
            window.setVisible(true);
            fullScreen = true;
        }

        window.revalidate();
        window.repaint();
        window.requestFocus();
    }
}
