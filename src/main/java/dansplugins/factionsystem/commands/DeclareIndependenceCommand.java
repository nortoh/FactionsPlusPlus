/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.builders.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareIndependenceCommand extends Command {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final ConfigService configService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public DeclareIndependenceCommand(
        PlayerService playerService,
        LocaleService localeService,
        MessageService messageService,
        ConfigService configService,
        PersistentData persistentData,
        FactionRepository factionRepository
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
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.configService = configService;
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        Player player = context.getPlayer();
        if (!(faction.hasLiege()) || faction.getLiege() == null) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("NotAVassalOfAFaction"), "NotAVassalOfAFaction", false);
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

                // break alliance if allied
                if (faction.isAlly(liege.getID())) {
                    faction.removeAlly(liege.getID());
                    liege.removeAlly(faction.getID());
                }
            }
        }
        this.messageService.messageServer(
            "&c" + this.localeService.getText("HasDeclaredIndependence", faction.getName(), liege.getName()), 
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasDeclaredIndependence"))
                .replace("#faction_a#", faction.getName())
                .replace("#faction_b#", liege.getName())
        );

    }
}