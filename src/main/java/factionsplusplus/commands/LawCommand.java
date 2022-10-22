package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataService;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class LawCommand extends Command {

    private final DataService dataService;

    @Inject
    public LawCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("law")
                .withAliases(LOCALE_PREFIX + "CMDLaw")
                .withDescription("Manage your factions laws.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresSubCommand()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("add")
                        .withAliases("create", "al", LOCALE_PREFIX + "CmdAddLaw")
                        .requiresPermissions("mf.addlaw")
                        .withDescription("Adds a new law to your faction.")
                        .setExecutorMethod("createCommand")
                        .addArgument(
                            "law",
                            new ArgumentBuilder()
                                .setDescription("the law to add")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("remove")
                        .withAliases("delete", LOCALE_PREFIX + "CmdRemoveLaw")
                        .requiresPermissions("mf.removelaw")
                        .withDescription("Removes a law from your faction.")
                        .setExecutorMethod("removeCommand")
                        .addArgument(
                            "law to remove",
                            new ArgumentBuilder()
                                .setDescription("the id of the law to remove")
                                .expectsInteger()
                                .setTabCompletionHandler("autocompleteLawNumbers")
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("edit")
                        .withAliases("modify", LOCALE_PREFIX + "CmdEditLaw")
                        .requiresPermissions("mf.editlaw")
                        .withDescription("Edits a law for your faction.")
                        .setExecutorMethod("editCommand")
                        .addArgument(
                            "law to edit",
                            new ArgumentBuilder()
                                .setDescription("the id of the law to remove")
                                .setTabCompletionHandler("autocompleteLawNumbers")
                                .expectsInteger()
                                .isRequired()
                        )
                        .addArgument(
                            "new law text", 
                            new ArgumentBuilder()
                                .setDescription("the new law text")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
        );
        this.dataService = dataService;
    }

    public void createCommand(CommandContext context) {
        context.getExecutorsFaction().addLaw(String.join(" ", context.getStringArgument("law")));
        context.replyWith(
            this.constructMessage("LawAdded")
                .with("law", context.getStringArgument("law"))
        );
    }

    public void removeCommand(CommandContext context) {
        final int lawToRemove = context.getIntegerArgument("law to remove") - 1;
        if (lawToRemove < 0 || lawToRemove > context.getExecutorsFaction().getNumLaws()-1) {
            context.replyWith("LawNotFound");
            return;
        }
        // Technically this returns a bool if we actually removed the law, but the checks it does are already done above.
        context.getExecutorsFaction().removeLaw(lawToRemove);
        context.replyWith("LawRemoved");
    }

    public void editCommand(CommandContext context) {
        final int lawToEdit = context.getIntegerArgument("law to edit") - 1;
        if (lawToEdit < 0 || lawToEdit > context.getExecutorsFaction().getLaws().size()-1) {
            context.replyWith("LawNotFound");
            return;
        }
        final String editedLaw = context.getStringArgument("new law text");
        context.getExecutorsFaction().editLaw(lawToEdit, editedLaw);
        context.replyWith("LawEdited");
    }

    public List<String> autocompleteLawNumbers(CommandSender sender, String argument) {
        if (! (sender instanceof Player)) return List.of();
        Faction playersFaction = this.dataService.getPlayersFaction((Player)sender);
        if (playersFaction == null || playersFaction.getLaws().size() == 0) return List.of();
        List<String> completions = new ArrayList<>();
        org.bukkit.util.StringUtil.copyPartialMatches(argument, IntStream.range(1, playersFaction.getLaws().size()+1).mapToObj(String::valueOf).collect(Collectors.toList()), completions);
        return completions;
    }
}