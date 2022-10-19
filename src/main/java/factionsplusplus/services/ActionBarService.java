/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Singleton;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class ActionBarService {
    private final Map<Player, TextComponent> playerActionBarMessages = new HashMap<>();

    public void schedule(Plugin plugin) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                this::sendPlayerActionBarMessages, 5, 20);
    }

    public void showPersistentActionBarMessage(Player player, TextComponent message) {
        playerActionBarMessages.put(player, message);
        this.sendPlayerActionBarMessage(player, message);
    }

    public void clearPlayerActionBar(Player player) {
        playerActionBarMessages.remove(player);
        this.sendPlayerActionBarMessage(player, new TextComponent(""));
    }

    private void sendPlayerActionBarMessages() {
        playerActionBarMessages.forEach(this::sendPlayerActionBarMessage);
    }

    private void sendPlayerActionBarMessage(Player player, TextComponent message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
    }
}
