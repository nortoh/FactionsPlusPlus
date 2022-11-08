package factionsplusplus.data.daos;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionBase;
import factionsplusplus.models.GroupMember;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.data.beans.FactionBean;
import factionsplusplus.models.ConfigurationFlag;

import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface FactionDao {
    // FACTIONS
    @SqlUpdate("INSERT IGNORE INTO factions (id, name, prefix) VALUES (:getUUID, :getName, :getPrefix)")
    void insert(@BindMethods Faction faction);

    @SqlUpdate("INSERT IGNORE INTO factions (id, name, prefix) VALUES (:uuid, :name, :name)")
    void insert(@Bind("uuid") UUID uuid, @Bind("name") String name);

    @SqlBatch("""
        UPDATE factions SET
            name = :getName,
            prefix = :getPrefix,
            description = :getDescription,
            bonus_power = :getBonusPower,
            should_autoclaim = :shouldAutoClaim
        WHERE
            id = :getUUID        
    """)
    void update(@BindMethods Collection<Faction> factions);

    @SqlUpdate("""
        UPDATE factions SET
            name = :getName,
            prefix = :getPrefix,
            description = :getDescription,
            bonus_power = :getBonusPower,
            should_autoclaim = :shouldAutoClaim
        WHERE
            id = :getUUID        
    """)
    void update(@BindMethods Faction faction);

    @SqlUpdate("DELETE FROM factions WHERE id = :getUUID")
    void delete(@BindMethods Faction faction);

    @SqlUpdate("DELETE FROM factions WHERE id = ?")
    void delete(UUID faction);

    @SqlQuery("SELECT * FROM factions")
    @RegisterFieldMapper(FactionBean.class)
    List<FactionBean> get();

    // MEMBERS
    @SqlUpdate("INSERT IGNORE INTO faction_members (faction_id, player_id, role) VALUES (?, ?, ?)")
    void insertMember(UUID faction, UUID player, int role);

    @SqlUpdate("DELETE FROM faction_members WHERE player_id = ? AND faction_id = ?")
    void deleteMember(UUID faction, UUID player);

    @SqlUpdate("UPDATE faction_members SET role = :getRole WHERE player_id = :getUUID")
    void update(@BindMethods GroupMember member);

    @SqlUpdate("INSERT INTO faction_members (faction_id, player_id, role) VALUES (:faction, :player, :role) ON DUPLICATE KEY UPDATE role = :role")
    void upsert(@Bind("faction") UUID faction, @Bind("player") UUID player, @Bind("role") int role);

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

    // RELATIONS

    @SqlUpdate("INSERT INTO faction_relations (source_faction, target_faction, type, updated_at) VALUES (:source, :target, :type, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE type = :type, updated_at = CURRENT_TIMESTAMP")
    void upsertRelation(@Bind("source") UUID source, @Bind("target") UUID target, @Bind("type") FactionRelationType type);

    @SqlUpdate("DELETE FROM faction_relations WHERE source_faction = ? AND target_faction = ?")
    void deleteRelation(UUID source, UUID target);

    @SqlQuery("""
        SELECT
            target_faction,
            type
        FROM faction_relations
        WHERE source_faction = ?     
    """)
    @KeyColumn("target_faction")
    @ValueColumn("type")
    Map<UUID, FactionRelationType> getRelations(UUID uuid);

    // INVITES
    @SqlUpdate("INSERT IGNORE INTO faction_invites (faction_id, player_id) VALUES (?, ?)")
    void insertInvite(UUID faction, UUID player);

    @SqlUpdate("DELETE FROM faction_invites WHERE faction_id = ? AND player_id = ?")
    void deleteInvite(UUID faction, UUID player);

    @SqlQuery("SELECT COUNT(1) FROM faction_invites WHERE faction_id = ? AND player_id = ?")
    int getInvite(UUID faction, UUID player);

    // CONFIGURATION FLAGS
    @SqlQuery("""
        SELECT
            name,
            description,
            expected_data_type,
            default_value,
            `value`
        FROM default_flags
        LEFT JOIN faction_flags
            ON flag_name = name AND faction_id = ?
        WHERE type = 1
    """)
    @KeyColumn("name")
    @RegisterFieldMapper(ConfigurationFlag.class)
    Map<String, ConfigurationFlag> getFlags(UUID uuid);

    @SqlUpdate("""
        INSERT INTO faction_flags (
            faction_id,
            flag_name,
            `value`
        ) VALUES (
            :faction,
            :flag,
            :value
        ) ON DUPLICATE KEY UPDATE
            `value` = :value
    """)
    void upsertFlag(@Bind("faction") UUID factionID, @Bind("flag") String flagName, @Bind("value") String value);

    @SqlUpdate("DELETE FROM faction_flags WHERE flag_name = :flag AND faction_id = :faction")   
    void deleteFlag(@Bind("faction") UUID factionID, @Bind("flag") String flagName);

    // LAWS
    @SqlUpdate("""
        INSERT INTO faction_laws (
            id,
            faction_id,
            text
        ) VALUES (
            :id,
            :faction,
            :law
        ) ON DUPLICATE KEY UPDATE
            text = :law
    """)
    void upsertLaw(@Bind("faction") UUID factionID, @Bind("id") UUID lawID, @Bind("law") String text);

    @SqlUpdate("DELETE FROM faction_laws WHERE id = ?")
    void deleteLaw(UUID lawID);

    @SqlQuery("""
        SELECT
            id,
            text
        FROM faction_laws
        WHERE faction_id = ?     
    """)
    @KeyColumn("id")
    @ValueColumn("text")
    Map<UUID, String> getLaws(UUID uuid);

    // BASES

    @SqlQuery("SELECT * FROM faction_bases WHERE faction_id = ?")
    @KeyColumn("name")
    @RegisterFieldMapper(FactionBase.class)
    Map<String, FactionBase> getBases(UUID uuid);

    @SqlUpdate("""
        INSERT INTO faction_bases (
            id,
            faction_id,
            world_id,
            x_position,
            y_position,
            z_position,
            name
        ) VALUES (
            :getUUID,
            :getFaction,
            :getWorld,
            :getLocationData.getX,
            :getLocationData.getY,
            :getLocationData.getZ,
            :getName
        ) ON DUPLICATE KEY UPDATE
            allow_allies = :shouldAllowAllies,
            allow_all_members = :shouldAllowAllFactionMembers,
            is_faction_default = :isFactionDefault,
            name = :getName
    """)
    void upsertBase(@BindMethods FactionBase base);

    @SqlUpdate("""
        INSERT INTO faction_bases (
            id,
            faction_id,
            world_id,
            x_position,
            y_position,
            z_position,
            name
        ) VALUES (
            :getUUID,
            :getFaction,
            :getWorld,
            :getLocationData.getX,
            :getLocationData.getY,
            :getLocationData.getZ,
            :getName
        )
    """)
    void insertBase(@BindMethods FactionBase base);

    @SqlUpdate("""
        UPDATE faction_bases SET
            allow_allies = :shouldAllowAllies,
            allow_all_members = :shouldAllowAllFactionMembers,
            is_faction_default = :isFactionDefault,
            name = :getName
        WHERE
            id = :getUUID    
    """)
    void updateBase(@BindMethods FactionBase base);

    @SqlUpdate("DELETE FROM faction_bases WHERE id = :getUUID")
    void deleteBase(@BindMethods FactionBase base);

    default List<FactionBean> getFactions() {
        List<FactionBean> results = new ArrayList<>();
        List<FactionBean> factionMap = get();
        factionMap.stream().forEach(faction -> {
            try {
                faction.setFlags(getFlags(faction.getId()));
                faction.setMembers(getMembers(faction.getId()));
                faction.setRelations(getRelations(faction.getId()));
                faction.setLaws(getLaws(faction.getId()));
                faction.setBases(getBases(faction.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            results.add(faction);
        });
        return results;
    }

    default Faction createNewFaction(Faction faction) {
        insert(faction);
        faction.upsertMember(faction.getOwner().getUUID(), GroupRole.Owner);
        return faction;
    }
}
