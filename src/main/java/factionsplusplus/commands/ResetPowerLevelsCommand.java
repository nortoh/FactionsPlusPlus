/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.PlayerService;
import factionsplusplus.utils.Logger;
import factionsplusplus.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class ResetPowerLevelsCommand extends Command {

    private final PlayerService playerService;
    private final Logger logger;

    @Inject
    public ResetPowerLevelsCommand(PlayerService playerService, Logger logger) {
        super(
            new CommandBuilder()
                .withName("resetpowerlevels")
                .withAliases("rpl", LOCALE_PREFIX + "CmdResetPowerLevels")
                .withDescription("Reset player power records and faction cumulative power levels.")
                .requiresPermissions("mf.resetpowerlevels", "mf.admin")
        );
        this.playerService = playerService;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        String msg = this.translate("&aPower Levels Resetting...");
        context.reply(msg);
        this.logger.print(msg);
        this.playerService.resetPowerLevels();
    }
}