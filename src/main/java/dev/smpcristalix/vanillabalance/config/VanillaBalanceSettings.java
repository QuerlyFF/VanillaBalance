package dev.smpcristalix.vanillabalance.config;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Неизменяемый снимок конфигурации, безопасный для world-generation потоков. */
public record VanillaBalanceSettings(
        boolean oresEnabled,
        double oreReductionPercent,
        boolean overworldOresEnabled,
        boolean netherOresEnabled,
        boolean endOresEnabled,
        Set<String> oreExcludedWorlds,
        Map<Material, Material> oreReplacements,
        boolean antiXrayEnabled,
        int expectedAntiXrayEngineMode,
        int expectedOverworldAntiXrayMaxHeight,
        int expectedNetherAntiXrayMaxHeight,
        int expectedAntiXrayUpdateRadius,
        boolean expectedAntiXrayUsePermission,
        boolean heavyCoreEnabled,
        double heavyCoreReductionPercent,
        boolean heavyCoreOminousOnly,
        boolean elytraEnabled,
        int elytraKeepOneIn,
        boolean ironGolemEnabled,
        double ironGolemReductionPercent,
        boolean experienceEnabled,
        double experienceMultiplier,
        boolean villagersEnabled,
        boolean blockVillagersWithoutCoinProvider,
        double emeraldToCoinRate
) {

    public static VanillaBalanceSettings load(FileConfiguration config) {
        boolean oresEnabled = config.getBoolean("modules.ores.enabled", true);
        double oreReduction = percent(config, "modules.ores.reduction-percent", 15.0);

        Set<String> excluded = new HashSet<>();
        for (String name : config.getStringList("modules.ores.excluded-worlds")) {
            if (name != null && !name.isBlank()) excluded.add(name.toLowerCase());
        }

        List<String> configuredOres = config.getStringList("modules.ores.ores");
        if (oresEnabled && configuredOres.isEmpty()) {
            throw new IllegalArgumentException("modules.ores.ores не может быть пустым.");
        }

        EnumMap<Material, Material> replacements = new EnumMap<>(Material.class);
        for (String rawName : configuredOres) {
            Material ore = Material.matchMaterial(rawName);
            if (ore == null) {
                throw new IllegalArgumentException("Неизвестный Material в modules.ores.ores: " + rawName);
            }
            Material replacement = replacementFor(ore);
            if (replacement == null) {
                throw new IllegalArgumentException(rawName + " не поддерживается как руда VanillaBalance.");
            }
            replacements.put(ore, replacement);
        }

        int engineMode = config.getInt("modules.anti-xray.expected-engine-mode", 3);
        if (engineMode < 1 || engineMode > 3) {
            throw new IllegalArgumentException("modules.anti-xray.expected-engine-mode должен быть 1, 2 или 3.");
        }
        int overworldHeight = config.getInt("modules.anti-xray.expected-overworld-max-height", 64);
        int netherHeight = config.getInt("modules.anti-xray.expected-nether-max-height", 128);
        validateHeight("expected-overworld-max-height", overworldHeight);
        validateHeight("expected-nether-max-height", netherHeight);
        int updateRadius = config.getInt("modules.anti-xray.expected-update-radius", 2);
        if (updateRadius < 0 || updateRadius > 5) {
            throw new IllegalArgumentException("modules.anti-xray.expected-update-radius должен быть от 0 до 5.");
        }

        int keepOneIn = config.getInt("modules.elytra.keep-one-in", 3);
        if (keepOneIn < 1) {
            throw new IllegalArgumentException("modules.elytra.keep-one-in должен быть >= 1.");
        }

        double xpMultiplier = config.getDouble("modules.experience.multiplier", 0.5);
        if (!Double.isFinite(xpMultiplier) || xpMultiplier < 0.0 || xpMultiplier > 1.0) {
            throw new IllegalArgumentException("modules.experience.multiplier должен быть от 0 до 1.");
        }

        double coinRate = config.getDouble("modules.villagers.emerald-to-coin-rate", 1.0);
        if (!Double.isFinite(coinRate) || coinRate <= 0.0) {
            throw new IllegalArgumentException("modules.villagers.emerald-to-coin-rate должен быть > 0.");
        }

        return new VanillaBalanceSettings(
                oresEnabled,
                oreReduction,
                config.getBoolean("modules.ores.environments.overworld", true),
                config.getBoolean("modules.ores.environments.nether", true),
                config.getBoolean("modules.ores.environments.end", false),
                Collections.unmodifiableSet(excluded),
                Collections.unmodifiableMap(replacements),
                config.getBoolean("modules.anti-xray.enabled", true),
                engineMode,
                overworldHeight,
                netherHeight,
                updateRadius,
                config.getBoolean("modules.anti-xray.expected-use-permission", false),
                config.getBoolean("modules.heavy-core.enabled", true),
                percent(config, "modules.heavy-core.reduction-percent", 20.0),
                config.getBoolean("modules.heavy-core.ominous-vault-only", true),
                config.getBoolean("modules.elytra.enabled", true),
                keepOneIn,
                config.getBoolean("modules.iron-golem.enabled", true),
                percent(config, "modules.iron-golem.iron-drop-reduction-percent", 50.0),
                config.getBoolean("modules.experience.enabled", true),
                xpMultiplier,
                config.getBoolean("modules.villagers.enabled", true),
                config.getBoolean("modules.villagers.block-when-coin-provider-missing", true),
                coinRate
        );
    }

    public boolean isOreNerfEnabledFor(World world) {
        if (!oresEnabled || oreExcludedWorlds.contains(world.getName().toLowerCase())) return false;
        return switch (world.getEnvironment()) {
            case NORMAL -> overworldOresEnabled;
            case NETHER -> netherOresEnabled;
            case THE_END -> endOresEnabled;
            default -> false;
        };
    }

    private static double percent(FileConfiguration config, String path, double fallback) {
        double value = config.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(path + " должен быть от 0 до 100.");
        }
        return value;
    }

    private static void validateHeight(String name, int value) {
        if (value < 0 || value % 16 != 0) {
            throw new IllegalArgumentException("modules.anti-xray." + name + " должен быть неотрицательным и кратным 16.");
        }
    }

    private static Material replacementFor(Material ore) {
        return switch (ore) {
            case COAL_ORE, COPPER_ORE, IRON_ORE, GOLD_ORE, REDSTONE_ORE,
                 LAPIS_ORE, DIAMOND_ORE, EMERALD_ORE -> Material.STONE;
            case DEEPSLATE_COAL_ORE, DEEPSLATE_COPPER_ORE, DEEPSLATE_IRON_ORE,
                 DEEPSLATE_GOLD_ORE, DEEPSLATE_REDSTONE_ORE, DEEPSLATE_LAPIS_ORE,
                 DEEPSLATE_DIAMOND_ORE, DEEPSLATE_EMERALD_ORE -> Material.DEEPSLATE;
            case NETHER_GOLD_ORE, NETHER_QUARTZ_ORE, ANCIENT_DEBRIS -> Material.NETHERRACK;
            default -> null;
        };
    }
}
