package com.combatreplay.replay;

import com.combatreplay.CombatReplayPlugin;
import com.combatreplay.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayManager implements Listener {
    private final CombatReplayPlugin plugin;
    private final Map<UUID, ReplayPlayer> activeReplays = new ConcurrentHashMap<>();

    public ReplayManager(CombatReplayPlugin plugin) {
        this.plugin = plugin;
    }

    public void startReplay(Player viewer, com.combatreplay.combat.CombatSession session) {
        if (activeReplays.containsKey(viewer.getUniqueId())) {
            MessageUtil.send(viewer, plugin.getPluginConfig().getMsg("already-watching"));
            return;
        }

        ReplayPlayer player = new ReplayPlayer(plugin, viewer, session);
        activeReplays.put(viewer.getUniqueId(), player);
        player.start();
        giveHotbarItems(viewer, player);
    }

    public void stopReplay(Player viewer) {
        ReplayPlayer player = activeReplays.get(viewer.getUniqueId());
        if (player != null) {
            player.stop();
        }
    }

    public void onReplayFinish(UUID uuid) {
        activeReplays.remove(uuid);
    }

    private void giveHotbarItems(Player viewer, ReplayPlayer player) {
        viewer.getInventory().clear();
        viewer.getInventory().setItem(0, createItem(Material.ARROW, "&eGeri Sar (5s)"));
        viewer.getInventory().setItem(1, createItem(Material.FEATHER, "&bÖnceki Vuruş"));
        viewer.getInventory().setItem(2, createItem(Material.LIME_DYE, "&aDuraklat / Başlat"));
        viewer.getInventory().setItem(3, createItem(Material.FEATHER, "&bSonraki Vuruş"));
        viewer.getInventory().setItem(4, createItem(Material.ARROW, "&eİleri Sar (5s)"));
        viewer.getInventory().setItem(5, createItem(Material.SUGAR, "&dOynatma Hızı: " + player.getSpeed() + "x"));
        viewer.getInventory().setItem(6, createItem(Material.BARRIER, "&cÇıkış"));
        viewer.getInventory().setHeldItemSlot(2);
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ReplayPlayer player = activeReplays.get(event.getPlayer().getUniqueId());
        if (player == null) return;

        event.setCancelled(true);
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        int slot = event.getPlayer().getInventory().getHeldItemSlot();
        switch (slot) {
            case 0 -> player.jump(-100);
            case 1 -> player.jumpToPrevHit();
            case 2 -> player.setPaused(!player.isPaused());
            case 3 -> player.jumpToNextHit();
            case 4 -> player.jump(100);
            case 5 -> {
                double speed = player.getSpeed();
                if (speed == 1.0) speed = 2.0;
                else if (speed == 2.0) speed = 0.5;
                else speed = 1.0;
                player.setSpeed(speed);
                event.getItem().editMeta(meta -> meta.displayName(MessageUtil.parse("&dOynatma Hızı: " + player.getSpeed() + "x")));
            }
            case 6 -> stopReplay(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopReplay(event.getPlayer());
    }
}
