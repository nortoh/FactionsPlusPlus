package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;

import factionsplusplus.models.LockedBlock;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

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

    @SqlQuery("SELECT * FROM locked_blocks")
    @RegisterFieldMapper(LockedBlock.class)
    List<LockedBlock> get();
}
