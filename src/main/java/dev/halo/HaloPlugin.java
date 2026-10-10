package dev.halo;

import dev.halo.command.GlowCommand;
import dev.halo.hook.HaloExpansion;
import dev.halo.hook.TabSupport;
import dev.halo.safe.ConfigMigrator;
import dev.halo.safe.Doctor;
import dev.halo.safe.FileBackups;
import dev.halo.safe.Guard;
import dev.halo.safe.Health;
import dev.halo.safe.Prep;
import dev.halo.safe.ServerId;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Halo: glowing outlines for players. The glows are in glows.yml, and menus are left to a menu plugin.
 */
public final class HaloPlugin extends JavaPlugin {

    private static final int CONFIG_VERSION = 1;
    private static final int LANG_VERSION = 1;

    private final List<Prep.Spec> files = List.of(
            new Prep.Spec("config.yml", "config-version", CONFIG_VERSION, Prep.configMigrator(CONFIG_VERSION), null),
            new Prep.Spec("messages.yml", "lang-version", LANG_VERSION, new ConfigMigrator("lang-version", LANG_VERSION), null));

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
        Health.storage("YAML files in the plugin folder (config.yml, messages.yml, glows.yml, data.yml)");
        Prep.startup(this, files);
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
        boolean beacon = getConfig().getBoolean("metrics.enabled", true);
        Metrics.start(this, ServerId.resolve(getDataFolder().toPath(),
                ServerId.inYaml(new File(getDataFolder(), "data.yml").toPath(), getLogger()), beacon, getLogger()));
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
        List<Guard.Problem> problems = Prep.validate(this, files);
        if (!problems.isEmpty()) {
            Prep.logRejected(this, problems);
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

    /** The text of /glow doctor. */
    public List<String> doctor() {
        List<String> extra = new ArrayList<>(Prep.versionLines(this, files));
        extra.add("Pending writes: 0 (this plugin keeps no queued saves)");
        return Doctor.report(getName(), getPluginMeta().getVersion(), extra);
    }

    /** /glow backup now: a verified copy of the settings and data files. */
    public boolean backupNow() {
        List<String> names = new ArrayList<>(Prep.fileNames(files));
        names.add("glows.yml");
        names.add("data.yml");
        return FileBackups.snapshot(getDataFolder().toPath(), names, 5, getLogger());
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
