/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.FactionFlag;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.stream.Collectors;
import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;
import java.util.List;
import java.util.HashMap;

/**
 * @author Callum Johnson
 */
@Singleton
public class FlagsCommand extends Command {

    private final PersistentData persistentData;
    private final ConfigService configService;

    @Inject
    public FlagsCommand(
        PersistentData persistentData,
        ConfigService configService
    ) {
        super(
            new CommandBuilder()
                .withName("flags")
                .withAliases(LOCALE_PREFIX + "CmdFlags")
                .withDescription("Manage faction flags.")
                .requiresPermissions("mf.flags")
                .requiresSubCommand()
                .expectsPlayerExecution()
                .expectsFactionOwnership()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("show")
                        .withAliases(LOCALE_PREFIX + "CmdFlagShow")
                        .withDescription("Shows the current faction flags.")
                        .setExecutorMethod("showCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("set")
                        .withAliases(LOCALE_PREFIX + "CmdFlagSet")
                        .withDescription("Sets a faction flag.")
                        .setExecutorMethod("setCommand")
                        .addArgument(
                            "flag name",
                            new ArgumentBuilder()
                                .setDescription("the flag you are setting")
                                .expectsFactionFlagName()
                            .isRequired()
                        )
                        .addArgument(
                            "value",
                            new ArgumentBuilder()
                                .setDescription("the value of the flag you are setting")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
        );
        this.persistentData = persistentData;
        this.configService = configService;
    }
    
    public void setCommand(CommandContext context) {
        final FactionFlag flag = context.getFactionFlagArgument("flag name");
        final String flagValue = context.getStringArgument("value");
        flag.set(flagValue);
        // TODO: error handling, alert that flag was set
    }

    public void showCommand(CommandContext context) {
        // TODO: move the logic for this into this class
        HashMap<String, FactionFlag> flagList = (HashMap<String, FactionFlag>)context.getExecutorsFaction().getFlags().clone();
        if (!this.configService.getBoolean("allowNeutrality")) flagList.remove("neutral");
        if (!this.configService.getBoolean("playersChatWithPrefixes") || this.configService.getBoolean("factionsCanSetPrefixColors")) flagList.remove("prefixColor");
        String flagOutput = flagList
            .keySet()
            .stream()
            .map(flagKey -> String.format("%s: %s", flagKey, flagList.get(flagKey).toString()))
            .collect(Collectors.joining(", "));
        context.reply(ChatColor.AQUA + "" + flagOutput);
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
            return TabCompleteTools.completeMultipleOptions(args[0], "set", "show");
        } else if (args.length == 2) {
            if (args[0] == "set") {
                if (this.persistentData.isInFaction(player.getUniqueId())) {
                    Faction faction = this.persistentData.getPlayersFaction(player.getUniqueId());
                    return TabCompleteTools.filterStartingWith(args[1], faction.getFlagNames().stream());
                }
            }
        }
        return null;
    }
}