package dev.halo.glow;

import dev.halo.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

/**
 * One glow from glows.yml. The frames are the colours it shows one after the other; a null frame means the
 * glow is off for that moment.
 */
public record Glow(String id, String display, String permission, int interval, List<NamedTextColor> frames) {

    public boolean animated() {
        return frames.size() > 1;
    }

    /** The first colour of the glow, used to colour its name in menus and messages. */
    public NamedTextColor firstColor() {
        for (NamedTextColor frame : frames) {
            if (frame != null) return frame;
        }
        return NamedTextColor.WHITE;
    }

    /** The display name with MiniMessage and old &amp; codes applied; the glow's first colour is the fallback colour. */
    public Component displayComponent() {
        return MiniMessage.miniMessage().deserialize(Messages.convertLegacy(display)).colorIfAbsent(firstColor());
    }
}
