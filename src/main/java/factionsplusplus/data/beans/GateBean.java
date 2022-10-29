package factionsplusplus.data.beans;
import java.util.UUID;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.json.Json;

import lombok.Data;

@Data
public class GateBean {
    private UUID id;
    @Nested
    @Json
    private LocationDataBean position_one_location;
    @Nested
    @Json
    private LocationDataBean position_two_location;
    @Nested
    @Json
    private LocationDataBean trigger_location;
}
