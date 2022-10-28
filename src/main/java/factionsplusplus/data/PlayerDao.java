package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.PlayerRecord;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;
import java.util.Map;

public interface PlayerDao {
    @SqlUpdate("INSERT IGNORE INTO players (id, power) VALUES (:getPlayerUUID, :getPower)")
    void insert(@BindMethods PlayerRecord player);

    @SqlUpdate("INSERT IGNORE INTO players (id, power) VALUES (?, ?)")
    void insert(UUID uuid, double initialPower);

    @SqlUpdate("DELETE FROM players WHERE id = ?")
    void delete(UUID uuid);
    
    @SqlQuery("SELECT * FROM players")
    @KeyColumn("id")
    @RegisterFieldMapper(PlayerRecord.class)
    Map<UUID, PlayerRecord> get();

    @SqlQuery("SELECT * FROM players WHERE id = ?")
    @RegisterFieldMapper(PlayerRecord.class)
    PlayerRecord get(UUID uuid);
}
