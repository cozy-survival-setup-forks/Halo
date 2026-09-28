package dev.halo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The texts in messages.yml, written in MiniMessage. Old style codes such as &amp;7 and &amp;#rrggbb work too.
 * A message missing from an older file falls back to the default one.
 */
public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final String[] TAGS = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};

    private final HaloPlugin plugin;
    private FileConfiguration file = new YamlConfiguration();

    Messages(HaloPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return false if messages.yml is broken, in which case the messages already loaded are kept */
    boolean load() {
        File target = new File(plugin.getDataFolder(), "messages.yml");
        if (!target.exists()) plugin.saveResource("messages.yml", false);

        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(target);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("messages.yml is broken, keeping the messages already loaded: " + e.getMessage());
            return false;
        }

        var defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
        file = loaded;
        return true;
    }

    /** {@code file.getString(key, "")} never falls back to a bundled default even after setDefaults - the
     * two-argument getString only ever returns the explicit default when the key is unset, it never
     * consults the defaults. The one-argument form does. */
    private String text(String key) {
        String value = file.getString(key);
        return value == null ? "" : value;
    }

    /** Turns &amp; codes into MiniMessage tags. */
    static String convertLegacy(String text) {
        String result = HEX.matcher(text).replaceAll("<#$1>");
        return CODE.matcher(result).replaceAll(match -> Matcher.quoteReplacement(tagFor(match.group(1).charAt(0))));
    }

    private static String tagFor(char code) {
        char lower = Character.toLowerCase(code);
        int index = "0123456789abcdef".indexOf(lower);
        if (index >= 0) return "<" + TAGS[index] + ">";
        return switch (lower) {
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            default -> "<reset>";
        };
    }

    /** Sends a message with the prefix. Empty messages are skipped, so any of them can be turned off. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        if (file.isList(key)) {
            for (String line : file.getStringList(key)) {
                to.sendMessage(MINI.deserialize(convertLegacy(line.replace("{prefix}", text("prefix"))), resolvers));
            }
            return;
        }
        String message = text(key);
        if (message.isEmpty()) return;
        to.sendMessage(component(text("prefix") + message, resolvers));
    }

    private static Component component(String text, TagResolver... resolvers) {
        return MINI.deserialize(convertLegacy(text), resolvers);
    }
}
