package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.World;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;
import java.util.UUID;
import java.util.List;

public interface WorldDao {
    @SqlUpdate("INSERT IGNORE INTO worlds (id) VALUES (:getUUID)")
    void insert(@BindMethods World world);

    @SqlUpdate("INSERT IGNORE INTO worlds (id) VALUES (?)")
    void insert(UUID uuid);

    @SqlQuery("SELECT BIN_TO_UUID(id) AS id FROM worlds")
    @RegisterFieldMapper(World.class)
    List<World> get();

    @SqlQuery("SELECT BIN_TO_UUID(id) AS id FROM worlds WHERE id = ?")
    @RegisterFieldMapper(World.class)
    World get(UUID uuid);

    @SqlQuery("""
        SELECT
            name,
            description,
            expected_data_type,
            default_value,
            w.value value
        FROM default_flags
        LEFT JOIN world_flags w
            ON w.flag_name = name AND w.world_id = ?
        WHERE type = 2
    """)
    @KeyColumn("name")
    @RegisterFieldMapper(ConfigurationFlag.class)
    Map<String, ConfigurationFlag> getFlags(UUID uuid);
}
