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
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.utils.TabCompleteTools;
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
public class BreakAllianceCommand extends Command {

    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public BreakAllianceCommand(
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("breakalliance")
                .withAliases("ba", LOCALE_PREFIX + "CmdBreakAlliance")
                .withDescription("Breaks an alliance with an allied faction.")
                .requiresPermissions("mf.breakalliance")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the allied faction to break alliance with")
                        .expectsAlliedFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
    }


    public void execute(CommandContext context) {
        final Faction otherFaction = context.getFactionArgument("faction name");

        if (otherFaction == context.getExecutorsFaction()) {
            context.replyWith("CannotBreakAllianceWithSelf");
            return;
        }

        context.getExecutorsFaction().removeAlly(otherFaction.getID());
        otherFaction.removeAlly(context.getExecutorsFaction().getID());
        context.messagePlayersFaction(
            this.constructMessage("AllianceBrokenWith")
                .with("faction", otherFaction.getName())
        );
        context.messageFaction(
            otherFaction, 
            this.constructMessage("AlertAllianceHasBeenBroken")
                .with("faction", context.getExecutorsFaction().getName())
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
        final List<String> factionsAllowedtoAlly = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> factionAllyNames = new ArrayList<>();
            for (UUID uuid : playerFaction.getAllies()) factionAllyNames.add(this.factionRepository.getByID(uuid).getName());
            return TabCompleteTools.filterStartingWith(args[0], factionAllyNames);
        }
        return null;
    }
}