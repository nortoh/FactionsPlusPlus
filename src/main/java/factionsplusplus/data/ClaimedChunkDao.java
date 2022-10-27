package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.Faction;
import factionsplusplus.models.GroupMember;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.ConfigurationFlag;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;
import java.util.UUID;
import java.util.List;

public interface ClaimedChunkDao {
    @SqlUpdate("INSERT IGNORE INTO claimed_chunks (faction_id, world_id, x_position, z_position) VALUES (:getHolder, :getWorldUUID, :getX, :getZ)")
    void insert(@BindMethods ClaimedChunk chunk);

    @SqlUpdate("DELETE FROM claimed_chunks WHERE faction_id = :getHolder AND world_id = :getWorldUUID AND x_position = :getX AND z_position = :getZ")
    void delete(@BindMethods ClaimedChunk chunk);

    @SqlQuery("""
        SELECT 
            faction_id,
            world_id,
            x_position,
            z_position
        FROM claimed_chunks
    """)
    @RegisterFieldMapper(ClaimedChunk.class)
    List<ClaimedChunk> get();
}
