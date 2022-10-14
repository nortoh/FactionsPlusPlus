/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class ResetPowerLevelsCommand extends SubCommand {

    private final LocaleService localeService;
    private final PersistentData persistentData;

    @Inject
    public ResetPowerLevelsCommand(LocaleService localeService, PersistentData persistentData) {
        super();
        this.localeService = localeService;
        this.persistentData = persistentData;
        this
            .setNames("resetpowerlevels", "rpl", LOCALE_PREFIX + "CmdResetPowerLevels")
            .requiresPermissions("mf.resetpowerlevels", "mf.admin");
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
        sender.sendMessage(this.translate("&aPower Levels Resetting..."));
        System.out.println(this.localeService.getText("ResettingIndividualPowerRecords"));
        this.persistentData.resetPowerLevels();
    }
}