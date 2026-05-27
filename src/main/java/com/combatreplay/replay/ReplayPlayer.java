package com.combatreplay.replay;

import com.combatreplay.CombatReplayPlugin;
import com.combatreplay.combat.CombatSession;
import com.combatreplay.combat.FrameData;
import com.combatreplay.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class ReplayPlayer {
    private final CombatReplayPlugin plugin;
    private final Player viewer;
    private final CombatSession session;
    private final FakeEntityManager entityManager;

    private BukkitTask task;
    private int currentFrame = 0;
    private boolean paused = false;
    private double speed = 1.0;

    private Location originalLocation;
    private GameMode originalGameMode;
    private ItemStack[] originalInventory;

    public ReplayPlayer(CombatReplayPlugin plugin, Player viewer, CombatSession session) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.session = session;
        this.entityManager = new FakeEntityManager(viewer);
        this.speed = plugin.getPluginConfig().getDefaultSpeed();
    }

    public void start() {
        saveState();
        prepareViewer();
        spawnFakePlayers();

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        MessageUtil.send(viewer, plugin.getPluginConfig().getMsg("replay-started"));
    }

    private void tick() {
        if (paused) return;
        if (currentFrame >= session.getFrames().size()) {
            stop();
            return;
        }

        FrameData frame = session.getFrames().get(currentFrame);
        updateEntities(frame);
        updateActionBar();

        // Handle speed
        if (speed == 1.0) currentFrame++;
        else if (speed == 2.0) currentFrame += 2;
        else if (speed == 0.5) {
            // Logic for half speed (alternate ticks)
            if (Bukkit.getCurrentTick() % 2 == 0) currentFrame++;
        } else {
            currentFrame++;
        }
    }

    private void updateEntities(FrameData frame) {
        CombatSession.PlayerInfo p1Info = session.getPlayers().get(0);
        CombatSession.PlayerInfo p2Info = session.getPlayers().get(1);

        entityManager.teleportEntity(p1Info.uuid(), frame.p1().x(), frame.p1().y(), frame.p1().z(), frame.p1().yaw(), frame.p1().pitch());
        entityManager.teleportEntity(p2Info.uuid(), frame.p2().x(), frame.p2().y(), frame.p2().z(), frame.p2().yaw(), frame.p2().pitch());

        if (frame.p1().animation() > 0) entityManager.playAnimation(p1Info.uuid(), frame.p1().animation());
        if (frame.p2().animation() > 0) entityManager.playAnimation(p2Info.uuid(), frame.p2().animation());

        if (frame.hit() != null && plugin.getPluginConfig().showDamageIndicators()) {
            FrameData.PlayerState victim = frame.hit().victimUuid().equals(p1Info.uuid()) ? frame.p1() : frame.p2();
            entityManager.spawnDamageIndicator(victim.x(), victim.y(), victim.z(), frame.hit().damage());
        }
    }

    private void updateActionBar() {
        long currentMs = session.getFrames().get(Math.min(currentFrame, session.getFrames().size()-1)).timestamp();
        long totalMs = session.getDurationMs();
        String timeStr = formatTime(currentMs) + " / " + formatTime(totalMs);
        viewer.sendActionBar(MessageUtil.parse("&bReplay: " + timeStr + " | Hız: " + speed + "x"));
    }

    private String formatTime(long ms) {
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    private void saveState() {
        originalLocation = viewer.getLocation();
        originalGameMode = viewer.getGameMode();
        originalInventory = viewer.getInventory().getContents();
    }

    private void prepareViewer() {
        viewer.setGameMode(GameMode.ADVENTURE);
        viewer.setAllowFlight(true);
        viewer.setFlying(true);
        viewer.setInvisible(true);

        FrameData firstFrame = session.getFrames().get(0);
        viewer.teleport(new Location(Bukkit.getWorld(session.getWorld()), firstFrame.p1().x(), firstFrame.p1().y() + 2, firstFrame.p1().z()));
    }

    private void spawnFakePlayers() {
        FrameData f = session.getFrames().get(0);
        entityManager.spawnPlayer(session.getPlayers().get(0).name(), UUID.fromString(session.getPlayers().get(0).uuid()), f.p1().x(), f.p1().y(), f.p1().z(), f.p1().yaw(), f.p1().pitch());
        entityManager.spawnPlayer(session.getPlayers().get(1).name(), UUID.fromString(session.getPlayers().get(1).uuid()), f.p2().x(), f.p2().y(), f.p2().z(), f.p2().yaw(), f.p2().pitch());
    }

    public void stop() {
        if (task != null) task.cancel();
        entityManager.cleanup();
        restoreState();
        plugin.getReplayManager().onReplayFinish(viewer.getUniqueId());
        MessageUtil.send(viewer, plugin.getPluginConfig().getMsg("replay-finished"));
    }

    private void restoreState() {
        viewer.teleport(originalLocation);
        viewer.setGameMode(originalGameMode);
        viewer.setAllowFlight(originalGameMode == GameMode.CREATIVE || originalGameMode == GameMode.SPECTATOR);
        viewer.setInvisible(false);
        viewer.getInventory().setContents(originalInventory);
    }

    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isPaused() { return paused; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getSpeed() { return speed; }
    public void jump(int frames) {
        currentFrame = Math.clamp(currentFrame + frames, 0, session.getFrames().size() - 1);
    }

    public void jumpToNextHit() {
        for (int i = currentFrame + 1; i < session.getFrames().size(); i++) {
            if (session.getFrames().get(i).hit() != null) {
                currentFrame = i;
                return;
            }
        }
    }

    public void jumpToPrevHit() {
        for (int i = currentFrame - 1; i >= 0; i--) {
            if (session.getFrames().get(i).hit() != null) {
                currentFrame = i;
                return;
            }
        }
    }
}
