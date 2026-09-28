package dev.halo;

import dev.halo.glow.Glow;
import dev.halo.glow.GlowLibrary;
import dev.halo.hook.TabSupport;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Makes players glow. The outline colour comes from the scoreboard team a player is in, so each colour has a
 * team called halo_&lt;colour&gt; and a glowing player is moved into the right one. Only the main thread uses this,
 * except reads through the PlaceholderAPI expansion.
 */
public final class GlowService {

    private static final String TEAM_PREFIX = "halo_";

    /** A glow that is showing on a player right now. */
    private static final class Running {
        final Glow glow;
        int frame;
        volatile @Nullable NamedTextColor shown;
        /** The name of the team the player was in before this glow, or null if they had none. Kept as a
         * name, not the Team object itself: if the team's owner unregisters and re-registers it (a
         * reload), the old object would still pass a name-only "does this team exist" check but would
         * silently be a detached, no-longer-the-real-team instance. */
        final @Nullable String beforeTeamName;
        boolean viaTab;

        Running(Glow glow, @Nullable String beforeTeamName) {
            this.glow = glow;
            this.beforeTeamName = beforeTeamName;
        }
    }

    private final HaloPlugin plugin;
    private final NamespacedKey beforeTeamKey;
    private final Map<UUID, Running> running = new ConcurrentHashMap<>();
    private GlowLibrary library = GlowLibrary.load(new YamlConfiguration(), java.util.logging.Logger.getAnonymousLogger());
    private @Nullable TabSupport tab;
    private BukkitTask ticker;
    private long tick;

    GlowService(HaloPlugin plugin) {
        this.plugin = plugin;
        this.beforeTeamKey = new NamespacedKey(plugin, "before-team");
    }

    void useTab(TabSupport support) {
        this.tab = support;
    }

    /** @return false if glows.yml is broken, in which case the glows already loaded are kept */
    boolean load() {
        File file = new File(plugin.getDataFolder(), "glows.yml");
        if (!file.exists()) plugin.saveResource("glows.yml", false);

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "glows.yml is broken, keeping the glows already loaded", e);
            return false;
        }
        library = GlowLibrary.load(yaml, plugin.getLogger());
        return true;
    }

    void start() {
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void stop() {
        if (ticker != null) ticker.cancel();
        if (tab != null) tab.stop();
        for (UUID id : running.keySet().toArray(UUID[]::new)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) remove(player);
        }
        running.clear();

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX) && team.getEntries().isEmpty()) team.unregister();
        }
    }

    public GlowLibrary library() {
        return library;
    }

    public boolean canUse(Player player, Glow glow) {
        if (!plugin.settings().usePermissions()) return true;
        String permission = glow.permission();
        // isPermissionSet lets an explicit "false" on one glow's permission win even when the player
        // also has halo.glow.* - a plain hasPermission(perm) || hasPermission("halo.glow.*") would let
        // the wildcard override that explicit denial.
        if (player.isPermissionSet(permission)) return player.hasPermission(permission);
        return player.hasPermission("halo.glow.*");
    }

    /** The glow the player should have now: what they picked, if it exists and they may still use it. */
    public @Nullable Glow equipped(Player player) {
        Selections.Pick pick = plugin.selections().get(player);
        if (pick == null) return null;
        Glow glow = library.get(pick.id());
        if (glow == null) return null;
        return pick.given() || canUse(player, glow) ? glow : null;
    }

    public @Nullable Glow showing(Player player) {
        Running current = running.get(player.getUniqueId());
        return current == null ? null : current.glow;
    }

    public @Nullable NamedTextColor colorOf(Player player) {
        Running current = running.get(player.getUniqueId());
        return current == null ? null : current.shown;
    }

    /** Starts the glow the player has picked, or removes the glow if there is none or the world does not allow it. */
    public void refresh(Player player) {
        remove(player);
        if (!player.isOnline()) return;

        if (plugin.settings().isDisabled(player.getWorld())) {
            cleanupOrphan(player);
            return;
        }

        Glow glow = equipped(player);
        if (glow == null) {
            cleanupOrphan(player);
            return;
        }

        Running current = new Running(glow, beforeTeamName(player));
        running.put(player.getUniqueId(), current);
        show(player, current);
    }

    /** Takes the glow off the player, without forgetting what they picked. */
    public void remove(Player player) {
        Running current = running.remove(player.getUniqueId());
        if (current == null) return;

        release(player, current);
        player.setGlowing(false);
    }

    /**
     * Cleans up a halo_ team and the glowing flag left over from before a crash (onDisable never ran to
     * restore them normally) - there is no in-memory Running to release for a player in this state.
     */
    private void cleanupOrphan(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team now = board.getEntryTeam(player.getName());
        if (now == null || !now.getName().startsWith(TEAM_PREFIX)) return;

        now.removeEntry(player.getName());
        restoreSavedTeam(player, board);
        player.setGlowing(false);
    }

    private void tick() {
        tick++;
        for (Map.Entry<UUID, Running> entry : running.entrySet()) {
            Running current = entry.getValue();
            if (!current.glow.animated() || tick % current.glow.interval() != 0) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            current.frame = (current.frame + 1) % current.glow.frames().size();
            show(player, current);
        }
    }

    private void show(Player player, Running current) {
        NamedTextColor color = current.glow.frames().get(current.frame);
        if (color == null) {
            if (current.shown != null) {
                release(player, current);
                player.setGlowing(false);
                current.shown = null;
            }
            return;
        }
        if (color == current.shown) return;

        boolean throughTab = tab != null && tab.apply(player, color);
        if (!throughTab) {
            if (current.beforeTeamName != null) {
                player.getPersistentDataContainer().set(beforeTeamKey, PersistentDataType.STRING, current.beforeTeamName);
            }
            team(color).addEntry(player.getName());
        } else if (!current.viaTab) {
            leaveTeam(player, current);
        }
        current.viaTab = throughTab;
        player.setGlowing(true);
        current.shown = color;
    }

    /** Gives back what the glow changed: the prefix from TAB, or the team the player was in. */
    private void release(Player player, Running current) {
        if (current.viaTab && tab != null) tab.clear(player);
        current.viaTab = false;
        leaveTeam(player, current);
    }

    /** Puts the player back in the team they were in before the glow, if there was one. Only touches
     * teams at all if the player is still on the halo_ team Halo itself put them on - if something else
     * moved them to a different team while they were glowing, that change is left alone. */
    private void leaveTeam(Player player, Running current) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team now = board.getEntryTeam(player.getName());
        if (now == null || !now.getName().startsWith(TEAM_PREFIX)) return;

        now.removeEntry(player.getName());
        if (current.beforeTeamName != null) {
            Team before = board.getTeam(current.beforeTeamName);
            if (before != null) before.addEntry(player.getName());
        }
        player.getPersistentDataContainer().remove(beforeTeamKey);
    }

    private void restoreSavedTeam(Player player, Scoreboard board) {
        String saved = player.getPersistentDataContainer().get(beforeTeamKey, PersistentDataType.STRING);
        if (saved == null) return;
        Team before = board.getTeam(saved);
        if (before != null) before.addEntry(player.getName());
        player.getPersistentDataContainer().remove(beforeTeamKey);
    }

    /** The player's real team, to restore once the glow ends. If they are currently on a halo_ team
     * already (most likely left over from a crash, since onDisable normally restores this on its own),
     * their real team isn't visible right now - fall back to whatever was saved the last time Halo
     * parked them on one. */
    private @Nullable String beforeTeamName(Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        if (team != null && !team.getName().startsWith(TEAM_PREFIX)) return team.getName();
        String saved = player.getPersistentDataContainer().get(beforeTeamKey, PersistentDataType.STRING);
        return saved == null || saved.isEmpty() ? null : saved;
    }

    private static Team team(NamedTextColor color) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String name = TEAM_PREFIX + NamedTextColor.NAMES.key(color);
        Team team = board.getTeam(name);
        if (team == null) team = board.registerNewTeam(name);
        team.color(color);
        return team;
    }
}
