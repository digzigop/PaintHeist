import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class LoadingPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int BAR_X = 300;
    private static final int BAR_Y = 440;
    private static final int BAR_WIDTH = 360;
    private int progress;
    private final Timer timer;
    private final Runnable onFinished;
    private final Color gold = new Color(225, 181, 100);

    public LoadingPanel(Runnable onFinished) {
        this.onFinished = onFinished;
        setBackground(new Color(12, 12, 17));
        setPreferredSize(new Dimension(960, 600));
        setDoubleBuffered(true);

        timer = new Timer(35, event -> {
            progress = Math.min(100, progress + 1);
            repaint();
            if (progress == 100) {
                ((Timer) event.getSource()).stop();
                SwingUtilities.invokeLater(this.onFinished);
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        int width = getWidth();
        int height = getHeight();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setPaint(new GradientPaint(0, 0, new Color(28, 25, 36), width, height, new Color(8, 8, 12)));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(130, 103, 64));
        g.drawRect(30, 30, width - 60, height - 60);
        g.setColor(gold);
        g.fillRect(30, 30, 70, 3);
        g.fillRect(30, 30, 3, 70);
        g.fillRect(width - 100, height - 33, 70, 3);
        g.fillRect(width - 33, height - 100, 3, 70);

        g.setFont(new Font("Monospaced", Font.BOLD, 34));
        g.setColor(gold);
        drawCentered(g, "PAINTHEIST", width / 2, 105);
        drawLoadingArtwork(g, width / 2, 190);

        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(new Color(204, 197, 207));
        drawCentered(g, "Every masterpiece hides a secret.", width / 2, 355);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(158, 151, 166));
        drawCentered(g, "SECURITY IS CLOSING IN...", width / 2, 384);

        g.setColor(new Color(48, 43, 53));
        g.fillRect(BAR_X, BAR_Y, BAR_WIDTH, 12);
        g.setColor(gold);
        g.fillRect(BAR_X, BAR_Y, BAR_WIDTH * progress / 100, 12);
        g.setColor(new Color(115, 94, 65));
        g.drawRect(BAR_X, BAR_Y, BAR_WIDTH, 12);
        drawRunningPair(g);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(Color.WHITE);
        drawCentered(g, "PREPARING THE HEIST... " + progress + "%", width / 2, BAR_Y + 34);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(128, 121, 135));
        drawCentered(g, "404 TULOG PRODUCTIONS", width / 2, height - 48);
        g.dispose();
    }

    private void drawLoadingArtwork(Graphics2D g, int centerX, int topY) {
        int x = centerX - 57;
        int y = topY;
        g.setColor(new Color(40, 30, 31));
        g.fillRect(x - 9, y - 9, 132, 143);
        g.setColor(new Color(135, 85, 49));
        g.fillRect(x - 5, y - 5, 124, 135);
        g.setColor(new Color(231, 190, 105));
        g.fillRect(x, y, 114, 125);
        g.setColor(new Color(43, 65, 77));
        g.fillRect(x + 7, y + 7, 100, 111);
        g.setColor(new Color(239, 191, 111));
        g.fillRect(x + 22, y + 22, 27, 27);
        g.setColor(new Color(40, 56, 68));
        g.fillRect(x + 10, y + 70, 53, 40);
        g.setColor(new Color(64, 94, 69));
        g.fillRect(x + 58, y + 58, 47, 52);
        g.setColor(new Color(212, 151, 84));
        g.fillRect(x + 74, y + 48, 13, 30);
        g.setColor(new Color(248, 217, 153));
        g.drawRect(x - 9, y - 9, 131, 142);
    }

    private void drawRunningPair(Graphics2D g) {
        int travel = BAR_WIDTH - 28;
        int thiefCenterX = BAR_X + 14 + travel * progress / 100;
        int guardCenterX = Math.max(BAR_X + 10, thiefCenterX - 42);
        int step = progress / 8 % 2;
        drawRunner(g, guardCenterX, BAR_Y - 9, step, false);
        drawRunner(g, thiefCenterX, BAR_Y - 9, step, true);
    }

    private void drawRunner(Graphics2D g, int centerX, int groundY, int step, boolean thief) {
        int x = centerX - 9;
        Color shirt = thief ? new Color(57, 67, 111) : new Color(71, 121, 83);
        Color highlight = thief ? new Color(113, 120, 183) : new Color(132, 180, 103);
        Color skin = new Color(222, 164, 119);
        Color dark = new Color(30, 27, 35);

        g.setColor(new Color(0, 0, 0, 105));
        g.fillOval(x - 3, groundY + 4, 25, 6);
        g.setColor(dark);
        g.fillRect(x + (step == 0 ? 2 : 5), groundY - 5, 5, 8);
        g.fillRect(x + (step == 0 ? 13 : 10), groundY - 5, 5, 8);
        g.setColor(shirt);
        g.fillRect(x + 3, groundY - 19, 14, 15);
        g.setColor(highlight);
        g.fillRect(x + 5, groundY - 17, 7, 8);
        g.setColor(skin);
        g.fillRect(x + 5, groundY - 29, 11, 11);
        if (thief) {
            g.setColor(dark);
            g.fillRect(x + 3, groundY - 30, 15, 4);
            g.fillRect(x + 7, groundY - 32, 8, 3);
            g.fillRect(x + 5, groundY - 22, 12, 3);
            g.setColor(new Color(207, 151, 78));
            g.fillRect(x + 14, groundY - 15, 7, 7);
        } else {
            g.setColor(new Color(225, 190, 104));
            g.fillRect(x + 4, groundY - 31, 14, 4);
            g.fillRect(x + 8, groundY - 19, 3, 5);
        }
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
