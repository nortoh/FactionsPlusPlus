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
import dansplugins.factionsystem.events.*;
import dansplugins.factionsystem.factories.FactionFactory;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;
import preponderous.ponder.misc.ArgumentParser;

import java.lang.reflect.Method;
import java.util.*;

// TODO: re-implement autocomplete 

/**
 * @author Callum Johnson
 */
@Singleton
public class ForceCommand extends SubCommand {
    private final MedievalFactions medievalFactions;
    private final Logger logger;
    private final FactionFactory factionFactory;
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final EphemeralData ephemeralData;
    private final PlayerService playerService;
    private final FactionRepository factionRepository;

    private final String[] commands = new String[]{
            "Save", "Load", "Peace", "Demote", "Join", "Kick", "Power", "Renounce", "Transfer", "RemoveVassal", "Rename", "BonusPower", "Unlock", "Create", "Claim", "Flag"
    };
    private final HashMap<List<String>, String> subMap = new HashMap<>();

    private final ArgumentParser argumentParser = new ArgumentParser();
    private final UUIDChecker uuidChecker = new UUIDChecker();

    @Inject
    public ForceCommand(
        PersistentData persistentData,
        LocaleService localeService,
        MessageService messageService,
        EphemeralData ephemeralData,
        FactionFactory factionFactory,
        MedievalFactions medievalFactions,
        Logger logger,
        PlayerService playerService,
        FactionRepository factionRepository
    ) {
        super();
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.messageService = messageService;
        this.ephemeralData = ephemeralData;
        this.medievalFactions = medievalFactions;
        this.logger = logger;
        this.factionFactory = factionFactory;
        this.playerService = playerService;
        this.factionRepository = factionRepository;
        this
            .setNames("force", LOCALE_PREFIX + "CmdForce");
        // Register sub-commands.
        Arrays.stream(commands).forEach(command ->
                subMap.put(Arrays.asList(command, this.localeService.getText("CmdForce" + command)), "force" + command)
        );
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
        if (!(args.length == 0)) { // If the Argument has Arguments in the 'args' list.
            for (Map.Entry<List<String>, String> entry : subMap.entrySet()) { // Loop through the SubCommands.
                // Map.Entry<List<String>, String> example => ([Save, CMDForceSave (translation key)], forceSave)
                try {
                    if (safeEquals(args[0], entry.getKey().toArray(new String[0]))) { // Do any of the Keys for the SubCommand match the Given Argument at index '0'.
                        try {
                            Method method = getClass().getDeclaredMethod(entry.getValue(), CommandSender.class, String[].class); // Get the Declared method for that SubCommand.
                            method.invoke(this, sender, args);  // Use reflection to invoke the command.
                            // Due to the nature of the force-command, it is safe to use reflection here.
                        } catch (ReflectiveOperationException ex) {
                            System.out.println("DEBUG: Failed to resolve method from '" + args[0] + "'!");
                        }
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG: Failed to use safeEquals to determine the command chosen.");
                }
            }
        }
        // Print out these messages if the command either isn't found or if an error occurs or if the arguments ('args') list is empty.
        sender.sendMessage(this.translate("&b" + this.localeService.getText("SubCommands")));
        Arrays.stream(commands).forEach(str -> sender.sendMessage(this.translate("&b" + this.localeService.getText("HelpForce" + str))));
    }

    @SuppressWarnings("unused")
    private void forceSave(CommandSender sender) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.save", "mf.force.*", "mf.admin"))) {
            return;
        }
        sender.sendMessage(this.translate("&a" + this.localeService.getText("AlertForcedSave")));
        this.persistentData.getLocalStorageService().save();
    }

    @SuppressWarnings("unused")
    private void forceLoad(CommandSender sender) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.load", "mf.force.*", "mf.admin"))) {
            return;
        }
        sender.sendMessage(this.translate("&a" + this.localeService.get("AlertForcedLoad")));
        this.persistentData.getLocalStorageService().load();
        this.medievalFactions.reloadConfig();
    }

    @SuppressWarnings("unused")
    private void forcePeace(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.peace", "mf.force.*", "mf.admin"))) {
            return;
        }
        if (!(args.length >= 3)) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force peace \"faction1\" \"faction2\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction former = this.factionRepository.get(doubleQuoteArgs.get(0));
        final Faction latter = this.factionRepository.get(doubleQuoteArgs.get(1));
        if (former == null || latter == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("DesignatedFactionNotFound")));
            return;
        }
        FactionWarEndEvent warEndEvent = new FactionWarEndEvent(former, latter);
        Bukkit.getPluginManager().callEvent(warEndEvent);
        if (!warEndEvent.isCancelled()) {
            if (former.isEnemy(latter.getName())) former.removeEnemy(latter.getName());
            if (latter.isEnemy(former.getName())) latter.removeEnemy(former.getName());

            // announce peace to all players on server.
            this.messageServer(
                "&a" + this.localeService.getText("AlertNowAtPeaceWith", former.getName(), latter.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAtPeaceWith"))
                    .replace("#p1#", former.getName())
                    .replace("#p2#", latter.getName())
            );
        }
    }

    @SuppressWarnings("unused")
    private void forceDemote(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.demote", "mf.force.*", "mf.admin"))) return;
        if (!(args.length > 1)) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("UsageForceDemote")));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(args[1]);
        if (playerUUID == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        final Faction faction = this.playerService.getPlayerFaction(player);
        if (!faction.isOfficer(player.getUniqueId())) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerIsNotOfficerOfFaction")));
            return;
        }
        faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(this.translate("&b" + this.localeService.getText("AlertForcedDemotion")));
        }
        sender.sendMessage(this.translate("&a" + this.localeService.getText("SuccessOfficerRemoval")));
    }

    @SuppressWarnings("unused")
    private void forceJoin(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.join", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 3)) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force join \"player\" \"faction\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction faction = this.factionRepository.get(doubleQuoteArgs.get(1));
        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(doubleQuoteArgs.get(0));
        if (playerUUID == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        if (this.persistentData.isInFaction(playerUUID)) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerAlreadyInFaction")));
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            this.logger.debug("Join event was cancelled.");
            return;
        }
        this.messageFaction(
            faction,
            this.translate("&a" + this.localeService.getText("HasJoined", player.getName(), faction.getName())),
            ""
        );
        faction.addMember(playerUUID);
        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(this.translate("&b" + this.localeService.getText("AlertForcedToJoinFaction")));
        }
        sender.sendMessage(this.translate("&a" + this.localeService.getText("SuccessForceJoin")));
    }

    @SuppressWarnings("unused")
    private void forceKick(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.kick", "mf.force.*", "mf.admin"))) return;
        if (!(args.length > 1)) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("UsageForceKick")));
            return;
        }
        if (this.medievalFactions.isDebugEnabled()) {
            System.out.printf("Looking for player UUID based on player name: '%s'%n", args[1]);
        }
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[1]);
        if (targetUUID == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        final Faction faction = this.playerService.getPlayerFaction(target);
        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }
        if (faction.isOwner(targetUUID)) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("CannotForciblyKickOwner")));
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, null); // no kicker so null is used
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        if (faction.isOfficer(targetUUID)) {
            faction.removeOfficer(targetUUID); // Remove Officer (if one)
        }
        this.ephemeralData.getPlayersInFactionChat().remove(targetUUID);
        faction.removeMember(targetUUID);
        this.messageFaction(
            faction,
            this.translate("&c" + this.localeService.getText("HasBeenKickedFrom", target.getName(), faction.getName())), 
            ""
        );
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(this.translate("&c" + this.localeService.getText("AlertKicked", "an admin")));
        }
        sender.sendMessage(this.translate("&a" + this.localeService.getText("SuccessFactionMemberRemoval")));
    }

    @SuppressWarnings("unused")
    private void forcePower(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.power", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 3)) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force power \"player\" \"number\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(doubleQuoteArgs.get(0));
        final int desiredPower = getIntSafe(doubleQuoteArgs.get(1), Integer.MIN_VALUE);
        if (desiredPower == Integer.MIN_VALUE) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("DesiredPowerMustBeANumber")));
            return;
        }
        final PowerRecord record = this.persistentData.getPlayersPowerRecord(playerUUID);
        record.setPower(desiredPower); // Set power :)
        sender.sendMessage(this.translate("&a" + this.localeService.getText("PowerLevelHasBeenSetTo", desiredPower)));
    }

    @SuppressWarnings("unused")
    private void forceRenounce(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.renounce", "mf.force.*", "mf.admin"))) return;
        if (args.length < 2) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force renounce \"faction\""));
            return;
        }
        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);

        if (doubleQuoteArgs.size() == 0) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final String factionName = doubleQuoteArgs.get(0);
        final Faction faction = this.factionRepository.get(factionName);
        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }

        long changes = this.persistentData.removeLiegeAndVassalReferencesToFaction(factionName);

        if (!faction.getLiege().equalsIgnoreCase("none")) {
            faction.setLiege("none");
            changes++;
        }
        if (faction.getNumVassals() != 0) {
            changes = changes + faction.getNumVassals();
            faction.clearVassals();
        }
        if (changes == 0) sender.sendMessage(this.translate("&a" + this.localeService.getText("NoVassalOrLiegeReferences")));
        else sender.sendMessage(this.translate("&a" + this.localeService.getText("SuccessReferencesRemoved")));
    }

    @SuppressWarnings("unused")
    private void forceTransfer(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.transfer", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 3)) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force transfer \"faction\" \"player\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction faction = this.factionRepository.get(doubleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(doubleQuoteArgs.get(1));
        if (playerUUID == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("PlayerNotFound")));
            return;
        }
        if (faction.isOwner(playerUUID)) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("AlertPlayerAlreadyOwner")));
            return;
        }
        if (!faction.isMember(playerUUID)) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("AlertPlayerNotInFaction")));
            return;
        }
        if (faction.isOfficer(playerUUID)) faction.removeOfficer(playerUUID); // Remove Officer.
        faction.setOwner(playerUUID);

        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(this.translate("&a" + this.localeService.getText("OwnershipTransferred", faction.getName())));
        }
        sender.sendMessage(this.translate("&a" + this.localeService.getText("OwnerShipTransferredTo", player.getName())));
    }

    @SuppressWarnings("unused")
    private void forceRemoveVassal(CommandSender sender, String[] args) {
        // TODO: inform of no permissions
        if (!(this.checkPermissions(sender, "mf.force.removevassal", "mf.force.*", "mf.admin"))) return;
        if (args.length < 3) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force removevassal \"liege\" \"vassal\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction liege = this.factionRepository.get(doubleQuoteArgs.get(0));
        final Faction vassal = this.factionRepository.get(doubleQuoteArgs.get(1));
        if (liege != null && vassal != null) {
            // remove vassal from liege
            if (liege.isVassal(vassal.getName())) liege.removeVassal(vassal.getName());
            // set liege to "none" for vassal (if faction exists)
            if (vassal.isLiege(liege.getName())) vassal.setLiege("none");
        }
        sender.sendMessage(this.translate("&a" + this.localeService.getText("Done")));
    }

    @SuppressWarnings("unused")
    private void forceRename(CommandSender sender, String[] args) {
        // TODO: force of no permission
        if (!(this.checkPermissions(sender, "mf.force.rename", "mf.force.*", "mf.admin"))) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force rename \"faction\" \"new name\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        Faction faction = this.factionRepository.get(doubleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }
        final String newName = doubleQuoteArgs.get(1);
        final String oldName = faction.getName();
        // rename faction
        if (this.persistentData.getFaction(newName) != null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionAlreadyExists")));
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
        sender.sendMessage(this.translate("&a" + this.localeService.getText("FactionNameChanged")));

        this.persistentData.updateFactionReferencesDueToNameChange(oldName, newName);

        // Prefix (if it was unset)
        if (faction.getPrefix().equalsIgnoreCase(oldName)) faction.setPrefix(newName);

        // Save again to overwrite current data
        this.persistentData.getLocalStorageService().save();
    }

    @SuppressWarnings("unused")
    private void forceBonusPower(CommandSender sender, String[] args) {
        // TODO: inform of no permission
        if (!(checkPermissions(sender, "mf.force.bonuspower", "mf.force.*", "mf.admin"))) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force bonuspower \"faction\" \"number\""));
            return;
        }

        final List<String> singleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }

        // get faction
        Faction faction = this.factionRepository.get(singleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }

        // get bonus power
        final int bonusPower = getIntSafe(singleQuoteArgs.get(1), Integer.MIN_VALUE);
        if (bonusPower == Integer.MIN_VALUE) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("DesiredPowerMustBeANumber")));
            return;
        }

        // set bonus power
        faction.setBonusPower(bonusPower);

        // inform sender
        sender.sendMessage(this.translate("&a" + this.localeService.getText("Done")));
    }

    @SuppressWarnings("unused")
    private void forceUnlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;

        // TODO: inform of no permission
        if (!(checkPermissions(player, "mf.force.unlock", "mf.force.*", "mf.admin"))) {
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("cancel")) {
            this.ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
            this.ephemeralData.getForcefullyUnlockingPlayers().remove(player.getUniqueId());
            player.sendMessage(this.translate("&c" + this.localeService.getText("AlertUnlockingCancelled")));
            return;
        }
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
        player.sendMessage(this.translate("&a" + this.localeService.getText("RightClickForceUnlock")));
    }

    @SuppressWarnings("unused")
    public void forceCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;

        // TODO: inform of no permission
        if (!(checkPermissions(player, "mf.force.create", "mf.force.*", "mf.admin"))) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force create \"faction name\""));
            return;
        }

        final List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 1) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }

        String newFactionName = doubleQuoteArgs.get(0);

        if (this.factionRepository.get(newFactionName) != null) {
            player.sendMessage(this.translate("&c" + this.localeService.getText("FactionAlreadyExists")));
            return;
        }

        this.faction = new Faction(newFactionName);
        this.factionRepository.create(this.faction);
        FactionCreateEvent createEvent = new FactionCreateEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            this.persistentData.addFaction(this.faction);
            player.sendMessage(this.translate("&a" + this.localeService.getText("FactionCreated")));
        }
    }

    @SuppressWarnings("unused")
    public void forceClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player p = (Player) sender;

        // TODO: inform of no permission
        if (!(p.hasPermission("mf.force.claim") || p.hasPermission("mf.force.*") || p.hasPermission("mf.admin"))) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(this.translate("&c" + "Usage: /mf force claim \"faction\""));
            return;
        }

        final List<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 1) {
            sender.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }

        String factionName = argumentsInsideDoubleQuotes.get(0);

        Faction faction = this.factionRepository.get(factionName);

        if (faction == null) {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }

        this.persistentData.getChunkDataAccessor().forceClaimAtPlayerLocation(p, faction);
        sender.sendMessage(this.translate("&a" + this.localeService.getText("Done")));
    }

    @SuppressWarnings("unused")
    private void forceFlag(CommandSender sender, String[] args) {
        // TODO: inform of no permission
        if (!(checkPermissions(sender, "mf.force.flag", "mf.force.*", "mf.admin"))) {
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player ");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 4) {
            player.sendMessage(this.translate("&c" + "Usage: /mf force flag \"faction\" \"flag\" \"value\""));
            return;
        }

        final List<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 3) {
            player.sendMessage(this.translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        Faction faction = this.factionRepository.get(argumentsInsideDoubleQuotes.get(0));
        if (faction == null) {
            player.sendMessage(this.translate("&c" + this.localeService.getText("FactionNotFound")));
            return;
        }
        final String option = argumentsInsideDoubleQuotes.get(1);
        final String value = argumentsInsideDoubleQuotes.get(2);

        faction.getFlags().setFlag(option, value, player);
    }
}