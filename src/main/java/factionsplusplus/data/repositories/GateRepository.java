package factionsplusplus.data.repositories;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import java.util.List;
import java.util.UUID;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;

import factionsplusplus.data.daos.GateDao;
import factionsplusplus.models.Gate;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class GateRepository {
    private List<Gate> gateStore = Collections.synchronizedList(new ArrayList<>());
    private final Logger logger;
    private final DataProviderService dataProviderService;

    @Inject
    public GateRepository(Logger logger, DataProviderService dataProviderService) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
    }

    // Load gates
    public void load() {
        try {
            this.gateStore.clear();
            this.gateStore = this.getDAO().getAll();
        } catch(Exception e) {
            this.logger.log(String.format("Error loading gates: %s", e.getMessage()));
        }
    }

    // Save a gate
    public void create(Gate gate) {
        this.getDAO().create(gate.getUUID(), gate.getName(), gate.getFaction(), gate.getMaterial().toString(), gate.getWorld().getUID(), gate.isOpen(), gate.isVertical(), gate.getCoord1(), gate.getCoord2(), gate.getTrigger());
        this.gateStore.add(gate);
    }

    // Delete a gate
    public void delete(Gate gate) {
        this.getDAO().delete(gate.getUUID());
        this.gateStore.remove(gate);
    }

    // Persist a gate
    public void persist(Gate gate) {
        this.getDAO().update(gate);
    }

    // Get factions gates
    public List<Gate> allForFaction(UUID factionUUID) {
        return this.gateStore.stream()
            .filter(gate -> gate.getFaction().equals(factionUUID))
            .toList();
    }

    public boolean isGateTriggerBlock(Block block) {
        return this.gateStore.stream()
            .filter(g -> g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
            g.getTrigger().getWorld().equals(block.getWorld().getUID()))
            .count() > 0;
    }

    public List<Gate> getGatesForTriggerBlock(Block block) {
        return this.gateStore.stream()
            .filter(g -> g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
            g.getTrigger().getWorld().equals(block.getWorld().getUID()))
            .toList();
    }

    public Gate getGateForBlock(Block block) {
        return this.gateStore.stream()
            .filter(gate -> gate.hasBlock(block))
            .findFirst()
            .orElse(null);
    }

    public GateDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(GateDao.class);
    }
}