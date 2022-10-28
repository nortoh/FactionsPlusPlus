package factionsplusplus.factories;

import factionsplusplus.beans.WarBean;
import com.google.inject.assistedinject.Assisted;
import factionsplusplus.models.War;
import factionsplusplus.models.Faction;

public interface WarFactory {
    War create();
    War create(@Assisted("attacker") Faction faction, @Assisted("defender") Faction defender, String reason);
    War create(WarBean bean);
}
