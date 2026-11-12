import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class StudioSplashPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 960;
    private static final int HEIGHT = 600;
    private static final int STATIC_SPLASH_DURATION_MILLIS = 2500;
    private final Image logoImage;
    private final Runnable onFinished;

    public StudioSplashPanel(Runnable onFinished) {
        this.onFinished = onFinished;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(10, 11, 17));
        setDoubleBuffered(true);

        URL logoUrl = getClass().getResource("/logo.gif");
        if (logoUrl == null) logoUrl = getClass().getResource("logo.gif");
        File gifFile = findSplashGif();
        Image loadedImage = logoUrl == null ? null : new ImageIcon(logoUrl).getImage();
        if (logoUrl == null && gifFile != null && gifFile.isFile()) {
            try {
                logoUrl = gifFile.toURI().toURL();
            } catch (java.net.MalformedURLException exception) {
                throw new IllegalStateException("Could not locate the studio splash GIF.", exception);
            }
            loadedImage = new ImageIcon(logoUrl).getImage();
        }
        logoImage = loadedImage;

        int loopDuration = logoUrl == null ? STATIC_SPLASH_DURATION_MILLIS
            : Math.max(3000, getGifLoopDurationMillis(logoUrl));
        Timer timer = new Timer(loopDuration, event -> {
            ((Timer) event.getSource()).stop();
            SwingUtilities.invokeLater(this.onFinished);
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        double offsetX = (getWidth() - WIDTH * scale) / 2;
        double offsetY = (getHeight() - HEIGHT * scale) / 2;
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setPaint(new GradientPaint(0, 0, new Color(31, 28, 38), WIDTH, HEIGHT, new Color(7, 9, 15)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(119, 91, 59));
        g.drawRect(30, 30, WIDTH - 60, HEIGHT - 60);
        g.setColor(new Color(225, 181, 100));
        g.fillRect(30, 30, 68, 3);
        g.fillRect(30, 30, 3, 68);
        g.fillRect(WIDTH - 98, HEIGHT - 33, 68, 3);
        g.fillRect(WIDTH - 33, HEIGHT - 98, 3, 68);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(new Color(197, 190, 201));
        drawCentered(g, "404Tulog Productions", WIDTH / 2, 103);
        g.setColor(new Color(225, 181, 100));
        g.fillRect(WIDTH / 2 - 62, 119, 124, 2);

        int logoSize = 250;
        int logoX = (WIDTH - logoSize) / 2;
        int logoY = 145;
        if (logoImage != null) {
            g.drawImage(logoImage, logoX, logoY, logoSize, logoSize, this);
        } else {
            g.setColor(new Color(45, 42, 59));
            g.fillRect(logoX, logoY, logoSize, logoSize);
            g.setColor(new Color(225, 181, 100));
            g.drawRect(logoX, logoY, logoSize, logoSize);
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            drawCentered(g, "404TULOG", WIDTH / 2, logoY + 120);
        }

        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 219, 142));
        drawCentered(g, "PAINTHEIST", WIDTH / 2, 443);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(204, 197, 207));
        drawCentered(g, "A 404Tulog Productions Game", WIDTH / 2, 468);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(158, 151, 166));
        drawCentered(g, "PRESENTS", WIDTH / 2, 526);
        g.dispose();
    }

    private static int getGifLoopDurationMillis(URL logoUrl) {
        try (InputStream stream = logoUrl.openStream();
             ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            if (imageStream == null) {
                throw new IOException("Unable to read the studio splash GIF.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                throw new IOException("No GIF reader is available for the studio splash.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, false, false);
                if (!"gif".equalsIgnoreCase(reader.getFormatName())) {
                    return STATIC_SPLASH_DURATION_MILLIS;
                }
                int frameCount = reader.getNumImages(true);
                long durationMillis = 0;
                for (int i = 0; i < frameCount; i++) {
                    Node root = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    Node control = findNode(root, "GraphicControlExtension");
                    if (control == null) continue;
                    NamedNodeMap attributes = control.getAttributes();
                    Node delay = attributes.getNamedItem("delayTime");
                    if (delay != null) {
                        durationMillis += Math.max(2, Integer.parseInt(delay.getNodeValue())) * 10L;
                    }
                }
                if (durationMillis == 0) {
                    throw new IOException("The studio splash GIF has no readable frame durations.");
                }
                return (int) Math.min(Integer.MAX_VALUE, durationMillis);
            } finally {
                reader.dispose();
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not read the studio splash GIF animation.", exception);
        }
    }

    private static Node findNode(Node parent, String name) {
        if (name.equals(parent.getNodeName())) return parent;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node match = findNode(child, name);
            if (match != null) return match;
        }
        return null;
    }

    private static File findSplashGif() {
        for (String candidatePath : new String[] {"src/logo.gif", "logo.gif"}) {
            File direct = new File(candidatePath);
            if (direct.isFile()) return direct;
            try {
                java.nio.file.Path classLocation = java.nio.file.Paths.get(
                    StudioSplashPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                for (java.nio.file.Path cur = classLocation; cur != null; cur = cur.getParent()) {
                    File candidate = cur.resolve(candidatePath).toFile();
                    if (candidate.isFile()) return candidate;
                }
            } catch (java.net.URISyntaxException ignored) {
                // Fall back to static splash screen if location cannot be resolved.
            }
        }
        return null;
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
