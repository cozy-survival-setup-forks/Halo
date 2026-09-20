package dev.halo.command;

import dev.halo.HaloPlugin;
import dev.halo.glow.Glow;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /glow equip &lt;id&gt;, unequip, list, reload, and for admins admin set &lt;player&gt; &lt;id&gt; and admin clear &lt;player&gt;.
 */
public final class GlowCommand implements TabExecutor {

    private static final String USE = "halo.use";
    private static final String ADMIN = "halo.admin";

    private final HaloPlugin plugin;

    public GlowCommand(HaloPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "equip" -> equip(sender, args);
            case "unequip" -> unequip(sender);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "admin" -> admin(sender, args);
            default -> plugin.messages().send(sender, "usage");
        }
        return true;
    }

    private void equip(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return;
        }
        if (!sender.hasPermission(USE)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        Glow glow = args.length < 2 ? null : plugin.glows().library().get(args[1]);
        if (glow == null) {
            plugin.messages().send(sender, "unknown-glow");
            return;
        }
        if (!plugin.glows().canUse(player, glow)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }

        plugin.selections().set(player, glow.id(), false);
        plugin.glows().refresh(player);
        plugin.messages().send(sender, "equipped", Placeholder.component("glow", glow.displayComponent()));
    }

    private void unequip(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return;
        }
        plugin.selections().clear(player);
        plugin.glows().refresh(player);
        plugin.messages().send(sender, "unequipped");
    }

    private void list(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return;
        }
        List<Glow> usable = plugin.glows().library().all().stream().filter(glow -> plugin.glows().canUse(player, glow)).toList();
        if (usable.isEmpty()) {
            plugin.messages().send(sender, "list-empty");
            return;
        }
        plugin.messages().send(sender, "list-header");
        for (Glow glow : usable) {
            plugin.messages().send(sender, "list-entry",
                    Placeholder.component("glow", glow.displayComponent()), Placeholder.unparsed("id", glow.id()));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        int count = plugin.reloadAll();
        plugin.messages().send(sender, "reloaded", Placeholder.unparsed("count", String.valueOf(count)));
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        String action = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        Player target = args.length < 3 ? null : Bukkit.getPlayerExact(args[2]);
        if (!action.equals("set") && !action.equals("clear")) {
            plugin.messages().send(sender, "usage");
            return;
        }
        if (target == null) {
            plugin.messages().send(sender, "player-not-found");
            return;
        }

        if (action.equals("clear")) {
            plugin.selections().clear(target);
            plugin.glows().refresh(target);
            plugin.messages().send(sender, "admin-cleared", Placeholder.unparsed("player", target.getName()));
            return;
        }

        Glow glow = args.length < 4 ? null : plugin.glows().library().get(args[3]);
        if (glow == null) {
            plugin.messages().send(sender, "unknown-glow");
            return;
        }
        plugin.selections().set(target, glow.id(), true);
        plugin.glows().refresh(target);
        plugin.messages().send(sender, "admin-set",
                Placeholder.unparsed("player", target.getName()), Placeholder.component("glow", glow.displayComponent()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("equip", "unequip", "list"));
            if (sender.hasPermission(ADMIN)) options.addAll(List.of("reload", "admin"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("equip") && sender instanceof Player player) {
            plugin.glows().library().all().stream().filter(glow -> plugin.glows().canUse(player, glow)).forEach(glow -> options.add(glow.id()));
        } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission(ADMIN)) {
            if (args.length == 2) options.addAll(List.of("set", "clear"));
            if (args.length == 3) Bukkit.getOnlinePlayers().forEach(online -> options.add(online.getName()));
            if (args.length == 4 && args[1].equalsIgnoreCase("set")) plugin.glows().library().all().forEach(glow -> options.add(glow.id()));
        }

        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }
}
