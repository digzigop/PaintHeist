
import javax.swing.*;
import java.awt.*;

public class LoadingPanel extends JPanel {

    private int progress = 0;
    private final Timer timer;

    public LoadingPanel(Runnable onFinished) {
        setBackground(new Color(18, 18, 24));

        timer = new Timer(35, e -> {
            progress += 1;
            repaint();

            if (progress >= 100) {
                ((Timer) e.getSource()).stop();
                onFinished.run();
            }
        });

        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();

        // Background
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(28, 25, 38),
            width, height, new Color(8, 8, 12)
        );

        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);

        // Decorative frame
        g2.setColor(new Color(190, 155, 95));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(35, 35, width - 70, height - 70);

        // Game title
        g2.setFont(new Font("Serif", Font.BOLD, 48));
        g2.setColor(new Color(235, 198, 130));

        String title = "PAINTHEIST";
        FontMetrics titleMetrics = g2.getFontMetrics();

        g2.drawString(
            title,
            (width - titleMetrics.stringWidth(title)) / 2,
            height / 2 - 55
        );

        // Subtitle
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);

        String subtitle = "Every masterpiece hides a secret.";
        FontMetrics subtitleMetrics = g2.getFontMetrics();

        g2.drawString(
            subtitle,
            (width - subtitleMetrics.stringWidth(subtitle)) / 2,
            height / 2 - 20
        );

        // Loading bar
        int barWidth = 360;
        int barHeight = 12;
        int barX = (width - barWidth) / 2;
        int barY = height / 2 + 35;

        g2.setColor(new Color(65, 60, 70));
        g2.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);

        g2.setColor(new Color(215, 170, 95));
        g2.fillRoundRect(
            barX, barY,
            barWidth * progress / 100,
            barHeight, 8, 8
        );

        // Loading text
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(Color.WHITE);

        String loadingText = "Preparing the heist... " + progress + "%";
        FontMetrics loadingMetrics = g2.getFontMetrics();

        g2.drawString(
            loadingText,
            (width - loadingMetrics.stringWidth(loadingText)) / 2,
            barY + 35
        );

        g2.dispose();
    }
}
