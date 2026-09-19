package dev.smpcristalix.vanillabalance.api;

import org.bukkit.inventory.ItemStack;

/**
 * Общий контракт Осколка характеристик SMP.
 * Будущая система характеристик должна получать этот provider через ServicesManager,
 * чтобы не создавать несовместимую копию предмета.
 */
public interface StatShardProvider {
    ItemStack createShard(int amount);
    boolean isShard(ItemStack item);
}
