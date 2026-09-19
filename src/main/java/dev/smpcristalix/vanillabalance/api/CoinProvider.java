package dev.smpcristalix.vanillabalance.api;

import org.bukkit.inventory.ItemStack;

/** Контракт физической монеты, которую позже зарегистрирует отдельный плагин валюты. */
public interface CoinProvider {
    ItemStack createCoins(int amount);
    boolean isCoin(ItemStack item);
}
