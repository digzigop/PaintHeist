
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("PaintHeist");

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setSize(960, 600);
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.add(new GamePanel());
            window.setVisible(true);
        });
    }
}
