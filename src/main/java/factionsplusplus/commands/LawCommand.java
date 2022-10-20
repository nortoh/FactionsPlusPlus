package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class LawCommand extends Command {

    @Inject
    public LawCommand() {
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
        if (context.getExecutorsFaction().removeLaw(lawToRemove)) {
            context.replyWith("LawRemoved");
        }
        // TODO: handle this returning false
    }

    public void editCommand(CommandContext context) {
        final int lawToEdit = context.getIntegerArgument("law to edit") - 1;
        if (lawToEdit < 0 || lawToEdit > context.getExecutorsFaction().getLaws().size()-1) {
            context.replyWith("LawNotFound");
            return;
        }
        final String editedLaw = context.getStringArgument("new law text");
        if (context.getExecutorsFaction().editLaw(lawToEdit, editedLaw)) context.replyWith("LawEdited");
        // TODO: handle this returning false
    }

    // TODO: reimplement autocomplete
}