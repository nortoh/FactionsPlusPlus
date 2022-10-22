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
                )
        );
        this.commandRepository = commandRepository;
    }

    public void execute(CommandContext context) {
        Integer requestedPage = StringUtils.parseAsInteger(context.getStringArgument("page or command"));
        if (requestedPage == null) {
            String requestedCommand = context.getStringArgument("page or command");
            Command command = this.commandRepository.get(requestedCommand);
            // TODO: implement this
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

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    /*public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.filterStartingWith(args[0], IntStream.range(1, this.generateHelpPages().size()).mapToObj(String::valueOf));
    }*/
}