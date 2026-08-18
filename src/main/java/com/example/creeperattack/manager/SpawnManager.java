package com.example.creeperattack.manager;

import com.example.creeperattack.CreeperAttackPlugin;
import com.example.creeperattack.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * Responsible for picking safe spawn locations around eligible players and
 * spawning real {@link Creeper} entities configured according to
 * {@code config.yml}.
 */
public final class SpawnManager {

    /** Number of location attempts before we give up on a single spawn and move on. */
    private static final int MAX_LOCATION_ATTEMPTS = 10;

    private final CreeperAttackPlugin plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    public SpawnManager(CreeperAttackPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Attempts to spawn one creeper near every currently eligible online
     * player. Called once per "round" by {@link com.example.creeperattack.task.CreeperSpawnTask}.
     */
    public void spawnRoundForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEligible(player)) {
                spawnNearPlayer(player);
            }
        }
    }

    /**
     * Determines whether a player should currently be targeted by the event:
     * online, alive, not bypassing, and not in a disabled world.
     */
    public boolean isEligible(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        if (player.hasPermission("creeperattack.bypass")) {
            return false;
        }
        World world = player.getWorld();
        return world != null && !configManager.getDisabledWorlds().contains(world.getName());
    }

    private void spawnNearPlayer(Player player) {
        World world = player.getWorld();
        Location origin = player.getLocation();

        for (int attempt = 0; attempt < MAX_LOCATION_ATTEMPTS; attempt++) {
            Location candidate = randomLocationAround(world, origin);
            if (candidate != null && isSafeLocation(candidate)) {
                spawnCreeper(candidate);
                return;
            }
        }
        // No safe location was found within the attempt budget for this round.
        // We simply skip this player for this round rather than risk an unsafe
        // spawn or repeatedly hammering the world for a location.
    }

    private Location randomLocationAround(World world, Location origin) {
        int minDistance = configManager.getMinimumDistance();
        int radius = configManager.getSpawnRadius();
        int range = Math.max(1, radius - minDistance);

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minDistance + (random.nextDouble() * range);

        double x = origin.getX() + (distance * Math.cos(angle));
        double z = origin.getZ() + (distance * Math.sin(angle));

        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return null;
        }

        int highestY = world.getHighestBlockYAt(blockX, blockZ);
        return new Location(world, blockX + 0.5, highestY + 1, blockZ + 0.5);
    }

    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        if (location.getBlockY() <= world.getMinHeight() || location.getBlockY() >= world.getMaxHeight() - 1) {
            return false;
        }

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);

        if (!feet.isPassable() || !head.isPassable()) {
            return false;
        }

        Material groundType = ground.getType();
        if (!groundType.isSolid() || groundType == Material.LAVA) {
            return false;
        }

        Material feetType = feet.getType();
        Material headType = head.getType();
        return feetType != Material.LAVA && feetType != Material.WATER
                && headType != Material.LAVA && headType != Material.WATER;
    }

    private void spawnCreeper(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawn(location, Creeper.class, creeper -> {
            creeper.setPowered(configManager.isCreeperPowered());
            applyExplosionRadius(creeper);
            applyCustomName(creeper);
            tagAsEventCreeper(creeper);
        });
    }

    private void applyExplosionRadius(Creeper creeper) {
        String setting = configManager.getExplosionRadiusSetting();
        if (setting == null || setting.equalsIgnoreCase("default")) {
            return;
        }
        try {
            int explosionRadius = Integer.parseInt(setting.trim());
            creeper.setExplosionRadius(Math.max(0, explosionRadius));
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Invalid creeper.explosion-radius value '" + setting
                    + "', keeping vanilla default.");
        }
    }

    private void applyCustomName(Creeper creeper) {
        if (!configManager.isCustomNameEnabled()) {
            return;
        }
        creeper.setCustomName(configManager.getCustomName());
        creeper.setCustomNameVisible(true);
    }

    private void tagAsEventCreeper(Creeper creeper) {
        creeper.getPersistentDataContainer().set(
                plugin.getEventCreeperKey(), PersistentDataType.BYTE, (byte) 1);

        if (!configManager.canDropItems()) {
            creeper.getPersistentDataContainer().set(
                    plugin.getNoDropKey(), PersistentDataType.BYTE, (byte) 1);
        }
    }
}
