package dansplugins.factionsystem.factories;

import dansplugins.factionsystem.objects.domain.Faction;
import java.util.Map;
import java.util.UUID;

public interface FactionFactory {
    Faction create(String factionName, UUID creatorUuid);
    Faction create(Map<String, String> data);
    Faction create(String factionName);
}