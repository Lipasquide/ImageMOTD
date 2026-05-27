package com.combatreplay.combat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CombatSession {
    private final String code;
    private final String timestamp;
    private final String world;
    private final List<PlayerInfo> players;
    private final List<FrameData> frames;
    private transient long startTime;
    private transient long lastHitTime;
    private long durationMs;

    public CombatSession(String code, String world, UUID p1Uuid, String p1Name, UUID p2Uuid, String p2Name) {
        this.code = code;
        this.timestamp = Instant.now().toString();
        this.world = world;
        this.players = List.of(
                new PlayerInfo(p1Uuid.toString(), p1Name),
                new PlayerInfo(p2Uuid.toString(), p2Name)
        );
        this.frames = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
        this.lastHitTime = startTime;
    }

    public String getCode() { return code; }
    public String getTimestamp() { return timestamp; }
    public String getWorld() { return world; }
    public List<PlayerInfo> getPlayers() { return players; }
    public List<FrameData> getFrames() { return frames; }
    public long getStartTime() { return startTime; }
    public long getLastHitTime() { return lastHitTime; }
    public void updateLastHit() { this.lastHitTime = System.currentTimeMillis(); }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public long getDurationMs() { return durationMs; }

    public record PlayerInfo(String uuid, String name) {}
}
