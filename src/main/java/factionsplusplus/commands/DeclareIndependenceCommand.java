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
import factionsplusplus.services.ConfigService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.data.repositories.WarRepository;
import factionsplusplus.events.internal.FactionWarStartEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareIndependenceCommand extends Command {

    private final ConfigService configService;
    private final WarRepository warRepository;
    private final BukkitAudiences adventure;

    @Inject
    public DeclareIndependenceCommand(
        ConfigService configService,
        WarRepository warRepository,
        @Named("adventure") BukkitAudiences adventure
    ) {
        super(
            new CommandBuilder()
                .withName("declareindependence")
                .withAliases("di", LOCALE_PREFIX + "CmdDeclareIndependence")
                .withDescription("Declare independence from your liege.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.declareindependence")
        );
        this.configService = configService;
        this.warRepository = warRepository;
        this.adventure = adventure;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        Player player = context.getPlayer();
        if (! (faction.hasLiege()) || faction.getLiege() == null) {
            context.error("Error.NotAVassal");
            return;
        }

        final Faction liege = faction.getLiege();

        // Does declaring independence mean war?
        if (! this.configService.getBoolean("allowNeutrality") || (! (faction.getFlag("neutral").toBoolean()) && ! (liege.getFlag("neutral").toBoolean()))) {
            // make enemies if (1) neutrality is disabled or (2) declaring faction is not neutral and liege is not neutral
            FactionWarStartEvent warStartEvent = new FactionWarStartEvent(faction, liege, player);
            Bukkit.getPluginManager().callEvent(warStartEvent);

            if (! warStartEvent.isCancelled()) {
                Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        faction.upsertRelation(liege.getID(), FactionRelationType.Enemy);
                        // TODO: localize this message
                        warRepository.create(faction, liege, String.format("%s declared independence from %s", faction.getName(), liege.getName()));
                    }
                });
            }
        } else {
            // No war, break ties.
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    faction.upsertRelation(liege.getID(), null);
                }
            });
        }
        this.adventure.players().sendMessage(
            Component.translatable("GlobalNotice.Independence.Declared").args(Component.text(faction.getName()), Component.text(liege.getName())).color(NamedTextColor.YELLOW)
        );

    }
}