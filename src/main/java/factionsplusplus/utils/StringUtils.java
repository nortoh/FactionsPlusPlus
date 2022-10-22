package factionsplusplus.utils;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class StringUtils {
    public static Integer parseAsInteger(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double parseAsDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean parseAsBoolean(String string) {
        final String[] trueValues = {"yes", "true", "y", "on"};
        final String[] falseValues = {"no", "off", "false", "n"};
        if (Arrays.asList(trueValues).contains(string.toLowerCase())) return true;
        if (Arrays.asList(falseValues).contains(string.toLowerCase())) return false;
        return null;
    }

    @SuppressWarnings("deprecation")
    public static OfflinePlayer parseAsPlayer(String playerName) {
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player.hasPlayedBefore() || Bukkit.getPlayer(player.getUniqueId()) != null) return player;
        return null;
    }

    public static OfflinePlayer parseAsPlayer(UUID playerUUID) {
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (player.hasPlayedBefore() || Bukkit.getPlayer(player.getUniqueId()) != null) return player;
        return null;
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}