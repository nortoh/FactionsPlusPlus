package factionsplusplus.data.beans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.FactionBase;
import factionsplusplus.models.GroupMember;
import factionsplusplus.models.LocationData;
import lombok.Data;

@Data
public class FactionBean {
    private UUID id;
    private String name;
    private String prefix = null;
    private String description = null;
    @ColumnName("bonus_power")
    private int bonusPower = 0;
    @ColumnName("should_autoclaim")
    private boolean shouldAutoclaim = false;
    @Nested("home")
    private LocationData home;
    private Map<String, ConfigurationFlag> flags = new HashMap<>();
    private Map<UUID, GroupMember> members = new HashMap<>();
    private Map<UUID, FactionRelationType> relations = new HashMap<>();
    private Map<UUID, String> laws = new HashMap<>();
    private Map<String, FactionBase> bases = new HashMap<>();
}
