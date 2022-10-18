/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
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
public class CheckClaimCommand extends Command {

    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;

    @Inject
    public CheckClaimCommand(
        PlayerService playerService,
        PersistentData persistentData,
        LocaleService localeService,
        MessageService messageService
    ) {
        super(
            new CommandBuilder()
                .withName("checkclaim")
                .withAliases("cc", LOCALE_PREFIX + "CmdCheckClaim")
                .withDescription("Check if land is claimed.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.checkclaim")
        );
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.messageService = messageService;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        final String result = this.persistentData.getChunkDataAccessor().checkOwnershipAtPlayerLocation(player);

        if (result.equals("unclaimed")) {
            this.playerService.sendMessage(player, "&a" + this.localeService.getText("LandIsUnclaimed"), "LandIsUnclaimed", false);
        } else {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("LandClaimedBy"), Objects.requireNonNull(this.messageService.getLanguage().getString("LandClaimedBy"))
                    .replace("#player#", result), true);
        }
    }
}