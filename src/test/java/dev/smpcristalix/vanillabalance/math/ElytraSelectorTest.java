package dev.smpcristalix.vanillabalance.math;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ElytraSelectorTest {

    @Test
    void oneInOneAlwaysKeeps() {
        Random random = new Random(1L);
        for (int i = 0; i < 100; i++) {
            assertTrue(ElytraSelector.keep(random, 1));
        }
    }

    @Test
    void roughlyOneInThreeIsKept() {
        Random random = new Random(998877665544L);
        int kept = 0;
        int total = 100_000;
        for (int i = 0; i < total; i++) {
            if (ElytraSelector.keep(random, 3)) kept++;
        }
        double ratio = kept / (double) total;
        assertTrue(ratio > 0.325 && ratio < 0.342, "ratio=" + ratio);
    }
}
