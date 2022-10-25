/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import preponderous.ponder.minecraft.bukkit.tools.ColorChecker;

import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class ChatHandler implements Listener {
    private final ConfigService configService;
    private final EphemeralData ephemeralData;
    private final MessageService messageService;
    private final DataService dataService;

    private String factionChatColor;
    private String prefixColor;
    private String prefix;
    private String message;

    @Inject
    public ChatHandler(ConfigService configService, EphemeralData ephemeralData, MessageService messageService, DataService dataService) {
        this.configService = configService;
        this.ephemeralData = ephemeralData;
        this.messageService = messageService;
        this.dataService = dataService;
    }

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        Faction playersFaction = this.dataService.getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }
        this.initializeValues(playersFaction, event);
        if (this.configService.getBoolean("playersChatWithPrefixes")) {
            this.addPrefix(event, prefixColor, prefix);
        }
        if (this.ephemeralData.isPlayerInFactionChat(event.getPlayer())) {
            this.sendMessage(playersFaction, prefixColor, prefix, event, factionChatColor, message);
            event.setCancelled(true);
        }
    }

    private void initializeValues(Faction playersFaction, AsyncPlayerChatEvent event) {
        this.factionChatColor = this.configService.getString("factionChatColor");
        this.prefixColor = playersFaction.getFlag("prefixColor").toString();
        this.prefix = playersFaction.getPrefix();
        this.message = event.getMessage();
    }

    private void sendMessage(Faction playersFaction, String prefixColor, String prefix, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        if (this.configService.getBoolean("chatSharedInVassalageTrees")) {
            this.sendMessageToVassalageTree(playersFaction, prefixColor, prefix, event, factionChatColor, message);
        } else {
            this.sendMessageToFaction(playersFaction, prefix, prefixColor, event, factionChatColor, message);
        }
    }

    private void addPrefix(AsyncPlayerChatEvent event, String prefixColor, String prefix) {
        ColorChecker colorChecker = new ColorChecker();
        event.setFormat(colorChecker.getColorByName(prefixColor) + "" + "[" + prefix + "] " + ChatColor.WHITE + " %s: %s");
    }

    private void sendMessageToVassalageTree(Faction playersFaction, String prefixColor, String prefix, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        List<Faction> factionsInVassalageTree = this.dataService.getFactionsInVassalageTree(playersFaction);
        ColorChecker colorChecker = new ColorChecker();
        for (Faction faction : factionsInVassalageTree) {
            if (configService.getBoolean("showPrefixesInFactionChat")) {
                this.messageService.sendToFaction(faction, colorChecker.getColorByName(prefixColor) + "" + "[" + prefix + "] " + "" + ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + colorChecker.getColorByName(factionChatColor) + message);
            } else {
                this.messageService.sendToFaction(faction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + colorChecker.getColorByName(factionChatColor) + message);
            }
        }
    }

    private void sendMessageToFaction(Faction playersFaction, String prefix, String prefixColor, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        ColorChecker colorChecker = new ColorChecker();
        if (configService.getBoolean("showPrefixesInFactionChat")) {
            this.messageService.sendToFaction(playersFaction, colorChecker.getColorByName(prefixColor) + "" + "[" + prefix + "] " + "" + ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + colorChecker.getColorByName(factionChatColor) + message);
        } else {
            this.messageService.sendToFaction(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + colorChecker.getColorByName(factionChatColor) + message);
        }
    }
}