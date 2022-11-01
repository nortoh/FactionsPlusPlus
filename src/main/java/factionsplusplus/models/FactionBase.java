package factionsplusplus.models;

import java.util.UUID;

import org.bukkit.Location;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import factionsplusplus.models.interfaces.Identifiable;

public class FactionBase implements Identifiable {
    @ColumnName("id")
    private UUID uuid;
    private String name;
    @ColumnName("faction_id")
    private UUID faction;
    @ColumnName("world_id")
    private UUID world;
    @Nested
    private LocationData location;
    @ColumnName("allow_allies")
    private boolean allowAllies = false;
    @ColumnName("allow_all_members")
    private boolean allowAllFactionMembers = true;
    @ColumnName("is_faction_default")
    private boolean isDefault = false;

    public FactionBase() { }
    
    public FactionBase(UUID uuid, String name, Faction faction, Location location) {
        this.uuid = uuid;
        this.name = name;
        this.faction = faction.getUUID();
        this.world = location.getWorld().getUID();
        this.location = new LocationData(location);
    }

    public String getName() {
        return this.name;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public UUID getFaction() {
        return this.faction;
    }

    public UUID getWorld() {
        return this.world;
    }

    public boolean isFactionDefault() {
        return this.isDefault;
    }

    public boolean shouldAllowAllies() {
        return this.allowAllies;
    }

    public LocationData getLocationData() {
        return this.location;
    }

    public boolean shouldAllowAllFactionMembers() {
        return this.allowAllFactionMembers;
    }

    public Location getBukkitLocation() {
        return this.location.getLocation();
    }
}
