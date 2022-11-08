package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.ArgumentBuilder;
import factionsplusplus.builders.CommandBuilder;

@Singleton
public class SayCommand extends Command {

    @Inject
    public SayCommand() {
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
    }

    public void execute(CommandContext context) {
        context.getExecutorsFaction().sendToFactionChatAs(context.getPlayer(), context.getStringArgument("text"));
    }
}