/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarEndEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.War;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.repositories.WarRepository;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
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
public class MakePeaceCommand extends Command {

    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;
    private final WarRepository warRepository;

    @Inject
    public MakePeaceCommand(
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData,
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
        this.localeService = localeService;
        this.messageService = messageService;
        this.persistentData = persistentData;
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
        this.messageService.messageFaction(
            target,
            this.translate("&a" + this.localeService.getText("HasAttemptedToMakePeaceWith", faction.getName(), target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasAttemptedToMakePeaceWith"))
                .replace("#f1#", faction.getName())
                .replace("#f2#", target.getName())
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
                this.messageService.messageServer(
                    "&a" + this.localeService.getText("AlertNowAtPeaceWith", faction.getName(), target.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAtPeaceWith"))
                        .replace("#p1#", faction.getName())
                        .replace("#p2#", target.getName())
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

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> factionEnemies = new ArrayList<>();
            for (UUID factionID : playerFaction.getEnemyFactions()) factionEnemies.add(this.factionRepository.getByID(factionID).getName());
            return TabCompleteTools.filterStartingWith(args[0], factionEnemies);
        }
        return null;
    }
}