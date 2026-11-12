public final class LevelManager {
    private final int[] bestScores = new int[MuseumLevel.all().size()];
    private final int[] bestTimes = new int[MuseumLevel.all().size()];
    private int unlockedLevels = 1;
    private int totalRewards;

    public LevelManager() {
        for (int i = 0; i < bestTimes.length; i++) bestTimes[i] = -1;
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
        return new Result(score, stars, reward, remaining, world.getDetectionIncidents(),
            world.getSuspicion(), world.getStolenCount());
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
