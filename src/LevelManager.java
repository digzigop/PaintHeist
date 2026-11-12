import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class LevelManager {
    public static final String[] OUTFIT_NAMES = {
        "Midnight Thief", "Crimson Phantom", "Emerald Forger", "Gilded Master"
    };
    public static final int[] OUTFIT_COSTS = {0, 15, 25, 40};
    public static final String UPGRADE_FAST_COPY = "fastCopy";
    public static final String UPGRADE_LONG_THROW = "longThrow";
    public static final String UPGRADE_LONG_SWITCH = "longSwitch";

    private static final Path SAVE_FILE = Paths.get(System.getProperty("user.home"), ".paintheist-save.properties");
    private final int[] bestScores = new int[MuseumLevel.all().size()];
    private final int[] bestTimes = new int[MuseumLevel.all().size()];
    private final Set<String> ownedUpgrades = new HashSet<>();
    private int unlockedLevels = 1;
    private int totalRewards;
    private int unlockedOutfitsMask = 1;
    private int selectedOutfit;
    private boolean lightingEnabled = true;

    public LevelManager() {
        for (int i = 0; i < bestTimes.length; i++) bestTimes[i] = -1;
        load();
    }

    public boolean isUnlocked(int levelIndex) {
        return levelIndex >= 0 && levelIndex < unlockedLevels;
    }

    public int getUnlockedLevels() {
        return unlockedLevels;
    }

    public int getBestScore(int levelIndex) {
        return bestScores[levelIndex];
    }

    public int getBestTime(int levelIndex) {
        return bestTimes[levelIndex];
    }

    public int getTotalRewards() {
        return totalRewards;
    }

    public boolean isOutfitUnlocked(int index) {
        return index >= 0 && index < OUTFIT_NAMES.length && (unlockedOutfitsMask & (1 << index)) != 0;
    }

    public int getSelectedOutfit() {
        return selectedOutfit;
    }

    public boolean selectOrBuyOutfit(int index) {
        if (index < 0 || index >= OUTFIT_NAMES.length) return false;
        if (isOutfitUnlocked(index)) {
            selectedOutfit = index;
            save();
            return true;
        }
        int cost = OUTFIT_COSTS[index];
        if (totalRewards < cost) return false;
        totalRewards -= cost;
        unlockedOutfitsMask |= (1 << index);
        selectedOutfit = index;
        save();
        return true;
    }

    public boolean hasUpgrade(String key) {
        return ownedUpgrades.contains(key);
    }

    public boolean buyUpgrade(String key, int cost) {
        if (ownedUpgrades.contains(key) || totalRewards < cost) return false;
        totalRewards -= cost;
        ownedUpgrades.add(key);
        save();
        return true;
    }

    public boolean isLightingEnabled() {
        return lightingEnabled;
    }

    public void setLightingEnabled(boolean enabled) {
        lightingEnabled = enabled;
        save();
    }

    public void saveSettings() {
        save();
    }

    public Result recordCompletion(GameWorld world) {
        if (!world.isWon()) throw new IllegalArgumentException("Only a completed heist can be scored.");
        int level = world.getLevelIndex();
        int remaining = world.getTimeLeft();
        int score = remaining * 15 + world.getStolenCount() * 400
            + Math.max(0, 100 - world.getSuspicion()) * 5
            + (world.getDetectionIncidents() == 0 ? 500 : 0);
        int stars = score >= 3000 ? 3 : score >= 1800 ? 2 : 1;
        int reward = score / 100;
        if (score > bestScores[level]) bestScores[level] = score;
        if (bestTimes[level] < 0 || world.getElapsedSeconds() < bestTimes[level]) {
            bestTimes[level] = world.getElapsedSeconds();
        }
        unlockedLevels = Math.max(unlockedLevels, Math.min(bestScores.length, level + 2));
        totalRewards += reward;
        save();
        return new Result(score, stars, reward, remaining, world.getDetectionIncidents(),
            world.getSuspicion(), world.getStolenCount());
    }

    private void load() {
        if (!Files.isRegularFile(SAVE_FILE)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(SAVE_FILE)) {
            props.load(in);
            unlockedLevels = Math.max(1, Math.min(bestScores.length,
                parseInt(props.getProperty("unlockedLevels"), 1)));
            totalRewards = Math.max(0, parseInt(props.getProperty("totalRewards"), 0));
            unlockedOutfitsMask = Math.max(1, parseInt(props.getProperty("unlockedOutfits"), 1));
            int savedOutfit = parseInt(props.getProperty("selectedOutfit"), 0);
            selectedOutfit = isOutfitUnlocked(savedOutfit) ? savedOutfit : 0;
            lightingEnabled = !"false".equalsIgnoreCase(props.getProperty("lightingEnabled", "true"));
            if ("true".equalsIgnoreCase(props.getProperty("muted", "false")) != SoundEffects.isMuted()) {
                SoundEffects.toggleMute();
            }
            for (String key : new String[] {UPGRADE_FAST_COPY, UPGRADE_LONG_THROW, UPGRADE_LONG_SWITCH}) {
                if ("true".equalsIgnoreCase(props.getProperty("upgrade." + key, "false"))) {
                    ownedUpgrades.add(key);
                }
            }
            for (int i = 0; i < bestScores.length; i++) {
                bestScores[i] = Math.max(0, parseInt(props.getProperty("bestScore." + i), 0));
                bestTimes[i] = parseInt(props.getProperty("bestTime." + i), -1);
            }
        } catch (IOException ignored) {
            // Keep default in-memory progression if save file cannot be read.
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("unlockedLevels", Integer.toString(unlockedLevels));
        props.setProperty("totalRewards", Integer.toString(totalRewards));
        props.setProperty("unlockedOutfits", Integer.toString(unlockedOutfitsMask));
        props.setProperty("selectedOutfit", Integer.toString(selectedOutfit));
        props.setProperty("lightingEnabled", Boolean.toString(lightingEnabled));
        props.setProperty("muted", Boolean.toString(SoundEffects.isMuted()));
        for (String key : new String[] {UPGRADE_FAST_COPY, UPGRADE_LONG_THROW, UPGRADE_LONG_SWITCH}) {
            props.setProperty("upgrade." + key, Boolean.toString(ownedUpgrades.contains(key)));
        }
        for (int i = 0; i < bestScores.length; i++) {
            props.setProperty("bestScore." + i, Integer.toString(bestScores[i]));
            props.setProperty("bestTime." + i, Integer.toString(bestTimes[i]));
        }
        try (OutputStream out = Files.newOutputStream(SAVE_FILE)) {
            props.store(out, "PaintHeist progress");
        } catch (IOException ignored) {
            // Non-fatal if user home directory is read-only.
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static final class Result {
        private final int score;
        private final int stars;
        private final int reward;
        private final int timeRemaining;
        private final int detectionIncidents;
        private final int suspicion;
        private final int paintingsStolen;

        private Result(int score, int stars, int reward, int timeRemaining,
                       int detectionIncidents, int suspicion, int paintingsStolen) {
            this.score = score;
            this.stars = stars;
            this.reward = reward;
            this.timeRemaining = timeRemaining;
            this.detectionIncidents = detectionIncidents;
            this.suspicion = suspicion;
            this.paintingsStolen = paintingsStolen;
        }

        public int getScore() { return score; }
        public int getStars() { return stars; }
        public int getReward() { return reward; }
        public int getTimeRemaining() { return timeRemaining; }
        public int getDetectionIncidents() { return detectionIncidents; }
        public int getSuspicion() { return suspicion; }
        public int getPaintingsStolen() { return paintingsStolen; }
    }
}
