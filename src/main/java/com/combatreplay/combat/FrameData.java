package com.combatreplay.combat;

import java.util.List;

public record FrameData(
        long timestamp, // ms offset from start
        PlayerState p1,
        PlayerState p2,
        HitEvent hit // null if no hit this frame
) {
    public record PlayerState(
            double x, double y, double z,
            float yaw, float pitch,
            String mainHand, String offHand,
            List<String> armor,
            boolean sneaking, boolean sprinting,
            int animation // 0: none, 1: swing, etc.
    ) {}

    public record HitEvent(
            String attackerUuid,
            String victimUuid,
            double damage
    ) {}
}
