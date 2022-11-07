package factionsplusplus.models;

import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.data.beans.FactionBean;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.models.interfaces.Feudal;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.utils.StringUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class Faction extends Nation implements Feudal, ForwardingAudience {
    private Map<String, ConfigurationFlag> flags = new ConcurrentHashMap<>();
    private Map<String, FactionBase> bases = new ConcurrentHashMap<>();
    private String prefix = null;
    private int bonusPower = 0;
    @ColumnName("should_autoclaim")
    private boolean autoclaim = false;
    private List<UUID> attemptedVassalizations = Collections.synchronizedList(new ArrayList<>());

    private final FactionRepository factionRepository;
    private final DataService dataService;
    private final BukkitAudiences adventure;
    private final ConfigService configService;

    // Constructor
    @AssistedInject
    public Faction(DataService dataService, @Named("adventure") BukkitAudiences adventure, ConfigService configService) {
        this.dataService = dataService;
        this.adventure = adventure;
        this.configService = configService;
        this.factionRepository = this.dataService.getFactionRepository();
    }

    @AssistedInject
    public Faction(
        @Assisted FactionBean bean,
        FactionRepository factionRepository,
        DataService dataService,
        ConfigService configService,
        @Named("adventure") BukkitAudiences adventure
    ) {
        this.uuid = bean.getId();
        this.name = bean.getName();
        this.description = bean.getDescription();
        this.prefix = bean.getPrefix();
        this.autoclaim = bean.isShouldAutoclaim();
        this.bonusPower = bean.getBonusPower();
        this.flags = bean.getFlags();
        this.members = bean.getMembers();
        this.relations = bean.getRelations();
        this.bases = bean.getBases();
        this.laws = bean.getLaws();
        this.factionRepository = factionRepository;
        this.dataService = dataService;
        this.adventure = adventure;
        this.configService = configService;
    }

    @AssistedInject
    public Faction(@Assisted String factionName, @Assisted Map<String, ConfigurationFlag> flags, DataService dataService, @Named("adventure") BukkitAudiences adventure, ConfigService configService) {
        this.name = factionName;
        this.flags = flags;
        this.prefix = factionName;
        this.dataService = dataService;
        this.adventure = adventure;
        this.configService = configService;
        this.factionRepository = this.dataService.getFactionRepository();
    }

    @AssistedInject
    public Faction(@Assisted String factionName, @Assisted UUID owner, @Assisted Map<String, ConfigurationFlag> flags, DataService dataService, @Named("adventure") BukkitAudiences adventure, ConfigService configService) {
        this.name = factionName;
        this.setOwner(owner);
        this.flags = flags;
        this.prefix = factionName;
        this.dataService = dataService;
        this.adventure = adventure;
        this.configService = configService;
        this.factionRepository = this.dataService.getFactionRepository();
    }

    /**
     * Set the faction flags. Should only be used for internal use.
     */
    public void setFlags(Map<String, ConfigurationFlag> flags) {
        this.flags = flags;
    }

    /*
     * Retrieves the factions UUID.
     *
     * @Deprecated Use getUUID()
     * @returns the factions UUID
     */
    public UUID getID() {
        return this.uuid;
    }

    // Flags
    public Map<String, ConfigurationFlag> getFlags() {
        return this.flags;
    }

    public ConfigurationFlag getFlag(String flagName) {
        return this.flags.get(flagName);
    }

    public String setFlag(String flagName, String flagValue) {
        if (! this.flags.containsKey(flagName)) return null;
        ConfigurationFlag flag = this.flags.get(flagName);
        String result = flag.set(flagValue);
        if (result == null) return null;
        this.factionRepository.persistFlag(this, flag);
        return result;
    }

    public Set<String> getFlagNames() {
        return this.flags.keySet();
    }

    // Bonus Power
    public int getBonusPower() {
        return this.bonusPower;
    }

    public void setBonusPower(int newAmount) {
        this.bonusPower = newAmount;
        this.persist();
    }

    // Overridden methods for persistence
    @Override
    public void setName(String name) {
        this.name = name;
        // Unset prefix if it's the current name
        if (this.prefix != null && this.prefix.equalsIgnoreCase(name)) this.prefix = name;
        this.persist();
    }
    @Override
    public void setDescription(String description) {
        this.description = description;
        this.persist();
    }

    public void upsertMember(UUID uuid, GroupRole role) {
        GroupMember member = this.members.get(uuid);
        if (member != null) member.setRole(role);
        else member = new GroupMember(uuid, role);
        this.members.put(uuid, member);
        this.factionRepository.persistMember(this.getUUID(), member);
    }

    public void clearMember(UUID uuid) {
        this.members.remove(uuid);
        this.factionRepository.deleteMember(this.getUUID(), uuid);
    }

    public void upsertRelation(UUID uuid, FactionRelationType type) {
        // avoid a pointless sql query
        FactionRelationType relation = this.relations.get(uuid);
        if (relation != null && relation == type) return;
        // persist
        this.updateRelation(uuid, type);
        this.factionRepository.persistRelation(this.getUUID(), uuid, type);        
    }

    public void updateRelation(UUID uuid, FactionRelationType type) {
        this.relations.put(uuid, type);
    }

    public void clearRelation(UUID uuid) {
        this.removeRelation(uuid);
        this.factionRepository.deleteRelation(this.getUUID(), uuid);
    }

    public void removeRelation(UUID uuid) {
        this.relations.remove(uuid);
    }

    // Laws
    public void addLaw(String lawText) {
        UUID lawUUID = UUID.randomUUID();
        this.laws.put(lawUUID, lawText);
        this.factionRepository.persistLaw(this.uuid, lawUUID, lawText);
    }

    @Override
    public boolean editLaw(int index, String text) {
        try {
            UUID foundUUID = (UUID)this.laws.keySet().toArray()[index];
            this.laws.put(foundUUID, text);
            this.factionRepository.persistLaw(this.uuid, foundUUID, text);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public boolean removeLaw(int index) {
        try {
            UUID foundUUID = (UUID)this.laws.keySet().toArray()[index];
            this.laws.remove(foundUUID);
            this.factionRepository.deleteLaw(foundUUID);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    // Prefix
    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
        this.persist();
    }

    // Faction Bases
    public Map<String, FactionBase> getBases() {
        return this.bases;
    }

    public boolean addBase(String name, Location location) {
        UUID baseUUID = UUID.randomUUID();
        try {
            FactionBase base = new FactionBase(baseUUID, name, this, location);
            this.bases.put(name, base);
            this.factionRepository.persistBase(base);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeBase(String name) {
        try {
            FactionBase base = this.bases.entrySet().stream()
                .filter(b -> b.getKey().toLowerCase().equals(name.toLowerCase()))
                .map(b -> b.getValue())
                .findFirst()
                .orElse(null);
            if (base == null) return false;
            this.bases.remove(base.getName());
            this.factionRepository.deleteBase(base);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public FactionBase getDefaultBase() {
        return this.bases.values().stream()
            .filter(b -> b.isFactionDefault())
            .findFirst()
            .orElse(null);
    }

    public FactionBase getBase(String name) {
        return this.bases.entrySet().stream()
            .filter(b -> b.getKey().toLowerCase().equals(name.toLowerCase()))
            .map(b -> b.getValue())
            .findFirst()
            .orElse(null);
    }

    public void renameBase(String oldName, String newName) {
        FactionBase base = this.getBase(oldName);
        if (base == null) return;
        this.bases.remove(base.getName());
        base.setName(newName);
        this.bases.put(newName, base);
    }

    public void persistBase(FactionBase base) {
        this.factionRepository.persistBase(base);
    }

    // Auto Claim
    public boolean toggleAutoClaim() {
        this.autoclaim = ! autoclaim;
        this.persist();
        return this.autoclaim;
    }
    
    public boolean shouldAutoClaim() {
        return this.autoclaim;
    }

    // Lieges
    public boolean isLiege() {
        return this.getVassals().size() > 0;
    }

    public UUID getLiege() {
        try {
            return this.relations.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == FactionRelationType.Liege)
                .map(entry -> entry.getKey())
                .findFirst()
                .orElse(null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void setLiege(UUID newLiege) {
        this.relations.put(newLiege, FactionRelationType.Liege);
    }

    public boolean hasLiege() {
        return this.getLiege() != null;
    }

    public boolean isLiege(UUID uuid) {
        FactionRelationType relation = this.relations.get(uuid);
        return relation == FactionRelationType.Liege;
    }

    public void unsetIfLiege(UUID uuid) {
        if (this.isLiege(uuid)) this.relations.remove(this.getLiege());
    }

    // Vassals
    public boolean isVassal(UUID uuid) {
        return this.relations.get(uuid) == FactionRelationType.Vassal;
    }

    public void addVassal(UUID uuid) {
        if (! this.isVassal(uuid)) this.relations.put(uuid, FactionRelationType.Vassal);
    }

    public void removeVassal(UUID uuid) {
        if (this.isVassal(uuid)) this.relations.remove(uuid);
    }

    public void clearVassals() {
        this.relations.entrySet()
            .removeIf(entry -> entry.getValue() == FactionRelationType.Vassal);
    }

    public int getNumVassals() {
        return this.getVassals().size();
    }

    public List<UUID> getVassals() {
        return this.relations.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == FactionRelationType.Vassal)
            .map(entry -> entry.getKey())
            .toList();
    }

    public void addAttemptedVassalization(UUID uuid) {
        if (! this.hasBeenOfferedVassalization(uuid)) this.attemptedVassalizations.add(uuid);
    }

    public boolean hasBeenOfferedVassalization(UUID uuid) {
        return this.attemptedVassalizations.contains(uuid);
    }

    public void removeAttemptedVassalization(UUID uuid) {
        if (this.hasBeenOfferedVassalization(uuid)) this.attemptedVassalizations.remove(uuid);
    }

    public String toString() {
        return this.name;
    }

    // Calculations

    // get max power without vassal contribution
    public double getMaximumCumulativePowerLevel() {
        return this.getMembers().keySet()
            .stream()
            .map(this.dataService::getPlayer)
            .mapToDouble(player -> player.getMaxPower())
            .sum();
    }

    public double calculateCumulativePowerLevelWithoutVassalContribution() {
        return this.getMembers().keySet()
            .stream()
            .map(this.dataService::getPlayer)
            .mapToDouble(player -> player.getPower())
            .sum();
    }

    public double calculateCumulativePowerLevelWithVassalContribution() {
        return this.getVassals()
            .stream()
            .map(this.dataService::getFaction)
            .mapToDouble(faction -> faction.getCumulativePowerLevel() * this.configService.getDouble("vassalContributionPercentageMultiplier"))
            .sum() + this.calculateCumulativePowerLevelWithoutVassalContribution();
    }

    public double getCumulativePowerLevel() {
        double withoutVassalContribution = this.calculateCumulativePowerLevelWithoutVassalContribution();
        double withVassalContribution = this.calculateCumulativePowerLevelWithVassalContribution();

        if (this.getVassals().size() == 0 || (withoutVassalContribution < (this.getMaximumCumulativePowerLevel() / 2))) {
            return withoutVassalContribution + this.getBonusPower();
        }
        return withVassalContribution + this.getBonusPower();
    }

    public UUID getTopLiege()
    {
        UUID liegeUUID = this.getLiege();
        if (liegeUUID == null) return null;
        Faction topLiege = null;
        do {
            topLiege = this.dataService.getFaction(liegeUUID);
            if (topLiege != null) liegeUUID = topLiege.getUUID();
        } while(topLiege != null);
        return liegeUUID;
    }

    public boolean isWeakened() {
        return this.calculateCumulativePowerLevelWithoutVassalContribution() < (this.getMaximumCumulativePowerLevel() / 2);
    }

    public int calculateMaxOfficers() {
        return 1 + (this.getMembers().size() / this.configService.getInt("officerPerMemberCount"));
    }


    // Tools
    public void persist() {
        this.factionRepository.persist(this);
    }

    public void message(ComponentLike message, MessageType type) {
        this.audience().sendMessage(message, type);
    }

    public void alert(ComponentLike message) {
        this.message(message, MessageType.SYSTEM);
    }

    public void alert(String localizationKey, Object... arguments) {
        this.alert(
            Component.translatable(localizationKey).color(NamedTextColor.YELLOW).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
        );
    }

    public Component generateFactionChatComponent(Faction faction, OfflinePlayer player, String message) {
        final TextColor factionChatColor = StringUtils.parseAsTextColor(this.configService.getString("factionChatColor"));
        Component chatComponent = Component.text(player.getName()+":").color(NamedTextColor.WHITE).append(Component.text(" "+message).color(factionChatColor));
        if (this.configService.getBoolean("showPrefixesInFactionChat")) {
            final TextColor prefixColor = StringUtils.parseAsTextColor(faction.getFlag("prefixColor").toString());
            chatComponent = Component.text(String.format("[%s] ", faction.getPrefix())).color(prefixColor).append(chatComponent);
        }
        return chatComponent;
    }

    public void sendToFactionChatAs(Faction faction, OfflinePlayer player, String message) {
        this.audience().sendMessage(this.generateFactionChatComponent(faction, player, message), MessageType.CHAT);
    }

    public void sendToFactionChatAs(OfflinePlayer player, String message) {
        this.audience().sendMessage(this.generateFactionChatComponent(this, player, message), MessageType.CHAT);
    }

    public Audience audience() {
        return this.adventure.filter(sender -> {
            if (sender instanceof Player) if (this.isMember((Player)sender)) return true;
            return false;
        });
    }

    @Override
    public Iterable<? extends Audience> audiences() {
        return this.members
            .keySet()
            .stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .map(this.adventure::player)
            .collect(Collectors.toSet());
    }
}