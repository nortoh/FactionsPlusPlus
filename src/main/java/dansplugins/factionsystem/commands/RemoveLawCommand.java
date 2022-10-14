/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
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
public class RemoveLawCommand extends SubCommand {

    public RemoveLawCommand() {
        super();
        this
            .setNames("removelaw", LOCALE_PREFIX + "CmdRemoveLaw")
            .requiresPermissions("mf.removelaw")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOwner();
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("UsageRemoveLaw"),
                "UsageRemoveLaw",
                false
            );
            return;
        }
        final int lawToRemove = this.getIntSafe(args[0], 0) - 1;
        if (lawToRemove < 0) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("UsageRemoveLaw"),
                "UsageRemoveLaw",
                false
            );
            return;
        }
        if (this.faction.removeLaw(lawToRemove)) {
            this.playerService.sendMessage(
                player, 
                "&a" + this.getText("LawRemoved"),
                "LawRemoved",
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