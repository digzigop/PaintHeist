import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MuseumLevel {
    private static final List<MuseumLevel> LEVELS = createLevels();

    private final int id;
    private final String name;
    private final String difficulty;
    private final String challenge;
    private final Color floorA;
    private final Color floorB;
    private final List<Rectangle> walls;
    private final List<Rectangle> cases;
    private final List<Rectangle> benches;
    private final List<Rectangle> statues;
    private final List<Rectangle> lasers;
    private final List<Rectangle> switches;
    private final List<Point> exits;
    private final List<Point> paintingPositions;
    private final List<String> paintingNames;
    private final List<GuardRoute> guards;
    private final List<HideSpotData> hideSpots;
    private final List<LaserBarrier.Data> laserPatterns;
    private final List<MovableObject.Data> movableObjects;
    private final List<SecretPassage.Data> secretPassages;
    private final Point playerStart;
    private final int missionSeconds;

    private MuseumLevel(int id, String name, String difficulty, String challenge,
                        Color floorA, Color floorB, int missionSeconds,
                        Point playerStart, List<Rectangle> walls, List<Rectangle> cases,
                        List<Rectangle> benches, List<Rectangle> statues, List<Rectangle> lasers,
                        List<Rectangle> switches, List<Point> exits, List<Point> paintingPositions,
                        List<String> paintingNames, List<GuardRoute> guards, List<HideSpotData> hideSpots) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.challenge = challenge;
        this.floorA = floorA;
        this.floorB = floorB;
        this.missionSeconds = missionSeconds;
        this.playerStart = new Point(playerStart);
        this.walls = copyRectangles(walls);
        this.cases = copyRectangles(cases);
        this.benches = copyRectangles(benches);
        this.statues = copyRectangles(statues);
        this.lasers = copyRectangles(lasers);
        this.switches = copyRectangles(switches);
        this.exits = copyPoints(exits);
        this.paintingPositions = copyPoints(paintingPositions);
        this.paintingNames = Collections.unmodifiableList(new ArrayList<>(paintingNames));
        this.guards = Collections.unmodifiableList(new ArrayList<>(guards));
        this.hideSpots = Collections.unmodifiableList(new ArrayList<>(hideSpots));
        this.laserPatterns = createLaserPatterns(id, this.lasers);
        this.movableObjects = createMovableObjects(id);
        this.secretPassages = createSecretPassages(id);
    }

    public static List<MuseumLevel> all() {
        return LEVELS;
    }

    public static MuseumLevel get(int index) {
        if (index < 0 || index >= LEVELS.size()) throw new IllegalArgumentException("Invalid museum level: " + index);
        return LEVELS.get(index);
    }

    private static List<MuseumLevel> createLevels() {
        List<MuseumLevel> levels = new ArrayList<>();

        levels.add(new MuseumLevel(
            0, "The Classic Gallery", "EASY", "Learn camera sweeps and move a noisy crate",
            new Color(125, 91, 70), new Color(114, 81, 65), 150,
            p(55, 108),
            rects(r(24, 248, 138, 18), r(207, 248, 235, 18), r(505, 248, 230, 18), r(785, 248, 151, 18),
                r(310, 90, 18, 72), r(310, 207, 18, 41), r(310, 266, 18, 111), r(310, 425, 18, 115),
                r(632, 90, 18, 72), r(632, 207, 18, 41), r(632, 266, 18, 111), r(632, 425, 18, 115)),
            rects(r(93, 173, 100, 39), r(421, 347, 110, 40)),
            rects(r(44, 294, 52, 23)),
            rects(),
            rects(), rects(),
            points(p(42, 474)),
            points(p(112, 112)),
            strings("Moonlit Orchard"),
            routes(route("Gallery Guard", 188, 118, 72, p(188, 118), p(270, 118), p(260, 204), p(62, 218))),
            hides(hide("Gallery wardrobe", 250, 316, 44, 52), hide("Storage cabinet", 680, 431, 46, 52))
        ));

        levels.add(new MuseumLevel(
            1, "The Grand Museum", "MEDIUM", "A sweeping camera watches the connected exhibition halls",
            new Color(132, 127, 119), new Color(112, 111, 108), 190,
            p(55, 108),
            rects(r(295, 90, 18, 118), r(295, 252, 18, 112), r(295, 407, 18, 133),
                r(638, 90, 18, 75), r(638, 207, 18, 137), r(638, 386, 18, 154),
                r(24, 267, 140, 18), r(210, 267, 210, 18), r(482, 267, 210, 18), r(755, 267, 181, 18)),
            rects(r(82, 158, 88, 42), r(362, 151, 104, 38), r(705, 151, 100, 38),
                r(113, 350, 104, 39), r(397, 345, 104, 39), r(704, 350, 104, 39)),
            rects(r(55, 300, 58, 24), r(840, 308, 52, 23), r(480, 456, 58, 22)),
            rects(r(534, 316, 26, 34)),
            rects(), rects(),
            points(p(42, 474), p(880, 474)),
            points(p(91, 110), p(437, 110), p(742, 110)),
            strings("The First Light", "Marble Garden", "Portrait in Blue"),
            routes(
                route("West Wing", 196, 115, 75, p(196, 115), p(268, 115), p(268, 230), p(54, 230)),
                route("East Wing", 780, 304, 70, p(780, 304), p(880, 304), p(880, 430), p(680, 430)),
                route("Grand Hall", 430, 444, 68, p(430, 444), p(594, 444), p(594, 505), p(350, 505))
            ),
            hides(hide("West wardrobe", 220, 318, 46, 52), hide("Restoration cabinet", 674, 430, 46, 52),
                hide("East locker", 868, 395, 44, 50))
        ));

        levels.add(new MuseumLevel(
            2, "The Royal Exhibition", "HARD", "Read the laser cycle, distract guards, or find a secret route",
            new Color(108, 66, 64), new Color(82, 50, 57), 220,
            p(55, 108),
            rects(r(255, 90, 20, 78), r(255, 310, 20, 62),
                r(685, 90, 20, 72), r(685, 315, 20, 58),
                r(24, 250, 145, 18), r(738, 250, 198, 18),
                r(444, 90, 18, 51), r(444, 445, 18, 82)),
            rects(r(92, 161, 98, 40), r(342, 153, 98, 40), r(738, 155, 110, 40),
                r(105, 350, 110, 40), r(535, 354, 105, 40), r(780, 350, 104, 40)),
            rects(r(52, 300, 52, 23), r(862, 303, 52, 23)),
            rects(r(566, 111, 28, 36), r(574, 431, 32, 38), r(347, 433, 30, 34)),
            rects(r(478, 288, 8, 118)),
            rects(r(466, 434, 38, 28)),
            points(p(42, 474)),
            points(p(115, 110), p(371, 110), p(765, 110), p(545, 472)),
            strings("The Crown's Reflection", "Gilded Promise", "Queen at Dusk", "The Royal Seal"),
            routes(
                route("West Sentinel", 180, 118, 78, p(180, 118), p(238, 118), p(238, 220), p(58, 220)),
                route("Royal Guard", 740, 304, 78, p(740, 304), p(890, 304), p(890, 440), p(720, 440)),
                route("Throne Room", 520, 440, 82, p(520, 440), p(635, 440), p(635, 500), p(490, 500)),
                route("North Passage", 510, 118, 72, p(510, 118), p(640, 118), p(640, 220), p(480, 220))
            ),
            hides(hide("Royal wardrobe", 205, 316, 44, 50), hide("Servants' cabinet", 726, 434, 44, 48))
        ));

        levels.add(new MuseumLevel(
            3, "The Museum of Ancient Art", "HARD", "Timed lasers, moving cover, and a hidden pillar passage",
            new Color(123, 108, 83), new Color(105, 91, 72), 230,
            p(55, 108),
            rects(r(218, 90, 18, 76), r(218, 300, 18, 66),
                r(492, 90, 18, 71), r(492, 298, 18, 68),
                r(756, 90, 18, 85), r(756, 307, 18, 70),
                r(295, 355, 18, 70), r(382, 355, 18, 70), r(579, 355, 18, 70)),
            rects(r(83, 151, 92, 39), r(336, 145, 98, 41), r(576, 152, 100, 39), r(812, 148, 92, 41),
                r(80, 352, 97, 40), r(343, 450, 98, 40), r(656, 448, 98, 40)),
            rects(r(42, 293, 56, 23), r(850, 298, 54, 23)),
            rects(r(270, 292, 34, 45), r(410, 292, 34, 45), r(535, 294, 36, 44),
                r(650, 290, 34, 44), r(463, 445, 35, 44)),
            rects(r(625, 277, 8, 98), r(625, 424, 8, 98)),
            rects(r(606, 438, 38, 26)),
            points(p(42, 474), p(880, 474)),
            points(p(100, 108), p(353, 106), p(593, 108), p(829, 108), p(100, 430)),
            strings("Burial at Dawn", "The Bronze Oracle", "Amber Dynasty", "River of Kings", "Hidden Pharaoh"),
            routes(
                route("Antiquities", 150, 112, 70, p(150, 112), p(195, 112), p(195, 214), p(55, 214)),
                route("Pillar Hall", 535, 130, 72, p(535, 130), p(730, 130), p(730, 218), p(530, 218)),
                route("Lower Gallery", 420, 510, 72, p(420, 510), p(570, 510), p(570, 442), p(330, 510)),
                route("East Gallery", 795, 310, 75, p(795, 310), p(900, 310), p(900, 432), p(795, 432))
            ),
            hides(hide("Dig storeroom", 245, 441, 46, 52), hide("Artifact cabinet", 692, 438, 44, 52))
        ));

        levels.add(new MuseumLevel(
            4, "The National Art Museum", "VERY HARD", "Cameras, laser checkpoints, movable cover, and a secret route",
            new Color(94, 111, 119), new Color(76, 91, 101), 250,
            p(55, 108),
            rects(r(190, 90, 16, 72), r(190, 315, 16, 64),
                r(386, 90, 16, 62), r(386, 310, 16, 68),
                r(582, 90, 16, 78), r(582, 313, 16, 66),
                r(778, 90, 16, 72), r(778, 315, 16, 64),
                r(265, 333, 16, 74), r(460, 333, 16, 74), r(655, 333, 16, 74)),
            rects(r(72, 152, 84, 36), r(231, 153, 84, 36), r(425, 151, 84, 36),
                r(620, 151, 84, 36), r(816, 151, 84, 36),
                r(77, 369, 86, 36), r(297, 452, 86, 36), r(500, 452, 86, 36), r(817, 367, 86, 36)),
            rects(r(49, 283, 48, 21), r(849, 283, 48, 21), r(454, 454, 42, 19)),
            rects(r(307, 278, 30, 40), r(503, 277, 30, 40), r(700, 278, 30, 40)),
            rects(r(604, 288, 8, 115), r(604, 426, 8, 105)),
            rects(r(627, 438, 38, 25)),
            points(p(42, 474), p(880, 474)),
            points(p(95, 107), p(255, 107), p(449, 107), p(644, 107), p(838, 107)),
            strings("Signal in Silver", "New City", "After the Rain", "Glass Horizon", "The Last Original"),
            routes(
                route("North Patrol", 120, 110, 78, p(120, 110), p(174, 110), p(174, 205), p(42, 205)),
                route("Modern East", 810, 294, 82, p(810, 294), p(906, 294), p(906, 426), p(810, 426)),
                route("Sculpture Hall", 300, 420, 80, p(300, 420), p(560, 420), p(560, 510), p(280, 510)),
                route("Upper Security", 418, 282, 78, p(418, 282), p(556, 282), p(556, 320), p(418, 320)),
                route("West Wing", 42, 315, 74, p(42, 315), p(170, 315), p(170, 410), p(42, 410))
            ),
            hides(hide("Staff locker", 214, 434, 42, 48), hide("Maintenance cabinet", 718, 437, 42, 48))
        ));
        return Collections.unmodifiableList(levels);
    }

    private static Point p(int x, int y) { return new Point(x, y); }
    private static Rectangle r(int x, int y, int width, int height) { return new Rectangle(x, y, width, height); }
    private static List<Rectangle> rects(Rectangle... values) { return Arrays.asList(values); }
    private static List<Point> points(Point... values) { return Arrays.asList(values); }
    private static List<String> strings(String... values) { return Arrays.asList(values); }
    private static List<GuardRoute> routes(GuardRoute... values) { return Arrays.asList(values); }
    private static List<HideSpotData> hides(HideSpotData... values) { return Arrays.asList(values); }

    private static List<LaserBarrier.Data> createLaserPatterns(int levelId, List<Rectangle> lasers) {
        List<LaserBarrier.Data> patterns = new ArrayList<>();
        for (int i = 0; i < lasers.size(); i++) {
            double period;
            double warning;
            double active;
            double offset;
            if (levelId == 2) {
                period = 9.0;
                warning = 2.0;
                active = 3.0;
                offset = 0;
            } else if (levelId == 3) {
                period = i == 0 ? 8.5 : 10.5;
                warning = i == 0 ? 1.8 : 2.2;
                active = i == 0 ? 2.8 : 3.3;
                offset = i == 0 ? 0 : 2.4;
            } else {
                period = i == 0 ? 8.0 : 10.0;
                warning = i == 0 ? 1.6 : 2.0;
                active = i == 0 ? 2.8 : 3.2;
                offset = i == 0 ? 0 : 3.0;
            }
            patterns.add(new LaserBarrier.Data("Laser Grid " + (i + 1), lasers.get(i),
                period, warning, active, offset));
        }
        return Collections.unmodifiableList(patterns);
    }

    private static List<MovableObject.Data> createMovableObjects(int levelId) {
        switch (levelId) {
            case 0:
                return Collections.singletonList(
                    new MovableObject.Data("Gallery crate", r(220, 184, 24, 24), true, false, 130));
            case 1:
                return Arrays.asList(
                    new MovableObject.Data("Maintenance cart", r(520, 208, 30, 24), true, true, 210),
                    new MovableObject.Data("Marble plinth", r(566, 416, 28, 30), false, false, 0));
            case 2:
                return Arrays.asList(
                    new MovableObject.Data("Royal service cart", r(540, 204, 30, 24), true, true, 230),
                    new MovableObject.Data("Gilded display stand", r(314, 416, 30, 28), false, false, 0));
            case 3:
                return Arrays.asList(
                    new MovableObject.Data("Ancient urn", r(452, 235, 22, 24), true, false, 165),
                    new MovableObject.Data("Stone sarcophagus", r(545, 456, 42, 30), false, false, 0));
            default:
                return Arrays.asList(
                    new MovableObject.Data("Exhibition trolley", r(540, 260, 32, 24), true, true, 245),
                    new MovableObject.Data("Sculpture plinth", r(705, 444, 30, 28), false, false, 0));
        }
    }

    private static List<SecretPassage.Data> createSecretPassages(int levelId) {
        if (levelId == 2) {
            return Collections.singletonList(new SecretPassage.Data("Servants' route",
                r(406, 316, 28, 28), r(526, 316, 28, 28)));
        }
        if (levelId == 3) {
            return Collections.singletonList(new SecretPassage.Data("Pillar gallery passage",
                r(448, 336, 28, 28), r(570, 336, 28, 28)));
        }
        if (levelId == 4) {
            return Collections.singletonList(new SecretPassage.Data("Conservation corridor",
                r(548, 214, 28, 28), r(612, 214, 28, 28)));
        }
        return Collections.emptyList();
    }

    private static List<Rectangle> copyRectangles(List<Rectangle> input) {
        List<Rectangle> result = new ArrayList<>();
        for (Rectangle rectangle : input) result.add(new Rectangle(rectangle));
        return Collections.unmodifiableList(result);
    }
    private static List<Point> copyPoints(List<Point> input) {
        List<Point> result = new ArrayList<>();
        for (Point point : input) result.add(new Point(point));
        return Collections.unmodifiableList(result);
    }

    private static GuardRoute route(String name, int x, int y, int speed, Point... points) {
        return new GuardRoute(name, new Point(x, y), speed, Arrays.asList(points));
    }

    private static HideSpotData hide(String name, int x, int y, int width, int height) {
        return new HideSpotData(name, new Rectangle(x, y, width, height));
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDifficulty() { return difficulty; }
    public String getChallenge() { return challenge; }
    public Color getFloorA() { return floorA; }
    public Color getFloorB() { return floorB; }
    public int getMissionSeconds() { return missionSeconds; }
    public Point getPlayerStart() { return new Point(playerStart); }
    public List<Rectangle> getWalls() { return walls; }
    public List<Rectangle> getCases() { return cases; }
    public List<Rectangle> getBenches() { return benches; }
    public List<Rectangle> getStatues() { return statues; }
    public List<Rectangle> getLasers() { return lasers; }
    public List<Rectangle> getSwitches() { return switches; }
    public List<Point> getExits() { return copyPoints(exits); }
    public List<Point> getPaintingPositions() { return copyPoints(paintingPositions); }
    public List<String> getPaintingNames() { return paintingNames; }
    public List<GuardRoute> getGuards() { return guards; }
    public List<HideSpotData> getHideSpots() { return hideSpots; }
    public List<LaserBarrier.Data> getLaserPatterns() { return laserPatterns; }
    public List<MovableObject.Data> getMovableObjects() { return movableObjects; }
    public List<SecretPassage.Data> getSecretPassages() { return secretPassages; }
    public int getPaintingCount() { return paintingPositions.size(); }
    public List<CameraData> getCameras() {
        switch (id) {
            case 0:
                return Collections.singletonList(
                    new CameraData("Gallery Entry Camera", 272, 127, 180, 38, 205, 0.65, 1.15));
            case 1:
                return Collections.singletonList(
                    new CameraData("Grand Hall Camera", 470, 96, 90, 50, 235, 0.72, 0.95));
            case 2:
                return Arrays.asList(
                    new CameraData("Royal North Camera", 350, 96, 90, 38, 250, 0.7, 0.9),
                    new CameraData("Throne Room Camera", 835, 272, 180, 34, 230, 0.9, 0.75));
            case 3:
                return Collections.singletonList(
                    new CameraData("Antiquities Camera", 468, 96, 90, 40, 245, 0.68, 0.9));
            case 4:
                return Arrays.asList(
                    new CameraData("West Security Camera", 166, 274, 90, 48, 260, 0.8, 0.8),
                    new CameraData("Central Security Camera", 548, 96, 90, 42, 290, 0.65, 0.65),
                    new CameraData("East Security Camera", 910, 290, 180, 42, 270, 0.9, 0.7));
            default:
                return Collections.emptyList();
        }
    }

    public static final class CameraData {
        private final String name;
        private final int x;
        private final int y;
        private final double facing;
        private final double sweep;
        private final double radius;
        private final double speed;
        private final double detectionSeconds;
        private CameraData(String name, int x, int y, double facing, double sweep, double radius, double speed) {
            this(name, x, y, facing, sweep, radius, speed, 0.9);
        }
        private CameraData(String name, int x, int y, double facing, double sweep, double radius, double speed,
                          double detectionSeconds) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.facing = facing;
            this.sweep = sweep;
            this.radius = radius;
            this.speed = speed;
            this.detectionSeconds = detectionSeconds;
        }
        public String getName() { return name; }
        public int getX() { return x; }
        public int getY() { return y; }
        public double getFacing() { return facing; }
        public double getSweep() { return sweep; }
        public double getRadius() { return radius; }
        public double getSpeed() { return speed; }
        public double getDetectionSeconds() { return detectionSeconds; }
    }

    public static final class GuardRoute {
        private final String name;
        private final Point start;
        private final int speed;
        private final List<Point> waypoints;
        private GuardRoute(String name, Point start, int speed, List<Point> waypoints) {
            this.name = name;
            this.start = new Point(start);
            this.speed = speed;
            this.waypoints = copyPoints(waypoints);
        }
        public String getName() { return name; }
        public Point getStart() { return new Point(start); }
        public int getSpeed() { return speed; }
        public List<Point> getWaypoints() { return waypoints; }
    }

    public static final class HideSpotData {
        private final String name;
        private final Rectangle bounds;
        private HideSpotData(String name, Rectangle bounds) {
            this.name = name;
            this.bounds = new Rectangle(bounds);
        }
        public String getName() { return name; }
        public Rectangle getBounds() { return new Rectangle(bounds); }
    }
}
