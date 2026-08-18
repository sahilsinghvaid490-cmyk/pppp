package com.example.creeperattack;

import com.example.creeperattack.command.CreeperAttackCommand;
import com.example.creeperattack.config.ConfigManager;
import com.example.creeperattack.listener.CreeperDeathListener;
import com.example.creeperattack.manager.EventManager;
import com.example.creeperattack.manager.SpawnManager;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the CreeperAttack plugin.
 *
 * <p>Wires together the configuration, spawn, and event management
 * subsystems and exposes the shared {@link NamespacedKey} instances used to
 * tag entities spawned by this plugin.</p>
 */
public final class CreeperAttackPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SpawnManager spawnManager;
    private EventManager eventManager;

    private NamespacedKey eventCreeperKey;
    private NamespacedKey noDropKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.eventCreeperKey = new NamespacedKey(this, "event_creeper");
        this.noDropKey = new NamespacedKey(this, "no_drops");

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.spawnManager = new SpawnManager(this, configManager);
        this.eventManager = new EventManager(this, configManager, spawnManager);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CreeperDeathListener(this), this);

        CreeperAttackCommand commandExecutor = new CreeperAttackCommand(this, configManager, eventManager);
        PluginCommand command = getCommand("creeperattack");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        } else {
            getLogger().warning("Failed to register command 'creeperattack'. Check plugin.yml.");
        }

        eventManager.start();

        getLogger().info("CreeperAttack has been enabled. Automatic events every "
                + configManager.getCooldownMinutes() + " minute(s).");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        getLogger().info("CreeperAttack has been disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public NamespacedKey getEventCreeperKey() {
        return eventCreeperKey;
    }

    public NamespacedKey getNoDropKey() {
        return noDropKey;
    }
}
