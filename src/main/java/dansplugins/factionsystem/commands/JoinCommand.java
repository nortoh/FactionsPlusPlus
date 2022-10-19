/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class JoinCommand extends Command {
    private final MessageService messageService;
    private final LocaleService localeService;
    private final Logger logger;
    private final PersistentData persistentData;

    @Inject
    public JoinCommand(
        MessageService messageService,
        LocaleService localeService,
        PersistentData persistentData,
        Logger logger
    ) {
        super(
            new CommandBuilder()
                .withName("join")
                .withAliases(LOCALE_PREFIX + "CmdJoin")
                .withDescription("Joins a faction.")
                .requiresPermissions("mf.join")
                .expectsPlayerExecution()
                .expectsNoFactionMembership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to join")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.messageService = messageService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        if (!target.isInvited(context.getPlayer().getUniqueId())) {
            context.replyWith("NotInvite");
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(target, context.getPlayer());
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            this.logger.debug("Join event was cancelled.");
            return;
        }
        this.messageService.messageFaction(
            target,
            "&a" + this.localeService.getText("HasJoined", context.getPlayer().getName(), target.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasJoined"))
                .replace("#name#", context.getPlayer().getName())
                .replace("#faction#", target.getName())
        );
        target.addMember(context.getPlayer().getUniqueId());
        target.uninvite(context.getPlayer().getUniqueId());
        context.replyWith("AlertJoinedFaction");
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}