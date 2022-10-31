package factionsplusplus.data.daos;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;

import factionsplusplus.models.LockedBlock;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface LockedBlockDao {
    @SqlUpdate("""
        INSERT IGNORE INTO locked_blocks (
            id,
            world_id,
            x_position,
            y_position,
            z_position,
            player_id
        ) VALUES (
            :getUUID,
            :getWorld,
            :getX,
            :getY,
            :getZ,
            :getOwner
        )
    """)
    void insert(@BindMethods LockedBlock block);

    @SqlUpdate("""
        UPDATE locked_blocks SET
            allow_allies = :shouldAllowAllies,
            allow_faction_members = :shouldAllowFactionMembers
        WHERE
            id = :getUUID        
    """)
    void update(@BindMethods LockedBlock lock);

    @SqlUpdate("""
        DELETE FROM locked_blocks WHERE id = :getUUID     
    """)
    void delete(@BindMethods LockedBlock lock);

    @SqlUpdate("""
        INSERT IGNORE INTO locked_block_access_list (
            locked_block_id,
            player_id
        ) VALUES (
            ?,
            ?
        )
    """)
    void insertPlayerAccess(UUID lockId, UUID playerId);

    @SqlUpdate("REMOVE FROM locked_block_access_list WHERE player_id = :player AND locked_block_id = :lock")
    void removePlayerAccess(@Bind("lock") UUID lock, @Bind("player") UUID player);

    @SqlQuery("SELECT * FROM locked_blocks")
    @RegisterFieldMapper(LockedBlock.class)
    List<LockedBlock> get();

    @SqlQuery("SELECT player_id FROM locked_block_access_list WHERE locked_block_id = ?")
    List<UUID> getAccessList(UUID lockUUID);

    default Map<UUID, LockedBlock> getAll() {
        List<LockedBlock> results = get();
        results.stream()
            .forEach(lock -> lock.setAccessList(getAccessList(lock.getUUID())));
        
        return results.stream().collect(Collectors.toMap(l -> l.getUUID(), l -> l));
    }

    default void create(LockedBlock block) {
        insert(block);
        insertPlayerAccess(block.getUUID(), block.getOwner());
    }
}
