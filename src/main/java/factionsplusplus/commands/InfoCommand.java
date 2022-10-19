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
import factionsplusplus.services.DataService;
import factionsplusplus.utils.TabCompleteTools;
import factionsplusplus.utils.extended.Messenger;
import org.bukkit.command.CommandSender;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class InfoCommand extends Command {
    private final Messenger messenger;
    private final DataService dataService;
    private final PersistentData persistentData;

    @Inject
    public InfoCommand(
        Messenger messenger,
        DataService dataService,
        PersistentData persistentData
    ) {
        super(
            new CommandBuilder()
                .withName("info")
                .withAliases(LOCALE_PREFIX + "CmdInfo")
                .withDescription("See your faction or another faction's information.")
                .requiresPermissions("mf.info")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("optional faction to get information on")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isOptional()
                )
        );
        this.messenger = messenger;
        this.dataService = dataService;
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final Faction target;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return;
            }
            target = context.getExecutorsFaction();
            if (target == null) {
                context.replyWith("AlertMustBeInFactionToUseCommand");
                return;
            }
        } else {
            target = context.getFactionArgument("faction name");
        }
        this.messenger.sendFactionInfo(context.getSender(), target, this.dataService.getClaimedChunksForFaction(target).size());
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}