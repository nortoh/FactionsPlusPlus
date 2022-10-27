package factionsplusplus.factories;

import factionsplusplus.beans.FactionBean;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.Faction;

import java.util.Map;
import java.util.UUID;

public interface FactionFactory {
    Faction create();
    Faction create(FactionBean bean);
    Faction create(String factionName, Map<String, ConfigurationFlag> flags);
    Faction create(String factionName, UUID owner, Map<String, ConfigurationFlag> flags);
}
