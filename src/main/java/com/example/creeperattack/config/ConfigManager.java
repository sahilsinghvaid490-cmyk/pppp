package com.example.creeperattack.config;

import com.example.creeperattack.CreeperAttackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads {@code config.yml} into strongly typed, validated fields and
 * exposes helper methods (such as colourised messages) to the rest of the
 * plugin. No gameplay values are hardcoded elsewhere; everything flows
 * through this class.
 */
public final class ConfigManager {

    private final CreeperAttackPlugin plugin;

    private boolean enabled;

    private int cooldownMinutes;
    private int durationSeconds;

    private int spawnAmount;
    private int spawnRadius;
    private int minimumDistance;

    private final Set<String> disabledWorlds = new HashSet<>();

    private boolean creeperPowered;
    private String explosionRadiusSetting;
    private boolean canDropItems;
    private boolean customNameEnabled;
    private String customName;

    public ConfigManager(CreeperAttackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) every value from the currently active
     * {@link FileConfiguration}, applying sane defaults and validation for
     * any missing or invalid entries so the plugin never crashes because of
     * a malformed config.
     */
    public void load() {
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);

        this.cooldownMinutes = Math.max(1, config.getInt("event.cooldown-minutes", 4));
        this.durationSeconds = Math.max(1, config.getInt("event.duration-seconds", 30));

        this.spawnAmount = Math.max(1, config.getInt("spawn.amount", 20));
        this.spawnRadius = Math.max(1, config.getInt("spawn.radius", 15));
        this.minimumDistance = Math.max(0, config.getInt("spawn.minimum-distance", 5));

        if (this.minimumDistance >= this.spawnRadius) {
            plugin.getLogger().warning(
                    "spawn.minimum-distance must be smaller than spawn.radius. Adjusting minimum-distance.");
            this.minimumDistance = Math.max(0, this.spawnRadius - 1);
        }

        this.disabledWorlds.clear();
        List<String> configuredWorlds = config.getStringList("worlds.disabled");
        this.disabledWorlds.addAll(configuredWorlds);

        this.creeperPowered = config.getBoolean("creeper.powered", false);
        this.explosionRadiusSetting = config.getString("creeper.explosion-radius", "default");
        this.canDropItems = config.getBoolean("creeper.can-drop-items", true);
        this.customNameEnabled = config.getBoolean("creeper.custom-name-enabled", false);
        this.customName = config.getString("creeper.custom-name", "&cEvent Creeper");
    }

    /**
     * Reloads {@code config.yml} from disk and re-applies all values.
     * Any exception encountered while reading the file is propagated to
     * the caller so it can be reported to the command sender.
     */
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    public String getMessage(String key) {
        String raw = plugin.getConfig().getString("messages." + key, "");
        return colorize(raw);
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getSpawnAmount() {
        return spawnAmount;
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public int getMinimumDistance() {
        return minimumDistance;
    }

    public Set<String> getDisabledWorlds() {
        return Collections.unmodifiableSet(disabledWorlds);
    }

    public boolean isCreeperPowered() {
        return creeperPowered;
    }

    public String getExplosionRadiusSetting() {
        return explosionRadiusSetting;
    }

    public boolean canDropItems() {
        return canDropItems;
    }

    public boolean isCustomNameEnabled() {
        return customNameEnabled;
    }

    public String getCustomName() {
        return colorize(customName);
    }
}
