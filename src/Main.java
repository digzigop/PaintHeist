
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("PaintHeist");

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            StudioSplashPanel splashScreen = new StudioSplashPanel(() -> {
                LoadingPanel loadingScreen = new LoadingPanel(() -> {
                    window.setContentPane(new GamePanel());
                    window.revalidate();
                    window.repaint();
                });
                window.setContentPane(loadingScreen);
                window.revalidate();
                window.repaint();
            });

            window.setContentPane(splashScreen);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
