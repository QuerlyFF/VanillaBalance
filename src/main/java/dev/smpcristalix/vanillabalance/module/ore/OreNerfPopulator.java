package dev.smpcristalix.vanillabalance.module.ore;

import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.math.OreSampler;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

/**
 * Нерф руд для новых чанков.
 * Вместо полного сканирования всех блоков проверяет только случайно выбранные p% позиций.
 */
public final class OreNerfPopulator extends BlockPopulator {

    private static final int CHUNK_AREA = 16 * 16;
    private final double removalChance;
    private final Map<Material, Material> replacements;

    public OreNerfPopulator(VanillaBalanceSettings settings) {
        removalChance = settings.oreReductionPercent() / 100.0;
        replacements = settings.oreReplacements();
    }

    @Override
    public void populate(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull LimitedRegion region
    ) {
        if (removalChance <= 0.0 || replacements.isEmpty()) return;

        int minY = worldInfo.getMinHeight();
        int maxY = scanMaxHeight(worldInfo);
        int height = maxY - minY;
        if (height <= 0) return;

        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int total = CHUNK_AREA * height;

        OreSampler.forEachSelected(total, removalChance, random, flat -> {
            int yIndex = flat / CHUNK_AREA;
            int column = flat - yIndex * CHUNK_AREA;
            int x = startX + (column >>> 4);
            int z = startZ + (column & 15);
            int y = minY + yIndex;

            Material current = region.getType(x, y, z);
            Material replacement = replacements.get(current);
            if (replacement != null) region.setType(x, y, z, replacement);
        });
    }

    private int scanMaxHeight(WorldInfo worldInfo) {
        if (worldInfo.getEnvironment() == World.Environment.NETHER) {
            return Math.min(worldInfo.getMaxHeight(), 128);
        }
        return worldInfo.getMaxHeight();
    }
}
