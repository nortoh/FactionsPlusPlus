package factionsplusplus.data.daos;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.Poll;
import factionsplusplus.models.PollOption;
import factionsplusplus.data.beans.PollOptionBean;

import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;
import java.util.Collection;
import java.util.List;

public interface PollOptionDao {
    @SqlUpdate("""
        INSERT IGNORE INTO poll_options (id, poll_id, text)
            VALUES (:getUUID, :getPollUUID, :getText)
        """)
    void insert(@BindMethods PollOption pollOption);

    @SqlUpdate("""
        INSERT IGNORE INTO poll_options (poll_id, text)
            VALUES (?, ?)
    """)
    void insert(UUID pollUUID, String text);

    @SqlUpdate("""
        INSERT IGNORE INTO poll_options (poll_id, text)
            VALUES (:getPollUUID, ?)
    """)
    void insert(@BindMethods Poll poll, String text);

    @SqlBatch("""
        UPDATE poll_options SET
            text = :getText
        WHERE
            poll_id = :getPollUUID
    """)
    void update(@BindMethods Collection<PollOption> pollOptions);

    @SqlUpdate("""
        UPDATE polls SET
            question = :getQuestion
        WHERE
            id = :getUUID
    """)
    void update(@BindMethods PollOption pollOption);

    @SqlUpdate("DELETE FROM poll_options WHERE id = :getUUID")
    void delete(@BindMethods PollOption pollOption);

    @SqlUpdate("DELETE FROM poll_options WHERE id = ?")
    void delete(UUID pollOptionUUID);

    @SqlUpdate("DELETE FROM poll_options WHERE poll_id = :getPollUUID")
    void deleteAllFromPoll(@BindMethods Poll poll);

    @SqlQuery("SELECT * FROM poll_options")
    @RegisterFieldMapper(PollOptionBean.class)
    List<PollOptionBean> get();

    @SqlQuery("SELECT * FROM poll_options WHERE poll_id = :getPollUUID")
    @RegisterFieldMapper(PollOptionBean.class)
    List<PollOptionBean> getForPoll(Poll poll);

    @SqlQuery("SELECT * FROM poll_options WHERE poll_id = :getPolUUID")
    @RegisterFieldMapper(PollOptionBean.class)
    PollOptionBean get(Poll poll);

    @SqlQuery("SELECT * FROM poll_options WHERE poll_id = ?")
    @RegisterFieldMapper(PollOptionBean.class)
    List<PollOptionBean> getForPoll(UUID pollUUID);

    default PollOption createNewPollOption(PollOption pollOption) {
        insert(pollOption);
        return pollOption;
    }
}
