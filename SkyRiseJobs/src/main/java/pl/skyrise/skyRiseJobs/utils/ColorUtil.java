package pl.skyrise.skyRiseJobs.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.builder()
            .hexColors()
            .character('&')
            .build();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .hexColors()
            .character('§')
            .build();

    private ColorUtil() {}

    public static Component mini(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI.deserialize(text);
    }

    /** Automatycznie parsuje MiniMessage (<red>) albo legacy (&c / &#RRGGBB). */
    public static Component auto(String text) {
        if (text == null || text.isEmpty()) return Component.empty().decoration(TextDecoration.ITALIC, false);
        if (text.contains("<") && text.contains(">")) return mini(text).decoration(TextDecoration.ITALIC, false);
        return legacy(text);
    }

    public static Component legacy(String text) {
        if (text == null || text.isEmpty()) return Component.empty().decoration(TextDecoration.ITALIC, false);
        return LEGACY_AMPERSAND.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static String legacyColor(String text) {
        if (text == null || text.isEmpty()) return "";
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(text));
    }

    public static String legacyToMini(String text) {
        if (text == null || text.isEmpty()) return "";
        return MINI.serialize(LEGACY_AMPERSAND.deserialize(text));
    }

    public static List<String> legacyColor(List<String> lines) {
        List<String> colored = new ArrayList<>();
        if (lines == null) return colored;
        for (String line : lines) colored.add(legacyColor(line));
        return colored;
    }

    public static String legacyStrip(String text) {
        return plain(legacy(text));
    }

    public static String escapeMini(String text) {
        if (text == null || text.isEmpty()) return "";
        return MINI.escapeTags(text);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
