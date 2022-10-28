package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.beans.PlayerBean;
import factionsplusplus.models.PlayerRecord;

import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlayerDao {
    @SqlUpdate("INSERT IGNORE INTO players (id, power) VALUES (:getPlayerUUID, :getPower)")
    void insert(@BindMethods PlayerRecord player);

    @SqlUpdate("INSERT IGNORE INTO players (id, power) VALUES (?, ?)")
    void insert(UUID uuid, double initialPower);

    @SqlUpdate("""
        UPDATE players SET
            power = :getPower,
            is_admin_bypassing = :isAdminBypassing,
            login_count = :getLogins,
            last_logout = :getLastLogout,
            offline_power_lost = :getPowerLost
        WHERE
            id = :getUUID      
    """)
    void update(@BindMethods PlayerRecord player);

    @SqlBatch("""
        UPDATE players SET
            power = :getPower,
            is_admin_bypassing = :isAdminBypassing,
            login_count = :getLogins,
            last_logout = :getLastLogout,
            offline_power_lost = :getPowerLost
        WHERE
            id = :getUUID       
    """)
    void update(@BindMethods Collection<PlayerRecord> players);

    @SqlUpdate("DELETE FROM players WHERE id = ?")
    void delete(UUID uuid);
    
    @SqlQuery("SELECT * FROM players")
    @KeyColumn("id")
    @RegisterFieldMapper(PlayerBean.class)
    List<PlayerBean> get();

    @SqlQuery("SELECT * FROM players WHERE id = ?")
    @RegisterFieldMapper(PlayerRecord.class)
    PlayerRecord get(UUID uuid);
}
