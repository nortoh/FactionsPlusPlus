package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.constants.FlagType;
import factionsplusplus.models.ConfigurationFlag;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;

public interface DefaultConfigurationFlagDao {
    @SqlUpdate("INSERT IGNORE INTO default_flags (name, type, expected_data_type, default_value) VALUES (:flagName, :type, :getRequiredType, :getDefaultValue)")
    void insert(@Bind("flagName") String flagName, @Bind("type") FlagType type, @BindMethods ConfigurationFlag flag);

    @SqlUpdate("DELETE FROM default_flags WHERE name = ? AND type = ?")
    void delete(String flagName, FlagType type);

    @SqlQuery("""
        SELECT 
            name,
            type,
            expected_data_type,
            default_value
        FROM default_flags
        WHERE type = ?
    """)
    @RegisterFieldMapper(ConfigurationFlag.class)
    @KeyColumn("name")
    Map<String, ConfigurationFlag> get(FlagType type);
}
