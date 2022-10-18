/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.ConfigOption;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import dansplugins.factionsystem.builders.*;
import dansplugins.factionsystem.constants.SetConfigResult;

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
    private final MedievalFactions medievalFactions;

    @Inject
    public ConfigCommand(ConfigService configService, LocaleService localeService, MessageService messageService, MedievalFactions medievalFactions) {
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
                        .withName("list")
                        .withAliases(LOCALE_PREFIX + "CmdConfigList")
                        .withDescription("Lists all current configuration values for this plugin.")
                        .setExecutorMethod("listCommand")
                        .addArgument(
                            "page",
                            new ArgumentBuilder()
                                .setDescription("the page to view")
                                .expectsInteger()
                                .setDefaultValue(1)
                                .isOptional()
                        )
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
        this.medievalFactions = medievalFactions;
    }

    public void execute(CommandSender sender, String[] args, String key) {
        /*if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Valid subcommands: show, set, reload");
            return;
        }

        final boolean show = this.safeEquals(args[0], "get", "show", this.localeService.getText("CmdConfigShow"));
        final boolean set = this.safeEquals(args[0], "set", this.localeService.getText("CmdConfigSet"));
        final boolean reload = this.safeEquals(args[0], "reload", "CmdConfigReload");

        if (show) {
            if (args.length < 2) {
                sender.sendMessage(this.translate("&c" + this.localeService.getText("UsageConfigShow")));
                return;
            }

            int page = this.getIntSafe(args[1], -1);

            if (page == -1) {
                sender.sendMessage(this.translate("&c" + this.localeService.getText("ArgumentMustBeNumber")));
                return;
            }

            switch (page) {
                case 1:
                    this.configService.sendPageOneOfConfigList(sender);
                    break;
                case 2:
                    this.configService.sendPageTwoOfConfigList(sender);
                    break;
                default:
                    sender.sendMessage(this.translate("&c" + this.localeService.getText("UsageConfigShow")));
            }
        } else if (set) {
            if (args.length < 3) {
                sender.sendMessage(this.translate("&c" + this.localeService.getText("UsageConfigSet")));
            } else {
                this.configService.setConfigOption(args[1], args[2], sender);
            }
        } else if (reload) {
            this.medievalFactions.reloadConfig();
            this.messageService.reloadLanguage();
            sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
        } else {
            sender.sendMessage(this.translate("&c" + this.localeService.getText("ValidSubCommandsShowSet")));
        }*/
    }

    public void listCommand(CommandContext context) {
        final int configPage = context.getIntegerArgument("page");
        switch(configPage) {
            case 1:
                this.configService.sendPageOneOfConfigList(context.getSender());
                break;
            case 2:
                this.configService.sendPageTwoOfConfigList(context.getSender());
                break;
            default:
                context.getSender().sendMessage(this.translate("&c" + this.localeService.getText("UsageConfigShow")));
        }
    }

    public void getCommand(CommandContext context) {
        final String configOption = context.getStringArgument("config option");
        context.replyWith(
            this.constructMessage("CurrentConfigValue")
                .with("option", configOption)
                .with("value", this.configService.getString(configOption))
        );
    }

    public void setCommand(CommandContext context) {
        final String configOption = context.getStringArgument("config option");
        final String configValue = context.getStringArgument("config value");
        AbstractMap.SimpleEntry<SetConfigResult, String> result = this.configService.setConfigOption(configOption, configValue);
        switch(result.getKey()) {
            case ValueSet:
                context.replyWith(
                    this.constructMessage("ConfigValueSet")
                        .with("option", configOption)
                        .with("value", result.getValue())
                );
                break;
            case NotExpectedType:
                context.replyWith(
                    this.constructMessage("ConfigValueInvalid")
                        .with("option", configOption)
                        .with("type", result.getValue())
                );
                break;
            case NotUserSettable:
                context.replyWith(
                    this.constructMessage("ConfigValueNotUserSettable")
                        .with("option", configOption)
                );
                break;
            default:
                break;
        }
    }

    public void reloadCommand(CommandContext context) {
        this.medievalFactions.reloadConfig();
        this.messageService.reloadLanguage();
        context.reply(ChatColor.GREEN + "Config reloaded.");
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "show", "set", "reload");
        } else if (args.length == 2) {
            if (args[0] == "show") return TabCompleteTools.completeMultipleOptions(args[1], "1", "2");
            if (args[0] == "set") return TabCompleteTools.filterStartingWith(args[1], this.configService.getStringConfigOptions());
        }
        return null;
    }
}