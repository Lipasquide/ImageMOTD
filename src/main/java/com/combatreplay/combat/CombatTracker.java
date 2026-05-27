package com.combatreplay.combat;

import com.combatreplay.CombatReplayPlugin;
import com.combatreplay.util.CodeGenerator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTracker implements Listener {
    private final CombatReplayPlugin plugin;
    private final Map<String, CombatSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingAnimations = new ConcurrentHashMap<>();
    private final Map<String, FrameData.HitEvent> pendingHits = new ConcurrentHashMap<>();

    public CombatTracker(CombatReplayPlugin plugin) {
        this.plugin = plugin;
        startRecordingTask();
    }

    private void startRecordingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long timeout = plugin.getPluginConfig().getInactivityTimeout() * 1000L;

            activeSessions.entrySet().removeIf(entry -> {
                CombatSession session = entry.getValue();
                if (now - session.getLastHitTime() > timeout) {
                    finishSession(session);
                    return true;
                }

                recordFrame(session);
                return false;
            });
        }, 1L, 1L);
    }

    private void recordFrame(CombatSession session) {
        Player p1 = Bukkit.getPlayer(UUID.fromString(session.getPlayers().get(0).uuid()));
        Player p2 = Bukkit.getPlayer(UUID.fromString(session.getPlayers().get(1).uuid()));

        if (p1 == null || p2 == null) return;

        long offset = System.currentTimeMillis() - session.getStartTime();

        FrameData.PlayerState s1 = captureState(p1);
        FrameData.PlayerState s2 = captureState(p2);

        FrameData.HitEvent hit = pendingHits.remove(session.getCode());

        session.getFrames().add(new FrameData(offset, s1, s2, hit));
    }

    private FrameData.PlayerState captureState(Player p) {
        int anim = pendingAnimations.getOrDefault(p.getUniqueId(), 0);
        pendingAnimations.remove(p.getUniqueId());

        return new FrameData.PlayerState(
                p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(),
                p.getLocation().getYaw(), p.getLocation().getPitch(),
                p.getInventory().getItemInMainHand().getType().name(),
                p.getInventory().getItemInOffHand().getType().name(),
                getArmor(p),
                p.isSneaking(), p.isSprinting(),
                anim
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) return;

        String key = getSessionKey(attacker.getUniqueId(), victim.getUniqueId());
        CombatSession session = activeSessions.computeIfAbsent(key, k -> {
            String code = CodeGenerator.generate(6);
            return new CombatSession(code, attacker.getWorld().getName(),
                    attacker.getUniqueId(), attacker.getName(),
                    victim.getUniqueId(), victim.getName());
        });

        session.updateLastHit();
        pendingHits.put(session.getCode(), new FrameData.HitEvent(attacker.getUniqueId().toString(), victim.getUniqueId().toString(), event.getDamage()));
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        pendingAnimations.put(event.getPlayer().getUniqueId(), 1);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        terminatePlayerSessions(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        terminatePlayerSessions(event.getPlayer().getUniqueId());
    }

    private void terminatePlayerSessions(UUID uuid) {
        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getKey().contains(uuid.toString())) {
                finishSession(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void finishSession(CombatSession session) {
        int hits = (int) session.getFrames().stream().filter(f -> f.hit() != null).count();
        if (hits >= plugin.getPluginConfig().getMinHits()) {
            session.setDurationMs(System.currentTimeMillis() - session.getStartTime());
            plugin.getStorage().saveSession(session);
        }
        pendingHits.remove(session.getCode());
    }

    private String getSessionKey(UUID u1, UUID u2) {
        List<String> uuids = Arrays.asList(u1.toString(), u2.toString());
        Collections.sort(uuids);
        return uuids.get(0) + ":" + uuids.get(1);
    }

    private List<String> getArmor(Player p) {
        return Arrays.stream(p.getInventory().getArmorContents())
                .map(item -> item == null ? "AIR" : item.getType().name())
                .toList();
    }
}
