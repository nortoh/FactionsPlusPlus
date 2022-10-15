/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.Gate;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class GateCommand extends SubCommand {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final EphemeralData ephemeralData;
    private final PersistentData persistentData;
    private final ConfigService configService;

    @Inject
    public GateCommand(
        PlayerService playerService,
        MessageService messageService,
        LocaleService localeService,
        EphemeralData ephemeralData,
        ConfigService configService,
        PersistentData persistentData,
        MedievalFactions medievalFactions
    ) {
        super();
        this.playerService = playerService;
        this.messageService = messageService;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this
            .setNames("gate", "gt", LOCALE_PREFIX + "CmdGate")
            .requiresPermissions("mf.gate")
            .isPlayerCommand()
            .requiresPlayerInFaction();
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
        if (args.length == 0) {
            if (!this.configService.getBoolean("useNewLanguageFile")) {
                player.sendMessage(this.translate("&b" + this.localeService.getText("SubCommands")));
                player.sendMessage(this.translate("&b" + this.localeService.getText("HelpGateCreate")));
                player.sendMessage(this.translate("&b" + this.localeService.getText("HelpGateName")));
                player.sendMessage(this.translate("&b" + this.localeService.getText("HelpGateList")));
                player.sendMessage(this.translate("&b" + this.localeService.getText("HelpGateRemove")));
                player.sendMessage(this.translate("&b" + this.localeService.getText("HelpGateCancel")));
            } else {
                this.playerService.sendMultipleMessages(player, this.messageService.getLanguage().getStringList("GateHelp"));
            }
            return;
        }
        if (this.safeEquals(args[0], "cancel", this.playerService.decideWhichMessageToUse(this.localeService.getText("CmdGateCancel"), this.messageService.getLanguage().getString("Alias.CmdGateCancel")))) {
            // Cancel Logic
            if (this.ephemeralData.getCreatingGatePlayers().remove(player.getUniqueId()) != null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("CreatingGateCancelled"),
                    "CreatingGateCancelled",
                    false
                );
                return;
            }
        }
        if (this.safeEquals(args[0], "create", this.playerService.decideWhichMessageToUse(this.localeService.getText("CmdGateCreate"), this.messageService.getLanguage().getString("Alias.CmdGateCreate")))) {
            // Create Logic
            if (this.ephemeralData.getCreatingGatePlayers().containsKey(player.getUniqueId())) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("AlertAlreadyCreatingGate"),
                    "AlertAlreadyCreatingGate",
                    false
                );
                return;
            }
            if (!this.faction.isOfficer(player.getUniqueId()) && !this.faction.isOwner(player.getUniqueId())) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("AlertMustBeOwnerOrOfficerToUseCommand"),
                    "AlertMustBeOwnerOrOfficerToUseCommand",
                    false
                );
                return;
            }
            final String gateName;
            if (args.length > 1) {
                String[] arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                gateName = String.join(" ", arguments);
            } else {
                gateName = this.playerService.decideWhichMessageToUse("Unnamed Gate", this.messageService.getLanguage().getString("UnnamedGate"));
            }
            this.startCreatingGate(player, gateName);
            this.playerService.sendMessage(
                player,
                "&b" + this.localeService.getText("CreatingGateClickWithHoe"),
                "CreatingGateClickWithHoe",
                false
            );
            return;
        }
        if (this.safeEquals(args[0], "list", this.playerService.decideWhichMessageToUse(this.localeService.getText("CmdGateList"), this.messageService.getLanguage().getString("Alias.CmdGateList")))) {
            // List logic
            if (this.faction.getGates().size() > 0) {
                this.playerService.sendMessage(player, "&bFaction Gates", "FactionGate", false);
                for (Gate gate : this.faction.getGates()) {
                    this.playerService.sendMessage(
                        player, 
                        "&b" + String.format("%s: %s", gate.getName(), gate.coordsToString()),
                        Objects.requireNonNull(this.messageService.getLanguage().getString("GateLocation"))
                            .replace("#name#", gate.getName())
                            .replace("#location#", gate.coordsToString()),
                        true
                    );
                }
            } else {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("AlertNoGatesDefined"),
                    "AlertNoGatesDefined",
                    false
                );
            }
            return;
        }
        final boolean remove = this.safeEquals(args[0], "remove", this.playerService.decideWhichMessageToUse(this.localeService.getText("CmdGateRemove"), this.messageService.getLanguage().getString("Alias.CmdGateRemove")));
        final boolean rename = this.safeEquals(args[0], "name", this.playerService.decideWhichMessageToUse(this.localeService.getText("CmdGateName"), this.messageService.getLanguage().getString("Alias.CmdGateName")));
        if (rename || remove) {
            final Block targetBlock = player.getTargetBlock(null, 16);
            if (targetBlock.getType().equals(Material.AIR)) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("NoBlockDetectedToCheckForGate"),
                    "NoBlockDetectedToCheckForGate",
                    false
                );
                return;
            }
            if (!this.persistentData.isGateBlock(targetBlock)) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("TargetBlockNotPartOfGate"),
                    "TargetBlockNotPartOfGate",
                    false
                );
                return;
            }
            final Gate gate = this.persistentData.getGate(targetBlock);
            if (gate == null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("TargetBlockNotPartOfGate"),
                    "TargetBlockNotPartOfGate",
                    false
                );
                return;
            }
            final Faction gateFaction = this.persistentData.getGateFaction(gate);
            if (gateFaction == null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("ErrorCouldNotFindGatesFaction", gate.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("ErrorCouldNotFindGatesFaction")).replace("#name#", gate.getName()),
                    true
                );
                return;
            }
            if (!gateFaction.isOfficer(player.getUniqueId()) && !gateFaction.isOwner(player.getUniqueId())) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("AlertMustBeOwnerOrOfficerToUseCommand"),
                    "AlertMustBeOwnerOrOfficerToUseCommand",
                    false
                );
                return;
            }
            if (remove) {
                gateFaction.removeGate(gate);
                this.playerService.sendMessage(
                    player,
                    "&b" + this.localeService.getText("RemovedGate", gate.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("RemovedGate")).replace("#name#", gate.getName()),
                    true
                );
            }
            if (rename) {
                String[] arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                gate.setName(String.join(" ", arguments));
                this.playerService.sendMessage(
                    player,
                    "&b" + this.localeService.getText("AlertChangedGateName", gate.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("AlertChangedGateName")).replace("#name#", gate.getName()),
                    true
                );
            }
        }
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

    private void startCreatingGate(Player player, String name) {
        this.ephemeralData.getCreatingGatePlayers().putIfAbsent(player.getUniqueId(), new Gate(name, medievalFactions, configService));
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "cancel", "create", "list", "remove", "name");
        }
        return null;
    }
}