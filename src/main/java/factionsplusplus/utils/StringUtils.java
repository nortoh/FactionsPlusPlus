package factionsplusplus.utils;

import java.util.Arrays;

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

    /**
     * The parseAsChatColor method attempts to convert a color string to a bukkit ChatColor
     * @param color string to conver to ChatColor
     * @return resolved ChatColor or null
     * @see org.bukkit.ChatColor
     */
    public static ChatColor parseAsChatColor(String color) {
        return Arrays.stream(ChatColor.values())
            .filter(c -> c.toString().equalsIgnoreCase(color))
            .findFirst()
            .orElse(ChatColor.WHITE);
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Method to pad a value with a zero to its left.
     *
     * @param value to pad
     * @return 00 or 0(0-9) or 10-(very big numbers)
     * @author Callum
     */
    public static String prefixWithZero(Number value) {
        String tmp = String.valueOf(value);
        return tmp.length() == 0 ? ("00") : (tmp.length() == 1 ? ("0" + value) : (tmp));
    }

    /**
     * Method to create an array of objects for parameter parsing.
     * 
     * @param list of values
     * @return Object[]
     */
    public static Object[] arrayFrom(Object... objects) {
        return objects;
    }
}