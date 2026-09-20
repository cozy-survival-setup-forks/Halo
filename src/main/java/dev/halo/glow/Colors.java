package dev.halo.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Reading the 16 colours a glow can have. */
public final class Colors {

    private static final String LEGACY_CODES = "0123456789abcdef";
    private static final NamedTextColor[] LEGACY_COLORS = {
            NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
            NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE
    };

    private Colors() {
    }

    /** A colour name such as {@code dark_red}, or a code such as {@code c} or {@code &c}. */
    static @Nullable NamedTextColor parse(@Nullable String input) {
        if (input == null) return null;
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (text.length() == 2 && text.charAt(0) == '&') text = text.substring(1);
        if (text.length() == 1) {
            int index = LEGACY_CODES.indexOf(text.charAt(0));
            return index < 0 ? null : LEGACY_COLORS[index];
        }
        return NamedTextColor.NAMES.value(text);
    }

    /** The old style colour code of one of the 16 colours, such as 'c' for red. */
    public static char code(NamedTextColor color) {
        for (int i = 0; i < LEGACY_COLORS.length; i++) {
            if (LEGACY_COLORS[i] == color) return LEGACY_CODES.charAt(i);
        }
        return 'f';
    }
}
