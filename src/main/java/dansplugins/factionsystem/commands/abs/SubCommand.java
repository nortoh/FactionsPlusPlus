/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands.abs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 * @since 05/05/2021 - 12:18
 */
public abstract class SubCommand implements ColorTranslator {
    public static final String LOCALE_PREFIX = "Locale_";
    protected MessageService messageService;
    protected PlayerService playerService;
    protected LocaleService localeService;
    protected ConfigService configService;
    protected PersistentData persistentData;
    protected EphemeralData ephemeralData;
    protected PersistentData.ChunkDataAccessor chunkDataAccessor;
    protected DynmapIntegrator dynmapIntegrator;
    private boolean playerCommand;
    private boolean requiresFaction;
    private boolean requiresOfficer;
    private boolean requiresOwner;
    protected Faction faction = null;
    protected String[] names;
    protected String[] requiredPermissions;

    public void setUserFaction(Faction faction) {
        this.faction = faction;
    }

    public void setName(Integer index, String name) {
        this.names[index] = name;
    }

    public SubCommand setNames(String... names) {
        this.names = names;
        return this;
    }

    public SubCommand requiresFactionOfficer() {
        this.requiresOfficer = true;
        return this;
    }

    public SubCommand requiresFactionOwner() {
        this.requiresOfficer = true;
        return this;
    }

    public SubCommand isPlayerCommand() {
        this.playerCommand = true;
        return this;
    }

    public SubCommand requiresPlayerInFaction() {
        this.requiresFaction = true;
        return this;
    }

    public SubCommand requiresPermissions(String... permissions) {
        this.requiredPermissions = permissions;
        return this;
    }

    public boolean shouldRequireFactionOwner() {
        return this.requiresOwner;
    }

    public boolean shouldRequireFactionOfficer() {
        return this.requiresOfficer;
    }

    public boolean shouldBePlayerCommand() {
        return this.playerCommand;
    }

    public boolean shouldRequirePlayerInFaction() {
        return this.requiresFaction;
    }
    
    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    public abstract void execute(Player player, String[] args, String key);

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    public abstract void execute(CommandSender sender, String[] args, String key);

    /**
     * Parent method to conduct tab completion. This will check permissions first, then hand to the child.
     */
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermissions(sender)) return null;
        if (this.playerCommand) {
            if (!(sender instanceof Player)) {
                return this.handleTabComplete((Player)sender, args);
            }
            return null;
        }
        return this.handleTabComplete(sender, args);
    }

    /**
     * Child method to conduct tab completion. Classes that inherit this class should override this if they can offer tab completion.
     */
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return null;
    }

    public List<String> handleTabComplete(Player player, String[] args) {
        return null;
    }


    /**
     * Method to determine if a String is this SubCommand or not.
     *
     * @param name of the command.
     * @return {@code true} if it is.
     */
    public boolean isCommand(String name) {
        return Arrays.stream(this.names).anyMatch(s -> s.equalsIgnoreCase(name));
    }

    // Helper methods for checkPermissions in different cases
    public boolean checkPermissions(CommandSender sender) {
        return this.checkPermissions(sender, false, this.requiredPermissions);
    }

    public boolean checkPermissions(CommandSender sender, boolean announcePermissionsMissing) {
        return this.checkPermissions(sender, announcePermissionsMissing, this.requiredPermissions);
    }

    public boolean checkPermissions(CommandSender sender, String... permissions) {
        return this.checkPermissions(sender, true, permissions);
    }

    /**
     * Method to check if a sender has a permission.
     * <p>
     * If the sender doesn't have the permission, they are messaged the formatted no Permission message.
     * </p>
     *
     * @param sender     to check.
     * @param permission to test for.
     * @return {@code true} if they do.
     */
    public boolean checkPermissions(CommandSender sender, boolean announcePermissionsMissing, String... permissions) {
        boolean hasPermission = false;
        List<String> missingPermissions = new ArrayList<String>();
        for (String perm : permissions) {
            hasPermission = sender.hasPermission(perm);
            if (hasPermission) break;
            missingPermissions.add(perm);
        }
        return hasPermission;
    }

    /**
     * Method to obtain text from a key.
     *
     * @param key of the message in LocaleManager.
     * @return String message
     */
    protected String getText(String key) {
        String text = this.localeService.getText(key);
        return text.replace("%d", "%s");
    }

    /**
     * Method to obtain text from a key with replacements.
     *
     * @param key          to obtain.
     * @param replacements to replace within the message using {@link String#format(String, Object...)}.
     * @return String message
     */
    protected String getText(String key, Object... replacements) {
        return String.format(this.getText(key), replacements);
    }

    /** 
     * Method to retrieve the list of command names for this command.
     */
    public String[] getCommandNames() {
        return this.names;
    }

    /**
     * Get primary command name.
    */
    public String getPrimaryCommandName() {
        return this.names[0];
    }

    /**
     * Method to obtain a Faction by name.
     * <p>
     * This is a passthrough function.
     * </p>
     *
     * @param name of the desired Faction.
     * @return {@link Faction}
     */
    protected Faction getFaction(String name) {
        return this.persistentData.getFaction(name);
    }

    /**
     * Method to send an entire Faction a message.
     *
     * @param faction    to send a message to.
     * @param oldMessage old message to send to the Faction.
     * @param newMessage new message to send to the Faction.
     */
    protected void messageFaction(Faction faction, String oldMessage, String newMessage) {
        faction.getMemberList()
            .stream()
            .map(Bukkit::getOfflinePlayer)
            .filter(OfflinePlayer::isOnline)
            .map(OfflinePlayer::getPlayer)
            .filter(Objects::nonNull)
            .forEach(player -> this.playerService.sendMessage(player, oldMessage, newMessage, true));
    }

    /**
     * Method to send the entire Server a message.
     *
     * @param oldMessage old message to send to the players.
     * @param newMessage old message to send to the players.
     */
    protected void messageServer(String oldMessage, String newMessage) {
        Bukkit.getOnlinePlayers().forEach(player -> this.playerService.sendMessage(player, oldMessage, newMessage, true));
    }

    /**
     * Method to get an Integer from a String.
     *
     * @param line   to convert into an Integer.
     * @param orElse if the conversion fails.
     * @return {@link Integer} numeric.
     */
    protected int getIntSafe(String line, int orElse) {
        try {
            return Integer.parseInt(line);
        } catch (Exception ex) {
            return orElse;
        }
    }

    /**
     * Method to test if something matches any goal string.
     *
     * @param what  to test
     * @param goals to compare with
     * @return {@code true} if something in goals matches what.
     */
    protected boolean safeEquals(String what, String... goals) {
        return Arrays.stream(goals).anyMatch(goal -> goal.equalsIgnoreCase(what));
    }

    /**
     * Method to obtain the Config.yml for Medieval Factions.
     *
     * @return {@link FileConfiguration}
     */
    protected FileConfiguration getConfig() {
        return this.configService.getConfig();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "names=" + Arrays.toString(this.names) + '}';
    }

}
