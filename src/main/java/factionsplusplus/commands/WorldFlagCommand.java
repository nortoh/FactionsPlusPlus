package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.World;
import factionsplusplus.services.DataService;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.FlagDataType;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class WorldFlagCommand extends Command {

    private final DataService dataService;

    @Inject
    public WorldFlagCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("worldflag")
                .withAliases(LOCALE_PREFIX + "CmdWorldFlag")
                .withDescription("Manage world flags.")
                .requiresPermissions("mf.admin", "mf.admin.world")
                .requiresSubCommand()
                .expectsPlayerExecution()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("show")
                        .withAliases(LOCALE_PREFIX + "CmdWorldFlagShow")
                        .withDescription("Shows a worlds flags.")
                        .setExecutorMethod("showCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("set")
                        .withAliases(LOCALE_PREFIX + "CmdWorldFlagSet")
                        .withDescription("Sets a faction flag.")
                        .setExecutorMethod("setCommand")
                        .addArgument(
                            "flag name",
                            new ArgumentBuilder()
                                .setDescription("the flag you are setting")
                                .expectsWorldFlagName()
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

        this.dataService = dataService;
    }
    
    public void setCommand(CommandContext context) {
        final ConfigurationFlag flag = context.getConfigurationFlagArgument("flag name");
        final String flagValue = context.getStringArgument("value");
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                String newValue = context.getExecutorsWorld().setFlag(flag.getName(), flagValue);
                if (newValue == null) {
                    context.error("Error.Setting.InvalidValue", flagValue, flag.getName());
                    return;
                }
                context.success("CommandResponse.Flag.Set", flag.getName(), newValue);
            }
        });
    }

    public void showCommand(CommandContext context) {
        String flagOutput = context.getExecutorsWorld().getFlags()
            .keySet()
            .stream()
            .map(flagKey -> String.format("<color:gold>%s:</color:gold> <color:aqua>%s</color:aqua>", flagKey, context.getExecutorsWorld().getFlags().get(flagKey).toString()))
            .collect(Collectors.joining("\n"));
        context.replyWithMiniMessage(flagOutput);
    }

    public List<String> autoCompleteValue(CommandSender sender, String argument, List<String> rawArguments) {
        if (! (sender instanceof Player)) return List.of();
        final World playersWorld = this.dataService.getWorld(((Player)sender).getWorld().getUID());
        final String flagName = rawArguments.get(2);
        final ConfigurationFlag flag = playersWorld.getFlag(flagName);
        List<String> completions = new ArrayList<>();
        List<String> options = List.of();
        if (flag.getRequiredType().equals(FlagDataType.Boolean)) options = List.of("true", "false", "yes", "no", "on", "off");
        org.bukkit.util.StringUtil.copyPartialMatches(argument, options, completions);
        return completions;
    }

}