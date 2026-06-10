package me.qyro.prismarenas.util;

import org.bukkit.Sound;

public final class SoundUtil {

    private SoundUtil() {
    }

    public static Sound parse(String name, Sound fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
