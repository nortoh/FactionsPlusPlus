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

import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Callum Johnson
 */
@Singleton
public class LawsCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public LawsCommand(
        PersistentData persistentData
    ) {
        super(
            new CommandBuilder()
                .withName("laws")
                .withAliases(LOCALE_PREFIX + "CmdLaws")
                .withDescription("List the laws of your faction or another faction.")
                .requiresPermissions("mf.laws")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to get a list of laws")
                        .consumesAllLaterArguments()
                        .expectsFaction()
                        .isOptional()
                )
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final Faction target;
        if (context.getRawArguments().length == 0) {
            target = context.getExecutorsFaction();
            if (target == null) {
                context.replyWith("AlertMustBeInFactionToUseCommand");
                return;
            }
            if (target.getNumLaws() == 0) {
                context.replyWith("AlertNoLaws");
                return;
            }
        } else {
            target = context.getFactionArgument("faction name");
            if (target.getNumLaws() == 0) {
                context.replyWith("FactionDoesNotHaveLaws");
                return;
            }
        }
        context.replyWith(
            this.constructMessage("LawsTitle")
                .with("name", target.getName())
        );
        IntStream.range(0, target.getNumLaws())
                .mapToObj(i -> translate("&b" + (i + 1) + ". " + target.getLaws().get(i)))
                .forEach(context::reply);
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}