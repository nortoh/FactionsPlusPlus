/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Faction;
import factionsplusplus.models.War;
import factionsplusplus.repositories.WarRepository;
import factionsplusplus.services.MessageService;
import factionsplusplus.builders.MessageBuilder;

import java.util.Objects;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class WarFactory {
    private final WarRepository warRepository;
    private final MessageService messageService;

    @Inject
    public WarFactory(WarRepository warRepository, MessageService messageService) {
        this.warRepository = warRepository;
        this.messageService = messageService;
    }

    public void createWar(Faction attacker, Faction defender, String reason) {
        War war = new War(attacker, defender, reason);

        this.messageService.sendAllPlayersLocalizedMessage(
            new MessageBuilder("HasDeclaredWarAgainst")
                .with("f_a", attacker.getName())
                .with("f_b", defender.getName())
        );

        this.warRepository.create(war);

    }
}