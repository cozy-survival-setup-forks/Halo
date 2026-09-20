package dev.halo;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/**
 * The glow each player picked, kept on the player itself so it survives restarts. A glow given by an admin is
 * marked, so it stays even if the player has no permission for it.
 */
public final class Selections {

    public record Pick(String id, boolean given) {
    }

    private final NamespacedKey glowKey;
    private final NamespacedKey givenKey;

    Selections(HaloPlugin plugin) {
        this.glowKey = new NamespacedKey(plugin, "glow");
        this.givenKey = new NamespacedKey(plugin, "given");
    }

    public @Nullable Pick get(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        String id = data.get(glowKey, PersistentDataType.STRING);
        if (id == null) return null;
        return new Pick(id, data.has(givenKey, PersistentDataType.BYTE));
    }

    public void set(Player player, String id, boolean given) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(glowKey, PersistentDataType.STRING, id);
        if (given) {
            data.set(givenKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            data.remove(givenKey);
        }
    }

    public void clear(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(glowKey);
        data.remove(givenKey);
    }
}
