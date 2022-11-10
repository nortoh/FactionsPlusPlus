/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.FlagDataType;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class FlagsCommand extends Command {

    private final ConfigService configService;
    private final DataService dataService;

    @Inject
    public FlagsCommand(ConfigService configService, DataService dataService) {
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
        this.configService = configService;
        this.dataService = dataService;
    }
    
    public void setCommand(CommandContext context) {
        final ConfigurationFlag flag = context.getConfigurationFlagArgument("flag name");
        final String flagValue = context.getStringArgument("value");
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
                public void run() {
                String newValue = context.getExecutorsFaction().setFlag(flag.getName(), flagValue);
                if (newValue == null) {
                    context.error("Error.Setting.InvalidValue", flagValue, flag.getName());
                    return;
                }
                context.success("CommandResponse.Flag.Set", flag.getName(), newValue);
            }
        });
    }

    public void showCommand(CommandContext context) {
        String flagOutput = context.getExecutorsFaction().getFlags()
            .keySet()
            .stream()
            .filter(flagKey -> {
                if (! this.configService.getBoolean("faction.allowNeutrality") && flagKey.equals("neutral")) return false;
                else if (! this.configService.getBoolean("pvp.friendlyFireConfigurationEnabled") && flagKey.equals("allowFriendlyFire")) return false;
                else if ((! this.configService.getBoolean("chat.global.prependFactionPrefix") || ! this.configService.getBoolean("faction.canSetPrefixColor")) && flagKey.equalsIgnoreCase("prefixColor")) return false;
                return true;
            })
            .map(flagKey -> String.format("<color:gold>%s:</color:gold> <color:aqua>%s</color:aqua>", flagKey, context.getExecutorsFaction().getFlags().get(flagKey).toString()))
            .collect(Collectors.joining("\n"));
        context.replyWithMiniMessage(flagOutput);
    }

    public List<String> autoCompleteValue(CommandSender sender, String argument, List<String> rawArguments) {
        if (! (sender instanceof OfflinePlayer)) return List.of();
        final Faction playersFaction = this.dataService.getPlayersFaction((OfflinePlayer)sender);
        final String flagName = rawArguments.get(2);
        final ConfigurationFlag flag = playersFaction.getFlag(flagName);
        List<String> completions = new ArrayList<>();
        List<String> options = List.of();
        if (flag.getRequiredType().equals(FlagDataType.Boolean)) options = List.of("true", "false", "yes", "no", "on", "off");
        if (flag.getRequiredType().equals(FlagDataType.Color)) options = NamedTextColor.NAMES.keys().stream().map(c -> c.toLowerCase()).toList();
        org.bukkit.util.StringUtil.copyPartialMatches(argument, options, completions);
        return completions;
    }

}