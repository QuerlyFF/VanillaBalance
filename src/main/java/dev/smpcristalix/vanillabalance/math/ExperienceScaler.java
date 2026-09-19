package dev.smpcristalix.vanillabalance.math;

/** Чистая математика масштабирования XP с переносом дробного остатка. */
public final class ExperienceScaler {

    private ExperienceScaler() {}

    public static Result scale(int original, double multiplier, double carriedFraction) {
        if (original <= 0) return new Result(original, carriedFraction);

        double exact = original * multiplier + carriedFraction;
        int granted = (int) Math.floor(exact + 1.0E-12);
        double remainder = exact - granted;
        if (remainder < 1.0E-12) remainder = 0.0;
        return new Result(granted, remainder);
    }

    public record Result(int granted, double remainder) {}
}
