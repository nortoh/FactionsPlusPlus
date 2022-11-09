package factionsplusplus.data.beans;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private ConcurrentMap<String, ConfigurationFlag> flags = new ConcurrentHashMap<>();
    private ConcurrentMap<UUID, GroupMember> members = new ConcurrentHashMap<>();
    private ConcurrentMap<UUID, FactionRelationType> relations = new ConcurrentHashMap<>();
    private ConcurrentMap<UUID, String> laws = new ConcurrentHashMap<>();
    private ConcurrentMap<String, FactionBase> bases = new ConcurrentHashMap<>();
}
