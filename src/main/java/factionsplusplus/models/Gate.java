/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.constants.GateStatus;
import factionsplusplus.data.beans.GateBean;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Caibinus
 * @author Daniel McCoy Stephenson
 */
public class Gate {
    private final Sound soundEffect = Sound.BLOCK_ANVIL_HIT;
    private String name = null;
    private UUID uuid;
    private UUID factionUUID;
    private boolean open = false;
    private boolean vertical = true;
    private LocationData coord1 = null;
    private LocationData coord2 = null;
    private LocationData trigger = null;
    private Material material = Material.IRON_BARS;
    private World _world = null;
    private UUID world;
    private GateStatus gateStatus = GateStatus.Ready;

    public Gate() {
        this.gateStatus = GateStatus.Ready;
    }

    public Gate(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    public Gate(GateBean bean) {
        this.name = bean.getName();
        this.uuid = bean.getId();
        this.factionUUID = bean.getFaction();
        this.world = bean.getWorld();
        this.open = bean.isOpen();
        this.vertical = bean.isVertical();
        this.material = bean.getMaterial();
        this.coord1 = new LocationData(bean.getPositionOne());
        this.coord2 = new LocationData(bean.getPositionTwo());
        this.trigger = new LocationData(bean.getTriggerLocation());
    }

    public World getWorld() {
        if (this._world != null) {
            return this._world;
        }
        this._world = getServer().getWorld(this.world);
        return this._world;
    }

    public void setWorld(UUID worldUUID) {
        this.world = worldUUID;
        this._world = null;
    }

    public boolean isVertical() {
        return this.vertical;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public UUID getFaction() {
        return this.factionUUID;
    }

    public void setFaction(UUID factionUUID) {
        this.factionUUID = factionUUID;
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

    public void setCoord1(LocationData coord) {
        this.coord1 = coord;
    }

    public void setCoord2(LocationData coord) {
        this.coord2 = coord;
    }

    public void setTrigger(LocationData coord) {
        this.trigger = coord;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public boolean isIntersecting(Gate gate) {
        boolean xoverlap = this.coord2.getX() > gate.coord1.getX() && this.coord1.getX() < this.coord2.getX();
        boolean yoverlap = this.coord2.getY() > gate.coord1.getY() && this.coord1.getY() < gate.coord1.getY();
        boolean zoverlap = this.coord2.getZ() > gate.coord1.getZ() && this.coord1.getZ() < this.coord2.getZ();
        return xoverlap && yoverlap && zoverlap;
    }

    public int getTopLeftX() {
        if (this.coord1 != null && this.coord2 != null) {
            return Math.min(this.coord1.getX(), this.coord2.getX());
        }
        return 0;
    }

    public int getTopLeftY() {
        if (this.coord1 != null && this.coord2 != null) {
            return Math.max(this.coord1.getY(), this.coord2.getY());
        }
        return 0;
    }

    public int getTopLeftZ() {
        if (this.coord1 != null && this.coord2 != null) {
            return Math.min(this.coord1.getZ(), this.coord2.getZ());
        }
        return 0;
    }

    public int getBottomRightX() {
        if (this.coord1 != null && this.coord2 != null) {
            return Math.max(this.coord1.getX(), this.coord2.getX());
        }
        return 0;
    }

    public int getBottomRightY() {
        if (this.coord1 != null && this.coord2 != null) {
            return Math.min(this.coord1.getY(), this.coord2.getY());
        }
        return 0;
    }

    public int getBottomRightZ() {
        if (this.coord1 != null && this.coord2 != null) {
            return Math.max(this.coord1.getZ(), this.coord2.getZ());
        }
        return 0;
    }

    public int getTopLeftChunkX() {
        if (this.coord1 != null && this.coord2 != null) {
            return this.coord1.getX() < this.coord2.getX() ? this.coord1.getX() / 16 : this.coord2.getX() / 16;
        }
        return 0;
    }

    public int getTopLeftChunkZ() {
        if (this.coord1 != null && this.coord2 != null) {
            return this.coord1.getZ() < this.coord2.getZ() ? this.coord1.getZ() / 16 : this.coord2.getZ() / 16;
        }
        return 0;
    }

    public int getBottomRightChunkX() {
        if (coord1 != null && coord2 != null) {
            return this.coord1.getX() < this.coord2.getX() ? this.coord2.getX() / 16 : this.coord1.getX() / 16;
        }
        return 0;
    }

    public int getBottomRightChunkZ() {
        if (this.coord1 != null && this.coord2 != null) {
            return this.coord1.getZ() < this.coord2.getZ() ? this.coord2.getZ() / 16 : this.coord1.getZ() / 16;
        }
        return 0;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isReady() {
        return this.gateStatus.equals(GateStatus.Ready);
    }

    public boolean isClosed() {
        return ! open;
    }

    public GateStatus getStatus() {
        return this.gateStatus;
    }

    public LocationData getTrigger() {
        return this.trigger;
    }

    public LocationData getCoord1() {
        return this.coord1;
    }

    public LocationData getCoord2() {
        return this.coord2;
    }

    public boolean isParallelToZ() {
        if (this.coord1 != null && this.coord2 != null) {
            return this.coord1.getZ() != this.coord2.getZ();
        } else {
            return false;
        }
    }

    public boolean isParallelToX() {
        if (this.coord1 != null && this.coord2 != null) {
            return this.coord1.getX() != this.coord2.getX();
        } else {
            return false;
        }
    }

    public ArrayList<Block> getGateBlocks() {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int y = this.coord1.getY(); y < this.coord2.getY(); y++) {
            for (int z = this.coord1.getZ(); z < this.coord2.getZ(); z++) {
                for (int x = this.coord1.getX(); x < this.coord2.getX(); x++) {
                    blocks.add(getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public boolean gateBlocksMatch(Material mat) {
        int topY = this.coord1.getY();
        int bottomY = this.coord2.getY();
        if (this.coord2.getY() > this.coord1.getY()) {
            topY = this.coord2.getY();
            bottomY = this.coord1.getY();
        }

        int leftX = this.coord1.getX();
        int rightX = this.coord2.getX();
        if (this.coord2.getX() < this.coord1.getX()) {
            leftX = this.coord2.getX();
            rightX = this.coord1.getX();
        }

        int leftZ = this.coord1.getZ();
        int rightZ = this.coord2.getZ();
        if (this.coord2.getZ() < this.coord1.getZ()) {
            leftZ = this.coord2.getZ();
            rightZ = this.coord1.getZ();
        }

        if (this.isParallelToZ()) {
            rightX++;
        } else if (this.isParallelToX()) {
            rightZ++;
        }

        for (int y = topY; y > bottomY; y--) {
            for (int z = leftZ; z < rightZ; z++) {
                for (int x = leftX; x < rightX; x++) {
                    if (! getWorld().getBlockAt(x, y, z).getType().equals(mat)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public int getDimX() {
        return this.getDimX(coord1, coord2);
    }

    public int getDimY() {
        return this.getDimY(coord1, coord2);
    }

    public int getDimZ() {
        return this.getDimZ(coord1, coord2);
    }

    public int getDimX(LocationData first, LocationData second) {
        LocationData tmp;
        if (first.getX() > second.getX()) {
            tmp = second;
            second = first;
            first = tmp;
        }
        return second.getX() - first.getX();
    }

    public int getDimY(LocationData first, LocationData second) {
        LocationData tmp;
        if (first.getY() > second.getY()) {
            tmp = second;
            second = first;
            first = tmp;
        }
        return second.getY() - first.getY();
    }

    public int getDimZ(LocationData first, LocationData second) {
        LocationData tmp;
        if (first.getZ() > second.getZ()) {
            tmp = second;
            second = first;
            first = tmp;
        }
        return second.getZ() - first.getZ();
    }

    public void fillGate() {
        if (! open) {
            return;
        }

        open = false;
        // For vertical, we only need to iterate over x/y
        if (this.vertical) {
            if (this.isParallelToX()) {
                int topY = this.coord1.getY();
                int bottomY = this.coord2.getY();
                if (this.coord2.getY() > this.coord1.getY()) {
                    topY = this.coord2.getY();
                    bottomY = this.coord1.getY();
                }

                int _leftX = this.coord1.getX();
                int _rightX = this.coord2.getX();
                if (this.coord2.getX() < this.coord1.getX()) {
                    _leftX = this.coord2.getX();
                    _rightX = this.coord1.getX();
                }

                final int leftX = _leftX;
                final int rightX = _rightX;

                for (int y = topY; y >= bottomY; y--) {
                    Block b = null;
                    for (int x = leftX; x <= rightX; x++) {
                        b = getWorld().getBlockAt(x, y, this.coord1.getZ());
                        b.setType(material);
                    }
                    if (b != null)
                        getWorld().playSound(b.getLocation(), this.soundEffect, 0.1f, 0.1f);
                }
            } else if (this.isParallelToZ()) {
                int topY = this.coord1.getY();
                int bottomY = this.coord2.getY();
                if (this.coord2.getY() > this.coord1.getY()) {
                    topY = this.coord2.getY();
                    bottomY = this.coord1.getY();
                }

                int _leftZ = this.coord1.getZ();
                int _rightZ = this.coord2.getZ();

                if (this.coord2.getZ() < this.coord1.getZ()) {
                    _leftZ = this.coord2.getZ();
                    _rightZ = this.coord1.getZ();
                }
                final int leftZ = _leftZ;
                final int rightZ = _rightZ;

                for (int y = topY; y >= bottomY; y--) {
                    Block b = null;
                    for (int z = leftZ; z <= rightZ; z++) {
                        b = getWorld().getBlockAt(this.coord1.getX(), y, z);
                        b.setType(material);
                    }
                    if (b != null) {
                        getWorld().playSound(b.getLocation(), this.soundEffect, 0.1f, 0.1f);
                    }
                }
            }
        }
    }

    public boolean hasBlock(Block targetBlock) {
        int topY = this.coord1.getY();
        int bottomY = this.coord2.getY();
        if (this.coord2.getY() > this.coord1.getY()) {
            topY = this.coord2.getY();
            bottomY = this.coord1.getY();
        }

        int _leftZ = this.coord1.getZ();
        int _rightZ = this.coord2.getZ();

        if (this.coord2.getZ() < this.coord1.getZ()) {
            _leftZ = this.coord2.getZ();
            _rightZ = this.coord1.getZ();
        }
        int leftZ = _leftZ;
        int rightZ = _rightZ;

        int _leftX = this.coord1.getX();
        int _rightX = this.coord2.getX();
        if (this.coord2.getX() < this.coord1.getX()) {
            _leftX = this.coord2.getX();
            _rightX = this.coord1.getX();
        }

        int leftX = _leftX;
        int rightX = _rightX;

        if (targetBlock.getX() >= leftX && targetBlock.getX() <= rightX
                && targetBlock.getY() >= bottomY && targetBlock.getY() <= topY
                && targetBlock.getZ() >= leftZ && targetBlock.getZ() <= rightZ
                && targetBlock.getWorld().getUID().equals(this.coord1.getWorld())) {
            return true;
        }

        return this.trigger.getBlock().equals(targetBlock);
    }

    public String coordsToString() {
        if (this.coord1 == null || this.coord2 == null || this.trigger == null) {
            return "";
        }

        return String.format("(%d, %d, %d to %d, %d, %d) Trigger (%d, %d, %d)", this.coord1.getX(), this.coord1.getY(), this.coord1.getZ(), this.coord2.getX(), this.coord2.getY(), this.coord2.getZ(),
                this.trigger.getX(), this.trigger.getY(), this.trigger.getZ());
    }
}