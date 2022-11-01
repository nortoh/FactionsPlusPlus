package factionsplusplus.data.beans;
import java.util.UUID;
import lombok.Data;

@Data
public class LocationDataBean {
    private Double x;
    private Double y;
    private Double z;
    private UUID world;
}
