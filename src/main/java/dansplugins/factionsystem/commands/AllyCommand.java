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
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class AllyCommand extends Command {
    protected final MessageService messageService;
    protected final PlayerService playerService;
    protected final PersistentData persistentData;
    protected final LocaleService localeService;
    protected final FactionRepository factionRepository;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AllyCommand(
        MessageService messageService,
        PlayerService playerService,
        LocaleService localeService,
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("ally")
                .withAliases(LOCALE_PREFIX + "CmdAlly")
                .withDescription("Attempt to ally with a faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.ally")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to request as an ally")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isRequired() 
                )
        );
        this.messageService = messageService;
        this.playerService = playerService;
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        // retrieve the Faction from the given arguments
        final Faction otherFaction = context.getFactionArgument("faction name");
        final Player player = context.getPlayer();

        // the faction can't be itself
        if (otherFaction == context.getExecutorsFaction()) {
            context.replyWith("CannotAllyWithSelf");
            return;
        }

        // no need to allow them to ally if they're already allies
        if (context.getExecutorsFaction().isAlly(otherFaction.getID())) {
            context.replyWith("FactionAlreadyAlly");
            return;
        }

        if (context.getExecutorsFaction().isEnemy(otherFaction.getID())) {
            context.replyWith("FactionIsEnemy");
            return;
        }

        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getID())) {
            context.replyWith("AlertAlreadyRequestedAlliance");
            return;
        }

        // send the request
        context.getExecutorsFaction().requestAlly(otherFaction.getID());

        this.messageService.sendFactionLocalizedMessage(
            context.getExecutorsFaction(),
            this.constructMessage("AlertAttemptedAlliance")
                .with("faction_a", context.getExecutorsFaction().getName())
                .with("faction_b", otherFaction.getName())
        );

        this.messageService.sendFactionLocalizedMessage(
            otherFaction,
            this.constructMessage("AlertAttemptedAlliance")
                .with("faction_a", otherFaction.getName())
                .with("faction_b", context.getExecutorsFaction().getName())
        );

        // check if both factions have requested an alliance
        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getID()) && otherFaction.isRequestedAlly(context.getExecutorsFaction().getID())) {
            // ally them
            context.getExecutorsFaction().addAlly(otherFaction.getID());
            otherFaction.addAlly(context.getExecutorsFaction().getID());
            // message player's faction
            this.messageService.sendFactionLocalizedMessage(
                context.getExecutorsFaction(), 
                this.constructMessage("AlertNowAlliedWith")
                    .with("faction", otherFaction.getName())
            );

            // message target faction
            this.messageService.sendFactionLocalizedMessage(
                otherFaction, 
                this.constructMessage("AlertNowAlliedWith")
                    .with("faction", context.getExecutorsFaction().getName())
            );

            // remove alliance requests
            context.getExecutorsFaction().removeAllianceRequest(otherFaction.getID());
            otherFaction.removeAllianceRequest(context.getExecutorsFaction().getID());
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
        final List<String> factionsAllowedtoAlly = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<UUID> playerAllies = playerFaction.getAllies();
            for(Faction faction : this.persistentData.getFactions()) {
                if(!playerAllies.contains(faction.getID()) && !faction.getID().equals(playerFaction.getID())) {
                    factionsAllowedtoAlly.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], factionsAllowedtoAlly);
        }
        return null;
    }
}