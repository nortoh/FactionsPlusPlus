/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionKickEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.Logger;
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
public class KickCommand extends Command {
    private final PersistentData persistentData;
    private final EphemeralData ephemeralData;
    private final Logger logger;

    @Inject
    public KickCommand(
        EphemeralData ephemeralData,
        PersistentData persistentData,
        Logger logger
    ) {
        super(
            new CommandBuilder()
                .withName("kick")
                .withAliases(LOCALE_PREFIX + "CmdKick")
                .withDescription("Kicks another player from your faction.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOfficership()
                .requiresPermissions("mf.kick")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to kick from your faction")
                        .expectsFactionMember()
                        .isRequired()
                )
        );
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        OfflinePlayer target = context.getOfflinePlayerArgument("player");
        if (target.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotKickSelf");
            return;
        }
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        if (faction.isOwner(target.getUniqueId())) {
            context.replyWith("CannotKickOwner");
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        if (faction.isOfficer(target.getUniqueId())) {
            faction.removeOfficer(target.getUniqueId()); // Remove Officer (if one)
        }
        this.ephemeralData.getPlayersInFactionChat().remove(target.getUniqueId());
        faction.removeMember(target.getUniqueId());
        context.messagePlayersFaction(
            this.constructMessage("HasBeenKickedFrom")
                .with("name", target.getName())
                .with("faction", faction.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) {
            context.messagePlayer(
                target.getPlayer(),
                this.constructMessage("AlertKicked")
                    .with("name", player.getName())
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
                    membersInFaction.add(member.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
        return null;
    }
}