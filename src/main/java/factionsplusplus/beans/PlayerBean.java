package factionsplusplus.beans;

import java.util.UUID;

import org.jdbi.v3.core.mapper.Nested;

import factionsplusplus.models.PlayerStats;
import lombok.Data;

@Data
public class PlayerBean {
    private UUID id;
    @Nested
    private PlayerStats stats;
    private double power;
}
