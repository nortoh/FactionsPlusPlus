package factionsplusplus.data.beans;

import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import lombok.Data;

@Data
public class PollVoteBean {
    private UUID id;
    @ColumnName("player_id")
    private UUID playerUUID;
    @ColumnName("poll_id")
    private UUID pollUUID;
    @ColumnName("option_id")
    private UUID pollOptionUUID;
}
