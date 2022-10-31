package factionsplusplus.data.beans;
import java.util.UUID;
import lombok.Data;

@Data
public class LocationDataBean {
    private Integer x;
    private Integer y;
    private Integer z;
    private UUID world;
}
