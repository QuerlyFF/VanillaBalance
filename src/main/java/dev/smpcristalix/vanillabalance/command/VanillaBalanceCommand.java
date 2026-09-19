package dev.smpcristalix.vanillabalance.command;

import dev.smpcristalix.vanillabalance.VanillaBalancePlugin;
import dev.smpcristalix.vanillabalance.api.CoinProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Админ-команды и диагностика VanillaBalance. */
public final class VanillaBalanceCommand implements CommandExecutor, TabCompleter {

    private final VanillaBalancePlugin plugin;

    public VanillaBalanceCommand(VanillaBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("vanillabalance.admin")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showStatus(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            String error = plugin.reloadPluginConfiguration();
            sender.sendMessage(error == null ? "§aVanillaBalance перезагружен." : "§cКонфиг не применён: " + error);
            return true;
        }
        if (args[0].equalsIgnoreCase("antixray")) {
            if (!plugin.settings().antiXrayEnabled()) {
                sender.sendMessage("§eAnti-Xray модуль выключен.");
                return true;
            }
            var result = plugin.antiXrayAdvisor().audit();
            if (result.ok()) sender.sendMessage("§aPaper Anti-Xray настроен как ожидается.");
            else {
                sender.sendMessage("§cPaper Anti-Xray требует настройки:");
                result.warnings().forEach(w -> sender.sendMessage("§7- §f" + w));
            }
            return true;
        }

        sender.sendMessage("§cНеизвестная подкоманда.");
        return true;
    }

    private void showStatus(CommandSender sender) {
        var s = plugin.settings();
        boolean provider = Bukkit.getServicesManager().getRegistration(CoinProvider.class) != null;
        sender.sendMessage("§6VanillaBalance:");
        sender.sendMessage("§7Руды: §f" + s.oresEnabled() + " §7(-" + s.oreReductionPercent() + "%)");
        sender.sendMessage("§7Anti-Xray: §f" + s.antiXrayEnabled());
        sender.sendMessage("§7Heavy Core: §f" + s.heavyCoreEnabled() + " §7(-" + s.heavyCoreReductionPercent() + "%)");
        sender.sendMessage("§7Elytra: §f1/" + s.elytraKeepOneIn() + " при генерации End City");
        sender.sendMessage("§7Железо големов: §f-" + s.ironGolemReductionPercent() + "%");
        sender.sendMessage("§7XP: §fx" + s.experienceMultiplier());
        sender.sendMessage("§7Жители за монеты: §f" + s.villagersEnabled());
        sender.sendMessage("§7CoinProvider: §f" + provider);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        return List.of("status", "reload", "antixray").stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
    }
}
