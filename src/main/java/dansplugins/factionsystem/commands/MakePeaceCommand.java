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
import dansplugins.factionsystem.repositories.FactionRepository;
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

/**
 * @author Callum Johnson
 */
@Singleton
public class MakePeaceCommand extends Command {

    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public MakePeaceCommand(
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData,
        FactionRepository factionRepository
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
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        final Faction faction = context.getExecutorsFaction();
        if (target == faction) {
            context.replyWith("CannotMakePeaceWithSelf");
            return;
        }
        if (faction.isTruceRequested(target.getName())) {
            context.replyWith("AlertAlreadyRequestedPeace");
            return;
        }
        faction.requestTruce(target.getName());
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
        if (faction.isTruceRequested(target.getName()) && target.isTruceRequested(faction.getName())) {
            FactionWarEndEvent warEndEvent = new FactionWarEndEvent(faction, target);
            Bukkit.getPluginManager().callEvent(warEndEvent);
            if (!warEndEvent.isCancelled()) {
                // remove requests in case war breaks out again, and they need to make peace again
                faction.removeRequestedTruce(target.getName());
                target.removeRequestedTruce(faction.getName());

                // make peace between factions
                faction.removeEnemy(target.getName());
                target.removeEnemy(faction.getName());

                // TODO: set active flag in war to false

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
            for (String vassalName : target.getVassals()) {
                faction.removeEnemy(vassalName);

                Faction vassal = this.factionRepository.get(vassalName);
                vassal.removeEnemy(faction.getName());
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
            ArrayList<String> factionEnemies = playerFaction.getEnemyFactions();
            return TabCompleteTools.filterStartingWith(args[0], factionEnemies);
        }
        return null;
    }
}