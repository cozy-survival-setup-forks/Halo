package dev.halo.hook;

import dev.halo.HaloPlugin;
import dev.halo.glow.Glow;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * %halo_id%, %halo_display%, %halo_color%, %halo_active%, and for menus %halo_equipped_&lt;id&gt;% and
 * %halo_owned_&lt;id&gt;%.
 */
public final class HaloExpansion extends PlaceholderExpansion {

    private final HaloPlugin plugin;

    public HaloExpansion(HaloPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "halo";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Halo";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        String request = params.toLowerCase(Locale.ROOT);
        Glow equipped = plugin.glows().equipped(player);

        if (request.equals("id")) return equipped == null ? "none" : equipped.id();
        if (request.equals("display")) return equipped == null ? "None" : equipped.display();
        if (request.equals("active")) return String.valueOf(plugin.glows().showing(player) != null);
        if (request.equals("color")) {
            NamedTextColor color = plugin.glows().colorOf(player);
            return color == null ? "none" : NamedTextColor.NAMES.key(color);
        }
        if (request.startsWith("equipped_")) {
            return String.valueOf(equipped != null && equipped.id().equals(request.substring(9)));
        }
        if (request.startsWith("owned_")) {
            Glow glow = plugin.glows().library().get(request.substring(6));
            return String.valueOf(glow != null && plugin.glows().canUse(player, glow));
        }
        return null;
    }
}
