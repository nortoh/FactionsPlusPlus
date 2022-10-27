package factionsplusplus.beans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.GroupMember;

import lombok.Data;

@Data
public class FactionBean {
    private UUID id;
    private String name;
    private String prefix;
    private String description;
    @ColumnName("bonus_power")
    private int bonusPower;
    @ColumnName("should_autoclaim")
    private boolean shouldAutoclaim;
    private Map<String, ConfigurationFlag> flags = new HashMap<>();
    private Map<UUID, GroupMember> members = new HashMap<>();
    private Map<UUID, FactionRelationType> relations = new HashMap<>();
}
