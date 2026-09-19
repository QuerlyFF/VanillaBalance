package dev.smpcristalix.vanillabalance.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerProgressionTest {

    @Test
    void usesVanillaJavaThresholds() {
        assertFalse(VillagerProgression.shouldLevelUp(1, 9));
        assertTrue(VillagerProgression.shouldLevelUp(1, 10));
        assertFalse(VillagerProgression.shouldLevelUp(2, 69));
        assertTrue(VillagerProgression.shouldLevelUp(2, 70));
        assertFalse(VillagerProgression.shouldLevelUp(3, 149));
        assertTrue(VillagerProgression.shouldLevelUp(3, 150));
        assertFalse(VillagerProgression.shouldLevelUp(4, 249));
        assertTrue(VillagerProgression.shouldLevelUp(4, 250));
        assertFalse(VillagerProgression.shouldLevelUp(5, 10000));
    }
}
