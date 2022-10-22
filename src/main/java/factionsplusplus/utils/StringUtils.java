package factionsplusplus.utils;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.ChatColor;

public class StringUtils {
    public final static String[] BOOLEAN_VALUES = {"yes", "no", "true", "false", "on", "off"};
    
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

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}