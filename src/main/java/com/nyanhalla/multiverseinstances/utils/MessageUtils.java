package com.nyanhalla.multiverseinstances.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtils {

    private MessageUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        return message.replace("&", "§");
    }

    public static Component colorizeToComponent(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(colorize(message));
    }
}
