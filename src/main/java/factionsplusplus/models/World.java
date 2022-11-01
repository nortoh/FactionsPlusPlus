package factionsplusplus.models;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.bukkit.Bukkit;

import factionsplusplus.data.beans.WorldBean;
import factionsplusplus.data.repositories.WorldRepository;
import factionsplusplus.models.interfaces.Identifiable;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class World implements Identifiable {
    @ColumnName("id")
    private UUID uuid;
    private Map<String, ConfigurationFlag> flags = new ConcurrentHashMap<>();
    private final WorldRepository worldRepository;

    @AssistedInject
    public World(WorldRepository worldRepository) {
        this.worldRepository = worldRepository;
     }
    
    @AssistedInject
    public World(@Assisted UUID uuid, WorldRepository worldRepository) {
        this.uuid = uuid;
        this.worldRepository = worldRepository;
    }

    @AssistedInject
    public World(@Assisted WorldBean bean, WorldRepository worldRepository) {
        this.uuid = bean.getId();
        this.flags = bean.getFlags();
        this.worldRepository = worldRepository;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public org.bukkit.World getWorld() {
        return Bukkit.getWorld(this.uuid);
    }

    public Map<String, ConfigurationFlag> getFlags() {
        return this.flags;
    }

    public ConfigurationFlag getFlag(String flagName) {
        return this.flags.get(flagName);
    }

    public void setFlags(Map<String, ConfigurationFlag> flags) {
        this.flags = flags;
    }

    public String setFlag(String flagName, String flagValue) {
        if (! this.flags.containsKey(flagName)) return null;
        ConfigurationFlag flag = this.flags.get(flagName);
        String result = flag.set(flagValue);
        if (result == null) return null;
        this.worldRepository.persistFlag(this, flag);
        return result;
    }
}