package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;

import factionsplusplus.beans.WarBean;
import factionsplusplus.models.War;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface WarDao {
    @SqlUpdate("DELETE FROM faction_wars WHERE id = :getUUID")
    void delete(@BindMethods War war);

    @SqlUpdate("""
        INSERT INTO faction_wars (
            id,
            attacker_id,
            defender_id,
            reason,
            is_active
        ) VALUES (
            :getUUID,
            :getAttacker,
            :getDefender,
            :getReason,
            1
        ) ON DUPLICATE KEY UPDATE
            is_active = :isActive,
            ended_at = :getEndDate
    """)
    void upsert(@BindMethods War war);

    @SqlQuery("SELECT * FROM faction_wars")
    @RegisterFieldMapper(WarBean.class)
    List<WarBean> get();
}
