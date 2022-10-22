/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.events.FactionWarStartEvent;
import factionsplusplus.factories.WarFactory;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.models.War;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.MessageService;
import factionsplusplus.builders.CommandBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareIndependenceCommand extends Command {

    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final WarFactory warFactory;

    @Inject
    public DeclareIndependenceCommand(
        ConfigService configService,
        FactionRepository factionRepository,
        WarFactory warFactory
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
        this.factionRepository = factionRepository;
        this.warFactory = warFactory;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        Player player = context.getPlayer();
        if (!(faction.hasLiege()) || faction.getLiege() == null) {
            context.replyWith("NotAVassalOfAFaction");
            return;
        }

        final Faction liege = this.factionRepository.get(faction.getLiege());

        // break vassal agreement.
        liege.removeVassal(faction.getID());
        faction.setLiege(null);

        if (!this.configService.getBoolean("allowNeutrality") || (!(faction.getFlag("neutral").toBoolean()) && !(liege.getFlag("neutral").toBoolean()))) {
            // make enemies if (1) neutrality is disabled or (2) declaring faction is not neutral and liege is not neutral
            FactionWarStartEvent warStartEvent = new FactionWarStartEvent(faction, liege, player);
            Bukkit.getPluginManager().callEvent(warStartEvent);

            if (!warStartEvent.isCancelled()) {
                faction.addEnemy(liege.getID());
                liege.addEnemy(faction.getID());

                // TODO: check if they're already at work (which would be weird since they were a previous vassal?)
                this.warFactory.createWar(faction, liege, String.format("%s declared independence from %s", faction.getName(), liege.getName()));

                // break alliance if allied
                if (faction.isAlly(liege.getID())) {
                    faction.removeAlly(liege.getID());
                    liege.removeAlly(faction.getID());
                }
            }
        }
        context.messageAllPlayers(
            this.constructMessage("HasDeclaredIndependence")
                .with("faction_a", faction.getName())
                .with("faction_b", liege.getName())
        );

    }
}