package dev.smpcristalix.vanillabalance.item;

import dev.smpcristalix.vanillabalance.api.StatShardProvider;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/** Общий Осколок характеристик SMP. */
public final class SmpStatShardProvider implements StatShardProvider {

    public static final String ITEM_ID = "smpcristalix:stat_shard";
    private final NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(ITEM_ID));

    @Override
    public ItemStack createShard(int amount) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dОсколок характеристик");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean isShard(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
