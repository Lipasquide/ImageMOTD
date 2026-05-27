package com.combatreplay.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FakeEntityManager {
    private final Player viewer;
    private final Map<String, Integer> entityIds = new HashMap<>();
    private final Map<String, UUID> entityUuids = new HashMap<>();

    public FakeEntityManager(Player viewer) {
        this.viewer = viewer;
    }

    public void spawnPlayer(String name, UUID uuid, double x, double y, double z, float yaw, float pitch) {
        int id = ThreadLocalRandom.current().nextInt(100000, 200000);
        entityIds.put(uuid.toString(), id);
        entityUuids.put(uuid.toString(), uuid);

        UserProfile profile = new UserProfile(uuid, name);
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile, true, 0, GameMode.SURVIVAL, null, null);

        WrapperPlayServerPlayerInfoUpdate infoUpdate = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER), List.of(info));

        WrapperPlayServerSpawnPlayer spawn = new WrapperPlayServerSpawnPlayer(
                id, uuid, new com.github.retrooper.packetevents.protocol.world.Location(new Vector3d(x, y, z), yaw, pitch));

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, infoUpdate);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawn);
    }

    public void teleportEntity(String uuidStr, double x, double y, double z, float yaw, float pitch) {
        Integer id = entityIds.get(uuidStr);
        if (id == null) return;

        WrapperPlayServerEntityTeleport tp = new WrapperPlayServerEntityTeleport(
                id, new Vector3d(x, y, z), yaw, pitch, true);

        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(id, yaw);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, tp);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, head);
    }

    public void playAnimation(String uuidStr, int animation) {
        Integer id = entityIds.get(uuidStr);
        if (id == null) return;

        WrapperPlayServerEntityAnimation.EntityAnimationType type = (animation == 1) ?
                WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM :
                WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;

        WrapperPlayServerEntityAnimation anim = new WrapperPlayServerEntityAnimation(id, type);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, anim);
    }

    public void spawnDamageIndicator(double x, double y, double z, double damage) {
        int id = ThreadLocalRandom.current().nextInt(200000, 300000);
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                id, UUID.randomUUID(), EntityTypes.ARMOR_STAND, new com.github.retrooper.packetevents.protocol.world.Location(new Vector3d(x, y + 1.5, z), 0, 0), 0, 0, null);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawn);

        // Metadata for invisibility and custom name
        List<EntityData<?>> data = new ArrayList<>();
        // Invisibility is in bitmask at index 0
        data.add(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20));
        // Custom name
        data.add(new EntityData(2, EntityDataTypes.OPTIONAL_COMPONENT, Optional.of(net.kyori.adventure.text.Component.text("§c-" + String.format("%.1f", damage)))));
        // Custom name visible
        data.add(new EntityData(3, EntityDataTypes.BOOLEAN, true));
        // Small armor stand, no base plate, etc. at index 15
        data.add(new EntityData(15, EntityDataTypes.BYTE, (byte) 0x11));

        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(id, data);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadata);

        Bukkit.getScheduler().runTaskLater(com.combatreplay.CombatReplayPlugin.getInstance(), () -> {
            despawnEntity(id);
        }, 20);
    }

    private void despawnEntity(int id) {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(id);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroy);
    }

    public void cleanup() {
        if (!entityIds.isEmpty()) {
            int[] ids = entityIds.values().stream().mapToInt(i -> i).toArray();
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(ids);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroy);

            List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> infoList = entityUuids.values().stream()
                    .map(u -> new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(new UserProfile(u, null), false, 0, null, null, null))
                    .toList();
            WrapperPlayServerPlayerInfoUpdate removeTab = new WrapperPlayServerPlayerInfoUpdate(
                    EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED), infoList);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, removeTab);
        }
    }
}
