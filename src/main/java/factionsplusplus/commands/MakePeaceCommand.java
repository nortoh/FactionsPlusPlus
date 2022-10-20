/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.events.FactionWarEndEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.models.War;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.repositories.WarRepository;
import org.bukkit.Bukkit;
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
public class MakePeaceCommand extends Command {

    private final FactionRepository factionRepository;
    private final WarRepository warRepository;

    @Inject
    public MakePeaceCommand(
        FactionRepository factionRepository,
        WarRepository warRepository
    ) {
        super(
            new CommandBuilder()
                .withName("makepeace")
                .withAliases("mp", LOCALE_PREFIX + "CmdMakePeace")
                .withDescription("Makes peace with an enemy faction.")
                .requiresPermissions("mf.breakalliance")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOfficership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the enemy faction to make peace with")
                        .expectsEnemyFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.factionRepository = factionRepository;
        this.warRepository = warRepository;
    }
    
    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        final Faction faction = context.getExecutorsFaction();
        if (target == faction) {
            context.replyWith("CannotMakePeaceWithSelf");
            return;
        }
        if (faction.isTruceRequested(target.getID())) {
            context.replyWith("AlertAlreadyRequestedPeace");
            return;
        }
        faction.requestTruce(target.getID());
        context.replyWith(
            this.constructMessage("AttemptedPeace")
                .with("name", target.getName())
        );
        context.messageFaction(
            target,
            this.constructMessage("HasAttemptedToMakePeaceWith")
                .with("f1", faction.getName())
                .with("f2", target.getName())
        );
        if (faction.isTruceRequested(target.getID()) && target.isTruceRequested(faction.getID())) {
            FactionWarEndEvent warEndEvent = new FactionWarEndEvent(faction, target);
            Bukkit.getPluginManager().callEvent(warEndEvent);
            if (!warEndEvent.isCancelled()) {
                // remove requests in case war breaks out again, and they need to make peace again
                faction.removeRequestedTruce(target.getID());
                target.removeRequestedTruce(faction.getID());

                // make peace between factions
                faction.removeEnemy(target.getID());
                target.removeEnemy(faction.getID());

                War war = this.warRepository.getActiveWarsBetween(target.getID(), faction.getID());
                war.end();

                // Notify
                context.messageAllPlayers(
                    this.constructMessage("AlertNowAtPeaceWith")
                        .with("p1", faction.getName())
                        .with("p2", target.getName())
                );
            }
        }

        // if faction was a liege, then make peace with all of their vassals as well
        if (target.isLiege()) {
            for (UUID vassalID : target.getVassals()) {
                faction.removeEnemy(vassalID);

                Faction vassal = this.factionRepository.getByID(vassalID);
                vassal.removeEnemy(faction.getID());
            }
        }
    }
}