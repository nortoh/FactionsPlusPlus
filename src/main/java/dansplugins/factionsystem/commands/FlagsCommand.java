/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class FlagsCommand extends SubCommand {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final PersistentData persistentData;

    @Inject
    public FlagsCommand(
        PersistentData persistentData,
        PlayerService playerService, 
        MessageService messageService,
        LocaleService localeService
    ) {
        super();
        this.playerService = playerService;
        this.messageService = messageService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this
            .setNames("flags", LOCALE_PREFIX + "CmdFlags")
            .requiresPermissions("mf.flags")
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
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("ValidSubCommandsShowSet"), "ValidSubCommandsShowSet", false);
            return;
        }

        final Faction playersFaction = this.playerService.getPlayerFaction(player);

        final boolean show = this.safeEquals(args[0], "get", "show", 
            this.playerService.decideWhichMessageToUse(
                this.localeService.getText("CmdFlagsShow"), 
                this.messageService.getLanguage().getString("Alias.CmdFlagsShow")
            )
        );
        final boolean set = this.safeEquals(args[0], "set", 
            this.playerService.decideWhichMessageToUse(
                this.localeService.getText("CmdFlagsSet"), 
                this.messageService.getLanguage().getString("Alias.CmdFlagsSet")
            )
        );
        if (show) {
            playersFaction.getFlags().sendFlagList(player);
        } else if (set) {
            if (args.length < 3) {
                this.playerService.sendMessage(player, "&c" + this.localeService.getText("UsageFlagsSet"), "UsageFlagsSet", false);
            } else {
                final StringBuilder builder = new StringBuilder(); // Send the flag_argument as one String
                for (int i = 2; i < args.length; i++) builder.append(args[i]).append(" ");
                playersFaction.getFlags().setFlag(args[1], builder.toString().trim(), player);

            }
        } else {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("ValidSubCommandsShowSet"), "ValidSubCommandsShowSet", false);

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
        if (args.length == 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "set", "show");
        } else if (args.length == 2) {
            if (args[0] == "set") {
                if (this.persistentData.isInFaction(player.getUniqueId())) {
                    Faction faction = this.persistentData.getPlayersFaction(player.getUniqueId());
                    return TabCompleteTools.filterStartingWith(args[1], faction.getFlags().getFlagNamesList());
                }
            }
        }
        return null;
    }
}