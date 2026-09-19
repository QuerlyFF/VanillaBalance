package dev.smpcristalix.vanillabalance;

import dev.smpcristalix.vanillabalance.command.VanillaBalanceCommand;
import dev.smpcristalix.vanillabalance.config.VanillaBalanceSettings;
import dev.smpcristalix.vanillabalance.module.antixray.AntiXrayAdvisor;
import dev.smpcristalix.vanillabalance.module.elytra.ElytraGenerationListener;
import dev.smpcristalix.vanillabalance.module.experience.ExperienceNerfListener;
import dev.smpcristalix.vanillabalance.module.golem.IronGolemNerfListener;
import dev.smpcristalix.vanillabalance.module.heavycore.HeavyCoreNerfListener;
import dev.smpcristalix.vanillabalance.module.ore.OreNerfPopulator;
import dev.smpcristalix.vanillabalance.module.villager.VillagerCoinTradeListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Composition root VanillaBalance. */
public final class VanillaBalancePlugin extends JavaPlugin implements Listener {

    private final Set<UUID> oreWorlds = new HashSet<>();

    private VanillaBalanceSettings settings;
    private OreNerfPopulator orePopulator;
    private AntiXrayAdvisor antiXrayAdvisor;
    private ElytraGenerationListener elytraListener;
    private HeavyCoreNerfListener heavyCoreListener;
    private IronGolemNerfListener golemListener;
    private ExperienceNerfListener experienceListener;
    private VillagerCoinTradeListener villagerListener;
    private boolean antiXrayAuditScheduled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            settings = VanillaBalanceSettings.load(getConfig());
        } catch (IllegalArgumentException ex) {
            getLogger().severe("Ошибка config.yml: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        createModules();
        registerCommand();

        // На STARTUP миров обычно ещё нет, но этот проход делает код устойчивым к необычной загрузке.
        Bukkit.getWorlds().forEach(this::registerOrePopulator);

        getLogger().info("VanillaBalance 1.0.0 включён.");
    }

    private void createModules() {
        orePopulator = settings.oresEnabled() ? new OreNerfPopulator(settings) : null;
        antiXrayAdvisor = new AntiXrayAdvisor(this, settings);
        elytraListener = new ElytraGenerationListener(this, settings);
        heavyCoreListener = new HeavyCoreNerfListener(settings);
        golemListener = new IronGolemNerfListener(settings);
        experienceListener = new ExperienceNerfListener(this, settings);
        villagerListener = new VillagerCoinTradeListener(this, settings);

        var manager = Bukkit.getPluginManager();
        manager.registerEvents(elytraListener, this);
        manager.registerEvents(heavyCoreListener, this);
        manager.registerEvents(golemListener, this);
        manager.registerEvents(experienceListener, this);
        manager.registerEvents(villagerListener, this);

        // Миры к этому моменту могут ещё не быть загружены, поэтому аудит повторяется командой вручную.
        if (settings.antiXrayEnabled() && !Bukkit.getWorlds().isEmpty()) antiXrayAdvisor.logStartupAudit();
    }

    private void registerCommand() {
        var command = getCommand("vanillabalance");
        if (command == null) return;
        var executor = new VanillaBalanceCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        registerOrePopulator(event.getWorld());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!settings.antiXrayEnabled() || antiXrayAuditScheduled) return;
        antiXrayAuditScheduled = true;
        Bukkit.getScheduler().runTask(this, () -> {
            antiXrayAuditScheduled = false;
            antiXrayAdvisor.logStartupAudit();
        });
    }

    private void registerOrePopulator(World world) {
        if (orePopulator == null || !settings.isOreNerfEnabledFor(world)) return;
        if (!oreWorlds.add(world.getUID())) return;
        world.getPopulators().add(orePopulator);
        getLogger().info("Нерф руд подключён к миру до генерации чанков: " + world.getName());
    }

    public String reloadPluginConfiguration() {
        reloadConfig();
        VanillaBalanceSettings loaded;
        try {
            loaded = VanillaBalanceSettings.load(getConfig());
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }

        removeOrePopulator();
        settings = loaded;
        oreWorlds.clear();
        orePopulator = settings.oresEnabled() ? new OreNerfPopulator(settings) : null;
        Bukkit.getWorlds().forEach(this::registerOrePopulator);

        elytraListener.reload(settings);
        heavyCoreListener.reload(settings);
        golemListener.reload(settings);
        experienceListener.reload(settings);
        villagerListener.reload(settings);
        antiXrayAdvisor = new AntiXrayAdvisor(this, settings);
        if (settings.antiXrayEnabled()) antiXrayAdvisor.logStartupAudit();
        return null;
    }

    private void removeOrePopulator() {
        if (orePopulator == null) return;
        for (World world : Bukkit.getWorlds()) {
            world.getPopulators().removeIf(populator -> populator == orePopulator);
        }
    }

    public VanillaBalanceSettings settings() {
        return settings;
    }

    public AntiXrayAdvisor antiXrayAdvisor() {
        return antiXrayAdvisor;
    }

    @Override
    public void onDisable() {
        removeOrePopulator();
        if (villagerListener != null) villagerListener.closeAllSessions();
        oreWorlds.clear();
    }
}
