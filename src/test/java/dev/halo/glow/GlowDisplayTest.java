package dev.halo.glow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlowDisplayTest {

    private static Glow glow(String display) {
        return new Glow("x", display, "halo.glow.x", 10, List.of(NamedTextColor.RED));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void plainNameUsesTheFirstColour() {
        Component name = glow("Rainbow").displayComponent();

        assertEquals("Rainbow", plain(name));
        assertEquals(NamedTextColor.RED, name.color());
    }

    @Test
    void legacyCodesAreApplied() {
        Component name = glow("&eRainbow").displayComponent();

        assertEquals("Rainbow", plain(name));
        assertEquals(NamedTextColor.YELLOW, name.color());
    }

    @Test
    void legacyHexIsApplied() {
        Component name = glow("&#FF5073Rainbow").displayComponent();

        assertEquals("Rainbow", plain(name));
        assertEquals(TextColor.fromHexString("#FF5073"), name.color());
    }

    @Test
    void gradientTagsAreParsedNotShownRaw() {
        assertEquals("Rainbow", plain(glow("<gradient:#FF5073:#77ECF0>Rainbow").displayComponent()));
    }
}
