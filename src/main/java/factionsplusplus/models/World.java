package factionsplusplus.models;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

import factionsplusplus.models.interfaces.Identifiable;

import com.google.gson.annotations.Expose;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class World implements Identifiable {
    @Expose
    @ColumnName("id")
    private UUID uuid;
    @Expose
    private Map<String, ConfigurationFlag> flags = new HashMap<>();

    public World() { }
    
    public World(UUID uuid) {
        this.uuid = uuid;
    }

    public void setFlags(Map<String, ConfigurationFlag> flags) {
        this.flags = flags;
    }

    public UUID getUUID() {
        return this.uuid;
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
}