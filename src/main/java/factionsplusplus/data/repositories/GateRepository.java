package factionsplusplus.data.repositories;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import factionsplusplus.data.beans.GateBean;
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

    // Load claimed chunks
    public void load() {
        try {
            this.gateStore.clear();
            //this.gateStore = this.getDAO().get();
            for (GateBean g : this.getDAO().get()) {
                System.out.println("Gate ID: "+g.getId());
                System.out.println("Trigger X: "+g.getTrigger_location().getX());
            }
        } catch(Exception e) {
            System.out.println("ERROR!");
            this.logger.log(String.format("Error loading gates: %s", e.getMessage()));
            e.printStackTrace();
        }
    }

    // Save a locked block
    public void create(Gate gate) {
        this.getDAO().create(gate.getUUID(), gate.getName(), gate.getFaction(), gate.getMaterial().toString(), gate.getWorld().getUID(), gate.isOpen(), gate.isVertical(), gate.getCoord1(), gate.getCoord2(), gate.getTrigger());
        this.gateStore.add(gate);
    }

    public GateDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(GateDao.class);
    }
}