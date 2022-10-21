/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.events.*;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.models.War;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.repositories.WarRepository;
import factionsplusplus.services.FactionService;
import factionsplusplus.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.*;

/**
 * @author Callum Johnson
 */
@Singleton
public class ForceCommand extends Command {
    private final FactionsPlusPlus factionsPlusPlus;
    private final Logger logger;
    private final PersistentData persistentData;
    private final EphemeralData ephemeralData;
    private final FactionRepository factionRepository;
    private final FactionService factionService;
    private final WarRepository warRepository;

    @Inject
    public ForceCommand(
        PersistentData persistentData,
        EphemeralData ephemeralData,
        FactionsPlusPlus factionsPlusPlus,
        Logger logger,
        FactionRepository factionRepository,
        FactionService factionService,
        WarRepository warRepository
    ) {
        super(
            new CommandBuilder()
                .withName("force")
                .withAliases(LOCALE_PREFIX + "CmdForce")
                .withDescription("Administrative commands for mangaging the plugin.")
                .requiresSubCommand()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("save")
                        .withAliases(LOCALE_PREFIX + "CmdForceSave")
                        .withDescription("Forces the plugin to save its configuration.")
                        .setExecutorMethod("saveCommand")
                        .requiresPermissions("mf.force.save", "mf.force.*", "mf.admin")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("load")
                        .withAliases(LOCALE_PREFIX + "CmdForceLoad")
                        .withDescription("Forces the plugin to reload its configuration.")
                        .setExecutorMethod("loadCommand")
                        .requiresPermissions("mf.force.load", "mf.force.*", "mf.admin")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("peace")
                        .withAliases(LOCALE_PREFIX + "CmdForcePeace")
                        .withDescription("Forces two factions at war to make peace.")
                        .setExecutorMethod("peaceCommand")
                        .requiresPermissions("mf.force.peace", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction one",
                            new ArgumentBuilder()
                                .setDescription("the first faction")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "faction two",
                            new ArgumentBuilder()
                                .setDescription("the second faction")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("demote")
                        .withAliases(LOCALE_PREFIX + "CmdForceDemote")
                        .withDescription("Forcefully demotes a player in a faction.")
                        .setExecutorMethod("demoteCommand")
                        .requiresPermissions("mf.force.demote", "mf.force.*", "mf.admin")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to demote")
                                .expectsAnyPlayer()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("join")
                        .withAliases(LOCALE_PREFIX + "CmdForceJoin")
                        .withDescription("Forcefully joins a player to a faction.")
                        .setExecutorMethod("joinCommand")
                        .requiresPermissions("mf.force.join", "mf.force.*", "mf.admin")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to force faction joining")
                                .expectsAnyPlayer()
                                .isRequired()
                        )
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("the faction this player should join")
                                .expectsFaction()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("kick")
                        .withAliases(LOCALE_PREFIX + "CmdForceKick")
                        .withDescription("Forcefully kicks a player from a faction.")
                        .setExecutorMethod("kickCommand")
                        .requiresPermissions("mf.force.kick", "mf.force.*", "mf.admin")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to kick")
                                .expectsAnyPlayer()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("power")
                        .withAliases(LOCALE_PREFIX + "CmdForcePower")
                        .withDescription("Forcefully sets a players power.")
                        .setExecutorMethod("powerCommand")
                        .requiresPermissions("mf.force.power", "mf.force.*", "mf.admin")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to kick")
                                .expectsAnyPlayer()
                                .isRequired()
                        )
                        .addArgument(
                            "desired power",
                            new ArgumentBuilder()
                                .setDescription("the power to set")
                                .expectsInteger()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("renounce")
                        .withAliases(LOCALE_PREFIX + "CmdForceRenounce")
                        .withDescription("Forcefully renounces a faction.")
                        .setExecutorMethod("renounceCommand")
                        .requiresPermissions("mf.force.renounce", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("the faction to renounce")
                                .expectsFaction()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("transfer")
                        .withAliases(LOCALE_PREFIX + "CmdForceTransfer")
                        .withDescription("Forcefully transfers ownership of a faction.")
                        .setExecutorMethod("transferCommand")
                        .requiresPermissions("mf.force.transfer", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("the faction to transfer ownership of")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to transfer ownership to")
                                .expectsAnyPlayer()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("removevassal")
                        .withAliases(LOCALE_PREFIX + "CmdForceRemoveVassal")
                        .withDescription("Forcefully removes a vassal from a liege.")
                        .setExecutorMethod("removeVassalCommand")
                        .requiresPermissions("mf.force.removevassal", "mf.force.*", "mf.admin")
                        .addArgument(
                            "liege",
                            new ArgumentBuilder()
                                .setDescription("the target faction to remove vassal from")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "vassal",
                            new ArgumentBuilder()
                                .setDescription("the vassal to remove")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("rename")
                        .withAliases(LOCALE_PREFIX + "CmdForceRename")
                        .withDescription("Forcefully renames a faction.")
                        .setExecutorMethod("renameCommand")
                        .requiresPermissions("mf.force.rename", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("the faction to rename")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "name",
                            new ArgumentBuilder()
                                .setDescription("the new faction name")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("bonuspower")
                        .withAliases(LOCALE_PREFIX + "CmdForceBonusPower")
                        .withDescription("Forcefully sets bonus power on a faction.")
                        .setExecutorMethod("bonusPowerCommand")
                        .requiresPermissions("mf.force.bonuspower", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("the target faction")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "amount",
                            new ArgumentBuilder()
                                .setDescription("the amount of bonus power")
                                .expectsInteger()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("unlock")
                        .withAliases(LOCALE_PREFIX + "CmdForceUnlock")
                        .withDescription("Forcefully unlock a locked door or chest.")
                        .setExecutorMethod("unlockCommand")
                        .requiresPermissions("mf.force.unlock", "mf.force.*", "mf.admin")
                        .addSubCommand(
                            new CommandBuilder()
                                .withName("cancel")
                                .withAliases(LOCALE_PREFIX + "CmdForceUnlockCancel")
                                .withDescription("Cancels pending force unlock request.")
                                .setExecutorMethod("cancelUnlockCommand")
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("create")
                        .withAliases(LOCALE_PREFIX + "CmdForceCreate")
                        .withDescription("Forcefully creates a faction.")
                        .setExecutorMethod("createCommand")
                        .requiresPermissions("mf.force.create", "mf.force.*", "mf.admin")
                        .addArgument(
                            "name",
                            new ArgumentBuilder()
                                .setDescription("faction name")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("claim")
                        .withAliases(LOCALE_PREFIX + "CmdForceClaim")
                        .withDescription("Forcefully claims land for a faction.")
                        .setExecutorMethod("claimCommand")
                        .requiresPermissions("mf.force.claim", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("faction name")
                                .expectsFaction()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("flag")
                        .withAliases(LOCALE_PREFIX + "CmdForceFlag")
                        .withDescription("Forcefully sets a faction flag.")
                        .setExecutorMethod("flagCommand")
                        .requiresPermissions("mf.force.flag", "mf.force.*", "mf.admin")
                        .addArgument(
                            "faction",
                            new ArgumentBuilder()
                                .setDescription("the target faction")
                                .expectsFaction()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "flag",
                            new ArgumentBuilder()
                                .setDescription("the faction flag to set")
                                .expectsString()
                                .isRequired()
                        )
                        .addArgument(
                            "value",
                            new ArgumentBuilder()
                                .setDescription("the value to set")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
        );        
        this.persistentData = persistentData;
        this.ephemeralData = ephemeralData;
        this.factionsPlusPlus = factionsPlusPlus;
        this.logger = logger;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
        this.warRepository = warRepository;
    }

    // "mf.force.save", "mf.force.*", "mf.admin"
    public void saveCommand(CommandContext context) {
        this.persistentData.getLocalStorageService().save();
        context.replyWith("AlertForcedSave");
    }

    // "mf.force.load", "mf.force.*", "mf.admin"
    public void loadCommand(CommandContext context) {
        this.persistentData.getLocalStorageService().load();
        this.factionsPlusPlus.reloadConfig();
        context.replyWith("AlertForcedLoad");
    }

    //  "mf.force.peace", "mf.force.*", "mf.admin"
    public void peaceCommand(CommandContext context) {
        final Faction former = context.getFactionArgument("faction one");
        final Faction latter = context.getFactionArgument("faction two");
        FactionWarEndEvent warEndEvent = new FactionWarEndEvent(former, latter);
        Bukkit.getPluginManager().callEvent(warEndEvent);
        if (!warEndEvent.isCancelled()) {
            if (former.isEnemy(latter.getID())) former.removeEnemy(latter.getID());
            if (latter.isEnemy(former.getID())) latter.removeEnemy(former.getID());

            War war = this.warRepository.getActiveWarsBetween(former.getID(), latter.getID());
            war.end();

            // announce peace to all players on server.
            context.messageAllPlayers(
                this.constructMessage("AlertNowAtPeaceWith")
                    .with("p1", former.getName())
                    .with("p2", latter.getName())  
            );
        }
    }

    // "mf.force.demote", "mf.force.*", "mf.admin"
    public void demoteCommand(CommandContext context) {
        final OfflinePlayer player = context.getOfflinePlayerArgument("player");
        final Faction faction = this.persistentData.getPlayersFaction(player.getUniqueId());
        if (faction == null) {
            context.replyWith("PlayerIsNotInAFaction");
            return;
        }
        if (!faction.isOfficer(player.getUniqueId())) {
            context.replyWith("PlayerIsNotOfficerOfFaction");
            return;
        }
        faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        if (player.isOnline() && player.getPlayer() != null) {
            context.messagePlayer(player.getPlayer(), "AlertForcedDemotion");
        }
        context.replyWith("SuccessOfficerRemoval");
    }

    // "mf.force.join", "mf.force.*", "mf.admin"
    public void joinCommand(CommandContext context) {
        final OfflinePlayer player = context.getOfflinePlayerArgument("player");
        final Faction faction = context.getFactionArgument("faction");
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            context.replyWith("PlayerAlreadyInFaction");
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            this.logger.debug("Join event was cancelled.");
            return;
        }
        context.messageFaction(
            faction,
            this.constructMessage("HasJoined")
                .with("player", player.getName())
                .with("faction", faction.getName())
        );
        faction.addMember(player.getUniqueId());
        if (player.isOnline() && player.getPlayer() != null) {
            context.messagePlayer(player.getPlayer(), "AlertForcedToJoinFaction");
        }
        context.replyWith("SuccessForceJoin");
    }

    // "mf.force.kick", "mf.force.*", "mf.admin"
    public void kickCommand(CommandContext context) {
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final Faction faction = this.persistentData.getPlayersFaction(target.getUniqueId());
        if (faction == null) {
            context.replyWith("PlayerIsNotInAFaction");
            return;
        }
        if (faction.isOwner(target.getUniqueId())) {
            context.replyWith("CannotForciblyKickOwner");
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, null); // no kicker so null is used
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        if (faction.isOfficer(target.getUniqueId())) {
            faction.removeOfficer(target.getUniqueId()); // Remove Officer (if one)
        }
        this.ephemeralData.getPlayersInFactionChat().remove(target.getUniqueId());
        faction.removeMember(target.getUniqueId());
        context.messageFaction(
            faction,
            this.constructMessage("HasBeenKickedFrom")
                .with("player", target.getName())
                .with("faction", faction.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) {
            context.messagePlayer(
                target.getPlayer(),
                this.constructMessage("AlertKicked")
                    .with("name", "an admin")
            );
        }
        context.replyWith("SuccessFactionMemberRemoval");
    }

    // "mf.force.power", "mf.force.*", "mf.admin"
    public void powerCommand(CommandContext context) {
        final OfflinePlayer player = context.getOfflinePlayerArgument("player");
        final Integer desiredPower = context.getIntegerArgument("desired power");
        final PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());
        record.setPower(desiredPower); // Set power :)
        context.replyWith(
            this.constructMessage("PowerLevelHasBeenSetTo")
                .with("level", String.valueOf(desiredPower))
        );
    }

    // "mf.force.renounce", "mf.force.*", "mf.admin"
    public void renounceCommand(CommandContext context) {
        final Faction faction = context.getFactionArgument("faction");
        long changes = this.persistentData.removeLiegeAndVassalReferencesToFaction(faction.getID());

        if (faction.getLiege() != null) {
            faction.setLiege(null);
            changes++;
        }
        if (faction.getNumVassals() != 0) {
            changes = changes + faction.getNumVassals();
            faction.clearVassals();
        }
        if (changes == 0) context.replyWith("NoVassalOrLiegeReferences");
        else context.replyWith("SuccessReferencesRemoved");
    }

    // "mf.force.transfer", "mf.force.*", "mf.admin"
    public void transferCommand(CommandContext context) {
        final OfflinePlayer player = context.getOfflinePlayerArgument("player");
        final Faction faction = context.getFactionArgument("faction");
        if (faction.isOwner(player.getUniqueId())) {
            context.replyWith("AlertPlayerAlreadyOwner");
            return;
        }
        if (!faction.isMember(player.getUniqueId())) {
            context.replyWith("AlertPlayerNotInFaction");
            return;
        }
        if (faction.isOfficer(player.getUniqueId())) faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        faction.setOwner(player.getUniqueId());

        if (player.isOnline() && player.getPlayer() != null) {
            context.messagePlayer(
                player.getPlayer(),
                this.constructMessage("OwnershipTransferred")
                    .with("name", faction.getName())
            );
        }
        context.replyWith(
            this.constructMessage("OwnerShipTransferredTo")
                .with("name", player.getName())
        );
    }

    // "mf.force.removevassal", "mf.force.*", "mf.admin"
    public void removeVassalCommand(CommandContext context) {
        final Faction liege = context.getFactionArgument("liege");
        final Faction vassal = context.getFactionArgument("vassal");
        if (liege.isVassal(vassal.getID())) liege.removeVassal(vassal.getID());
        if (vassal.isLiege(liege.getID())) vassal.setLiege(null);
        context.replyWith("Done");
    }

    // "mf.force.rename", "mf.force.*", "mf.admin"
    public void renameCommand(CommandContext context) {
        final Faction faction = context.getFactionArgument("faction");
        final String oldName = faction.getName();
        final String newName = context.getStringArgument("name");
        // rename faction
        if (this.persistentData.getFaction(newName) != null) {
            context.replyWith("FactionAlreadyExists");
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(faction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            this.logger.debug("Rename event was cancelled.");
            return;
        }

        // change name
        faction.setName(newName);
        context.replyWith("FactionNameChanged");

        // Prefix (if it was unset)
        if (faction.getPrefix().equalsIgnoreCase(oldName)) faction.setPrefix(newName);

        // Save again to overwrite current data
        this.persistentData.getLocalStorageService().save();
    }

    // "mf.force.bonuspower", "mf.force.*", "mf.admin"
    public void bonusPowerCommand(CommandContext context) {
        final Faction faction = context.getFactionArgument("faction");
        final Integer amount = context.getIntegerArgument("amount");
        // set bonus power
        this.factionService.setBonusPower(faction, amount);
        // inform sender
        context.replyWith("Done");
    }

    // "mf.force.unlock", "mf.force.*", "mf.admin"
    public void unlockCommand(CommandContext context) {
        Player player = context.getPlayer();
        if (!this.ephemeralData.getUnlockingPlayers().contains(player.getUniqueId())) {
            // add player to playersAboutToLockSomething list
            this.ephemeralData.getUnlockingPlayers().add(player.getUniqueId());
        }

        if (!this.ephemeralData.getForcefullyUnlockingPlayers().contains(player.getUniqueId())) {
            // add player to playersAboutToLockSomething list
            this.ephemeralData.getForcefullyUnlockingPlayers().add(player.getUniqueId());
        }

        this.ephemeralData.getLockingPlayers().remove(player.getUniqueId()); // Remove from locking

        // inform them they need to right-click the block that they want to lock or type /mf lock cancel to cancel it
        context.replyWith("RightClickForceUnlock");
    }

    public void cancelUnlockCommand(CommandContext context) {
        this.ephemeralData.getUnlockingPlayers().remove(context.getPlayer().getUniqueId());
        this.ephemeralData.getForcefullyUnlockingPlayers().remove(context.getPlayer().getUniqueId());
        context.replyWith("AlertUnlockingCancelled");
    }

    // "mf.force.create", "mf.force.*", "mf.admin"
    public void createCommand(CommandContext context) {
        String newFactionName = context.getStringArgument("name");

        if (this.factionRepository.get(newFactionName) != null) {
            context.replyWith("FactionAlreadyExists");
            return;
        }

        final Faction faction = this.factionService.createFaction(newFactionName);
        FactionCreateEvent createEvent = new FactionCreateEvent(faction, context.getPlayer());
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            this.factionRepository.create(faction);
            context.replyWith("FactionCreated");
        }
    }

    // "mf.force.claim", "mf.force.*", "mf.admin"
    public void claimCommand(CommandContext context) {
        final Faction faction = context.getFactionArgument("faction");
        this.persistentData.getChunkDataAccessor().forceClaimAtPlayerLocation(context.getPlayer(), faction);
        context.replyWith("Done");
    }

    // "mf.force.flag", "mf.force.*", "mf.admin"
    public void flagCommand(CommandContext context) {
        final Faction faction = context.getFactionArgument("faction");
        final String flagName = context.getStringArgument("flag");
        final FactionFlag flag = faction.getFlag(flagName);
        if (flag == null) {
            context.replyWith(
                this.constructMessage("InvalidFactionFlag")
                    .with("flag", flagName)
            );
        }
        final String value = context.getStringArgument("value");
        // TODO: handle errors & informing of flag being set
        flag.set(value);
    }
}