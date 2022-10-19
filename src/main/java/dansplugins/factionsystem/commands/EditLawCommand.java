/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class EditLawCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public EditLawCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("editlaw")
                .withAliases("el", LOCALE_PREFIX + "CmdEditLaw")
                .withDescription("Edit an already existing law in your faction.")
                .requiresPermissions("mf.editlaw")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .expectsPlayerExecution()
                .addArgument(
                    "law to edit",
                    new ArgumentBuilder()
                        .setDescription("the id of the law to edit")
                        .expectsInteger()
                        .isRequired()
                )
                .addArgument(
                    "edited law",
                    new ArgumentBuilder()
                        .setDescription("the edited law")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final int lawToEdit = context.getIntegerArgument("law to edit");
        if (lawToEdit < 0 || lawToEdit >= context.getExecutorsFaction().getLaws().size()) {
            context.replyWith("UsageEditLaw");
            return;
        }
        final String editedLaw = context.getStringArgument("edited law");
        if (context.getExecutorsFaction().editLaw(lawToEdit, editedLaw)) context.replyWith("LawEdited");
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            if (playerFaction.getNumLaws() != 0) {
                ArrayList<String> numbers = new ArrayList<>();
                for (int i = 1; i < playerFaction.getNumLaws() + 1; i++) {
                    numbers.add(Integer.toString(i));
                }
                return TabCompleteTools.filterStartingWith(args[0], numbers);
            }
            return null;
        }
        return null;
    }
}