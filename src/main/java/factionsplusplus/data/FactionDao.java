package factionsplusplus.data;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.Faction;
import factionsplusplus.models.GroupMember;
import factionsplusplus.beans.FactionBean;
import factionsplusplus.models.ConfigurationFlag;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface FactionDao {
    @SqlUpdate("INSERT IGNORE INTO factions (id, name) VALUES (:getUUID, :getName)")
    void insert(@BindMethods Faction faction);

    @SqlUpdate("INSERT IGNORE INTO factions (id, name) VALUES (?, ?)")
    void insert(UUID uuid, String name);

    @SqlUpdate("INSERT IGNORE INTO faction_members (faction_id, player_id, role) VALUES (?, ?, ?)")
    void insertMember(UUID faction, UUID player, int role);

    @SqlUpdate("DELETE FROM faction_members WHERE player_id = ? AND faction_id = ?")
    void deleteMember(UUID player, UUID faction);

    @SqlUpdate("DELETE FROM factions WHERE id = :getUUID")
    void delete(@BindMethods Faction faction);

    @SqlUpdate("DELETE FROM factions WHERE id = ?")
    void delete(UUID faction);

    @SqlUpdate("UPDATE faction_members SET role = :getRole WHERE player_id = :getUUID")
    void update(@BindMethods GroupMember member);

    @SqlUpdate("INSERT INTO faction_members (faction_id, player_id, role) VALUES (:faction, :player, :role) ON DUPLICATE KEY UPDATE role = :role")
    void upsert(@Bind("faction") UUID faction, @Bind("player") UUID player, @Bind("role") int role);

    @SqlUpdate("INSERT IGNORE INTO faction_invites (faction_id, player_id) VALUES (?, ?)")
    void insertInvite(UUID faction, UUID player);

    @SqlUpdate("DELETE FROM faction_invites WHERE faction_id = ? AND player_id = ?")
    void deleteInvite(UUID faction, UUID player);

    @SqlQuery("SELECT COUNT(1) FROM faction_invites WHERE faction_id = ? AND player_id = ?")
    int getInvite(UUID faction, UUID player);
    
    @SqlQuery("SELECT * FROM factions")
    @RegisterFieldMapper(FactionBean.class)
    List<FactionBean> get();

    @SqlQuery("""
        SELECT
            name,
            description,
            expected_data_type,
            default_value,
            f.value value
        FROM default_flags
        LEFT JOIN faction_flags f
            ON f.flag_name = name AND f.faction_id = ?
        WHERE type = 1
    """)
    @KeyColumn("name")
    @RegisterFieldMapper(ConfigurationFlag.class)
    Map<String, ConfigurationFlag> getFlags(UUID uuid);

    @SqlQuery("""
        SELECT
            player_id id,
            role
        FROM faction_members
        WHERE faction_id = ?
    """)
    @KeyColumn("id")
    @RegisterFieldMapper(GroupMember.class)
    Map<UUID, GroupMember> getMembers(UUID uuid);

    default List<FactionBean> getFactions() {
        List<FactionBean> results = new ArrayList<>();
        List<FactionBean> factionMap = get();
        factionMap.stream().forEach(faction -> {
            faction.setFlags(getFlags(faction.getId()));
            faction.setMembers(getMembers(faction.getId()));
            results.add(faction);
        });
        return results;
    }

    default Faction createNewFaction(Faction faction) {
        insert(faction);
        faction.initialize();
        faction.setFlags(getFlags(faction.getUUID()));
        faction.setMembers(getMembers(faction.getUUID()));
        return faction;
    }
}
