/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.services.LocaleService;

/**
 * @author Callum Johnson
 */
@Singleton
public class AutoClaimCommand extends Command {

    private final PlayerService playerService;
    private final LocaleService localeService;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AutoClaimCommand(PlayerService playerService, LocaleService localeService) {
        super(
            new CommandBuilder()
                .withName("autoclaim")
                .withAliases("ac", LOCALE_PREFIX + "CmdAutoClaim")
                .withDescription("toggles auto claim for your player")
                .expectsPlayerExecution()
                .requiresPermissions("mf.autoclaim")
                .expectsFactionMembership()
                .expectsFactionOwnership()
        );
        this.playerService = playerService;
        this.localeService = localeService;
    }

    public void execute(CommandContext context) {
        context.getExecutorsFaction().toggleAutoClaim();
        this.playerService.sendMessage(context.getPlayer(), "&b" + this.localeService.getText("AutoclaimToggled"), "AutoclaimToggled", false);
    }
}