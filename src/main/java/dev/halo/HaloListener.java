package dev.halo;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffectType;

/** Gives the glow back when a player joins, respawns or changes world, and takes it off when they leave. */
public final class HaloListener implements Listener {

    private final HaloPlugin plugin;

    HaloListener(HaloPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        later(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        later(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        later(event.getPlayer());
    }

    /** An invisibility effect starting, changing or ending (also expiring, milk, death) turns the glow off or on. */
    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.CLEARED && event.getModifiedType() != PotionEffectType.INVISIBILITY) return;
        later(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.glows().remove(event.getPlayer());
    }

    /** One tick later, when the player is really in the world. */
    private void later(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) plugin.glows().refresh(player);
        });
    }
}
