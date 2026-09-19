package dev.smpcristalix.vanillabalance.module.heavycore;

import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.math.ProbabilityReducer;
import org.bukkit.Material;
import org.bukkit.block.data.type.Vault;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Дополнительно уменьшает Heavy Core только после ванильного выбора loot. */
public final class HeavyCoreNerfListener implements Listener {

    private volatile VanillaBalanceSettings settings;

    public HeavyCoreNerfListener(VanillaBalanceSettings settings) {
        this.settings = settings;
    }

    public void reload(VanillaBalanceSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVaultLoot(BlockDispenseLootEvent event) {
        VanillaBalanceSettings snapshot = settings;
        if (!snapshot.heavyCoreEnabled() || event.getBlock().getType() != Material.VAULT) return;

        if (snapshot.heavyCoreOminousOnly()) {
            if (!(event.getBlock().getBlockData() instanceof Vault vault) || !vault.isOminous()) return;
        }

        List<ItemStack> modified = new ArrayList<>(event.getDispensedLoot().size());
        for (ItemStack original : event.getDispensedLoot()) {
            if (original.getType() != Material.HEAVY_CORE) {
                modified.add(original);
                continue;
            }
            int kept = ProbabilityReducer.keptAmount(
                    original.getAmount(),
                    snapshot.heavyCoreReductionPercent(),
                    ThreadLocalRandom.current()
            );
            if (kept > 0) {
                ItemStack copy = original.clone();
                copy.setAmount(kept);
                modified.add(copy);
            }
        }
        event.setDispensedLoot(modified);
    }
}
