package dev.smpcristalix.vanillabalance.module.elytra;

import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.math.ElytraSelector;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureGenerateEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;

/** Оставляет Elytra случайно в одном из N End City прямо во время естественной генерации. */
public final class ElytraGenerationListener implements Listener {

    private final NamespacedKey transformerKey;
    private volatile VanillaBalanceSettings settings;

    public ElytraGenerationListener(Plugin plugin, VanillaBalanceSettings settings) {
        this.transformerKey = new NamespacedKey(plugin, "elytra_generation_nerf");
        this.settings = settings;
    }

    public void reload(VanillaBalanceSettings settings) {
        this.settings = settings;
    }

    @EventHandler
    public void onStructureGenerate(AsyncStructureGenerateEvent event) {
        VanillaBalanceSettings snapshot = settings;
        if (!snapshot.elytraEnabled()) return;
        if (event.getCause() != AsyncStructureGenerateEvent.Cause.WORLD_GENERATION) return;
        if (!Structure.END_CITY.equals(event.getStructure())) return;

        // Событие естественной генерации может быть асинхронным. Поэтому здесь не вызываем
        // методы World и не храним глобальные счётчики: решение принимается локально один раз.
        if (ElytraSelector.keep(ThreadLocalRandom.current(), snapshot.elytraKeepOneIn())) return;

        event.setEntityTransformer(transformerKey, (region, x, y, z, entity, allowedToSpawn) -> {
            if (entity instanceof ItemFrame frame && frame.getItem().getType() == Material.ELYTRA) {
                frame.setItem(new ItemStack(Material.AIR), false);
            }
            return allowedToSpawn;
        });
    }
}
