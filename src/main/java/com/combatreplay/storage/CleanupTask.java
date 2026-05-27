package com.combatreplay.storage;

import com.combatreplay.CombatReplayPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CleanupTask implements Runnable {
    private final ReplayStorage storage;
    private final CombatReplayPlugin plugin;

    public CleanupTask(ReplayStorage storage, CombatReplayPlugin plugin) {
        this.storage = storage;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Instant threshold = Instant.now().minus(plugin.getPluginConfig().getRetentionDays(), ChronoUnit.DAYS);
            int deletedCount = 0;

            for (ReplayIndex.Entry entry : storage.getRecentReplays()) {
                Instant ts = Instant.parse(entry.timestamp());
                if (ts.isBefore(threshold)) {
                    storage.deleteSession(entry.code());
                    deletedCount++;
                }
            }

            if (deletedCount > 0) {
                plugin.getLogger().info("Cleaned up " + deletedCount + " old replays.");
            }

            // Check max replays
            int max = plugin.getPluginConfig().getMaxReplays();
            if (storage.getRecentReplays().size() > max) {
                int toDelete = storage.getRecentReplays().size() - max;
                storage.getRecentReplays().stream()
                        .limit(toDelete)
                        .forEach(e -> storage.deleteSession(e.code()));
                plugin.getLogger().info("Cleaned up " + toDelete + " replays to maintain storage limit.");
            }
        });
    }
}
