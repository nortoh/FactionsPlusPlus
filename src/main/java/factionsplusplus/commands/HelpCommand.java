/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.command.CommandSender;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.data.repositories.CommandRepository;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.List;
import java.util.stream.IntStream;

@Singleton
public class HelpCommand extends Command {
    private final CommandRepository commandRepository;

    @Inject
    public HelpCommand(CommandRepository commandRepository) {
        super(
            new CommandBuilder()
                .withName("help")
                .withAliases(LOCALE_PREFIX + "CmdHelp")
                .withDescription("Returns a list of commands for this plugin.")
                .requiresPermissions("mf.help")
                .addArgument(
                    "page or command",
                    new ArgumentBuilder()
                        .setDescription("the page or command to show")
                        .isOptional()
                        .expectsString()
                        .setDefaultValue(1)
                        .setTabCompletionHandler("autocompletePage")
                        .consumesAllLaterArguments()
                )
        );
        this.commandRepository = commandRepository;
    }

    public void execute(CommandContext context) {
        Integer requestedPage = StringUtils.parseAsInteger(context.getStringArgument("page or command"));
        if (requestedPage == null) {
            String requestedCommand = context.getStringArgument("page or command");
            Command command = this.commandRepository.get(requestedCommand);
            if (command == null) {
                context.error("Error.CommandNotFound");
                return;
            }
            List<ComponentLike> components = new ArrayList<>();
            components.add(Component.translatable("Help.Command.Title").args(Component.text(command.getName())).decorate(TextDecoration.BOLD).color(NamedTextColor.LIGHT_PURPLE));
            components.add(Component.translatable("Help.Command.Description").color(NamedTextColor.AQUA).args(Component.text(command.getDescription())));
            if (command.getAliases().length > 0) components.add(Component.translatable("Help.Command.Aliases").color(NamedTextColor.AQUA).args(Component.text(String.join(", ", command.getAliases()))));
            if (command.hasSubCommands()) {
                components.add(Component.translatable("Help.Command.Subcommands.Header").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                for (Command subCommand : command.getSubCommands().values()) {
                    components.add(Component.translatable("Help.Command.Subcommand").color(NamedTextColor.AQUA).args(Component.text(subCommand.getName())));
                }
            }
            if (command.getRequiredPermissions().length > 0) components.add(Component.translatable("Help.Command.Permissions").color(NamedTextColor.AQUA).args(Component.text(String.join(", ", command.getRequiredPermissions()))));
            components.stream().forEach(component -> {
                context.getExecutorsAudience().sendMessage(component);
            });
            return;
        }
        final ArrayList<ArrayList<Command>> partitionedList = this.generateHelpPages();
        requestedPage--; // pages start at 0
        if (requestedPage > partitionedList.size()-1) {
            requestedPage = partitionedList.size()-1; // Upper Limit over LAST_PAGE
        }
        if (requestedPage < 0) {
            requestedPage = 0; // Lower Limit to 0
        }
        // TODO: new messaging api
        context.replyWith("Help.ListTitle", requestedPage+1, partitionedList.size());
        partitionedList.get(requestedPage).forEach(line -> {
            context.getExecutorsAudience().sendMessage(
                Component.text(String.format("/mf %s %s: ", line.getName(), line.buildSyntax())).color(NamedTextColor.GOLD).append(
                    Component.text(line.getDescription()).color(NamedTextColor.WHITE)
                )
            );
        });
    }

    public ArrayList<ArrayList<Command>> generateHelpPages() {
        ArrayList<ArrayList<Command>> result = new ArrayList<>();
        Spliterator<Command> split = this.commandRepository.all().values().stream().spliterator();
        while(true) {
            ArrayList<Command> chunk = new ArrayList<>();
            for (int i = 0; i < 9 && split.tryAdvance(chunk::add); i++){};
            if (chunk.isEmpty()) break;
            result.add(chunk);
        }
        return result;
    }

    public List<String> autocompletePage(CommandSender sender, String argument) {
        List<String> completions = new ArrayList<>();
        List<String> argsToCompare = IntStream.range(1, this.generateHelpPages().size()+1).mapToObj(String::valueOf).collect(Collectors.toList());
        if (argument.length() == 0 || StringUtils.parseAsInteger(argument) == null) {
            if (argument.length() != 0) argsToCompare.clear();
            argsToCompare.addAll(this.commandRepository.all().keySet());
        }
        org.bukkit.util.StringUtil.copyPartialMatches(argument, argsToCompare, completions);
        return completions;
    }
}