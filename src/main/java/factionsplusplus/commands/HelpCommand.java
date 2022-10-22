/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.repositories.CommandRepository;
import factionsplusplus.services.LocaleService;
import factionsplusplus.utils.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.MultiMessageBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Spliterator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Callum Johnson
 */
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
                context.replyWith("CommandNotRecognized");
                return;
            }
            MultiMessageBuilder builder = new MultiMessageBuilder();
            builder
                .add(this.constructMessage("CommandInfo.Title").with("name", command.getName()))
                .add(this.constructMessage("CommandInfo.Description").with("desc", command.getDescription()));
            if (command.getAliases().length > 0) builder.add(this.constructMessage("CommandInfo.Aliases").with("aliases", String.join(", ", command.getAliases())));
            if (command.hasSubCommands()) {
                builder.add(this.constructMessage("CommandInfo.SubcommandHeader"));
                for (Command subCommand : command.getSubCommands().values()) {
                    builder.add(this.constructMessage("CommandInfo.Subcommand").with("command", subCommand.getName()));
                }
            }
            if (command.getRequiredPermissions().length > 0) builder.add(this.constructMessage("CommandInfo.Permissions").with("permissions", String.join(", ", command.getRequiredPermissions())));
            context.replyWith(builder);
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
        context.replyWith(
            this.constructMessage("CommandsPageTitle")
                .with("page", String.valueOf(requestedPage+1))
                .with("pages", String.valueOf(partitionedList.size()))
        );
        partitionedList.get(requestedPage).forEach(line -> context.reply(this.constructHelpTextForCommand(line)));
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

    public String constructHelpTextForCommand(Command command) {
        return String.format("&b/mf %s %s - %s", command.getName(), command.buildSyntax(), command.getDescription());
    }

    public List<String> autocompletePage(CommandSender sender, String argument) {
        List<String> completions = new ArrayList<>();
        org.bukkit.util.StringUtil.copyPartialMatches(argument, IntStream.range(1, this.generateHelpPages().size()).mapToObj(String::valueOf).collect(Collectors.toList()), completions);
        return completions;
    }
}