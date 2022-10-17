/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.objects.helper.GateCoord;
import dansplugins.factionsystem.constants.GateStatus;
import dansplugins.factionsystem.constants.ErrorCodeAddCoord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

import dansplugins.factionsystem.jsonadapters.GateCoordAdapter;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

/**
 * @author Caibinus
 * @author Daniel McCoy Stephenson
 */
public class Gate {
    private final Sound soundEffect = Sound.BLOCK_ANVIL_HIT;
    @Expose
    private String name = "gateName";
    @Expose
    private boolean open = false;
    @Expose
    private boolean vertical = true;
    @Expose
    @JsonAdapter(GateCoordAdapter.class)
    private GateCoord coord1 = null;
    @Expose
    @JsonAdapter(GateCoordAdapter.class)
    private GateCoord coord2 = null;
    @Expose
    @JsonAdapter(GateCoordAdapter.class)
    private GateCoord trigger = null;
    @Expose
    private Material material = Material.IRON_BARS;
    private World _world = null;
    @Expose
    private String world = "";
    private GateStatus gateStatus = GateStatus.Ready;

    public Gate() { }

    public Gate(String name) {
        setName(name);
    }

    public Gate load(String jsonData) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Gate newGate = new Gate();

        try {
            Gate.GateJson data = gson.fromJson(jsonData, Gate.GateJson.class);

            newGate.world = data.world;
            newGate.coord1 = new GateCoord();
            newGate.coord1 = GateCoord.fromString(data.coord1);
            newGate.coord2 = new GateCoord();
            newGate.coord2 = GateCoord.fromString(data.coord2);
            newGate.trigger = new GateCoord();
            newGate.trigger = GateCoord.fromString(data.triggerCoord);
            newGate.material = Material.getMaterial(data.material);
            newGate.open = Boolean.parseBoolean(data.open);
            newGate.vertical = Boolean.parseBoolean(data.vertical);
        } catch (Exception e) {
            System.out.println("ERROR: Could not load faction gate.\n");
        }

        return newGate;
    }

    public World getWorld() {
        if (_world != null) {
            return _world;
        }
        _world = getServer().getWorld(world);
        return _world;
    }

    public void setWorld(String worldName) {
        world = worldName;
        _world = null;
    }

    public boolean isVertical() {
        return this.vertical;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Sound getSoundEffect() {
        return this.soundEffect;
    }

    public void setStatus(GateStatus status) {
        this.gateStatus = status;
    }

    public Material getMaterial() {
        return this.material;
    }

    public void setCoord1(GateCoord coord) {
        this.coord1 = coord;
    }

    public void setCoord2(GateCoord coord) {
        this.coord2 = coord;
    }

    public void setTrigger(GateCoord coord) {
        this.trigger = coord;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }
    
    public JsonElement toJson() {
        return new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().serializeNulls().create().toJsonTree(this);
    }

    public boolean isIntersecting(Gate gate) {
        boolean xoverlap = coord2.getX() > gate.coord1.getX() && coord1.getX() < coord2.getX();
        boolean yoverlap = coord2.getY() > gate.coord1.getY() && coord1.getY() < gate.coord1.getY();
        boolean zoverlap = coord2.getZ() > gate.coord1.getZ() && coord1.getZ() < coord2.getZ();
        return xoverlap && yoverlap && zoverlap;
    }

    public int getTopLeftX() {
        if (coord1 != null && coord2 != null) {
            return Math.min(coord1.getX(), coord2.getX());
        }
        return 0;
    }

    public int getTopLeftY() {
        if (coord1 != null && coord2 != null) {
            return Math.max(coord1.getY(), coord2.getY());
        }
        return 0;
    }

    public int getTopLeftZ() {
        if (coord1 != null && coord2 != null) {
            return Math.min(coord1.getZ(), coord2.getZ());
        }
        return 0;
    }

    public int getBottomRightX() {
        if (coord1 != null && coord2 != null) {
            return Math.max(coord1.getX(), coord2.getX());
        }
        return 0;
    }

    public int getBottomRightY() {
        if (coord1 != null && coord2 != null) {
            return Math.min(coord1.getY(), coord2.getY());
        }
        return 0;
    }

    public int getBottomRightZ() {
        if (coord1 != null && coord2 != null) {
            return Math.max(coord1.getZ(), coord2.getZ());
        }
        return 0;
    }

    public int getTopLeftChunkX() {
        if (coord1 != null && coord2 != null) {
            return coord1.getX() < coord2.getX() ? coord1.getX() / 16 : coord2.getX() / 16;
        }
        return 0;
    }

    public int getTopLeftChunkZ() {
        if (coord1 != null && coord2 != null) {
            return coord1.getZ() < coord2.getZ() ? coord1.getZ() / 16 : coord2.getZ() / 16;
        }
        return 0;
    }

    public int getBottomRightChunkX() {
        if (coord1 != null && coord2 != null) {
            return coord1.getX() < coord2.getX() ? coord2.getX() / 16 : coord1.getX() / 16;
        }
        return 0;
    }

    public int getBottomRightChunkZ() {
        if (coord1 != null && coord2 != null) {
            return coord1.getZ() < coord2.getZ() ? coord2.getZ() / 16 : coord1.getZ() / 16;
        }
        return 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isReady() {
        return gateStatus.equals(GateStatus.Ready);
    }

    public boolean isClosed() {
        return !open;
    }

    public GateStatus getStatus() {
        return this.gateStatus;
    }

    public GateCoord getTrigger() {
        return trigger;
    }

    public GateCoord getCoord1() {
        return coord1;
    }

    public GateCoord getCoord2() {
        return coord2;
    }

    public boolean isParallelToZ() {
        if (coord1 != null && coord2 != null) {
            return coord1.getZ() != coord2.getZ();
        } else {
            return false;
        }
    }

    public boolean isParallelToX() {
        if (coord1 != null && coord2 != null) {
            return coord1.getX() != coord2.getX();
        } else {
            return false;
        }
    }

    public ArrayList<Block> getGateBlocks() {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int y = coord1.getY(); y < coord2.getY(); y++) {
            for (int z = coord1.getZ(); z < coord2.getZ(); z++) {
                for (int x = coord1.getX(); x < coord2.getX(); x++) {
                    blocks.add(getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public boolean gateBlocksMatch(Material mat) {
        int topY = coord1.getY();
        int bottomY = coord2.getY();
        if (coord2.getY() > coord1.getY()) {
            topY = coord2.getY();
            bottomY = coord1.getY();
        }

        int leftX = coord1.getX();
        int rightX = coord2.getX();
        if (coord2.getX() < coord1.getX()) {
            leftX = coord2.getX();
            rightX = coord1.getX();
        }

        int leftZ = coord1.getZ();
        int rightZ = coord2.getZ();
        if (coord2.getZ() < coord1.getZ()) {
            leftZ = coord2.getZ();
            rightZ = coord1.getZ();
        }

        if (isParallelToZ()) {
            rightX++;
        } else if (isParallelToX()) {
            rightZ++;
        }

        for (int y = topY; y > bottomY; y--) {
            for (int z = leftZ; z < rightZ; z++) {
                for (int x = leftX; x < rightX; x++) {
                    if (!getWorld().getBlockAt(x, y, z).getType().equals(mat)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public int getDimX() {
        return getDimX(coord1, coord2);
    }

    public int getDimY() {
        return getDimY(coord1, coord2);
    }

    public int getDimZ() {
        return getDimZ(coord1, coord2);
    }

    public int getDimX(GateCoord first, GateCoord second) {
        GateCoord tmp;
        if (first.getX() > second.getX()) {
            tmp = second;
            second = first;
            first = tmp;
        }
        return second.getX() - first.getX();
    }

    public int getDimY(GateCoord first, GateCoord second) {
        GateCoord tmp;
        if (first.getY() > second.getY()) {
            tmp = second;
            second = first;
            first = tmp;
        }
        return second.getY() - first.getY();
    }

    public int getDimZ(GateCoord first, GateCoord second) {
        GateCoord tmp;
        if (first.getZ() > second.getZ()) {
            tmp = second;
            second = first;
            first = tmp;
        }
        return second.getZ() - first.getZ();
    }

    public void fillGate() {
        if (!open) {
            return;
        }

        open = false;
        // For vertical, we only need to iterate over x/y
        if (vertical) {
            if (isParallelToX()) {
                int topY = coord1.getY();
                int bottomY = coord2.getY();
                if (coord2.getY() > coord1.getY()) {
                    topY = coord2.getY();
                    bottomY = coord1.getY();
                }

                int _leftX = coord1.getX();
                int _rightX = coord2.getX();
                if (coord2.getX() < coord1.getX()) {
                    _leftX = coord2.getX();
                    _rightX = coord1.getX();
                }

                final int leftX = _leftX;
                final int rightX = _rightX;

                for (int y = topY; y >= bottomY; y--) {
                    Block b = null;
                    for (int x = leftX; x <= rightX; x++) {
                        b = getWorld().getBlockAt(x, y, coord1.getZ());
                        b.setType(material);
                    }
                    if (b != null)
                        getWorld().playSound(b.getLocation(), soundEffect, 0.1f, 0.1f);
                }
            } else if (isParallelToZ()) {
                int topY = coord1.getY();
                int bottomY = coord2.getY();
                if (coord2.getY() > coord1.getY()) {
                    topY = coord2.getY();
                    bottomY = coord1.getY();
                }

                int _leftZ = coord1.getZ();
                int _rightZ = coord2.getZ();

                if (coord2.getZ() < coord1.getZ()) {
                    _leftZ = coord2.getZ();
                    _rightZ = coord1.getZ();
                }
                final int leftZ = _leftZ;
                final int rightZ = _rightZ;

                for (int y = topY; y >= bottomY; y--) {
                    Block b = null;
                    for (int z = leftZ; z <= rightZ; z++) {
                        b = getWorld().getBlockAt(coord1.getX(), y, z);
                        b.setType(material);
                    }
                    if (b != null) {
                        getWorld().playSound(b.getLocation(), soundEffect, 0.1f, 0.1f);
                    }
                }
            }
        }
    }

    public boolean hasBlock(Block targetBlock) {
        int topY = coord1.getY();
        int bottomY = coord2.getY();
        if (coord2.getY() > coord1.getY()) {
            topY = coord2.getY();
            bottomY = coord1.getY();
        }

        int _leftZ = coord1.getZ();
        int _rightZ = coord2.getZ();

        if (coord2.getZ() < coord1.getZ()) {
            _leftZ = coord2.getZ();
            _rightZ = coord1.getZ();
        }
        int leftZ = _leftZ;
        int rightZ = _rightZ;

        int _leftX = coord1.getX();
        int _rightX = coord2.getX();
        if (coord2.getX() < coord1.getX()) {
            _leftX = coord2.getX();
            _rightX = coord1.getX();
        }

        int leftX = _leftX;
        int rightX = _rightX;

        if (targetBlock.getX() >= leftX && targetBlock.getX() <= rightX
                && targetBlock.getY() >= bottomY && targetBlock.getY() <= topY
                && targetBlock.getZ() >= leftZ && targetBlock.getZ() <= rightZ
                && targetBlock.getWorld().getName().equalsIgnoreCase(coord1.getWorld())) {
            return true;
        }

        return trigger.equals(targetBlock);
    }

    public String coordsToString() {
        if (coord1 == null || coord2 == null || trigger == null) {
            return "";
        }

        return String.format("(%d, %d, %d to %d, %d, %d) Trigger (%d, %d, %d)", coord1.getX(), coord1.getY(), coord1.getZ(), coord2.getX(), coord2.getY(), coord2.getZ(),
                trigger.getX(), trigger.getY(), trigger.getZ());
    }

    @SuppressWarnings("unused")
    private static class GateJson {
        public String name;
        public String factionName;
        public String open;
        public String vertical;
        public String material;
        public String world;
        public String coord1;
        public String coord2;
        public String triggerCoord;
    }
}