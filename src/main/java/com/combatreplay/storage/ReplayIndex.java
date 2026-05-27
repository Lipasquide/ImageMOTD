package com.combatreplay.storage;

import java.util.List;

public record ReplayIndex(List<Entry> entries) {
    public record Entry(
            String code,
            String timestamp,
            List<String> playerNames,
            long durationMs
    ) {}
}
