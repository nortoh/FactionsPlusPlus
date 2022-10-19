/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.objects.domain.War;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class WarFactory {
    private final PersistentData persistentData;

    @Inject
    public WarFactory(PersistentData persistentData) {
        this.persistentData = persistentData;
    }

    public void createWar(Faction attacker, Faction defender, String reason) {
        War war = new War(attacker, defender, reason);

        // TODO: inform factions of war here instead of in the declare war command

        persistentData.addWar(war);
    }
}