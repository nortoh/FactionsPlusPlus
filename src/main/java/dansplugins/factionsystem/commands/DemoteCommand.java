/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class DemoteCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public DemoteCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("demote")
                .withAliases(LOCALE_PREFIX + "CmdDemote")
                .withDescription("Demote an officer of your faction.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.demote")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the officer to demote")
                        .expectsFactionOfficer()
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        OfflinePlayer playerToBeDemoted = context.getOfflinePlayerArgument("player");

        if (playerToBeDemoted.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotDemoteSelf");
            return;
        }

        context.getExecutorsFaction().removeOfficer(playerToBeDemoted.getUniqueId());

        if (playerToBeDemoted.isOnline()) {
            context.messagePlayer(playerToBeDemoted.getPlayer(), "AlertDemotion");
        }
        context.replyWith(
            this.constructMessage("PlayerDemoted")
                .with("name", playerToBeDemoted.getName())
        );
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        final List<String> officersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            for (UUID uuid : playerFaction.getOfficerList()) {
                Player officer = Bukkit.getPlayer(uuid);
                if (officer != null) {
                    officersInFaction.add(officer.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], officersInFaction);
        }
        return null;
    }
}