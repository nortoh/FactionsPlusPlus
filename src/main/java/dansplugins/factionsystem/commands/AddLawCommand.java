/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class AddLawCommand extends SubCommand {

    protected final MessageService messageService;
    protected final PlayerService playerService;
    protected final LocaleService localeService;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AddLawCommand(MessageService messageService, PlayerService playerService, LocaleService localeService) {
        this.messageService = messageService;
        this.playerService = playerService;
        this.localeService = localeService;
        this
            .setNames("addlaw", LOCALE_PREFIX + "CMDAddLaw", "AL")
            .requiresPermissions("mf.addlaw")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOwner();
    }

    /**
     * Method to execute the command.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        // check if they have provided any strings beyond "addlaw"
        if (args.length == 0) {
            this.playerService.sendMessage(player, translate("&c" + this.localeService.getText("UsageAddLaw")), "UsageAddLaw", false);
            return;
        }

        // add the law and send a success message.
        this.faction.addLaw(String.join(" ", args));
        this.playerService.sendMessage(player, "&a" + this.localeService.getText("LawAdded"), Objects.requireNonNull(this.messageService.getLanguage().getString("LawAdded"))
                .replace("#law#", String.join(" ", args)), true);
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
}