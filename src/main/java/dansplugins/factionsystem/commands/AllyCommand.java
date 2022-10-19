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
        if (context.getExecutorsFaction().isAlly(otherFaction.getName())) {
            context.replyWith("FactionAlreadyAlly");
            return;
        }

        if (context.getExecutorsFaction().isEnemy(otherFaction.getName())) {
            context.replyWith("FactionIsEnemy");
            return;
        }

        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getName())) {
            context.replyWith("AlertAlreadyRequestedAlliance");
            return;
        }

        // send the request
        context.getExecutorsFaction().requestAlly(otherFaction.getName());

        this.messageService.messageFaction(
                context.getExecutorsFaction(),
                this.translate("&a" + this.localeService.getText("AlertAttemptedAlliance", context.getExecutorsFaction().getName(), otherFaction.getName())),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedAlliance"))
                        .replace("#faction_a#", context.getExecutorsFaction().getName())
                        .replace("#faction_b#", otherFaction.getName())
        );

        this.messageService.messageFaction(
                otherFaction,
                this.translate("&a" + this.localeService.getText("AlertAttemptedAlliance", context.getExecutorsFaction().getName(), otherFaction.getName())),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedAlliance"))
                        .replace("#faction_a#", context.getExecutorsFaction().getName())
                        .replace("#faction_b#", otherFaction.getName())
        );

        // check if both factions are have requested an alliance
        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getName()) && otherFaction.isRequestedAlly(context.getExecutorsFaction().getName())) {
            // ally them
            context.getExecutorsFaction().addAlly(otherFaction.getName());
            otherFaction.addAlly(context.getExecutorsFaction().getName());
            // message player's faction
            this.messageService.messageFaction(
                context.getExecutorsFaction(), 
                this.translate("&a" + this.localeService.getText("AlertNowAlliedWith", otherFaction.getName())), 
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAlliedWith")).replace("#faction#", otherFaction.getName())
            );

            // message target faction
            this.messageService.messageFaction(
                otherFaction, 
                this.translate("&a" + this.localeService.getText("AlertNowAlliedWith", context.getExecutorsFaction().getName())), Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAlliedWith")).replace("#faction#", context.getExecutorsFaction().getName())
            );

            // remove alliance requests
            context.getExecutorsFaction().removeAllianceRequest(otherFaction.getName());
            otherFaction.removeAllianceRequest(context.getExecutorsFaction().getName());
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
            ArrayList<String> playerAllies = playerFaction.getAllies();
            for(Faction faction : this.persistentData.getFactions()) {
                if(!playerAllies.contains(faction.getName()) && !faction.getName().equals(playerFaction.getName())) {
                    factionsAllowedtoAlly.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], factionsAllowedtoAlly);
        }
        return null;
    }
}