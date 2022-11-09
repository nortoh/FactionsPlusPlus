/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.utils.StringUtils;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class ChatHandler implements Listener {
    private final ConfigService configService;
    private final EphemeralData ephemeralData;
    private final DataService dataService;

    @Inject
    public ChatHandler(ConfigService configService, EphemeralData ephemeralData, DataService dataService) {
        this.configService = configService;
        this.ephemeralData = ephemeralData;
        this.dataService = dataService;
    }

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        Faction playersFaction = this.dataService.getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }
        
        if (this.ephemeralData.isPlayerInFactionChat(event.getPlayer())) {
            if (this.configService.getBoolean("chat.faction.sharedInVassalageTrees")) {
                this.dataService.getFactionsInVassalageTree(playersFaction).stream().forEach(faction -> playersFaction.sendToFactionChatAs(faction, event.getPlayer(), event.getMessage()));
                return;
            }
            playersFaction.sendToFactionChatAs(event.getPlayer(), event.getMessage()); 
            event.setCancelled(true);
            return;
        }

        if (this.configService.getBoolean("chat.global.prependFactionPrefix")) {
            event.setFormat(StringUtils.parseAsChatColor(playersFaction.getFlag("prefixColor").toString()) + "" + "[" + playersFaction.getPrefix() + "] " + ChatColor.WHITE + " %s: %s");
        }
    }
}