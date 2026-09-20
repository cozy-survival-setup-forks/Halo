package dev.halo.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlowLibraryTest {

    private static final Logger LOG = Logger.getLogger("test");

    private static GlowLibrary library(String text) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return GlowLibrary.load(yaml, LOG);
    }

    @Test
    void oneColourIsASingleGlow() throws Exception {
        Glow glow = library("glows:\n  red:\n    color: red\n").get("red");

        assertEquals(List.of(NamedTextColor.RED), glow.frames());
        assertFalse(glow.animated());
        assertEquals("halo.glow.red", glow.permission());
        assertEquals("Red", glow.display());
    }

    @Test
    void severalColoursCycleInOrder() throws Exception {
        Glow glow = library("glows:\n  rainbow:\n    colors: [red, gold, green]\n    interval: 7\n").get("rainbow");

        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.GREEN), glow.frames());
        assertTrue(glow.animated());
        assertEquals(7, glow.interval());
    }

    @Test
    void aGradientStartsAndEndsOnItsColours() throws Exception {
        Glow glow = library("glows:\n  fade:\n    type: GRADIENT\n    steps: 6\n    mirror: false\n    colors: ['#ff5555', '#5555ff']\n").get("fade");

        assertEquals(NamedTextColor.RED, glow.frames().get(0));
        assertEquals(NamedTextColor.BLUE, glow.frames().get(glow.frames().size() - 1));
        assertEquals(7, glow.frames().size());
    }

    @Test
    void aMirroredGradientFadesBackWithoutRepeatingTheEnds() throws Exception {
        Glow glow = library("glows:\n  fade:\n    type: GRADIENT\n    steps: 4\n    colors: ['#ff5555', '#5555ff']\n").get("fade");

        // 4 fade steps and the end colour, then back down without the two end colours again
        assertEquals(5 + 3, glow.frames().size());
        assertEquals(NamedTextColor.RED, glow.frames().get(0));
        assertEquals(NamedTextColor.BLUE, glow.frames().get(4));
        assertEquals(glow.frames().get(1), glow.frames().get(glow.frames().size() - 1));
    }

    @Test
    void aGradientOverSeveralColoursPassesThroughThem() throws Exception {
        Glow glow = library("glows:\n  three:\n    type: GRADIENT\n    steps: 4\n    mirror: false\n    colors: ['#ff5555', '#55ff55', '#5555ff']\n").get("three");

        assertEquals(NamedTextColor.RED, glow.frames().get(0));
        assertEquals(NamedTextColor.GREEN, glow.frames().get(4));
        assertEquals(NamedTextColor.BLUE, glow.frames().get(glow.frames().size() - 1));
    }

    @Test
    void hexColoursTurnIntoTheClosestGlowColour() throws Exception {
        Glow glow = library("glows:\n  pink:\n    color: '#ff5555'\n").get("pink");

        assertEquals(List.of(NamedTextColor.RED), glow.frames());
    }

    @Test
    void flashingAlternatesColoursWithDarkFrames() throws Exception {
        Glow glow = library("glows:\n  police:\n    type: FLASH\n    colors: [red, blue]\n").get("police");

        assertEquals(4, glow.frames().size());
        assertEquals(NamedTextColor.RED, glow.frames().get(0));
        assertNull(glow.frames().get(1));
        assertEquals(NamedTextColor.BLUE, glow.frames().get(2));
        assertNull(glow.frames().get(3));
    }

    @Test
    void theNameOfAGlowUsesItsFirstColour() throws Exception {
        Glow glow = library("glows:\n  police:\n    type: FLASH\n    colors: [blue, red]\n").get("police");

        assertEquals(NamedTextColor.BLUE, glow.firstColor());
    }

    @Test
    void badEntriesAreSkippedButTheRestStay() throws Exception {
        GlowLibrary library = library("""
                glows:
                  nothing:
                    type: CYCLE
                  wrongtype:
                    type: SPIN
                    colors: [red]
                  mixed:
                    colors: [banana, red]
                  fine:
                    color: blue
                """);

        assertNull(library.get("nothing"));
        assertNull(library.get("wrongtype"));
        assertEquals(List.of(NamedTextColor.RED), library.get("mixed").frames());
        assertNotNull(library.get("fine"));
    }

    @Test
    void idsAndPermissionsAreCleaned() throws Exception {
        Glow glow = library("glows:\n  'My Glow!':\n    color: red\n    permission: 'ranks.{id}'\n").get("myglow");

        assertNotNull(glow);
        assertEquals("ranks.myglow", glow.permission());
    }

    @Test
    void aTooSmallIntervalIsRaised() throws Exception {
        assertEquals(1, library("glows:\n  a:\n    colors: [red, blue]\n    interval: 0\n").get("a").interval());
    }

    @Test
    void coloursCanBeWrittenInAllTheWaysPlayersKnow() {
        assertEquals(NamedTextColor.RED, Colors.nearest(Colors.parse("&c")));
        assertEquals(NamedTextColor.RED, Colors.nearest(Colors.parse("c")));
        assertEquals(NamedTextColor.DARK_RED, Colors.nearest(Colors.parse("dark_red")));
        assertEquals(NamedTextColor.BLUE, Colors.nearest(Colors.parse("&#5555ff")));
        assertNull(Colors.parse("banana"));
        assertNull(Colors.parse("#12"));
        assertNull(Colors.parse(""));
        assertNull(Colors.parse(null));
    }

    @Test
    void theExampleFileLoadsWithoutProblems() {
        var stream = getClass().getResourceAsStream("/glows.yml");
        assertNotNull(stream);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));

        GlowLibrary library = GlowLibrary.load(yaml, new Logger("strict", null) {
            @Override
            public void warning(String msg) {
                throw new AssertionError("glows.yml has a problem: " + msg);
            }
        });

        assertTrue(library.all().size() >= 15);
        for (Glow glow : library.all()) {
            assertFalse(glow.frames().isEmpty(), glow.id());
            assertFalse(glow.display().contains("<"), glow.id());
        }
        assertTrue(library.get("sunset").frames().size() > 10);
    }
}
