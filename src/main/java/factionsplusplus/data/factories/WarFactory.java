package factionsplusplus.data.factories;

import com.google.inject.assistedinject.Assisted;
import factionsplusplus.models.War;
import factionsplusplus.data.beans.WarBean;
import factionsplusplus.models.Faction;

public interface WarFactory {
    War create();
    War create(@Assisted("attacker") Faction faction, @Assisted("defender") Faction defender, String reason);
    War create(WarBean bean);
}
