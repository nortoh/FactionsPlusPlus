package factionsplusplus.data.daos;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;

import factionsplusplus.data.beans.WorldBean;
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

    @SqlQuery("SELECT * FROM worlds")
    @RegisterFieldMapper(WorldBean.class)
    List<WorldBean> get();

    @SqlQuery("SELECT * FROM worlds WHERE id = ?")
    @RegisterFieldMapper(WorldBean.class)
    WorldBean get(UUID uuid);

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

    @SqlUpdate("""
        INSERT INTO world_flags (
            world_id,
            flag_name,
            value
        ) VALUES (
            :world,
            :flag,
            :value
        ) ON DUPLICATE KEY UPDATE
            value = :value
    """)
    void upsertFlag(@Bind("world") UUID worldID, @Bind("flag") String flagName, @Bind("value") String value);

    @SqlUpdate("DELETE FROM world_flags WHERE flag_name = :flag AND world_id = :world")   
    void deleteFlag(@Bind("world") UUID worldID, @Bind("flag") String flagName);

    default List<WorldBean> getWorlds() {
        List<WorldBean> worldMap = get();
        worldMap.stream().forEach(world -> world.setFlags(getFlags(world.getId())));
        return worldMap;
    }
}
