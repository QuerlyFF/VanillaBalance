package dev.smpcristalix.vanillabalance.module.experience;

import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.math.ExperienceScaler;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** x0.5 натурального XP с точным переносом дробного остатка через PDC игрока. */
public final class ExperienceNerfListener implements Listener {

    private final NamespacedKey fractionKey;
    private volatile VanillaBalanceSettings settings;

    public ExperienceNerfListener(Plugin plugin, VanillaBalanceSettings settings) {
        this.fractionKey = new NamespacedKey(plugin, "xp_fraction");
        this.settings = settings;
    }

    public void reload(VanillaBalanceSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExperience(PlayerExpChangeEvent event) {
        VanillaBalanceSettings snapshot = settings;
        if (!snapshot.experienceEnabled() || event.getAmount() <= 0) return;

        Player player = event.getPlayer();
        double carried = player.getPersistentDataContainer().getOrDefault(
                fractionKey,
                PersistentDataType.DOUBLE,
                0.0
        );
        ExperienceScaler.Result result = ExperienceScaler.scale(
                event.getAmount(),
                snapshot.experienceMultiplier(),
                carried
        );
        event.setAmount(result.granted());

        if (result.remainder() == 0.0) {
            player.getPersistentDataContainer().remove(fractionKey);
        } else {
            player.getPersistentDataContainer().set(
                    fractionKey,
                    PersistentDataType.DOUBLE,
                    result.remainder()
            );
        }
    }
}
