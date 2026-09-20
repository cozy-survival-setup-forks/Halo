package dev.halo.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reading colours, and fitting any colour to the 16 that a glow can have. */
final class Colors {

    private static final String LEGACY_CODES = "0123456789abcdef";
    private static final NamedTextColor[] LEGACY_COLORS = {
            NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
            NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE
    };

    private Colors() {
    }

    /** {@code #ff8800}, {@code &#ff8800}, a name such as {@code dark_red}, or a code such as {@code c} or {@code &c}. */
    static @Nullable TextColor parse(@Nullable String input) {
        if (input == null) return null;
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("&#")) text = text.substring(1);
        if (text.length() == 2 && text.charAt(0) == '&') text = text.substring(1);
        if (text.isEmpty()) return null;

        if (text.charAt(0) == '#') {
            return text.length() == 7 ? TextColor.fromHexString(text) : null;
        }
        if (text.length() == 1) {
            int index = LEGACY_CODES.indexOf(text.charAt(0));
            return index < 0 ? null : LEGACY_COLORS[index];
        }
        return NamedTextColor.NAMES.value(text);
    }

    static NamedTextColor nearest(TextColor color) {
        return color instanceof NamedTextColor named ? named : NamedTextColor.nearestTo(color);
    }

    /**
     * A fade through the colours. Each fade step shows the closest of the 16 colours, so the outline changes only
     * when the fade gets closer to another one.
     */
    static List<NamedTextColor> gradient(List<TextColor> stops, int steps, boolean mirror) {
        List<NamedTextColor> frames = new ArrayList<>();
        for (int i = 0; i < stops.size() - 1; i++) {
            for (int step = 0; step < steps; step++) {
                frames.add(nearest(TextColor.lerp((float) step / steps, stops.get(i), stops.get(i + 1))));
            }
        }
        frames.add(nearest(stops.get(stops.size() - 1)));

        if (mirror) {
            for (int i = frames.size() - 2; i >= 1; i--) {
                frames.add(frames.get(i));
            }
        }
        return frames;
    }
}
