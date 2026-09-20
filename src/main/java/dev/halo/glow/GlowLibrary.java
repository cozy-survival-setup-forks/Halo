package dev.halo.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** The glows of glows.yml, in the order they are written. */
public final class GlowLibrary {

    private final Map<String, Glow> glows;

    private GlowLibrary(Map<String, Glow> glows) {
        this.glows = glows;
    }

    public static GlowLibrary load(FileConfiguration file, Logger log) {
        Map<String, Glow> loaded = new LinkedHashMap<>();
        ConfigurationSection section = file.getConfigurationSection("glows");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                String id = cleanId(key);
                if (entry == null || id.isEmpty()) {
                    log.warning("glows.yml: '" + key + "' is not a glow, skipping it.");
                } else if (loaded.containsKey(id)) {
                    log.warning("glows.yml: '" + key + "' is written twice, the first one is used.");
                } else {
                    Glow glow = parse(id, entry, log);
                    if (glow != null) loaded.put(id, glow);
                }
            }
        }
        return new GlowLibrary(Collections.unmodifiableMap(loaded));
    }

    static String cleanId(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    private static @Nullable Glow parse(String id, ConfigurationSection entry, Logger log) {
        String type = entry.getString("type", "").trim().toUpperCase(Locale.ROOT);
        List<String> names = new ArrayList<>(entry.getStringList("colors"));
        if (entry.isString("color")) names.add(0, entry.getString("color", ""));

        List<NamedTextColor> colors = new ArrayList<>();
        for (String name : names) {
            NamedTextColor color = Colors.parse(name);
            if (color == null) {
                log.warning("glows.yml: " + id + " has the colour '" + name + "', which is not one of the 16 colours. Skipping it.");
            } else {
                colors.add(color);
            }
        }
        if (colors.isEmpty()) {
            log.warning("glows.yml: " + id + " has no colours, skipping it.");
            return null;
        }
        if (type.isEmpty()) type = colors.size() == 1 ? "SINGLE" : "CYCLE";

        List<NamedTextColor> frames = new ArrayList<>();
        int interval;
        switch (type) {
            case "SINGLE" -> {
                frames.add(colors.get(0));
                interval = 20;
            }
            case "CYCLE" -> {
                frames.addAll(colors);
                interval = 10;
            }
            case "BOUNCE" -> {
                frames.addAll(colors);
                for (int i = colors.size() - 2; i >= 1; i--) frames.add(colors.get(i));
                interval = 10;
            }
            case "FLASH" -> {
                for (NamedTextColor color : colors) {
                    frames.add(color);
                    frames.add(null);
                }
                interval = 5;
            }
            default -> {
                log.warning("glows.yml: " + id + " has the type '" + type + "', which is not SINGLE, CYCLE, BOUNCE or FLASH.");
                return null;
            }
        }

        int wanted = entry.getInt("interval", interval);
        if (wanted < 1) {
            log.warning("glows.yml: " + id + " needs an interval of at least 1 tick.");
            wanted = 1;
        }

        String permission = entry.getString("permission", "halo.glow.{id}").replace("{id}", id);
        return new Glow(id, entry.getString("display", prettyName(id)), permission, wanted, Collections.unmodifiableList(frames));
    }

    private static String prettyName(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("[_-]+")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public @Nullable Glow get(String id) {
        return glows.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Glow> all() {
        return glows.values();
    }
}
