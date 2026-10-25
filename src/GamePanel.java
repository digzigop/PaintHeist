
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JPanel;

public class GamePanel extends JPanel {

    public GamePanel() {
        setBackground(new Color(35, 32, 40));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(65, 57, 55));
        g.fillRect(0, 0, getWidth(), 430);

        g.setColor(new Color(100, 75, 55));
        g.fillRect(0, 430, getWidth(), 170);

        g.setColor(new Color(210, 170, 95));
        g.fillRect(365, 130, 230, 250);

        g.setColor(new Color(235, 220, 185));
        g.fillRect(380, 145, 200, 220);

        g.setColor(new Color(75, 110, 95));
        g.fillOval(410, 180, 140, 140);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.drawString("PAINTHEIST", 390, 70);

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Museum Prototype", 400, 100);
    }
}
