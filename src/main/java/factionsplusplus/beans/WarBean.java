package factionsplusplus.beans;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import lombok.Data;

@Data
public class WarBean {
    private UUID id;
    private UUID attacker;
    private UUID defender;
    private String reason;
    @ColumnName("is_active")
    private boolean active;
    @ColumnName("started_at")
    private ZonedDateTime startedAt;
    @ColumnName("ended_at")
    private ZonedDateTime endedAt;
}
