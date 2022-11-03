/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.utils.StringUtils;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.MessageService;
import factionsplusplus.builders.ArgumentBuilder;
import factionsplusplus.builders.CommandBuilder;

import org.bukkit.ChatColor;

@Singleton
public class SayCommand extends Command {

    private final ConfigService configService;
    private final MessageService messageService;

    @Inject
    public SayCommand(ConfigService configService, MessageService messageService) {
        super(
            new CommandBuilder()
                .withName("say")
                .withAliases(LOCALE_PREFIX + "CmdSay")
                .withDescription("Send a message to faction chat.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .requiresPermissions("mf.chat")
                .addArgument(
                    "text",
                    new ArgumentBuilder()
                        .setDescription("The text to send to faction chat.")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.configService = configService;
        this.messageService = messageService;
    }

    public void execute(CommandContext context) {
        final String message = context.getStringArgument("text");
        final String factionChatColor = this.configService.getString("factionChatColor");
        final String prefixColor = context.getExecutorsFaction().getFlag("prefixColor").toString();
        final String prefix = context.getExecutorsFaction().getPrefix();
        // TODO: use new messaging api for this
        if (this.configService.getBoolean("showPrefixesInFactionChat")) {
            this.messageService.sendToFaction(context.getExecutorsFaction(), StringUtils.parseAsChatColor(prefixColor) + "" + "[" + prefix + "] " + "" + ChatColor.WHITE + "" + context.getPlayer().getName() + ": " + StringUtils.parseAsChatColor(factionChatColor) + message);
            return;
        }
        this.messageService.sendToFaction(context.getExecutorsFaction(), ChatColor.WHITE + "" + context.getPlayer().getName() + ": " + StringUtils.parseAsChatColor(factionChatColor) + message);
    }
}