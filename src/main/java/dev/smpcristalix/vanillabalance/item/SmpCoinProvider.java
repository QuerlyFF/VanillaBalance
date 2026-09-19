package dev.smpcristalix.vanillabalance.item;

import dev.smpcristalix.vanillabalance.api.CoinProvider;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/** Физическая бронзовая монета, общая для торговли, мобов и структур. */
public final class SmpCoinProvider implements CoinProvider {

    public static final String ITEM_ID = "smpcristalix:bronze_coin";
    private final NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(ITEM_ID));

    @Override
    public ItemStack createCoins(int amount) {
        ItemStack item = new ItemStack(Material.COPPER_INGOT, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Бронзовая монета");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean isCoin(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
