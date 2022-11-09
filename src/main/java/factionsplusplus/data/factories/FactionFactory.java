package factionsplusplus.data.factories;

import factionsplusplus.data.beans.FactionBean;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.Faction;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public interface FactionFactory {
    Faction create();
    Faction create(FactionBean bean);
    Faction create(String factionName, ConcurrentMap<String, ConfigurationFlag> flags);
    Faction create(String factionName, UUID owner, ConcurrentMap<String, ConfigurationFlag> flags);
}
