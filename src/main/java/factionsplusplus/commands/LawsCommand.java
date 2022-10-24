/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.stream.IntStream;

/**
 * @author Callum Johnson
 */
@Singleton
public class LawsCommand extends Command {

    @Inject
    public LawsCommand()
    {
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
}