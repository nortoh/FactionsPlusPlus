package factionsplusplus.data.daos;

import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;

import factionsplusplus.models.LocationData;
import factionsplusplus.models.Gate;
import factionsplusplus.data.beans.GateBean;
import factionsplusplus.data.mappers.GateMapper;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.List;

public interface GateDao {

    @SqlUpdate("""
        INSERT INTO faction_gates (
            id,
            name,
            faction_id,
            material,
            world_id,
            is_open,
            is_vertical,
            position_one_location,
            position_two_location,
            trigger_location
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """)
    void create(UUID id, String name, UUID faction, String material, UUID world, boolean open, boolean vertical, @Json LocationData coord1, @Json LocationData coord2, @Json LocationData trigger);

    @SqlUpdate("DELETE FROM faction_gates WHERE id = ?")
    void delete(UUID id);

    @SqlUpdate("UPDATE faction_gates SET name = :getName WHERE id = :getUUID")
    void update(@BindMethods Gate g);

    @SqlQuery("SELECT * FROM faction_gates")
    @RegisterRowMapper(GateMapper.class)
    List<GateBean> get();

    default ConcurrentMap<UUID, Gate> getAll() {
        return get().stream().map(g -> new Gate(g)).collect(Collectors.toConcurrentMap(g -> g.getUUID(), g -> g));
    }
}