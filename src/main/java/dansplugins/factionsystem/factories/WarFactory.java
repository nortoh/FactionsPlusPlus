/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.War;
import dansplugins.factionsystem.repositories.WarRepository;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;

import java.util.Objects;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class WarFactory {
    private final WarRepository warRepository;
    private final MessageService messageService;
    private final LocaleService localeService;

    @Inject
    public WarFactory(WarRepository warRepository, MessageService messageService, LocaleService localeService) {
        this.warRepository = warRepository;
        this.messageService = messageService;
        this.localeService = localeService;
    }

    public void createWar(Faction attacker, Faction defender, String reason) {
        War war = new War(attacker, defender, reason);

        this.messageService.messageServer(
            "&c" + this.localeService.getText("HasDeclaredWarAgainst", attacker.getName(), defender.getName()), 
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasDeclaredWarAgainst"))
                .replace("#f_a#", attacker.getName())
                .replace("#f_b#", defender.getName())
        );

        this.warRepository.create(war);

    }
}