package factionsplusplus.data.mappers;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Material;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import com.google.gson.Gson;

import factionsplusplus.data.beans.GateBean;
import factionsplusplus.data.beans.LocationDataBean;

public class GateMapper implements RowMapper<GateBean> {
    private final Gson gson = new Gson();

    @Override
    public GateBean map(ResultSet r, StatementContext ctx) throws SQLException {
        try {
            LocationDataBean positionOne = gson.fromJson(r.getString("position_one_location"), LocationDataBean.class);
            LocationDataBean positionTwo = gson.fromJson(r.getString("position_two_location"), LocationDataBean.class);
            LocationDataBean triggerPosition = gson.fromJson(r.getString("trigger_location"), LocationDataBean.class);
            ByteBuffer byteBuffer = ByteBuffer.wrap(r.getBytes("id"));
            GateBean result = new GateBean();
            result.setId(new UUID(byteBuffer.getLong(), byteBuffer.getLong()));
            result.setPositionOne(positionOne);
            result.setPositionTwo(positionTwo);
            result.setTriggerLocation(triggerPosition);
            result.setMaterial(Material.getMaterial(r.getString("material")));
            // World UUID
            byteBuffer = ByteBuffer.wrap(r.getBytes("world_id"));
            result.setWorld(new UUID(byteBuffer.getLong(), byteBuffer.getLong()));
            // Faction UUID
            byteBuffer = ByteBuffer.wrap(r.getBytes("faction_id"));
            result.setFaction(new UUID(byteBuffer.getLong(), byteBuffer.getLong()));
            result.setName(r.getString("name"));
            result.setOpen(r.getBoolean("is_open"));
            result.setVertical(r.getBoolean("is_vertical"));
            return result;
        } catch(Exception e) {
            System.out.println("Gate parsing error: "+e.getMessage());
        }
        return null;
    }
}
