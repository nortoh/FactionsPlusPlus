/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class ResetPowerLevelsCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public ResetPowerLevelsCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("resetpowerlevels")
                .withAliases("rpl", LOCALE_PREFIX + "CmdResetPowerLevels")
                .withDescription("Reset player power records and faction cumulative power levels.")
                .requiresPermissions("mf.resetpowerlevels", "mf.admin")
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        context.reply(this.translate("&aPower Levels Resetting..."));
        // TODO: log this?
        this.persistentData.resetPowerLevels();
    }
}