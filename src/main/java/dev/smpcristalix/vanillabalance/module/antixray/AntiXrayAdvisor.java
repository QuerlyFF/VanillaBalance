package dev.smpcristalix.vanillabalance.module.antixray;

import dev.smpcristalix.vanillabalance.VanillaBalancePlugin;
import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Полный аудит эффективной per-world конфигурации встроенного Paper Anti-Xray. */
public final class AntiXrayAdvisor {

    private static final String BASE = "anticheat.anti-xray.";

    private static final Set<String> OVERWORLD_HIDDEN = Set.of(
            "copper_ore", "deepslate_copper_ore", "raw_copper_block",
            "diamond_ore", "deepslate_diamond_ore",
            "gold_ore", "deepslate_gold_ore",
            "iron_ore", "deepslate_iron_ore", "raw_iron_block",
            "lapis_ore", "deepslate_lapis_ore",
            "redstone_ore", "deepslate_redstone_ore"
    );

    private static final Set<String> OVERWORLD_REPLACEMENTS = Set.of(
            "chest", "amethyst_block", "andesite", "budding_amethyst", "calcite",
            "coal_ore", "deepslate_coal_ore", "deepslate", "diorite", "dirt",
            "emerald_ore", "deepslate_emerald_ore", "granite", "gravel",
            "oak_planks", "smooth_basalt", "stone", "tuff"
    );

    private static final Set<String> NETHER_HIDDEN = Set.of(
            "ancient_debris", "bone_block", "glowstone", "magma_block",
            "nether_bricks", "nether_gold_ore", "nether_quartz_ore",
            "polished_blackstone_bricks"
    );

    private static final Set<String> NETHER_REPLACEMENTS = Set.of(
            "basalt", "blackstone", "gravel", "netherrack", "soul_sand", "soul_soil"
    );

    private final VanillaBalancePlugin plugin;
    private final VanillaBalanceSettings settings;

    public AntiXrayAdvisor(VanillaBalancePlugin plugin, VanillaBalanceSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public AuditResult audit() {
        List<String> warnings = new ArrayList<>();
        File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();
        File defaultsFile = new File(serverRoot, "config/paper-world-defaults.yml");
        if (!defaultsFile.isFile()) {
            return new AuditResult(false, List.of("Не найден config/paper-world-defaults.yml."));
        }

        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(defaultsFile);
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL
                    && world.getEnvironment() != World.Environment.NETHER) continue;

            File worldFile = new File(world.getWorldFolder(), "paper-world.yml");
            YamlConfiguration override = worldFile.isFile()
                    ? YamlConfiguration.loadConfiguration(worldFile)
                    : new YamlConfiguration();

            auditWorld(world, defaults, override, warnings);
        }
        return new AuditResult(warnings.isEmpty(), List.copyOf(warnings));
    }

    private void auditWorld(
            World world,
            YamlConfiguration defaults,
            YamlConfiguration override,
            List<String> warnings
    ) {
        String label = world.getName();
        boolean nether = world.getEnvironment() == World.Environment.NETHER;
        int expectedHeight = nether
                ? settings.expectedNetherAntiXrayMaxHeight()
                : settings.expectedOverworldAntiXrayMaxHeight();
        Set<String> hidden = nether ? NETHER_HIDDEN : OVERWORLD_HIDDEN;
        Set<String> replacements = nether ? NETHER_REPLACEMENTS : OVERWORLD_REPLACEMENTS;

        if (!boolValue(defaults, override, BASE + "enabled", false)) {
            warnings.add(label + ": Anti-Xray выключен.");
            return;
        }
        int mode = intValue(defaults, override, BASE + "engine-mode", 1);
        if (mode != settings.expectedAntiXrayEngineMode()) {
            warnings.add(label + ": engine-mode=" + mode
                    + ", ожидается " + settings.expectedAntiXrayEngineMode() + ".");
        }
        int height = intValue(defaults, override, BASE + "max-block-height", 64);
        if (height != expectedHeight) {
            warnings.add(label + ": max-block-height=" + height
                    + ", ожидается " + expectedHeight + ".");
        }
        int radius = intValue(defaults, override, BASE + "update-radius", 2);
        if (radius != settings.expectedAntiXrayUpdateRadius()) {
            warnings.add(label + ": update-radius=" + radius
                    + ", ожидается " + settings.expectedAntiXrayUpdateRadius() + ".");
        }
        boolean permission = boolValue(defaults, override, BASE + "use-permission", false);
        if (permission != settings.expectedAntiXrayUsePermission()) {
            warnings.add(label + ": use-permission=" + permission
                    + ", ожидается " + settings.expectedAntiXrayUsePermission() + ".");
        }

        Set<String> actualHidden = stringSet(defaults, override, BASE + "hidden-blocks");
        Set<String> missingHidden = new HashSet<>(hidden);
        missingHidden.removeAll(actualHidden);
        if (!missingHidden.isEmpty()) warnings.add(label + ": hidden-blocks не хватает " + missingHidden + ".");

        Set<String> actualReplacements = stringSet(defaults, override, BASE + "replacement-blocks");
        Set<String> missingReplacements = new HashSet<>(replacements);
        missingReplacements.removeAll(actualReplacements);
        if (!missingReplacements.isEmpty()) {
            warnings.add(label + ": replacement-blocks не хватает " + missingReplacements + ".");
        }
    }

    private boolean boolValue(YamlConfiguration defaults, YamlConfiguration override, String path, boolean fallback) {
        return override.contains(path) ? override.getBoolean(path) : defaults.getBoolean(path, fallback);
    }

    private int intValue(YamlConfiguration defaults, YamlConfiguration override, String path, int fallback) {
        return override.contains(path) ? override.getInt(path) : defaults.getInt(path, fallback);
    }

    private Set<String> stringSet(YamlConfiguration defaults, YamlConfiguration override, String path) {
        Collection<String> source = override.contains(path)
                ? override.getStringList(path)
                : defaults.getStringList(path);
        Set<String> result = new HashSet<>();
        for (String value : source) {
            if (value != null) {
                String normalized = value.toLowerCase(Locale.ROOT);
                if (normalized.startsWith("minecraft:")) normalized = normalized.substring("minecraft:".length());
                result.add(normalized);
            }
        }
        return result;
    }

    public void logStartupAudit() {
        AuditResult result = audit();
        if (result.ok()) {
            plugin.getLogger().info("Paper Anti-Xray настроен как ожидается.");
            return;
        }
        plugin.getLogger().warning("Paper Anti-Xray требует внимания:");
        result.warnings().forEach(w -> plugin.getLogger().warning("Anti-Xray: " + w));
    }

    public record AuditResult(boolean ok, List<String> warnings) {}
}
