package dev.smpcristalix.vanillabalance.math;

import java.util.random.RandomGenerator;

/** Общая чистая логика дополнительного процентного удаления предметов. */
public final class ProbabilityReducer {

    private ProbabilityReducer() {}

    public static int keptAmount(int amount, double reductionPercent, RandomGenerator random) {
        if (amount <= 0) return 0;
        double chance = reductionPercent / 100.0;
        if (chance <= 0.0) return amount;
        if (chance >= 1.0) return 0;

        int kept = 0;
        for (int i = 0; i < amount; i++) {
            if (random.nextDouble() >= chance) kept++;
        }
        return kept;
    }
}
