package com.example.creeperattack.task;

import com.example.creeperattack.manager.EventManager;
import com.example.creeperattack.manager.SpawnManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Repeating task that drives a single creeper-attack event. Each execution
 * ("round") asks the {@link SpawnManager} to spawn one creeper near every
 * eligible player. Once the configured number of rounds has completed, the
 * task cancels itself and notifies the {@link EventManager} that the event
 * has finished naturally.
 */
public final class CreeperSpawnTask extends BukkitRunnable {

    private final EventManager eventManager;
    private final SpawnManager spawnManager;
    private final int totalRounds;

    private int roundsCompleted;

    public CreeperSpawnTask(EventManager eventManager, SpawnManager spawnManager, int totalRounds) {
        this.eventManager = eventManager;
        this.spawnManager = spawnManager;
        this.totalRounds = totalRounds;
    }

    @Override
    public void run() {
        if (roundsCompleted >= totalRounds) {
            this.cancel();
            eventManager.onSpawnTaskFinished();
            return;
        }

        spawnManager.spawnRoundForAllPlayers();
        roundsCompleted++;
    }
}
