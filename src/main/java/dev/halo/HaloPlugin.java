package dev.halo;

import dev.halo.command.GlowCommand;
import dev.halo.hook.HaloExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Halo: glowing outlines for players. The glows are in glows.yml, and menus are left to a menu plugin.
 */
public final class HaloPlugin extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private Selections selections;
    private GlowService glows;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(getConfig());
        messages = new Messages(this);
        messages.load();
        selections = new Selections(this);
        glows = new GlowService(this);
        glows.load();
        glows.start();

        getServer().getPluginManager().registerEvents(new HaloListener(this), this);

        GlowCommand command = new GlowCommand(this);
        var glow = getCommand("glow");
        if (glow != null) {
            glow.setExecutor(command);
            glow.setTabCompleter(command);
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new HaloExpansion(this).register();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            glows.refresh(player);
        }
    }

    @Override
    public void onDisable() {
        if (glows != null) glows.stop();
    }

    /** Reads all the files again. Returns how many glows there are. */
    public int reloadAll() {
        reloadConfig();
        settings = new Settings(getConfig());
        messages.load();
        glows.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            glows.refresh(player);
        }
        return glows.library().all().size();
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public Selections selections() {
        return selections;
    }

    public GlowService glows() {
        return glows;
    }
}
