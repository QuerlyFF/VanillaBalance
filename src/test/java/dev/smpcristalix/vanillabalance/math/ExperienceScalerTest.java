package dev.smpcristalix.vanillabalance.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExperienceScalerTest {

    @Test
    void halfXpCarriesFractionExactly() {
        double fraction = 0.0;
        int total = 0;
        for (int i = 0; i < 1001; i++) {
            var result = ExperienceScaler.scale(1, 0.5, fraction);
            total += result.granted();
            fraction = result.remainder();
        }
        assertEquals(500, total);
        assertEquals(0.5, fraction, 1.0E-12);
    }

    @Test
    void nonPositiveXpIsUntouched() {
        var result = ExperienceScaler.scale(0, 0.5, 0.25);
        assertEquals(0, result.granted());
        assertEquals(0.25, result.remainder(), 1.0E-12);
    }

    @Test
    void discardsCorruptPersistedFraction() {
        var result = ExperienceScaler.scale(3, 0.5, Double.NaN);
        assertEquals(1, result.granted());
        assertEquals(0.5, result.remainder(), 1.0E-12);
    }
}
