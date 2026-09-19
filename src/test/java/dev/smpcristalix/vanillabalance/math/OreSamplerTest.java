package dev.smpcristalix.vanillabalance.math;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OreSamplerTest {

    @Test
    void samplesAboutFifteenPercentWithoutFullScan() {
        int total = 1_000_000;
        AtomicInteger selected = new AtomicInteger();
        OreSampler.forEachSelected(total, 0.15, new Random(123456789L), ignored -> selected.incrementAndGet());
        double ratio = selected.get() / (double) total;
        assertTrue(ratio > 0.148 && ratio < 0.152, "ratio=" + ratio);
    }
}
