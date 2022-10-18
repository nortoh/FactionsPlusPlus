/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.builders.*;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class BypassCommand extends Command {

    private final EphemeralData ephemeralData;
    private final LocaleService localeService;
    private final PlayerService playerService;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public BypassCommand(PlayerService playerService, LocaleService localeService, EphemeralData ephemeralData) {
        super(
            new CommandBuilder()
                .withName("bypass")
                .withAliases(LOCALE_PREFIX + "CmdBypass")
                .withDescription("Toggle bypass protections.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.bypass", "mf.admin")
        );
        this.playerService = playerService;
        this.localeService = localeService;
        this.ephemeralData = ephemeralData;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        final boolean contains = this.ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());

        final String path = (contains ? "NoLonger" : "Now") + "BypassingProtections";

        if (contains) {
            this.ephemeralData.getAdminsBypassingProtections().remove(player.getUniqueId());
        } else {
            this.ephemeralData.getAdminsBypassingProtections().add(player.getUniqueId());
        }
        this.playerService.sendMessage(player, "&a" + this.localeService.getText(path), path, false);
    }
}