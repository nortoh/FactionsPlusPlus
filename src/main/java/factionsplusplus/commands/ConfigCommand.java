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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;
import factionsplusplus.constants.SetConfigResult;

import java.util.AbstractMap;

@Singleton
public class ConfigCommand extends Command {

    private final ConfigService configService;
    private final LocaleService localeService;
    private final FactionsPlusPlus factionsPlusPlus;

    @Inject
    public ConfigCommand(ConfigService configService, LocaleService localeService, FactionsPlusPlus factionsPlusPlus) {
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
        this.factionsPlusPlus = factionsPlusPlus;
    }

    public void getCommand(CommandContext context) {
        final ConfigOption configOption = context.getConfigOptionArgument("config option");
        context.getExecutorsAudience().sendMessage(Component.translatable("ConfigurationOption.Title").args(Component.text(configOption.getName())).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        context.replyWith("ConfigurationOption.Description", configOption.getDescription());
        context.replyWith("ConfigurationOption.Default", configOption.getDefaultValue());
        context.replyWith("ConfigurationOption.Current", this.configService.getString(configOption.getName()));
    }

    public void setCommand(CommandContext context) {
        final ConfigOption configOption = context.getConfigOptionArgument("config option");
        final String configValue = context.getStringArgument("config value");
        AbstractMap.SimpleEntry<SetConfigResult, String> result = this.configService.setConfigOption(configOption.getName(), configValue);
        switch(result.getKey()) {
            case ValueSet:
                context.success("CommandResponse.Config.Set", configOption.getName(), result.getValue());
                break;
            case NotExpectedType:
                context.error("Error.Setting.InvalidValue", result.getValue(), configOption.getName());
                break;
            case NotUserSettable:
                context.error("Error.Setting.NotUserSettable", configOption.getName());
                break;
            default:
                break;
        }
    }

    public void reloadCommand(CommandContext context) {
        this.factionsPlusPlus.reloadConfig();
        this.localeService.reloadLanguage();
        context.success("CommandResponse.Config.Reloaded");
    }
}