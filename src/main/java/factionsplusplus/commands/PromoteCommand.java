/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.FactionService;
import factionsplusplus.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class PromoteCommand extends Command {

    private final PersistentData persistentData;
    private final FactionService factionService;

    @Inject
    public PromoteCommand(
        PersistentData persistentData,
        FactionService factionService
    ) {
        super(
            new CommandBuilder()
                .withName("promote")
                .withAliases(LOCALE_PREFIX + "CmdPromote")
                .withDescription("Promotes a member of your faction to an officer.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.promote")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the member to promote")
                        .expectsFactionMember()
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        if (faction.isOfficer(target.getUniqueId())) {
            context.replyWith("PlayerAlreadyOfficer");
            return;
        }
        if (context.getPlayer().getUniqueId().equals(target.getUniqueId())) {
            context.replyWith("CannotPromoteSelf");
            return;
        }
        int maxOfficers = this.factionService.calculateMaxOfficers(faction);
        if (faction.getOfficerList().size() <= maxOfficers) {
            faction.addOfficer(target.getUniqueId());
            context.replyWith("PlayerPromoted");
            if (target.isOnline() && target.getPlayer() != null) {
                context.messagePlayer(target.getPlayer(), "PromotedToOfficer");
            }
        } else {
            context.replyWith(
                this.constructMessage("PlayerCantBePromotedBecauseOfLimit")
                    .with("number", String.valueOf(maxOfficers))
            );
        }
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        final List<String> membersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            for (UUID uuid : playerFaction.getMemberList()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    if (!playerFaction.getOfficerList().contains(uuid)) {
                        membersInFaction.add(member.getName());
                    }
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
        return null;
    }
}