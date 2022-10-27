package factionsplusplus.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import factionsplusplus.models.War;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class WarRepository {
    private List<War> warStore = new ArrayList<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;

    @Inject
    public WarRepository(Logger logger, DataProviderService dataProviderService) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
    }

    // Load wars
    public void load() {
        try {
            this.warStore.clear();
        } catch(Exception e) {
            this.logger.error(String.format("Error loading wars: %s", e.getMessage()));
        }
        /*try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.warStore = gson.fromJson(reader, WarRepository.JSON_TYPE);
        } catch (FileNotFoundException ignored) {
            this.logger.error(String.format("File %s not found", this.dataPath), ignored);
        }*/
    }

    // Save a war
    public void create(War war) {
        this.warStore.add(war);
    }

    // Delete a war
    public void delete(War war) {
        this.warStore.remove(war);
    }

    // Retrieve a list of wars a faction is involved in
    public List<War> getAllForFaction(UUID factionUUID) {
        ArrayList<War> results = new ArrayList<>();
        for (War war : this.warStore) {
            if (war.getAttacker().equals(factionUUID) || war.getDefender().equals(factionUUID)) results.add(war);
        }
        return results;
    }

    // Retrieve an active war between two factions
    public War getActiveWarsBetween(UUID factionOneUUID, UUID factionTwoUUID) {
        for (War war : this.warStore) {
            if (
                (factionOneUUID.equals(war.getAttacker()) || factionOneUUID.equals(war.getDefender())) &&
                (factionTwoUUID.equals(war.getAttacker()) || factionTwoUUID.equals(war.getDefender())) &&
                war.isActive()
            ) {
                return war;
            }
        }
        return null;
    }

    // Retrieve all wars
    public List<War> all() {
        return this.warStore;
    }

    // Write to file
    public void persist() {
        /*File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(this.warStore, WarRepository.JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            this.logger.error(String.format("Failed to write to %s", this.dataPath), e);
        }*/
    }
}