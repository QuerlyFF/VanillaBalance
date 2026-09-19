package dev.smpcristalix.vanillabalance.math;

import java.util.random.RandomGenerator;

/** Чистая равномерная логика решения 1/N для Elytra во время worldgen. */
public final class ElytraSelector {

    private ElytraSelector() {}

    public static boolean keep(RandomGenerator random, int oneIn) {
        if (oneIn <= 1) return true;
        return random.nextInt(oneIn) == 0;
    }
}
