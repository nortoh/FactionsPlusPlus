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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.stream.Collectors;

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

        ArrayList<Component> lawComponents = new ArrayList<>();

        lawComponents.add(
            Component.text()
                .append(
                    Component.text(String.format("%s %s", target.getName(), context.getLocalizedString("Generic.Law.Plural")))
                        .decorate(TextDecoration.BOLD)
                )
                .append(
                    Component.text("\n\nThis would be some text about the law book and how to use it.")
                )
                .asComponent()       
        );

        lawComponents.addAll(
            target.getLaws().stream()
                .map(law -> Component.text(law).color(NamedTextColor.BLACK))
                .collect(Collectors.toList())
        );

        context.getExecutorsAudience().openBook(Book.book(Component.translatable("Generic.Law.Plural"), Component.text(target.getName()), lawComponents));
    }
}