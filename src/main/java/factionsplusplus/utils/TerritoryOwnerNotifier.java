/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Faction;
import factionsplusplus.services.ActionBarService;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.LocaleService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.ColorChecker;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class TerritoryOwnerNotifier {
    private final LocaleService localeService;
    private final ConfigService configService;
    private final ActionBarService actionBarService;

    @Inject
    public TerritoryOwnerNotifier(LocaleService localeService, ConfigService configService, ActionBarService actionBarService) {
        this.localeService = localeService;
        this.configService = configService;
        this.actionBarService = actionBarService;
    }

    public void sendPlayerTerritoryAlert(Player player, Faction holder) {
        ChatColor territoryAlertColor = getColor(holder);
        String title = getTitle(holder);
        setActionBar(holder, player, territoryAlertColor, title);
        sendAlert(player, territoryAlertColor, title);
    }

    private String getTitle(Faction holder) {
        if (holder != null) {
            return holder.getName();
        } else {
            return this.localeService.get("Wilderness");
        }
    }

    private ChatColor getColor(Faction holder) {
        String territoryAlertColorString;
        if (holder != null) {
            territoryAlertColorString = holder.getFlag("territoryAlertColor").toString();
        } else {
            territoryAlertColorString = configService.getString("territoryAlertColor");
        }
        ColorChecker colorChecker = new ColorChecker();
        return colorChecker.getColorByName(territoryAlertColorString);
    }

    private void setActionBar(Faction holder, Player player, ChatColor territoryAlertColor, String title) {
        if (configService.getBoolean("territoryIndicatorActionbar")) {

            if (holder == null) {
                actionBarService.clearPlayerActionBar(player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(territoryAlertColor + title));
            } else {
                actionBarService.showPersistentActionBarMessage(player, new TextComponent(territoryAlertColor + title));
            }
        }
    }

    private void sendAlert(Player player, ChatColor territoryAlertColor, String title) {
        if (configService.getBoolean("territoryAlertPopUp")) {
            int fadeIn = 10;
            int stay = 70;
            int fadeOut = 20;
            player.sendTitle(territoryAlertColor + title, null, fadeIn, stay, fadeOut);
        } else {
            player.sendMessage(territoryAlertColor + title);
        }
    }
}