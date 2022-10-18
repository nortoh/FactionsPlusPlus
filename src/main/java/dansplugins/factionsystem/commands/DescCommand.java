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
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DescCommand extends Command {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;

    @Inject
    public DescCommand(PlayerService playerService, LocaleService localeService, MessageService messageService) {
        super(
            new CommandBuilder()
                .withName("description")
                .withAliases("desc", LOCALE_PREFIX + "CmdDesc")
                .withDescription("Set your faction description.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.desc")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "description",
                    new ArgumentBuilder()
                        .setDescription("the description to set")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
    }

    public void execute(CommandContext context) {
        String description = (String)context.getArgument("description");
        context.getExecutorsFaction().setDescription(description);
        this.playerService.sendMessage(
            context.getPlayer(), 
            "&c" + this.localeService.getText("DescriptionSet"),
            Objects.requireNonNull(this.messageService.getLanguage().getString("Description")).replace("#desc#", description), 
            true
        );
    }
}