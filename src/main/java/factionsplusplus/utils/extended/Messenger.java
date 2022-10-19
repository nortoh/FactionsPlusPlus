/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils.extended;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.LocaleService;
import factionsplusplus.services.MessageService;
import factionsplusplus.services.PlayerService;
import factionsplusplus.builders.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class Messenger extends preponderous.ponder.minecraft.bukkit.tools.Messenger {
    @Inject private LocaleService localeService;
    @Inject private PlayerService playerService;
    @Inject private MessageService messageService;
    @Inject private FactionsPlusPlus factionsPlusPlus;
    @Inject private ConfigService configService;
    @Inject private FactionService factionService;
    @Inject private FactionRepository factionRepository;

    public void sendFactionInfo(CommandSender sender, Faction faction, int power) {
        UUIDChecker uuidChecker = new UUIDChecker();
        localeService.getStrings("FactionInfo").forEach(s -> {
            s = s.replace("#faction#", faction.getName())
                    .replace("#name#", faction.getName())
                    .replace("#owner#", uuidChecker.findPlayerNameBasedOnUUID(faction.getOwner()))
                    .replace("#desc#", faction.getDescription())
                    .replace("#pplt#", String.valueOf(faction.getPopulation()))
                    .replace("#aw#", faction.getAlliesSeparatedByCommas())
                    .replace("#aww#", faction.getEnemiesSeparatedByCommas())
                    .replace("#pl#", String.valueOf(this.factionService.getCumulativePowerLevel(faction)))
                    .replace("#pl_max#", String.valueOf(this.factionService.getMaximumCumulativePowerLevel(faction)))
                    .replace("#number#", String.valueOf(power))
                    .replace("#max#", String.valueOf(this.factionService.getCumulativePowerLevel(faction)));
            messageService.send(sender, s);
        });
        sendLiegeInfoIfVassal(faction, sender);
        sendLiegeInfoIfLiege(faction, sender);
        sendBonusPowerInfo(faction, sender);
    }

    private void sendBonusPowerInfo(Faction faction, CommandSender sender) {
        if (faction.getBonusPower() != 0) {
            this.messageService.sendLocalizedMessage(
                sender,
                new MessageBuilder("BonusPower")
                    .with("amount", String.valueOf(faction.getBonusPower()))
            );
        }
    }

    private void sendLiegeInfoIfLiege(Faction faction, CommandSender sender) {
        int vassalContribution = this.factionService.calculateCumulativePowerLevelWithVassalContribution(faction) - this.factionService.calculateCumulativePowerLevelWithoutVassalContribution(faction);
        if (faction.isLiege()) {
            if (this.factionService.isWeakened(faction)) vassalContribution = 0;
            messageService.sendLocalizedMessage(
                sender,
                new MessageBuilder("VassalContribution")
                    .with("amount", String.valueOf(vassalContribution))
            );
        }
    }

    private void sendLiegeInfoIfVassal(Faction faction, CommandSender sender) {
        if (faction.hasLiege()) {
            Faction factionLiege = this.factionRepository.getByID(faction.getLiege());
            messageService.sendLocalizedMessage(
                sender,
                new MessageBuilder("Liege")
                    .with("name", factionLiege.getName())    
            );
        }
        if (faction.isLiege()) {
            messageService.sendLocalizedMessage(
                sender,
                new MessageBuilder("Vasslas")
                    .with("name", faction.getVassalsSeparatedByCommas())    
            );
        }
    }
}