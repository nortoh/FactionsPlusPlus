/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.events.internal.FactionKickEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class KickCommand extends Command {
    private final EphemeralData ephemeralData;
    private final Logger logger;

    @Inject
    public KickCommand(
        EphemeralData ephemeralData,
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
                        .addFilters(ArgumentFilterType.ExcludeSelf)
                        .isRequired()
                )
        );
        this.ephemeralData = ephemeralData;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        OfflinePlayer target = context.getOfflinePlayerArgument("player");
        if (target.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.error("Error.Kick.Self");
            return;
        }
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        if (faction.isOwner(target.getUniqueId())) {
            context.error("Error.Kick.Owner");
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        this.ephemeralData.getPlayersInFactionChat().remove(target.getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                faction.clearMember(target.getUniqueId());
                context.error("CommandResponse.Kick", target.getName(), faction.getName());
                faction.alert("FactionNotice.PlayerKicked", target.getName());
                if (target.isOnline() && target.getPlayer() != null) {
                    context.alertPlayer(player, "PlayerNotice.KickedFromFaction", faction.getName(), player.getName());
                }
            }
        });
    }
}