/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class EditLawCommand extends SubCommand {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final LocaleService localeService;

    @Inject
    public EditLawCommand(PlayerService playerService, LocaleService localeService, MessageService messageService, PersistentData persistentData) {
        super();
        this.playerService = playerService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.localeService = localeService;
        this
            .setNames("editlaw", "el", LOCALE_PREFIX + "CmdEditLaw")
            .requiresPermissions("mf.editlaw")
            .requiresPlayerInFaction()
            .requiresFactionOwner()
            .isPlayerCommand();
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final int lawToEdit = this.getIntSafe(args[0], 0) - 1;
        if (lawToEdit < 0 || lawToEdit >= this.faction.getLaws().size()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("UsageEditLaw"),
                "UsageEditLaw",
                false
            );
            return;
        }
        String[] arguments = new String[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        final String editedLaw = String.join(" ", arguments);
        if (this.faction.editLaw(lawToEdit, editedLaw)) {
            this.playerService.sendMessage(
                player,
                "&a" + this.localeService.getText("LawEdited"),
                "LawEdited",
                false
            );
        }
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

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