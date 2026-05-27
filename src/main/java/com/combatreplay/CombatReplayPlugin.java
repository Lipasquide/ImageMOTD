package com.combatreplay;

import com.combatreplay.combat.CombatTracker;
import com.combatreplay.commands.CombatReplayCommand;
import com.combatreplay.config.PluginConfig;
import com.combatreplay.storage.ReplayStorage;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CombatReplayPlugin extends JavaPlugin {
    private static CombatReplayPlugin instance;
    private PluginConfig pluginConfig;
    private ReplayStorage storage;
    private CombatTracker tracker;
    private com.combatreplay.replay.ReplayManager replayManager;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginConfig = new PluginConfig(getConfig());

        File replaysDir = new File(getDataFolder(), "replays");
        if (!replaysDir.exists()) replaysDir.mkdirs();

        storage = new ReplayStorage(this);
        tracker = new CombatTracker(this);
        replayManager = new com.combatreplay.replay.ReplayManager(this);

        getServer().getPluginManager().registerEvents(tracker, this);
        getServer().getPluginManager().registerEvents(replayManager, this);
        getCommand("creplay").setExecutor(new CombatReplayCommand(this));

        PacketEvents.getAPI().init();
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public static CombatReplayPlugin getInstance() { return instance; }
    public PluginConfig getPluginConfig() { return pluginConfig; }
    public ReplayStorage getStorage() { return storage; }
    public CombatTracker getTracker() { return tracker; }
    public com.combatreplay.replay.ReplayManager getReplayManager() { return replayManager; }
}
