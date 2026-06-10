package me.qyro.prismarenas.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {

    public enum Format {
        AUTO,
        LEGACY,
        MINIMESSAGE
    }

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {
    }

    public static Component parse(String input, Format format) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return switch (format) {
            case LEGACY -> LEGACY.deserialize(input);
            case MINIMESSAGE -> MINI_MESSAGE.deserialize(input);
            case AUTO -> parseAuto(input);
        };
    }

    private static Component parseAuto(String input) {
        if (looksLikeMiniMessage(input)) {
            try {
                return MINI_MESSAGE.deserialize(input);
            } catch (Exception ignored) {
                return LEGACY.deserialize(input);
            }
        }
        return LEGACY.deserialize(input);
    }

    private static boolean looksLikeMiniMessage(String input) {
        int index = input.indexOf('<');
        while (index >= 0 && index < input.length() - 1) {
            int end = input.indexOf('>', index);
            if (end > index) {
                String tag = input.substring(index + 1, end);
                if (!tag.isEmpty() && tag.charAt(0) != ' ') {
                    return true;
                }
            }
            index = input.indexOf('<', index + 1);
        }
        return false;
    }

    public static String replacePlaceholders(String input, String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholder pairs must be even.");
        }
        String result = input;
        for (int i = 0; i < pairs.length; i += 2) {
            result = result.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return result;
    }
}
