/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.constants.GroupRole;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.extended.Scheduler;
import factionsplusplus.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class HomeCommand extends Command {
    private final Scheduler scheduler;

    @Inject
    public HomeCommand(Scheduler scheduler) {
        super(
            new CommandBuilder()
                .withName("home")
                .withAliases(LOCALE_PREFIX + "CmdHome")
                .withDescription("Teleport to your faction home.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .requiresPermissions("mf.home")
        );
        this.scheduler = scheduler;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        if (faction.getDefaultBase() == null) {
            context.replyWith("NoDefeaultBase");
            return;
        }
        // Do they have permission?
        if (! faction.getDefaultBase().shouldAllowAllFactionMembers() && ! context.getExecutorsFaction().getMember(context.getPlayer().getUniqueId()).hasRole(GroupRole.Officer)) {
            context.replyWith("BaseTeleportDenied");
            return;
        }
        this.scheduler.scheduleTeleport(context.getPlayer(), faction.getDefaultBase().getBukkitLocation());
    }
}