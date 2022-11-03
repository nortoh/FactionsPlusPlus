package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionBase;
import factionsplusplus.services.ClaimService;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.utils.StringUtils;
import factionsplusplus.utils.extended.Scheduler;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class BaseCommand extends Command {
    private final ConfigService configService;
    private final ClaimService claimService;
    private final DataService dataService;
    private final Scheduler scheduler;

    @Inject
    public BaseCommand(ConfigService configService, DataService dataService, ClaimService claimService, Scheduler scheduler) {
        super(
            new CommandBuilder()
                .withName("base")
                .withAliases("home", LOCALE_PREFIX + "CmdBase")
                .withDescription("Manage your factions bases.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .setExecutorMethod("teleportCommand")
                .addSubCommand(
                    new CommandBuilder()
                        .withName("list")
                        .withAliases(LOCALE_PREFIX + "CmdListBases")
                        .requiresPermissions("mf.listbases")
                        .withDescription("Lists your factions bases.")
                        .setExecutorMethod("listCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("add")
                        .withAliases("create", "al", LOCALE_PREFIX + "CmdAddBase")
                        .requiresPermissions("mf.addbase")
                        .withDescription("Adds a new base to your faction.")
                        .setExecutorMethod("createCommand")
                        .expectsFactionOfficership()
                        .addArgument(
                            "name",
                            new ArgumentBuilder()
                                .setDescription("the name of the base to add")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("remove")
                        .withAliases("delete", LOCALE_PREFIX + "CmdRemoveBase")
                        .requiresPermissions("mf.removebase")
                        .withDescription("Removes a base from your faction.")
                        .setExecutorMethod("removeCommand")
                        .expectsFactionOfficership()
                        .addArgument(
                            "base to remove",
                            new ArgumentBuilder()
                                .setDescription("the name of the base to remove")
                                .expectsFactionBaseName()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("edit")
                        .withAliases("modify", LOCALE_PREFIX + "CmdEditBase")
                        .requiresPermissions("mf.editbase")
                        .withDescription("Edits a base for your faction.")
                        .setExecutorMethod("editCommand")
                        .expectsFactionOfficership()
                        .addArgument(
                            "base to edit",
                            new ArgumentBuilder()
                                .setDescription("the name of the base to edit")
                                .expectsDoubleQuotes()
                                .expectsFactionBaseName()
                                .isRequired()
                        )
                        .addArgument(
                            "option", 
                            new ArgumentBuilder()
                                .setDescription("the option to edit for the base")
                                .expectsString()
                                .isRequired()
                                .setTabCompletionHandler("autocompleteBaseConfigValues")
                        )
                        .addArgument(
                            "value",
                            new ArgumentBuilder()
                                .setDescription("the value to set for the option for the base")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("rename")
                        .withAliases(LOCALE_PREFIX + "CmdRenameBase")
                        .requiresPermissions("mf.renamebase")
                        .withDescription("Rename a base")
                        .setExecutorMethod("renameCommand")
                        .expectsFactionOfficership()
                        .addArgument(
                            "name",
                            new ArgumentBuilder()
                                .setDescription("the current name of the base to rename")
                                .expectsFactionBaseName()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                        .addArgument(
                            "new name",
                            new ArgumentBuilder()
                                .setDescription("the new name of the base you wish to rename")
                                .expectsString()
                                .expectsDoubleQuotes()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("teleport")
                        .withAliases("go", LOCALE_PREFIX + "CmdTeleportBase")
                        .requiresPermissions("mf.teleport")
                        .withDescription("teleport to a base")
                        .setExecutorMethod("teleportCommand")
                        .addArgument(
                            "name",
                            new ArgumentBuilder()
                                .setDescription("the name of the base to teleport to")
                                .expectsFactionBaseName()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
        );
        this.configService = configService;
        this.claimService = claimService;
        this.scheduler = scheduler;
        this.dataService = dataService;
    }

    public void createCommand(CommandContext context) {
        if (context.getExecutorsFaction().getBases().size() >= this.configService.getInt("factionMaxNumberBases")) {
            context.replyWith("MaxBasesReached");
            return;
        }
        final Faction chunkOwner = this.claimService.checkOwnershipAtPlayerLocation(context.getPlayer());
        if (chunkOwner == null || ! chunkOwner.equals(context.getExecutorsFaction())) {
            context.replyWith("CanOnlyCreateBasesInClaimedTerritory");
            return;
        }
        final String baseName = context.getStringArgument("name");
        final boolean ok = context.getExecutorsFaction().addBase(baseName, context.getPlayer().getLocation());
        if (ok) {
            context.success("CommandResponse.Base.Created", baseName);
            return;
        }
        context.error("Error.Base.Creating");
    }

    public void configCommand(CommandContext context) {
        // TODO: localize these
        final FactionBase base = context.getFactionBaseArgument("base to edit");
        final String option = context.getStringArgument("option");
        final String value = context.getStringArgument("value");
        switch(option.toLowerCase()) {
            case "factiondefault":
                boolean isDefault = StringUtils.parseAsBoolean(value);
                if (isDefault == true) {
                    FactionBase defaultBase = context.getExecutorsFaction().getDefaultBase();
                    if (defaultBase != null && defaultBase.equals(base)) {
                        context.reply("&cThis base is already your factions default base.");
                        return;
                    } else {
                        if (defaultBase != null) { 
                            defaultBase.toggleDefault();
                            context.getExecutorsFaction().persistBase(defaultBase);
                        }
                        base.toggleDefault();
                        context.reply("&aBase set as default.");
                    }
                } else {
                    if (base.isFactionDefault()) {
                        base.toggleDefault();
                        context.reply("&aBase no longer set as default.");
                    } else {
                        context.reply("&cThis base isn't your factions default base.");
                        return;
                    }
                }
                break;
            case "allowallies":
                String newText = base.shouldAllowAllies() ? "are not" : "are";
                base.toggleAllowAllies();
                context.reply(String.format("&aAllies %s allowed to teleport to this base.", newText));
                break;
            case "allowallfactionmembers":
                newText = base.shouldAllowAllFactionMembers() ? "are not" : "are";
                base.toggleAllowAllFactionMembers();
                context.reply(String.format("&aRanks below officer %s allowed to teleport to this base.", newText));
                break;
            default:
                context.reply("&cUnknown setting name.");
                return;
        }
        context.getExecutorsFaction().persistBase(base);
    }

    public void renameCommand(CommandContext context) {
        final FactionBase base = context.getFactionBaseArgument("name");
        final String oldName = base.getName();
        final String newName = context.getStringArgument("new name");
        if (context.getExecutorsFaction().getBase(newName) != null) {
            context.error("Error.Base.AlreadyExists", newName);
            return;
        }
        context.getExecutorsFaction().renameBase(oldName, newName);
        context.getExecutorsFaction().persistBase(base);
        context.success("CommandResponse.Base.Renamed", oldName, newName);
    }

    public void listCommand(CommandContext context) {
        if (context.getExecutorsFaction().getBases().isEmpty()) {
            context.error("Error.Base.NoneAccessible");
            return;
        }
        context.replyWith("FactionBaseList.Title");
        // TODO: if they have access to another factions bases, include in this list
        context.getExecutorsFaction().getBases().values().stream()
            .forEach(base -> {
                if (base.shouldAllowAllFactionMembers() || context.getExecutorsFaction().getMember(context.getPlayer().getUniqueId()).hasRole(GroupRole.Officer)) {
                    context.replyWith(
                        this.constructMessage("FactionBaseList.Base")
                            .with("name", base.getName())
                    );
                }
            });
    }

    public void removeCommand(CommandContext context) {
        final FactionBase base = context.getFactionBaseArgument("base to remove");
        final boolean ok = context.getExecutorsFaction().removeBase(base.getName());
        if (ok) {
            context.success("CommandResponse.Base.Removed", base.getName());
            return;
        }
        context.error("Error.Base.Removing");
    }

    public void teleportCommand(CommandContext context) {
        // TODO: add ability for allies to go to an allied factions bases if permissable, probably adding an optional faction param 
        FactionBase base = context.getFactionBaseArgument("name");
        if (base == null) {
            // must be a fall through, try to final a default base
            base = context.getExecutorsFaction().getDefaultBase();
            if (base == null) {
                context.error("Error.Base.NoFactionDefault");
                return;
            }
        }
        // Check if they have permissions
        if (
            (
                ! base.shouldAllowAllFactionMembers() && 
                ! context.getExecutorsFaction().getMember(context.getPlayer().getUniqueId()).hasRole(GroupRole.Officer)
            )
            ||
            (
                base.shouldAllowAllies() &&
                context.getExecutorsFaction().isAlly(null) // TODO: this should be replaced when we support a target faction
            )
        ) {
            context.error("Error.Base.NotAccessible", base.getName());
            return;
        }
        this.scheduler.scheduleTeleport(context.getPlayer(), base.getBukkitLocation());
    }

    public List<String> autocompleteBaseConfigValues(CommandSender sender, String argument) {
        if (! (sender instanceof Player)) return List.of();
        Faction playersFaction = this.dataService.getPlayersFaction((Player)sender);
        if (playersFaction == null || playersFaction.getBases().isEmpty()) return List.of();
        List<String> completions = new ArrayList<>();
        List<String> options = List.of("allowAllies", "allowAllFactionMembers", "factionDefault");
        org.bukkit.util.StringUtil.copyPartialMatches(argument, options, completions);
        return completions;
    }

}
