package factionsplusplus.data.beans;
import java.util.UUID;

import com.google.gson.annotations.Expose;

import lombok.Data;

@Data
public class LocationDataBean {
    @Expose
    private Integer x;
    @Expose
    private Integer y;
    @Expose
    private Integer z;
    @Expose
    private UUID world;
}
