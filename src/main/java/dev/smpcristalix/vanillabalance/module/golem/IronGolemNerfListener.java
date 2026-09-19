package dev.smpcristalix.vanillabalance.module.golem;

import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.math.ProbabilityReducer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;

/** Уменьшает только IRON_INGOT с любых Iron Golem. */
public final class IronGolemNerfListener implements Listener {

    private volatile VanillaBalanceSettings settings;

    public IronGolemNerfListener(VanillaBalanceSettings settings) {
        this.settings = settings;
    }

    public void reload(VanillaBalanceSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGolemDeath(EntityDeathEvent event) {
        VanillaBalanceSettings snapshot = settings;
        if (!snapshot.ironGolemEnabled() || event.getEntityType() != EntityType.IRON_GOLEM) return;

        ListIterator<ItemStack> it = event.getDrops().listIterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (item.getType() != Material.IRON_INGOT) continue;

            int kept = ProbabilityReducer.keptAmount(
                    item.getAmount(),
                    snapshot.ironGolemReductionPercent(),
                    ThreadLocalRandom.current()
            );
            if (kept <= 0) it.remove();
            else if (kept != item.getAmount()) {
                ItemStack copy = item.clone();
                copy.setAmount(kept);
                it.set(copy);
            }
        }
    }
}
