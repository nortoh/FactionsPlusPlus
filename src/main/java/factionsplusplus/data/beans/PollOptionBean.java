package factionsplusplus.data.beans;

import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import lombok.Data;

@Data
public class PollOptionBean {
    private UUID id;
    @ColumnName("poll_id")
    private UUID pollUUID;
    private String text;
}
