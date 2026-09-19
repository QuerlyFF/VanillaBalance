package dev.smpcristalix.vanillabalance.math;

import java.util.Random;
import java.util.function.IntConsumer;

/**
 * Равномерно выбирает позиции с вероятностью p без полного прохода по каждой позиции.
 * Использует геометрические пропуски между выбранными индексами.
 */
public final class OreSampler {

    private OreSampler() {}

    public static void forEachSelected(int totalPositions, double probability, Random random, IntConsumer consumer) {
        if (totalPositions <= 0 || probability <= 0.0) return;
        if (probability >= 1.0) {
            for (int i = 0; i < totalPositions; i++) consumer.accept(i);
            return;
        }

        double logFailure = Math.log1p(-probability);
        int index = -1;
        while (true) {
            double u = random.nextDouble();
            int skipped = (int) Math.floor(Math.log1p(-u) / logFailure);
            index += skipped + 1;
            if (index >= totalPositions) return;
            consumer.accept(index);
        }
    }
}
