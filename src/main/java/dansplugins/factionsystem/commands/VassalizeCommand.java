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
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
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
public class VassalizeCommand extends Command {

    private final MessageService messageService;
    private final Logger logger;
    private final LocaleService localeService;
    private final PersistentData persistentData;

    
    @Inject
    public VassalizeCommand(
        MessageService messageService,
        LocaleService localeService,
        PersistentData persistentData,
        Logger logger
    ) {
        super(
            new CommandBuilder()
                .withName("vassalize")
                .withAliases(LOCALE_PREFIX + "CmdVassalize")
                .withDescription("Offer to vassalize a faction.")
                .requiresPermissions("mf.vassalize")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to attempt vassalization")
                        .expectsFaction()
                        .isRequired()
                )
        );
        this.messageService = messageService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        // make sure player isn't trying to vassalize their own faction
        if (context.getExecutorsFaction().getName().equalsIgnoreCase(target.getName())) {
            context.replyWith("CannotVassalizeSelf");
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getName().equalsIgnoreCase(context.getExecutorsFaction().getLiege())) {
            context.replyWith("CannotVassalizeLiege");
            return;
        }
        // make sure player isn't trying to vassalize a vassal
        if (target.hasLiege()) {
            context.replyWith("CannotVassalizeVassal");
            return;
        }
        // make sure this vassalization won't result in a vassalization loop
        final int loopCheck = this.willVassalizationResultInLoop(context.getExecutorsFaction(), target);
        if (loopCheck == 1 || loopCheck == 2) {
            this.logger.debug("Vassalization was cancelled due to potential loop");
            return;
        }
        // add faction to attemptedVassalizations
        context.getExecutorsFaction().addAttemptedVassalization(target.getName());

        // inform all players in that faction that they are trying to be vassalized
        this.messageService.messageFaction(
            target, 
            this.translate("&a" + this.localeService.getText("AlertAttemptedVassalization", context.getExecutorsFaction().getName(), context.getExecutorsFaction().getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedVassalization"))
                .replace("#name#", context.getExecutorsFaction().getName())
        );

        // inform all players in players faction that a vassalization offer was sent
        this.messageService.messageFaction(
            context.getExecutorsFaction(),
            this.translate("&a" + this.localeService.getText("AlertFactionAttemptedToVassalize", target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionAttemptedToVassalize"))
                .replace("#name#", target.getName())
        );
    }

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            String liegeName = current.getLiege();
            if (liegeName.equalsIgnoreCase("none")) return 0; // no loop will be formed
            if (liegeName.equalsIgnoreCase(potentialVassal.getName())) return 1; // loop will be formed
            current = this.persistentData.getFaction(liegeName);
            steps++;
        }
        return 2; // We don't know :/
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
            ArrayList<String> vassalizeableFactions = new ArrayList<>();
            for (Faction faction : this.persistentData.getFactions()) {
                if (!playerFaction.getVassals().contains(faction.getName())) {
                    vassalizeableFactions.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], vassalizeableFactions);
        }
        return null;
    }
    
}