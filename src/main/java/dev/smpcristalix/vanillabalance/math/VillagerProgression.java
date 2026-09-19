package dev.smpcristalix.vanillabalance.math;

/** Ванильные суммарные пороги опыта профессии жителя Java Edition. */
public final class VillagerProgression {

    private static final int[] NEXT_LEVEL_XP = {0, 10, 70, 150, 250};

    private VillagerProgression() {}

    /** Возвращает опыт, необходимый для перехода с текущего уровня на следующий. */
    public static int nextLevelThreshold(int currentLevel) {
        if (currentLevel < 1 || currentLevel >= 5) return Integer.MAX_VALUE;
        return NEXT_LEVEL_XP[currentLevel];
    }

    public static boolean shouldLevelUp(int currentLevel, int totalExperience) {
        return currentLevel < 5 && totalExperience >= nextLevelThreshold(currentLevel);
    }
}
