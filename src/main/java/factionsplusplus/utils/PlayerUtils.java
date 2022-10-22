package factionsplusplus.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PlayerUtils {
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
}