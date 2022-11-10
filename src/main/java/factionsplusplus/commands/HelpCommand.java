/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.utils.Pagination;
import factionsplusplus.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.data.repositories.CommandRepository;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.Arrays;
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
                context.error("Error.CommandNotFound", requestedCommand);
                return;
            }
            List<ComponentLike> components = new ArrayList<>();
            components.add(Component.translatable("Help.Command.Title").args(Component.text(command.getFullName())).decorate(TextDecoration.BOLD).color(NamedTextColor.LIGHT_PURPLE));
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
        final Pagination pagination = new Pagination(
            MiniMessage.miniMessage().deserialize("<color:gray><lang:HelpCommand.Description>"),
            this.generateCommandListMiniMessages()
        );
        final List<Component> pageResults = pagination.generatePage(requestedPage);
        if (pageResults == null) {
            context.error("Error.Pagination.NoSuchPage", requestedPage);
            return;
        }
        pageResults.forEach(context.getExecutorsAudience()::sendMessage);
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

    public String buildCommandSyntax(Command command) {
        return command.buildSyntax().length() > 0 ? String.format(" %s", command.buildSyntax()) : "";
    }

    public List<String> generateCommandHelp(Command command) {
        List<String> results = new ArrayList<>();
        if (! command.shouldHideParentCommand()) results.add(String.format("<color:gold>/f %s%s:</color:gold> %s", command.getName(), this.buildCommandSyntax(command), command.getDescription()));
        if (command.shouldShowSubCommandsSeparately() && command.hasSubCommands()) {
            command.getSubCommands().values().stream().forEach(cmd -> {
                results.add(String.format("<color:gold>/f %s %s%s:</color:gold> %s", command.getName(), cmd.getName(), this.buildCommandSyntax(cmd), cmd.getDescription()));
            });
        }
        return results;
    }

    public List<Component> generateCommandListMiniMessages() {
        List<String> results = new ArrayList<>();
        this.commandRepository
            .all()
            .values()
            .stream()
            .forEach(command -> {
                results.addAll(this.generateCommandHelp(command));
            });
        return Pagination.partsFromMiniMessages(results);
    }

    public List<String> autocompletePage(CommandSender sender, String argument, List<String> rawArguments) {
        List<String> completions = new ArrayList<>();
        List<String> argsToCompare = IntStream.range(1, this.generateHelpPages().size()+1).mapToObj(String::valueOf).collect(Collectors.toList());
        if (argument.length() == 0 || StringUtils.parseAsInteger(argument) == null) {
            final List<String> spaceSplit = new ArrayList<String>(Arrays.asList(argument.split(" ")));
            if (argument.length() != 0) argsToCompare.clear();
            if (spaceSplit.size() > 1 || argument.endsWith(" ")) {
                final String commandName = argument.endsWith(" ")  ? argument.trim() : spaceSplit.subList(0, spaceSplit.size() - 1).stream().collect(Collectors.joining(" ")).trim();
                argument = spaceSplit.size() > 1 ? spaceSplit.get(spaceSplit.size() - 1) : "";
                argsToCompare.addAll(this.commandRepository.get(commandName).getSubCommands().keySet());
            }
            else argsToCompare.addAll(this.commandRepository.all().keySet());
        }
        org.bukkit.util.StringUtil.copyPartialMatches(argument, argsToCompare, completions);
        return completions;
    }
}