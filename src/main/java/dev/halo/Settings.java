package dev.halo;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The values of config.yml. */
public final class Settings {

    private final boolean usePermissions;
    private final Set<String> disabledWorlds = new HashSet<>();

    Settings(FileConfiguration config) {
        usePermissions = config.getBoolean("use-permissions", true);
        List<String> worlds = config.getStringList("disabled-worlds");
        worlds.forEach(world -> disabledWorlds.add(world.toLowerCase(Locale.ROOT)));
    }

    public boolean usePermissions() {
        return usePermissions;
    }

    public boolean isDisabled(World world) {
        return disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }
}
