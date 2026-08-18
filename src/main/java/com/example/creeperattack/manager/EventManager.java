package com.example.creeperattack.manager;

import com.example.creeperattack.CreeperAttackPlugin;
import com.example.creeperattack.config.ConfigManager;
import com.example.creeperattack.task.CreeperSpawnTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns the repeating "cooldown" scheduler that automatically triggers
 * creeper-attack events, as well as the lifecycle of the currently active
 * event (if any). Only ever keeps at most two live scheduler tasks: the
 * cooldown timer and, while an event is running, the active spawn task.
 */
public final class EventManager {

    private final CreeperAttackPlugin plugin;
    private final ConfigManager configManager;
    private final SpawnManager spawnManager;

    private BukkitTask cooldownTask;
    private CreeperSpawnTask activeSpawnTask;
    private boolean eventActive;

    public EventManager(CreeperAttackPlugin plugin, ConfigManager configManager, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.spawnManager = spawnManager;
    }

    /**
     * Starts (or restarts) the repeating cooldown timer that automatically
     * triggers events. Safe to call multiple times (e.g. after a reload
     * that changes the cooldown) - any previous timer is cancelled first so
     * duplicate timers never accumulate.
     */
    public void start() {
        if (cooldownTask != null) {
            cooldownTask.cancel();
            cooldownTask = null;
        }

        long periodTicks = configManager.getCooldownMinutes() * 60L * 20L;

        cooldownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!configManager.isEnabled()) {
                return;
            }
            if (eventActive) {
                plugin.getLogger().info("Skipped a scheduled creeper attack because one is already active.");
                return;
            }
            beginEvent();
        }, periodTicks, periodTicks);
    }

    /**
     * Called by the {@code start} subcommand. Immediately begins an event
     * without touching the automatic cooldown timer, replacing any event
     * that is already running.
     *
     * @return {@code true} if the event was started, {@code false} if the
     *         plugin is disabled via configuration.
     */
    public boolean startManualEvent() {
        if (!configManager.isEnabled()) {
            return false;
        }
        if (eventActive && activeSpawnTask != null) {
            safeCancel(activeSpawnTask);
        }
        beginEvent();
        return true;
    }

    /**
     * Called by the {@code stop} subcommand. Cancels the active spawn task
     * and clears event state.
     *
     * @return {@code true} if an event was stopped, {@code false} if none was active.
     */
    public boolean stopManualEvent() {
        if (!eventActive) {
            return false;
        }
        endEvent(true);
        return true;
    }

    /** Invoked by {@link CreeperSpawnTask} once it has completed all of its rounds. */
    public void onSpawnTaskFinished() {
        endEvent(false);
    }

    public boolean isEventActive() {
        return eventActive;
    }

    /** Cancels every scheduled task. Called from {@code onDisable}. */
    public void shutdown() {
        if (cooldownTask != null) {
            cooldownTask.cancel();
            cooldownTask = null;
        }
        endEvent(true);
    }

    private void beginEvent() {
        int amount = configManager.getSpawnAmount();
        long durationTicks = configManager.getDurationSeconds() * 20L;
        long periodTicks = Math.max(1L, durationTicks / amount);

        eventActive = true;
        activeSpawnTask = new CreeperSpawnTask(this, spawnManager, amount);
        activeSpawnTask.runTaskTimer(plugin, 0L, periodTicks);

        broadcast(configManager.getMessage("event-started"));
    }

    /**
     * @param cancelTask whether the active spawn task still needs to be
     *                   cancelled. {@code false} when called from
     *                   {@link #onSpawnTaskFinished()}, since the task has
     *                   already cancelled itself at that point.
     */
    private void endEvent(boolean cancelTask) {
        if (activeSpawnTask != null) {
            if (cancelTask) {
                safeCancel(activeSpawnTask);
            }
            activeSpawnTask = null;
        }

        if (eventActive) {
            eventActive = false;
            broadcast(configManager.getMessage("event-ended"));
        }
    }

    private void safeCancel(CreeperSpawnTask task) {
        try {
            task.cancel();
        } catch (IllegalStateException ignored) {
            // Task was already cancelled (e.g. it finished naturally moments ago).
        }
    }

    private void broadcast(String message) {
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage(message);
        }
    }
}
