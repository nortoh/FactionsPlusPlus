package factionsplusplus.data.beans;
import java.util.UUID;
import org.bukkit.Material;
import lombok.Data;

@Data
public class GateBean {
    private UUID id;
    private String name;
    private boolean open;
    private boolean vertical;
    private UUID world;
    private UUID faction;
    private Material material;
    private LocationDataBean positionOne;
    private LocationDataBean positionTwo;
    private LocationDataBean triggerLocation;
}
