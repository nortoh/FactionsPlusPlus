package factionsplusplus.data.daos;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;

import factionsplusplus.models.Faction;
import factionsplusplus.models.Poll;
import factionsplusplus.data.beans.PollBean;

import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

public interface PollDao {
    @SqlUpdate("INSERT IGNORE INTO polls (id, faction_id, question) VALUES (:getUUID, :getFactionUUID, :getQuestion)")
    void insert(@BindMethods Poll poll);

    @SqlUpdate("INSERT IGNORE INTO polls (id, question) VALUES (?, ?)")
    void insert(UUID uuid, String question);

    @SqlBatch("""
        UPDATE polls SET
            question = :getQuestion
        WHERE
            id = :getUUID
    """)
    void update(@BindMethods Collection<Poll> polls);

    @SqlUpdate("""
        UPDATE polls SET
            question = :getQuestion
        WHERE
            id = :getUUID
    """)
    void update(@BindMethods Poll poll);

    @SqlUpdate("DELETE FROM polls WHERE id = :getUUID")
    void delete(@BindMethods Poll polls);

    @SqlUpdate("DELETE FROM polls WHERE id = ?")
    void delete(UUID poll);

    @SqlQuery("SELECT * FROM polls")
    @RegisterFieldMapper(PollBean.class)
    List<PollBean> get();

    @SqlQuery("""
        SELECT
            *
        FROM polls
        WHERE faction_id = ?
    """)
    @KeyColumn("id")
    @RegisterFieldMapper(Poll.class)
    Map<UUID, Poll> getFactionPolls(UUID factionUUID);

    @SqlQuery("""
        SELECT
            *
        FROM polls
        WHERE faction_id = :getUUID
    """)
    @KeyColumn("id")
    @RegisterFieldMapper(Poll.class)
    Map<UUID, Poll> getFactionPolls(@BindMethods Faction faction);

    default Poll createNewPoll(Poll poll) {
        insert(poll);
        // insert options
        return poll;
    }
}
