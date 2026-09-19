package dev.smpcristalix.vanillabalance.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaBalanceSettingsTest {

    @Test
    void defaultConfigMatchesApprovedBalance() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertTrue(stream != null);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        VanillaBalanceSettings settings = VanillaBalanceSettings.load(yaml);
        assertEquals(15.0, settings.oreReductionPercent());
        assertEquals(20.0, settings.heavyCoreReductionPercent());
        assertEquals(3, settings.elytraKeepOneIn());
        assertEquals(50.0, settings.ironGolemReductionPercent());
        assertEquals(0.5, settings.experienceMultiplier());
        assertTrue(settings.heavyCoreOminousOnly());
    }

    @Test
    void rejectsInvalidPercent() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("modules.ores.enabled", false);
        yaml.set("modules.ores.reduction-percent", 101.0);
        assertThrows(IllegalArgumentException.class, () -> VanillaBalanceSettings.load(yaml));
    }
}
