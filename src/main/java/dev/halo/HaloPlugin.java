package dev.halo;

import dev.halo.command.GlowCommand;
import dev.halo.hook.HaloExpansion;
import dev.halo.hook.TabSupport;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

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
        try {
            enableInner();
        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, "Halo could not start, check config.yml, glows.yml and messages.yml for mistakes", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void enableInner() {
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

        if (settings.tabIntegration() && getServer().getPluginManager().isPluginEnabled("TAB")) {
            TabSupport tab = new TabSupport(this);
            glows.useTab(tab);
            tab.listen();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            glows.refresh(player);
        }
        Banner.print(this, "Thanks for letting every server shine a little.");
    }

    @Override
    public void onDisable() {
        if (glows != null) glows.stop();
    }

    /** Reads all the files again. @return how many glows there are, or -1 if a file was broken (the
     * old settings/messages/glows are kept in that case, and nothing is refreshed) */
    public int reloadAll() {
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration loadedConfig = new YamlConfiguration();
        try {
            loadedConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("config.yml is broken, keeping the settings already loaded: " + e.getMessage());
            return -1;
        }
        boolean messagesOk = messages.load();
        boolean glowsOk = glows.load();
        if (!messagesOk || !glowsOk) return -1;

        settings = new Settings(loadedConfig);
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
