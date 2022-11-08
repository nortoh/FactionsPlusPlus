/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.factories.InteractionContextFactory;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.models.Gate;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.models.InteractionContext;

import org.bukkit.Material;
import org.bukkit.block.Block;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.List;

@Singleton
public class GateCommand extends Command {

    private final EphemeralData ephemeralData;
    private final DataService dataService;
    private final InteractionContextFactory interactionContextFactory;
    private final ConfigService configService;

    @Inject
    public GateCommand(
        EphemeralData ephemeralData,
        DataService dataService,
        InteractionContextFactory interactionContextFactory,
        ConfigService configService
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
        this.dataService = dataService;
        this.interactionContextFactory = interactionContextFactory;
        this.configService = configService;
    }

    public Gate doCommonBlockChecks(CommandContext context) {
        final Block targetBlock = context.getPlayer().getTargetBlock(null, 16);
        if (targetBlock.getType().equals(Material.AIR)) {
            context.error("Error.Gate.NoBlock");
            return null;
        }
        final Gate gate = this.dataService.getGateWithBlock(targetBlock);
        if (gate == null) {
            context.error("Error.Gate.BlockNotPartOf");
            return null;
        }
        final Faction gateFaction = this.dataService.getFaction(gate.getFaction());
        if (gateFaction == null) {
            context.error("Error.Gate.UnknownFaction", gate.getName());
            return null;
        }
        return gate;
    }

    public void removeCommand(CommandContext context) {
        Gate targetGate = this.doCommonBlockChecks(context);
        if (targetGate != null) {
            this.dataService.removeGate(targetGate);
            context.success("CommandResponse.Gate.Removed", targetGate.getName());
        }
    }

    public void renameCommand(CommandContext context) {
        Gate targetGate = this.doCommonBlockChecks(context);
        if (targetGate != null) {
            final String oldName = targetGate.getName();
            final String newName = context.getStringArgument("new name");
            targetGate.setName(newName);
            this.dataService.getGateRepository().persist(targetGate);
            context.success("CommandResponse.Gate.Renamed", oldName, newName);
        }
    }

    public void cancelCommand(CommandContext context) {
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext != null) {
            if (interactionContext.isGateCreating()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(context.getPlayer().getUniqueId());
                context.success("CommandResponse.Gate.Cancelled");
            }
        }
    }

    public void listCommand(CommandContext context) {
        List<Gate> factionGates = this.dataService.getFactionsGates(context.getExecutorsFaction());
        if (factionGates.size() > 0) {
            context.replyWith("GateList.Title", context.getExecutorsFaction().getName());
            for (Gate gate : factionGates) {
                context.replyWith("GateList.Gate", gate.getName(), gate.coordsToString());
            }
            return;
        }
        context.error("Error.Gate.NoneDefined");
    }

    public void createCommand(CommandContext context) {
        if (this.dataService.getFactionsGates(context.getExecutorsFaction()).size() >= this.configService.getInt("faction.limits.gate.count")) {
            context.error("Error.Gate.MaximumReached");
            return;
        }
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext != null) {
            if (interactionContext.isGateCreating()) {
                context.error("Error.Gate.AlreadyCreating");
                return;
            }
            context.replyWith("Error.InteractionEvent.Replaced", interactionContext.toString());
        }
        String gateName = context.getStringArgument("gate name");
        // TODO: new messaging api
        if (gateName == null) gateName = context.getLocalizedString("UnnamedGate");
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.GateCreating,
                new Gate(gateName)
            )
        );
        // TODO: new messaging api
        context.replyWith("CreatingGateClickWithHoe");
    }
}