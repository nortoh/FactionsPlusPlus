package factionsplusplus.data.daos;

import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import factionsplusplus.models.LocationData;
import factionsplusplus.data.beans.GateBean;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;
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

    @SqlQuery("SELECT * FROM faction_gates")
    @RegisterFieldMapper(GateBean.class)
    List<GateBean> get();
}