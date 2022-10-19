/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.builders.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import preponderous.ponder.minecraft.bukkit.tools.ColorChecker;

import java.util.ArrayList;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class ChatHandler implements Listener {
    private final PersistentData persistentData;
    private final ConfigService configService;
    private final EphemeralData ephemeralData;
    private final MessageService messageService;

    private String factionChatColor;
    private String prefixColor;
    private String prefix;
    private String message;

    @Inject
    public ChatHandler(PersistentData persistentData, ConfigService configService, EphemeralData ephemeralData, MessageService messageService) {
        this.persistentData = persistentData;
        this.configService = configService;
        this.ephemeralData = ephemeralData;
        this.messageService = messageService;
    }

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        Faction playersFaction = persistentData.getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }
        initializeValues(playersFaction, event);
        if (configService.getBoolean("playersChatWithPrefixes")) {
            addPrefix(event, prefixColor, prefix);
        }
        if (ephemeralData.isPlayerInFactionChat(event.getPlayer())) {
            sendMessage(playersFaction, prefixColor, prefix, event, factionChatColor, message);
            event.setCancelled(true);
        }
    }

    private void initializeValues(Faction playersFaction, AsyncPlayerChatEvent event) {
        factionChatColor = configService.getString("factionChatColor");
        prefixColor = playersFaction.getFlag("prefixColor").toString();
        prefix = playersFaction.getPrefix();
        message = event.getMessage();
    }

    private void sendMessage(Faction playersFaction, String prefixColor, String prefix, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        if (configService.getBoolean("chatSharedInVassalageTrees")) {
            sendMessageToVassalageTree(playersFaction, prefixColor, prefix, event, factionChatColor, message);
        } else {
            sendMessageToFaction(playersFaction, prefix, prefixColor, event, factionChatColor, message);
        }
    }

    private void addPrefix(AsyncPlayerChatEvent event, String prefixColor, String prefix) {
        ColorChecker colorChecker = new ColorChecker();
        event.setFormat(colorChecker.getColorByName(prefixColor) + "" + "[" + prefix + "] " + ChatColor.WHITE + " %s: %s");
    }

    private void sendMessageToVassalageTree(Faction playersFaction, String prefixColor, String prefix, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        ArrayList<Faction> factionsInVassalageTree = persistentData.getFactionsInVassalageTree(playersFaction);
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