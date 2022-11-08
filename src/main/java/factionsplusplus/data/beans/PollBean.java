package factionsplusplus.data.beans;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import lombok.Data;

@Data
public class PollBean {
    private UUID id;
    @ColumnName("faction_id")
    private UUID faction;
    @ColumnName("choiced_allowed")
    private int choicesAllowed;
    private String question;
    @ColumnName("created_at")
    private ZonedDateTime createdAt;
}
