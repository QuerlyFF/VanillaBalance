package dev.smpcristalix.vanillabalance.math;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbabilityReducerTest {

    @Test
    void boundariesAreExact() {
        assertEquals(10, ProbabilityReducer.keptAmount(10, 0.0, new Random(1)));
        assertEquals(0, ProbabilityReducer.keptAmount(10, 100.0, new Random(1)));
    }

    @Test
    void twentyPercentReductionIsStatistical() {
        int kept = ProbabilityReducer.keptAmount(100_000, 20.0, new Random(42));
        assertTrue(kept > 79_000 && kept < 81_000, "kept=" + kept);
    }
}
