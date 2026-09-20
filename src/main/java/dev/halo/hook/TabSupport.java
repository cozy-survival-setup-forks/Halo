package dev.halo.hook;

import dev.halo.HaloPlugin;
import dev.halo.glow.Colors;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Gives the glow colour to TAB. TAB sends its own name tag team for every player and the client takes the glow colour
 * from the last colour code of that team's prefix, so the glow colour is put at the end of the prefix TAB already has.
 * Only used when TAB is installed; nothing else touches these classes.
 */
public final class TabSupport {

    private final HaloPlugin plugin;

    public TabSupport(HaloPlugin plugin) {
        this.plugin = plugin;
    }

    /** Starts the glows again when a player finishes loading in TAB, or after /tab reload. */
    public void listen() {
        TabAPI api = TabAPI.getInstance();
        api.getEventBus().register(PlayerLoadEvent.class, event -> {
            Player player = Bukkit.getPlayer(event.getPlayer().getUniqueId());
            if (player != null) refreshLater(player);
        });
        api.getEventBus().register(TabLoadEvent.class, event -> Bukkit.getOnlinePlayers().forEach(this::refreshLater));
    }

    /** These events may come from another thread. */
    private void refreshLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) plugin.glows().refresh(player);
        });
    }

    /** @return false when TAB does not handle name tags for this player, so Halo has to use its own team */
    public boolean apply(Player player, NamedTextColor color) {
        NameTagManager tags = TabAPI.getInstance().getNameTagManager();
        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tags == null || tabPlayer == null || !tabPlayer.isLoaded()) return false;

        String original = tags.getOriginalRawPrefix(tabPlayer);
        tags.setPrefix(tabPlayer, (original == null ? "" : original) + "&" + Colors.code(color));
        return true;
    }

    /** Gives the player the prefix TAB had. */
    public void clear(Player player) {
        NameTagManager tags = TabAPI.getInstance().getNameTagManager();
        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tags != null && tabPlayer != null && tabPlayer.isLoaded()) tags.setPrefix(tabPlayer, null);
    }
}
