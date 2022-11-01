package factionsplusplus.data.beans;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import lombok.Data;

@Data
public class PlayerBean {
    private UUID id;
    @ColumnName("login_count")
    private int loginCount;
    @ColumnName("last_logout")
    private ZonedDateTime lastLogout;
    @ColumnName("offline_power_lost")
    private int offlinePowerLost;
    private double power;
    @ColumnName("is_admin_bypassing")
    private boolean adminBypassing;
}
