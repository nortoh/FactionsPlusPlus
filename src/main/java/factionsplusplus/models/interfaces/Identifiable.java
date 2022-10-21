package factionsplusplus.models.interfaces;

import java.util.UUID;

public interface Identifiable {    
    /*
     * Retrieves the objects UUID.
     * 
     * @return The UUID that represents this object.
     */
    UUID getUUID();
}