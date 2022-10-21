package factionsplusplus.models;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

import com.google.gson.annotations.Expose;

public class AccessList {
    @Expose
    protected ArrayList<UUID> players = new ArrayList<>();
    @Expose
    protected boolean factionMembers = false;
    @Expose
    protected boolean allies = false;

    public List<UUID> getPlayers() {
        return this.players;
    }

    public void addPlayerToAccessList(UUID player) {
        if (! this.players.contains(player)) this.players.add(player);
    }

    public void removePlayerFromAccessList(UUID player) {
        if (this.players.contains(player)) this.players.remove(player);
    }

    public void addAlliesToAccessList() {
        this.allies = true;
    }

    public void removeAlliesFromAccessList() {
        this.allies = false;
    }

    public void addFactionMembersToAccessList() {
        this.factionMembers = true;
    }

    public void removeFactionMembersFromAccessList() {
        this.factionMembers = false;
    }
    
    public boolean playerOnAccessList(UUID player) {
        return this.players.contains(player);
    }

    public boolean alliesPermitted() {
        return this.allies;
    }

    public boolean factionMembersPermitted() {
        return this.factionMembers;
    }
}