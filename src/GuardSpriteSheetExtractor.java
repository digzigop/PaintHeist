import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.imageio.ImageIO;

public final class GuardSpriteSheetExtractor {
    private static final int FRAME_WIDTH = 96;
    private static final int FRAME_HEIGHT = 192;
    private static final int FRAME_SLOTS = 4;

    private static final String[] ACTIONS = {"idle", "walk", "run"};
    private static final int[][] FRAME_COUNTS = {
        {4, 4, 4, 4},
        {4, 4, 4, 4},
        {4, 4, 4, 4}
    };
    private static final double[][][] FRAME_CENTERS = {
        {
            {.068, .122, .176, .229}, {.306, .358, .410, .466},
            {.550, .602, .654, .706}, {.799, .852, .904, .955}
        },
        {
            {.061, .113, .165, .218, .270},
            {.312, .365, .417, .469, .521},
            {.563, .615, .667, .719, .771},
            {.813, .865, .917, .955, .985}
        },
        {
            {.052, .099, .146, .193, .240, .287},
            {.312, .356, .400, .444, .488, .532},
            {.550, .598, .646, .694, .742, .790},
            {.808, .850, .892, .934, .966, .990}
        }
    };
    private static final double[] ROW_TOPS = {.052, .250, .435};
    private static final double[] ROW_HEIGHTS = {.145, .130, .135};
    private static final double[] FRAME_WIDTHS = {.060, .052, .052};

    private GuardSpriteSheetExtractor() { }

    public static int getFrameCount(int action, int direction) {
        return FRAME_COUNTS[action][direction];
    }

    public static void main(String[] args) throws IOException {
        File projectRoot = args.length == 0 ? new File(".") : new File(args[0]);
        generate(projectRoot);
    }

    public static void generate(File projectRoot) throws IOException {
        File sheetFile = new File(projectRoot,
            "assets/sprites/Guard/Pixel Guard Animation Sprite Sheet.png");
        BufferedImage[][][] frames = loadFrames(sheetFile);
        File outputDirectory = new File(projectRoot, "assets/sprites/Guard");
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IOException("Unable to create " + outputDirectory.getAbsolutePath());
        }

        for (int action = 0; action < ACTIONS.length; action++) {
            BufferedImage atlas = new BufferedImage(
                FRAME_WIDTH * FRAME_SLOTS, FRAME_HEIGHT * 4, BufferedImage.TYPE_INT_ARGB);
            for (int direction = 0; direction < 4; direction++) {
                for (int frame = 0; frame < FRAME_COUNTS[action][direction]; frame++) {
                    Graphics2D graphics = atlas.createGraphics();
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    graphics.drawImage(frames[action][direction][frame], frame * FRAME_WIDTH,
                        direction * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT, null);
                    graphics.dispose();
                }
            }
            File output = new File(outputDirectory, ACTIONS[action] + ".png");
            if (!ImageIO.write(atlas, "png", output)) {
                throw new IOException("No PNG writer is available for " + output.getAbsolutePath());
            }
            System.out.println("Created " + output.getAbsolutePath());
        }
    }

    public static BufferedImage[][][] loadFrames(File sheetFile) throws IOException {
        BufferedImage sheet = ImageIO.read(sheetFile);
        if (sheet == null) {
            throw new IOException("Unable to read " + sheetFile.getAbsolutePath());
        }
        return loadFrames(sheet);
    }

    public static BufferedImage[][][] loadFrames(BufferedImage sheet) throws IOException {
        BufferedImage[][][] frames = new BufferedImage[ACTIONS.length][4][FRAME_SLOTS];
        for (int action = 0; action < ACTIONS.length; action++) {
            for (int direction = 0; direction < 4; direction++) {
                for (int frame = 0; frame < FRAME_COUNTS[action][direction]; frame++) {
                    frames[action][direction][frame] = cropFrame(sheet,
                        FRAME_CENTERS[action][direction][frame], ROW_TOPS[action],
                        ROW_HEIGHTS[action], FRAME_WIDTHS[action]);
                }
            }
        }
        return frames;
    }

    public static BufferedImage[][][] loadAtlases(BufferedImage[] atlases) throws IOException {
        if (atlases.length != ACTIONS.length) {
            throw new IOException("Expected " + ACTIONS.length + " guard animation atlases.");
        }
        BufferedImage[][][] frames = new BufferedImage[ACTIONS.length][4][FRAME_SLOTS];
        for (int action = 0; action < ACTIONS.length; action++) {
            BufferedImage atlas = atlases[action];
            int expectedWidth = FRAME_WIDTH * FRAME_SLOTS;
            int expectedHeight = FRAME_HEIGHT * 4;
            if (atlas == null || atlas.getWidth() != expectedWidth || atlas.getHeight() != expectedHeight) {
                throw new IOException("Unexpected " + ACTIONS[action] + " guard atlas dimensions.");
            }
            for (int direction = 0; direction < 4; direction++) {
                for (int frame = 0; frame < FRAME_COUNTS[action][direction]; frame++) {
                    frames[action][direction][frame] = atlas.getSubimage(
                        frame * FRAME_WIDTH, direction * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
                }
            }
        }
        return frames;
    }

    private static BufferedImage cropFrame(BufferedImage sheet, double centerX, double top,
                                           double height, double width) {
        int sourceWidth = Math.max(1, (int) Math.round(sheet.getWidth() * width));
        int sourceHeight = Math.max(1, (int) Math.round(sheet.getHeight() * height));
        int sourceX = (int) Math.round(sheet.getWidth() * centerX) - sourceWidth / 2;
        int sourceY = (int) Math.round(sheet.getHeight() * top);
        sourceX = Math.max(0, Math.min(sourceX, sheet.getWidth() - sourceWidth));
        sourceY = Math.max(0, Math.min(sourceY, sheet.getHeight() - sourceHeight));

        BufferedImage crop = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
        int background = sheet.getRGB(sourceX, sourceY);
        int[] pixels = sheet.getRGB(sourceX, sourceY, sourceWidth, sourceHeight, null, 0, sourceWidth);
        boolean[] visited = new boolean[pixels.length];
        Queue<Integer> queue = new ArrayDeque<>();
        for (int x = 0; x < sourceWidth; x++) {
            enqueueBackground(pixels, visited, queue, x, background);
            enqueueBackground(pixels, visited, queue, (sourceHeight - 1) * sourceWidth + x, background);
        }
        for (int y = 1; y < sourceHeight - 1; y++) {
            enqueueBackground(pixels, visited, queue, y * sourceWidth, background);
            enqueueBackground(pixels, visited, queue, y * sourceWidth + sourceWidth - 1, background);
        }
        while (!queue.isEmpty()) {
            int index = queue.remove();
            pixels[index] = 0;
            int x = index % sourceWidth;
            if (x > 0) enqueueBackground(pixels, visited, queue, index - 1, background);
            if (x + 1 < sourceWidth) enqueueBackground(pixels, visited, queue, index + 1, background);
            if (index >= sourceWidth) enqueueBackground(pixels, visited, queue, index - sourceWidth, background);
            if (index + sourceWidth < pixels.length) {
                enqueueBackground(pixels, visited, queue, index + sourceWidth, background);
            }
        }
        crop.setRGB(0, 0, sourceWidth, sourceHeight, pixels, 0, sourceWidth);

        BufferedImage frame = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = frame.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double scale = Math.min((FRAME_WIDTH - 8.0) / sourceWidth,
            (FRAME_HEIGHT - 8.0) / sourceHeight);
        int scaledWidth = (int) Math.round(sourceWidth * scale);
        int scaledHeight = (int) Math.round(sourceHeight * scale);
        graphics.drawImage(crop, (FRAME_WIDTH - scaledWidth) / 2,
            FRAME_HEIGHT - scaledHeight, scaledWidth, scaledHeight, null);
        graphics.dispose();
        return frame;
    }

    private static void enqueueBackground(int[] pixels, boolean[] visited, Queue<Integer> queue,
                                          int index, int background) {
        if (visited[index] || !isBackground(pixels[index], background)) return;
        visited[index] = true;
        queue.add(index);
    }

    private static boolean isBackground(int color, int background) {
        int red = ((color >> 16) & 0xFF) - ((background >> 16) & 0xFF);
        int green = ((color >> 8) & 0xFF) - ((background >> 8) & 0xFF);
        int blue = (color & 0xFF) - (background & 0xFF);
        return red * red + green * green + blue * blue < 52 * 52;
    }
}
