/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands.abs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
    private boolean playerCommand;
    private boolean requiresFaction;
    private boolean requiresOfficer;
    private boolean requiresOwner;
    protected Faction faction = null;
    protected String[] names;
    protected String[] requiredPermissions;

    public SubCommand() {
        this.requiredPermissions = new String[]{};
    }

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
        if (this.checkPermissions(sender).size() > 0) return null;
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
    public List<String> checkPermissions(CommandSender sender) {
        return this.checkPermissions(sender, this.requiredPermissions);
    }

    public List<String> checkPermissions(CommandSender sender, boolean announcePermissionsMissing) {
        return this.checkPermissions(sender, this.requiredPermissions);
    }

    public List<String> checkPermissions(CommandSender sender, boolean announcePermissionsMissing, String... permissions) {
        return this.checkPermissions(sender, permissions);
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
    public List<String> checkPermissions(CommandSender sender, String... permissions) {
        List<String> missingPermissions = new ArrayList<>();
        if (permissions.length == 0) return missingPermissions;
        boolean hasPermission = false;
        for (String perm : permissions) {
            hasPermission = sender.hasPermission(perm);
            if (hasPermission) return missingPermissions;
            missingPermissions.add(perm);
        }
        return missingPermissions;
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "names=" + Arrays.toString(this.names) + '}';
    }

}
