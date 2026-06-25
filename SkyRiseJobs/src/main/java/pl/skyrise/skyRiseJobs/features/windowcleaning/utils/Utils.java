package pl.skyrise.skyRiseJobs.features.windowcleaning.utils;

import org.bukkit.ChatColor;

public class Utils {
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}