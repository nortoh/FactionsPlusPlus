/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.TabCompleteTools;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class RemoveLawCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public RemoveLawCommand(
        PersistentData persistentData
    ) {
        super(
            new CommandBuilder()
                .withName("removelaw")
                .withAliases("rl", LOCALE_PREFIX + "CmdRemoveLaw")
                .withDescription("Removes an already existing law in your faction.")
                .requiresPermissions("mf.removelaw")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .expectsPlayerExecution()
                .addArgument(
                    "law to remove",
                    new ArgumentBuilder()
                        .setDescription("the id of the law to remove")
                        .expectsInteger()
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final int lawToRemove = context.getIntegerArgument("law to remove");
        if (lawToRemove < 0 || lawToRemove > context.getExecutorsFaction().getNumLaws()-1) {
            context.replyWith("UsageRemoveLaw");
            return;
        }
        if (context.getExecutorsFaction().removeLaw(lawToRemove)) {
            context.replyWith("LawRemoved");
        }
        // TODO: handle this returning false
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
            if (playerFaction.getNumLaws() != 0) {
                ArrayList<String> numbers = new ArrayList<>();
                for (int i = 1; i < playerFaction.getNumLaws() + 1; i++) {
                    numbers.add(Integer.toString(i));
                }
                return TabCompleteTools.filterStartingWith(args[0], numbers);
            }
            return null;
        }
        return null;
    }
}