package com.combatreplay.combat;

import java.util.List;

public record HitData(
        int tick,
        String attackerUuid,
        String victimUuid,
        Pos attackerPos,
        Pos victimPos,
        double damage,
        String weapon,
        List<String> attackerArmor,
        List<String> victimArmor
) {
    public record Pos(double x, double y, double z, float yaw, float pitch) {}
}
