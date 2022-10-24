/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.services.ConfigService;
import org.bukkit.ChatColor;

import java.util.stream.Collectors;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.builders.ArgumentBuilder;

// TODO: implement tab complete for basic values (i.e. true/false for boolean)

/**
 * @author Callum Johnson
 */
@Singleton
public class FlagsCommand extends Command {

    private final ConfigService configService;

    @Inject
    public FlagsCommand(ConfigService configService) {
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
    }
    
    public void setCommand(CommandContext context) {
        final FactionFlag flag = context.getFactionFlagArgument("flag name");
        final String flagValue = context.getStringArgument("value");
        String newValue = flag.set(flagValue);
        if (newValue == null) {
            context.replyWith(
                new MessageBuilder("FactionFlagValueInvalid")
                    .with("type", flag.getRequiredType().toString())
            );
            return;
        }
        context.replyWith(
            new MessageBuilder("FactionFlagValueSet")
                .with("value", newValue)
        );
    }

    public void showCommand(CommandContext context) {
        String flagOutput = context.getExecutorsFaction().getFlags()
            .keySet()
            .stream()
            .filter(flagKey -> {
                return (
                    (!this.configService.getBoolean("allowNeutrality") && !flagKey.equalsIgnoreCase("neutral")) &&
                    ((!this.configService.getBoolean("playersChatWithPrefixes") || this.configService.getBoolean("factionsCanSetPrefixColors")) && !flagKey.equalsIgnoreCase("prefixColor"))
                );
            })
            .map(flagKey -> String.format("%s: %s", flagKey, context.getExecutorsFaction().getFlags().get(flagKey).toString()))
            .collect(Collectors.joining(", "));
        context.reply(ChatColor.AQUA + "" + flagOutput);
    }
}