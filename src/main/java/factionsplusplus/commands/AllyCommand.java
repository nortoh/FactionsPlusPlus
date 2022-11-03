/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import org.bukkit.Bukkit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.LocaleService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class AllyCommand extends Command {
    protected final LocaleService localeService;
    protected final FactionRepository factionRepository;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AllyCommand(
        LocaleService localeService,
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
                        .addFilters(ArgumentFilterType.NotAllied, ArgumentFilterType.NotOwnFaction)
                        .consumesAllLaterArguments()
                        .isRequired() 
                )
        );
        this.localeService = localeService;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        // retrieve the Faction from the given arguments
        final Faction otherFaction = context.getFactionArgument("faction name");

        // the faction can't be itself
        if (otherFaction == context.getExecutorsFaction()) {
            context.replyWith("CannotAllyWithSelf");
            return;
        }

        // no need to allow them to ally if they're already allies
        if (context.getExecutorsFaction().isAlly(otherFaction.getUUID())) {
            context.replyWith("FactionAlreadyAlly");
            return;
        }

        if (context.getExecutorsFaction().isEnemy(otherFaction.getUUID())) {
            context.replyWith("FactionIsEnemy");
            return;
        }

        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getUUID())) {
            context.replyWith("AlertAlreadyRequestedAlliance");
            return;
        }

        // send the request
        context.getExecutorsFaction().requestAlly(otherFaction.getID());

        context.getExecutorsFaction().alert(
            Component.translatable("FactionNotice.AllianceRequest.Source")
                .color(NamedTextColor.AQUA)
                .args(Component.text(otherFaction.getName()))
        );

        otherFaction.alert(
            Component.translatable("FactionNotice.AllianceRequest.Target")
                .color(NamedTextColor.AQUA)
                .args(Component.text(context.getExecutorsFaction().getName()))
        );

        // check if both factions have requested an alliance
        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getUUID()) && otherFaction.isRequestedAlly(context.getExecutorsFaction().getUUID())) {
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    // Running this against one faction will trickle down to the other.
                    context.getExecutorsFaction().upsertRelation(otherFaction.getUUID(), FactionRelationType.Ally);
                    // message player's faction
                    context.messagePlayersFaction(
                        constructMessage("AlertNowAlliedWith")
                            .with("faction", otherFaction.getName())
                    );

                    // message target faction
                    context.messageFaction(
                        otherFaction, 
                        constructMessage("AlertNowAlliedWith")
                            .with("faction", context.getExecutorsFaction().getName())
                    );

                    // remove alliance requests
                    context.getExecutorsFaction().removeAllianceRequest(otherFaction.getUUID());
                    otherFaction.removeAllianceRequest(context.getExecutorsFaction().getUUID());
                }
            });
        }
    }
}