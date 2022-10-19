/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.Gate;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class GateCommand extends Command {

    private final EphemeralData ephemeralData;
    private final PersistentData persistentData;
    private final MessageService messageService;

    @Inject
    public GateCommand(
        EphemeralData ephemeralData,
        PersistentData persistentData,
        MessageService messageService
    ) {
        super(
            new CommandBuilder()
                .withName("gate")
                .withAliases("gt", LOCALE_PREFIX + "CmdGate")
                .withDescription("Manage gates.")
                .requiresPermissions("mf.gate")
                .requiresSubCommand()
                .expectsPlayerExecution()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("create")
                        .withAliases(LOCALE_PREFIX + "CmdGateCreate")
                        .withDescription("Create a new gate.")
                        .setExecutorMethod("createCommand")
                        .expectsFactionOfficership()
                        .addArgument(
                            "gate name",
                            new ArgumentBuilder()
                                .setDescription("the name of the gate you are creating")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isOptional()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("rename")
                        .withAliases(LOCALE_PREFIX + "CmdGateRename")
                        .withDescription("Renames a gate.")
                        .setExecutorMethod("renameCommand")
                        .expectsFactionOfficership()
                        .addArgument(
                            "new name",
                            new ArgumentBuilder()
                                .setDescription("the new name of the gate")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isOptional()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("remove")
                        .withAliases("delete", LOCALE_PREFIX + "CmdGateRemove")
                        .withDescription("Removes a gate.")
                        .setExecutorMethod("removeCommand")
                        .expectsFactionOfficership()
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("list")
                        .withAliases(LOCALE_PREFIX + "CmdGateList")
                        .withDescription("Lists your factions gates.")
                        .setExecutorMethod("listCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdGateCancel")
                        .withDescription("Cancel creating a gate.")
                        .setExecutorMethod("cancelCommand")
                )
        );
        this.ephemeralData = ephemeralData;
        this.messageService = messageService;
        this.persistentData = persistentData;
    }

    public Gate doCommonBlockChecks(CommandContext context) {
        final Block targetBlock = context.getPlayer().getTargetBlock(null, 16);
        if (targetBlock.getType().equals(Material.AIR)) {
            context.replyWith("NoBlockDetectedToCheckForGate");
            return null;
        }
        if (!this.persistentData.isGateBlock(targetBlock)) {
            context.replyWith("TargetBlockNotPartOfGate");
            return null;
        }
        final Gate gate = this.persistentData.getGate(targetBlock);
        final Faction gateFaction = this.persistentData.getGateFaction(gate);
        if (gateFaction == null) {
            context.replyWith(
                this.constructMessage("ErrorCouldNotFindGatesFaction")
                    .with("name", gate.getName())
            );
            return null;
        }
        return gate;
    }

    public void removeCommand(CommandContext context) {
        Gate targetGate = this.doCommonBlockChecks(context);
        if (targetGate != null) {
            final Faction gateFaction = this.persistentData.getGateFaction(targetGate);
            gateFaction.removeGate(targetGate);
            context.replyWith(
                this.constructMessage("RemovedGate")
                    .with("name", targetGate.getName())
            );
        }
    }

    public void renameCommand(CommandContext context) {
        Gate targetGate = this.doCommonBlockChecks(context);
        if (targetGate != null) {
            final Faction gateFaction = this.persistentData.getGateFaction(targetGate);
            final String newName = context.getStringArgument("new name");
            targetGate.setName(newName);
            context.replyWith(
                this.constructMessage("AlertChangedGateName")
                    .with("name", targetGate.getName())
            );
        }
    }

    public void cancelCommand(CommandContext context) {
        if (this.ephemeralData.getCreatingGatePlayers().remove(context.getPlayer().getUniqueId()) != null) {
            context.replyWith("CreatingGateCancelled");
        }
    }

    public void listCommand(CommandContext context) {
        if (context.getExecutorsFaction().getGates().size() > 0) {
            context.replyWith("FactionGate");
            for (Gate gate : context.getExecutorsFaction().getGates()) {
                context.replyWith(
                    this.constructMessage("GateLocation")
                        .with("name", gate.getName())
                        .with("location", gate.coordsToString())
                );
            }
            return;
        }
        context.replyWith("AlertNoGatesDefined");
    }

    public void createCommand(CommandContext context) {
        final Player player = context.getPlayer();
        if (this.ephemeralData.getCreatingGatePlayers().containsKey(player.getUniqueId())) {
            context.replyWith("AlertAlreadyCreatingGate");
            return;
        }
        // Require officer
        String gateName = context.getStringArgument("gate name");
        if (gateName == null) gateName = this.messageService.getLanguage().getString("UnnamedGate");
        this.startCreatingGate(player, gateName);
        context.replyWith("CreatingGateClickWithHoe");
    }

    private void startCreatingGate(Player player, String name) {
        this.ephemeralData.getCreatingGatePlayers().putIfAbsent(player.getUniqueId(), new Gate(name));
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