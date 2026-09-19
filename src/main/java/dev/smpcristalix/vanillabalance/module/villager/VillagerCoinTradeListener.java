package dev.smpcristalix.vanillabalance.module.villager;

import dev.smpcristalix.vanillabalance.api.CoinProvider;
import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.math.VillagerProgression;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Открывает временный Merchant с физическими монетами, не записывая монетные
 * рецепты в NBT самого жителя. Uses/demand и опыт профессии синхронизируются
 * обратно только после фактически совершённых сделок.
 */
public final class VillagerCoinTradeListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    private volatile VanillaBalanceSettings settings;

    public VillagerCoinTradeListener(Plugin plugin, VanillaBalanceSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void reload(VanillaBalanceSettings settings) {
        closeAllSessions();
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        VanillaBalanceSettings snapshot = settings;
        if (!snapshot.villagersEnabled() || event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof AbstractVillager villager)) return;

        CoinProvider provider = findProvider();
        if (provider == null) {
            if (snapshot.blockVillagersWithoutCoinProvider()) {
                event.setCancelled(true);
                event.getPlayer().sendActionBar(Component.text("Монетная система ещё не подключена."));
            }
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> openCoinMerchant(player, villager, provider, snapshot));
    }

    private void openCoinMerchant(
            Player player,
            AbstractVillager villager,
            CoinProvider provider,
            VanillaBalanceSettings snapshot
    ) {
        if (!player.isOnline() || !villager.isValid()) return;

        closeSession(player.getUniqueId());

        List<MerchantRecipe> source = villager.getRecipes();
        List<MerchantRecipe> converted = new ArrayList<>(source.size());
        List<Integer> initialUses = new ArrayList<>(source.size());

        try {
            for (MerchantRecipe recipe : source) {
                converted.add(convertRecipe(recipe, provider, snapshot.emeraldToCoinRate()));
                initialUses.add(recipe.getUses());
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("Не удалось открыть торговлю за монеты: " + ex.getMessage());
            player.sendActionBar(Component.text("Ошибка монетной системы. Сообщи администрации."));
            return;
        }

        Merchant merchant = Bukkit.createMerchant(Component.text("Торговля за монеты"));
        merchant.setRecipes(converted);

        // Важно: session кладём ПОСЛЕ openMerchant. Само открытие может закрыть предыдущее
        // окно и синхронно вызвать InventoryCloseEvent.
        InventoryView view = player.openMerchant(merchant, true);
        if (view != null) {
            sessions.put(player.getUniqueId(), new TradeSession(villager, merchant, List.copyOf(initialUses)));
        }
    }

    private MerchantRecipe convertRecipe(MerchantRecipe source, CoinProvider provider, double rate) {
        ItemStack result = source.getResult().getType() == Material.EMERALD
                ? currencyItem(source.getResult().getAmount(), provider, rate)
                : source.getResult().clone();

        MerchantRecipe copy = new MerchantRecipe(
                result,
                source.getUses(),
                source.getMaxUses(),
                source.hasExperienceReward(),
                source.getVillagerExperience(),
                source.getPriceMultiplier(),
                source.getDemand(),
                source.getSpecialPrice(),
                source.shouldIgnoreDiscounts()
        );

        List<ItemStack> ingredients = new ArrayList<>();
        for (ItemStack ingredient : source.getIngredients()) {
            if (ingredient.getType() == Material.EMERALD) {
                ingredients.add(currencyItem(ingredient.getAmount(), provider, rate));
            } else {
                ingredients.add(ingredient.clone());
            }
        }
        copy.setIngredients(ingredients);
        return copy;
    }

    private ItemStack currencyItem(int emeraldAmount, CoinProvider provider, double rate) {
        int amount = Math.max(1, (int) Math.ceil(emeraldAmount * rate));
        ItemStack coins = provider.createCoins(amount);
        if (coins == null || coins.getType().isAir() || !provider.isCoin(coins)) {
            throw new IllegalStateException("CoinProvider вернул некорректный ItemStack монет.");
        }
        if (amount > coins.getMaxStackSize()) {
            throw new IllegalStateException("Цена в монетах не помещается в один слот: " + amount);
        }
        if (coins.getAmount() != amount) {
            ItemStack copy = coins.clone();
            copy.setAmount(amount);
            coins = copy;
        }
        return coins;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) closeSession(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        closeSession(event.getPlayer().getUniqueId());
    }

    public void closeAllSessions() {
        for (UUID id : List.copyOf(sessions.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) player.closeInventory();
            closeSession(id); // fallback, если close event не был вызван
        }
    }

    private void closeSession(UUID playerId) {
        TradeSession session = sessions.remove(playerId);
        if (session == null || !session.villager().isValid()) return;

        // Монетные MerchantRecipe никогда не записываются в жителя.
        List<MerchantRecipe> coinRecipes = session.merchant().getRecipes();
        List<MerchantRecipe> realRecipes = session.villager().getRecipes();
        int limit = Math.min(Math.min(coinRecipes.size(), realRecipes.size()), session.initialUses().size());
        boolean recipeStateChanged = false;
        int earnedVillagerXp = 0;

        for (int i = 0; i < limit; i++) {
            MerchantRecipe coin = coinRecipes.get(i);
            MerchantRecipe real = realRecipes.get(i);
            int previousUses = session.initialUses().get(i);
            int newUses = Math.max(previousUses, coin.getUses());
            int completedTrades = Math.max(0, newUses - previousUses);

            if (real.getUses() != newUses) {
                real.setUses(newUses);
                recipeStateChanged = true;
            }
            if (real.getDemand() != coin.getDemand()) {
                real.setDemand(coin.getDemand());
                recipeStateChanged = true;
            }
            if (real.getMaxUses() != coin.getMaxUses()) {
                real.setMaxUses(coin.getMaxUses());
                recipeStateChanged = true;
            }

            if (completedTrades > 0) {
                earnedVillagerXp += completedTrades * Math.max(0, real.getVillagerExperience());
            }
        }

        if (recipeStateChanged) session.villager().setRecipes(realRecipes);
        if (earnedVillagerXp > 0 && session.villager() instanceof Villager villager) {
            int newExperience = Math.max(0, villager.getVillagerExperience() + earnedVillagerXp);
            villager.setVillagerExperience(newExperience);

            // Java Edition повышает максимум один уровень после закрытия одной торговой сессии.
            if (VillagerProgression.shouldLevelUp(villager.getVillagerLevel(), newExperience)) {
                villager.increaseLevel(1);
            }
        }
    }

    private CoinProvider findProvider() {
        RegisteredServiceProvider<CoinProvider> registration =
                Bukkit.getServicesManager().getRegistration(CoinProvider.class);
        return registration == null ? null : registration.getProvider();
    }

    private record TradeSession(
            AbstractVillager villager,
            Merchant merchant,
            List<Integer> initialUses
    ) {}
}
