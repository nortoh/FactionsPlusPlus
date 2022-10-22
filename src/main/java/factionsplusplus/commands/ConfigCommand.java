/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.LocaleService;
import factionsplusplus.services.MessageService;
import factionsplusplus.builders.*;
import factionsplusplus.constants.SetConfigResult;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class ConfigCommand extends Command {

    private final ConfigService configService;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final FactionsPlusPlus factionsPlusPlus;

    @Inject
    public ConfigCommand(ConfigService configService, LocaleService localeService, MessageService messageService, FactionsPlusPlus factionsPlusPlus) {
        super(
            new CommandBuilder()
                .withName("config")
                .withAliases(LOCALE_PREFIX + "CmdConfig")
                .withDescription("Manage your servers plugin configuration.")
                .requiresPermissions("mf.config", "mf.admin")
                .requiresSubCommand()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("reload")
                        .withAliases(LOCALE_PREFIX + "CmdConfigReload")
                        .withDescription("Reloads the configuration and language files for this plugin.")
                        .setExecutorMethod("reloadCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("set")
                        .withAliases(LOCALE_PREFIX + "CmdConfigSet")
                        .withDescription("Sets a configuration option.")
                        .setExecutorMethod("setCommand")
                        .addArgument(
                            "config option",
                            new ArgumentBuilder()
                                .setDescription("the config option to set")
                                .expectsConfigOptionName()
                                .isRequired()
                        )
                        .addArgument(
                            "config value",
                            new ArgumentBuilder()
                                .setDescription("the value to set")
                                .expectsString()
                                .consumesAllLaterArguments()
                                .isRequired()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("get")
                        .withAliases("show", LOCALE_PREFIX + "CmdConfigGet")
                        .withDescription("Gets the value of a configuration option.")
                        .setExecutorMethod("getCommand")
                        .addArgument(
                            "config option",
                            new ArgumentBuilder()
                                .setDescription("the config option to retrieve")
                                .expectsConfigOptionName()
                                .isRequired()
                        )
                )
        );
        this.configService = configService;
        this.localeService = localeService;
        this.messageService = messageService;
        this.factionsPlusPlus = factionsPlusPlus;
    }

    public void getCommand(CommandContext context) {
        final ConfigOption configOption = context.getConfigOptionArgument("config option");
        context.replyWith(
            new MultiMessageBuilder()
                .add(this.constructMessage("ConfigOptionInfo.Title").with("name", configOption.getName()))
                .add(this.constructMessage("ConfigOptionInfo.Description").with("desc", configOption.getDescription()))
                .add(this.constructMessage("ConfigOptionInfo.DefaultValue").with("value", String.valueOf(configOption.getDefaultValue())))
                .add(this.constructMessage("ConfigOptionInfo.Value").with("value", this.configService.getString(configOption.getName())))
        );
    }

    public void setCommand(CommandContext context) {
        final ConfigOption configOption = context.getConfigOptionArgument("config option");
        final String configValue = context.getStringArgument("config value");
        AbstractMap.SimpleEntry<SetConfigResult, String> result = this.configService.setConfigOption(configOption.getName(), configValue);
        switch(result.getKey()) {
            case ValueSet:
                context.replyWith(
                    this.constructMessage("ConfigValueSet")
                        .with("option", configOption.getName())
                        .with("value", result.getValue())
                );
                break;
            case NotExpectedType:
                context.replyWith(
                    this.constructMessage("ConfigValueInvalid")
                        .with("option", configOption.getName())
                        .with("type", result.getValue())
                );
                break;
            case NotUserSettable:
                context.replyWith(
                    this.constructMessage("ConfigValueNotUserSettable")
                        .with("option", configOption.getName())
                );
                break;
            default:
                break;
        }
    }

    public void reloadCommand(CommandContext context) {
        this.factionsPlusPlus.reloadConfig();
        this.localeService.reloadLanguage();
        context.reply(ChatColor.GREEN + "Config reloaded.");
    }
}