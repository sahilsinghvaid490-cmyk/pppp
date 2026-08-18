package com.example.creeperattack.listener;

import com.example.creeperattack.CreeperAttackPlugin;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Enforces {@code creeper.can-drop-items: false} for creepers spawned by
 * this plugin. Only entities tagged with the plugin's event marker are
 * affected, so naturally spawned creepers are never touched.
 */
public final class CreeperDeathListener implements Listener {

    private final CreeperAttackPlugin plugin;

    public CreeperDeathListener(CreeperAttackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Creeper)) {
            return;
        }

        PersistentDataContainer container = entity.getPersistentDataContainer();
        boolean isEventCreeper = container.has(plugin.getEventCreeperKey(), PersistentDataType.BYTE);
        if (!isEventCreeper) {
            return;
        }

        boolean dropsDisabled = container.has(plugin.getNoDropKey(), PersistentDataType.BYTE);
        if (dropsDisabled) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
