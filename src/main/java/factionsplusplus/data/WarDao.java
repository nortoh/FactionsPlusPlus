package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.War;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface WarDao {
    @SqlUpdate("INSERT IGNORE INTO claimed_chunks (attacker_id, defender_id, reason, active) VALUES (:getAttacker, :getDefender, :getReason, 1)")
    void insert(@BindMethods War war);

    @SqlQuery("SELECT * FROM faction_wars")
    @RegisterFieldMapper(War.class)
    List<War> get();
}
