/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.factories.WarFactory;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.War;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.builders.CommandBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareIndependenceCommand extends Command {

    private final MessageService messageService;
    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final WarFactory warFactory;

    @Inject
    public DeclareIndependenceCommand(
        MessageService messageService,
        ConfigService configService,
        PersistentData persistentData,
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
        this.messageService = messageService;
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

        final Faction liege = this.factionRepository.getByID(faction.getLiege());

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
        this.messageService.sendAllPlayersLocalizedMessage(
            this.constructMessage("HasDeclaredIndependence")
                .with("faction_a", faction.getName())
                .with("faction_b", liege.getName())
        );

    }
}