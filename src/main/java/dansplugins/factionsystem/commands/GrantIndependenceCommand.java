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
import java.util.Objects;
import java.util.List;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class GrantIndependenceCommand extends Command {

    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public GrantIndependenceCommand(
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("grantindependence")
                .withAliases("gi", LOCALE_PREFIX + "CmdGrandIndependence")
                .withDescription("Grants independence to a vassaled faction.")
                .requiresPermissions("mf.grantindependence")
                .expectsPlayerExecution()
                .expectsNoFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to get a members list of")
                        .expectsVassaledFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        target.setLiege(null);
        context.getExecutorsFaction().removeVassal(target.getID());
        
        // inform all players in that faction that they are now independent
        context.messageFaction(
            target,
            this.constructMessage("AlertGrantedIndependence")
                .with("name", context.getExecutorsFaction().getName())
        );
        // inform all players in players faction that a vassal was granted independence
        context.messagePlayersFaction(
            this.constructMessage("AlertNoLongerVassalFaction")
                .with("name", target.getName())
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
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> vassalNames = new ArrayList<>();
            for (UUID factionUUID : playerFaction.getVassals()) vassalNames.add(this.factionRepository.getByID(factionUUID).getName());
            return TabCompleteTools.filterStartingWith(args[0], vassalNames);
        }
        return null;
    }
}