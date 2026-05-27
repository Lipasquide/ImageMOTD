package com.combatreplay.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    private final FileConfiguration config;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
    }

    public int getInactivityTimeout() { return config.getInt("combat.inactivity-timeout-seconds", 10); }
    public int getMinHits() { return config.getInt("combat.min-hits-to-save", 2); }
    public int getRetentionDays() { return config.getInt("storage.retention-days", 7); }
    public int getCleanupInterval() { return config.getInt("storage.cleanup-interval-hours", 6); }
    public int getMaxReplays() { return config.getInt("storage.max-replays-stored", 500); }
    public double getDefaultSpeed() { return config.getDouble("playback.default-speed", 1.0); }
    public boolean showDamageIndicators() { return config.getBoolean("playback.show-damage-indicators", true); }
    public String getPrefix() { return config.getString("messages.prefix", "&8[&bCombatReplay&8] "); }
    public String getMsg(String path) { return config.getString("messages." + path, ""); }
}
