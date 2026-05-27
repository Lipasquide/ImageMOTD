package com.combatreplay.commands;

import com.combatreplay.CombatReplayPlugin;
import com.combatreplay.storage.ReplayIndex;
import com.combatreplay.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CombatReplayCommand implements CommandExecutor {
    private final CombatReplayPlugin plugin;

    public CombatReplayCommand(CombatReplayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "watch" -> handleWatch(sender, args);
            case "list" -> handleList(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleWatch(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can watch replays.");
            return;
        }
        if (!player.hasPermission("creplay.watch")) {
            MessageUtil.send(player, "&cNo permission.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "&cUsage: /creplay watch <code>");
            return;
        }

        plugin.getStorage().loadSession(args[1]).ifPresentOrElse(
                session -> plugin.getReplayManager().startReplay(player, session),
                () -> MessageUtil.send(player, plugin.getPluginConfig().getMsg("replay-not-found"))
        );
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("creplay.list")) {
            MessageUtil.send(sender, "&cNo permission.");
            return;
        }

        List<ReplayIndex.Entry> replays = plugin.getStorage().getRecentReplays();
        if (args.length > 1) {
            String filter = args[1].toLowerCase();
            replays = replays.stream()
                    .filter(e -> e.playerNames().stream().anyMatch(n -> n.toLowerCase().contains(filter)))
                    .toList();
        }

        MessageUtil.send(sender, "&8--- &bRecent Replays &8---");
        for (ReplayIndex.Entry e : replays) {
            MessageUtil.send(sender, "&b" + e.code() + " &7- &f" + String.join(", ", e.playerNames()) + " &7(" + (e.durationMs() / 1000) + "s)");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("creplay.admin")) {
            MessageUtil.send(sender, "&cNo permission.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /creplay delete <code>");
            return;
        }

        plugin.getStorage().deleteSession(args[1]);
        MessageUtil.send(sender, "&aReplay " + args[1] + " deleted.");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("creplay.admin")) {
            MessageUtil.send(sender, "&cNo permission.");
            return;
        }
        plugin.reloadConfig();
        plugin.getPluginConfig().reload(plugin.getConfig());
        MessageUtil.send(sender, "&aConfiguration reloaded.");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("creplay.watch")) {
            MessageUtil.send(sender, "&cNo permission.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /creplay info <code>");
            return;
        }

        plugin.getStorage().loadSession(args[1]).ifPresentOrElse(
                session -> {
                    MessageUtil.send(sender, "&bCode: &f" + session.getCode());
                    MessageUtil.send(sender, "&bPlayers: &f" + session.getPlayers().get(0).name() + ", " + session.getPlayers().get(1).name());
                    MessageUtil.send(sender, "&bDate: &f" + session.getTimestamp());
                    MessageUtil.send(sender, "&bDuration: &f" + (session.getDurationMs() / 1000) + "s");
                    MessageUtil.send(sender, "&bHits: &f" + session.getFrames().stream().filter(f -> f.hit() != null).count());
                },
                () -> MessageUtil.send(sender, plugin.getPluginConfig().getMsg("replay-not-found"))
        );
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, "&b&lCombatReplay Help");
        MessageUtil.send(sender, "&b/creplay watch <code> &7- Watch a replay");
        MessageUtil.send(sender, "&b/creplay list [player] &7- List replays");
        MessageUtil.send(sender, "&b/creplay delete <code> &7- Delete a replay");
        MessageUtil.send(sender, "&b/creplay info <code> &7- View replay info");
        MessageUtil.send(sender, "&b/creplay reload &7- Reload config");
    }
}
