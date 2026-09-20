package dev.halo;

import dev.halo.glow.Glow;
import dev.halo.glow.GlowLibrary;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Makes players glow. The outline colour comes from the scoreboard team a player is in, so each colour has a
 * team called halo_&lt;colour&gt; and a glowing player is moved into the right one. Only the main thread uses this.
 */
public final class GlowService {

    private static final String TEAM_PREFIX = "halo_";

    /** A glow that is showing on a player right now. */
    private static final class Running {
        final Glow glow;
        int frame;
        @Nullable NamedTextColor shown;
        @Nullable Team before;

        Running(Glow glow, @Nullable Team before) {
            this.glow = glow;
            this.before = before;
        }
    }

    private final HaloPlugin plugin;
    private final Map<UUID, Running> running = new HashMap<>();
    private GlowLibrary library;
    private BukkitTask ticker;
    private long tick;

    GlowService(HaloPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        File file = new File(plugin.getDataFolder(), "glows.yml");
        if (!file.exists()) plugin.saveResource("glows.yml", false);
        library = GlowLibrary.load(YamlConfiguration.loadConfiguration(file), plugin.getLogger());
    }

    void start() {
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void stop() {
        if (ticker != null) ticker.cancel();
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
        return player.hasPermission(glow.permission()) || player.hasPermission("halo.glow.*");
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
        if (!player.isOnline() || plugin.settings().isDisabled(player.getWorld())) return;

        Glow glow = equipped(player);
        if (glow == null) return;

        Running current = new Running(glow, currentTeam(player));
        running.put(player.getUniqueId(), current);
        show(player, current);
    }

    /** Takes the glow off the player, without forgetting what they picked. */
    public void remove(Player player) {
        Running current = running.remove(player.getUniqueId());
        if (current == null) return;

        leaveTeam(player, current);
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
                leaveTeam(player, current);
                player.setGlowing(false);
                current.shown = null;
            }
            return;
        }
        if (color == current.shown) return;

        team(color).addEntry(player.getName());
        player.setGlowing(true);
        current.shown = color;
    }

    /** Puts the player back in the team they were in before the glow, if there was one. */
    private void leaveTeam(Player player, Running current) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team now = board.getEntryTeam(player.getName());
        if (now != null && now.getName().startsWith(TEAM_PREFIX)) now.removeEntry(player.getName());

        Team before = current.before;
        if (before != null && board.getTeam(before.getName()) != null) before.addEntry(player.getName());
    }

    private static @Nullable Team currentTeam(Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        return team == null || team.getName().startsWith(TEAM_PREFIX) ? null : team;
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
