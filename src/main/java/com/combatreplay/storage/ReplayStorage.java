package com.combatreplay.storage;

import com.combatreplay.CombatReplayPlugin;
import com.combatreplay.combat.CombatSession;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReplayStorage {
    private final CombatReplayPlugin plugin;
    private final Gson gson;
    private final File replaysDir;
    private final File indexFile;
    private ReplayIndex index;

    public ReplayStorage(CombatReplayPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.replaysDir = new File(plugin.getDataFolder(), "replays");
        this.indexFile = new File(plugin.getDataFolder(), "index.json");
        loadIndex();
        startCleanupTask();
    }

    private void loadIndex() {
        if (indexFile.exists()) {
            try (FileReader reader = new FileReader(indexFile)) {
                index = gson.fromJson(reader, ReplayIndex.class);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load index.json: " + e.getMessage());
            }
        }
        if (index == null) index = new ReplayIndex(new ArrayList<>());
    }

    private void saveIndex() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (index) {
                try (FileWriter writer = new FileWriter(indexFile)) {
                    gson.toJson(index, writer);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to save index.json: " + e.getMessage());
                }
            }
        });
    }

    public void saveSession(CombatSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(replaysDir, session.getCode() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(session, writer);

                synchronized (index) {
                    index.entries().add(new ReplayIndex.Entry(
                            session.getCode(),
                            Instant.now().toString(),
                            session.getPlayers().stream().map(CombatSession.PlayerInfo::name).toList(),
                            session.getDurationMs()
                    ));
                }
                saveIndex();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save replay " + session.getCode() + ": " + e.getMessage());
            }
        });
    }

    public Optional<CombatSession> loadSession(String code) {
        File file = new File(replaysDir, code + ".json");
        if (!file.exists()) return Optional.empty();
        try (FileReader reader = new FileReader(file)) {
            return Optional.ofNullable(gson.fromJson(reader, CombatSession.class));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load replay " + code + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public void deleteSession(String code) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(replaysDir, code + ".json");
            if (file.exists()) file.delete();
            synchronized (index) {
                index.entries().removeIf(e -> e.code().equals(code));
            }
            saveIndex();
        });
    }

    public List<ReplayIndex.Entry> getRecentReplays() {
        synchronized (index) {
            return new ArrayList<>(index.entries());
        }
    }

    private void startCleanupTask() {
        long interval = plugin.getPluginConfig().getCleanupInterval() * 3600L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, new CleanupTask(this, plugin), 20L, interval);
    }
}
