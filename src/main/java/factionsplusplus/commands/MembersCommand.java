/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;
import org.bukkit.Bukkit;

@Singleton
public class MembersCommand extends Command {

    @Inject
    public MembersCommand() {
        super(
            new CommandBuilder()
                .withName("members")
                .withAliases(LOCALE_PREFIX + "CmdMembers")
                .withDescription("List the members of your faction or another faction.")
                .requiresPermissions("mf.members")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to get a members list of")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isOptional()
                )
        );
    }

    public void execute(CommandContext context) {
        final Faction faction;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.error("Error.PlayerExecutionRequired");
                return;
            }
            faction = context.getExecutorsFaction();
            if (faction == null) {
                context.error("Error.Faction.MembershipNeeded");
                return;
            }
        } else {
            faction = context.getFactionArgument("faction name");
        }
        // send Faction Members
        context.getExecutorsAudience().sendMessage(
            Component.translatable("FactionMemberList.Title").args(Component.text(faction.getName())).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)
        );
        faction.getMembers().keySet().stream()
                .map(Bukkit::getOfflinePlayer)
                .forEach(player -> {
                    final GroupRole role = GroupRole.getFromLevel(faction.getMember(player.getUniqueId()).getRole());
                    context.getExecutorsAudience().sendMessage(
                        Component
                            .translatable("FactionMemberList.Member")
                            .args(Component.text(player.getName()))
                            .hoverEvent(HoverEvent.showText(Component.translatable("Generic.Role."+role.name())))
                    );
                });
    }
}