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
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class VassalizeCommand extends Command {

    private final MessageService messageService;
    private final Logger logger;
    private final PersistentData persistentData;

    
    @Inject
    public VassalizeCommand(
        MessageService messageService,
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
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        // make sure player isn't trying to vassalize their own faction
        if (context.getExecutorsFaction().getID().equals(target.getID())) {
            context.replyWith("CannotVassalizeSelf");
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getID().equals(context.getExecutorsFaction().getLiege())) {
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
        context.getExecutorsFaction().addAttemptedVassalization(target.getID());

        // inform all players in that faction that they are trying to be vassalized
        this.messageService.sendFactionLocalizedMessage(
            target,
            this.constructMessage("AlertAttemptedVassalization")
                .with("name", context.getExecutorsFaction().getName())
        );

        // inform all players in players faction that a vassalization offer was sent
        this.messageService.sendFactionLocalizedMessage(
            context.getExecutorsFaction(),
            this.constructMessage("AlertFactionAttemptedToVassalize")
                .with("name", target.getName())
        );
    }

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            UUID liegeID = current.getLiege();
            if (liegeID == null) return 0;
            if (liegeID.equals(potentialVassal.getID())) return 1;
            current = this.persistentData.getFactionByID(liegeID);
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
                if (!playerFaction.getVassals().contains(faction.getID())) {
                    vassalizeableFactions.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], vassalizeableFactions);
        }
        return null;
    }
    
}