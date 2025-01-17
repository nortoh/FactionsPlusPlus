/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
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
public class BreakAllianceCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public BreakAllianceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "breakalliance", "ba", LOCALE_PREFIX + "CmdBreakAlliance"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        final String permission = "mf.breakalliance";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            playerService.sendMessage(player, "&c" + getText("UsageBreakAlliance"), "UsageBreakAlliance", false);
            return;
        }

        final Faction otherFaction = getFaction(String.join(" ", args));
        if (otherFaction == null) {
            playerService.sendMessage(player, "&c" + getText("FactionNotFound"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                            .replace("#faction#", String.join(" ", args)), true);
            return;
        }

        if (otherFaction == faction) {
            playerService.sendMessage(player, "&c" + getText("CannotBreakAllianceWithSelf"), "CannotBreakAllianceWithSelf", false);
            return;
        }

        if (!faction.isAlly(otherFaction.getName())) {
            playerService.sendMessage(player, "&c" + getText("AlertNotAllied", otherFaction.getName()),
                    Objects.requireNonNull(messageService.getLanguage().getString("AlertNotAllied"))
                            .replace("#faction#", otherFaction.getName()), true);
            return;
        }

        faction.removeAlly(otherFaction.getName());
        otherFaction.removeAlly(faction.getName());
        messageFaction(faction, translate("&c" + getText("AllianceBrokenWith", otherFaction.getName()))
                , Objects.requireNonNull(messageService.getLanguage().getString("AllianceBrokenWith"))
                        .replace("#faction#", otherFaction.getName()));
        messageFaction(otherFaction, translate("&c" + getText("AlertAllianceHasBeenBroken", faction.getName())),
                Objects.requireNonNull(messageService.getLanguage().getString("AlertAllianceHasBeenBroken"))
                        .replace("#faction#", faction.getName()));
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