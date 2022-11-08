/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.models.War;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.data.repositories.WarRepository;
import factionsplusplus.events.internal.FactionWarEndEvent;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class MakePeaceCommand extends Command {

    private final WarRepository warRepository;
    private final BukkitAudiences adventure;

    @Inject
    public MakePeaceCommand(WarRepository warRepository, @Named("adventure") BukkitAudiences adventure) {
        super(
            new CommandBuilder()
                .withName("makepeace")
                .withAliases("mp", LOCALE_PREFIX + "CmdMakePeace")
                .withDescription("Makes peace with an enemy faction.")
                .requiresPermissions("mf.breakalliance")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOfficership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the enemy faction to make peace with")
                        .expectsEnemyFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.warRepository = warRepository;
        this.adventure = adventure;
    }
    
    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        final Faction faction = context.getExecutorsFaction();
        if (target == faction) {
            context.error("Error.MakePeace.Self");
            return;
        }
        if (faction.isTruceRequested(target.getUUID())) {
            context.error("Error.MakePeace.AlreadyRequested", target.getName());
            return;
        }
        faction.requestTruce(target.getUUID());
        faction.alert("FactionNotice.PeaceRequest.Source", target.getName());
        target.alert("FactionNotice.PeaceRequest.Target", faction.getName());
        if (faction.isTruceRequested(target.getUUID()) && target.isTruceRequested(faction.getUUID())) {
            FactionWarEndEvent warEndEvent = new FactionWarEndEvent(faction, target);
            Bukkit.getPluginManager().callEvent(warEndEvent);
            if (! warEndEvent.isCancelled()) {
                // remove requests in case war breaks out again, and they need to make peace again
                faction.removeRequestedTruce(target.getUUID());
                target.removeRequestedTruce(faction.getUUID());

                Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        faction.clearRelation(target.getUUID());
                        if (target.isLiege()) {
                            target.getVassals().stream().forEach(vassal -> {
                                faction.clearRelation(vassal);
                            });
                        }
                        War war = warRepository.getActiveWarsBetween(target.getUUID(), faction.getUUID());
                        war.end();
                        adventure.players().sendMessage(
                            Component.translatable("GlobalNotice.War.Ended").color(NamedTextColor.GREEN).args(Component.text(faction.getName()), Component.text(target.getName()))
                        );
                    }
                });
            }
        }
    }
}