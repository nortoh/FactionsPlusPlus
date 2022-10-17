/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;

import dansplugins.factionsystem.builders.*;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class AddLawCommand extends Command {

    protected final MessageService messageService;
    protected final PlayerService playerService;
    protected final LocaleService localeService;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AddLawCommand(MessageService messageService, PlayerService playerService, LocaleService localeService) {
        super(
            new CommandBuilder()
                .withName("addlaw")
                .withAliases("al", LOCALE_PREFIX + "CMDAddLaw")
                .withDescription("adds a law to your faction")
                .expectsPlayerExecution()
                .requiresPermissions("mf.addlaw")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "law",
                    new ArgumentBuilder()
                        .setDescription("the law to add")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
        this.messageService = messageService;
        this.playerService = playerService;
        this.localeService = localeService;
    }

    public void execute(CommandContext context) {
        context.getExecutorsFaction().addLaw(String.join(" ", (String)context.getArgument("law")));
        this.playerService.sendMessage(context.getPlayer(), "&a" + this.localeService.getText("LawAdded"), Objects.requireNonNull(this.messageService.getLanguage().getString("LawAdded"))
            .replace("#law#", (String)context.getArgument("law")), true);
    }
}