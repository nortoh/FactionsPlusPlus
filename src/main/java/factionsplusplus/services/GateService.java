/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.constants.GateStatus;
import factionsplusplus.constants.ErrorCodeAddCoord;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.Gate;
import factionsplusplus.models.InteractionContext;
import factionsplusplus.models.LocationData;
import factionsplusplus.models.PlayerRecord;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

/**
 * @author Caibinus
 */
@Singleton
public class GateService {
    private final EphemeralData ephemeralData;
    private final FactionsPlusPlus factionsPlusPlus;
    private final ConfigService configService;
    private final DataService dataService;

    @Inject
    public GateService(
        EphemeralData ephemeralData,
        FactionsPlusPlus factionsPlusPlus,
        ConfigService configService,
        DataService dataService
    ) {
        this.ephemeralData = ephemeralData;
        this.factionsPlusPlus = factionsPlusPlus;
        this.configService = configService;
        this.dataService = dataService;
    }

    public void handlePotentialGateInteraction(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (! this.dataService.isChunkClaimed(clickedBlock.getChunk())) {
            return;
        }

        ClaimedChunk claim = this.dataService.getClaimedChunk(clickedBlock.getChunk());
        Faction faction = this.dataService.getFaction(claim.getHolder());
        Faction playersFaction = this.dataService.getPlayersFaction(player.getUniqueId());

        if (! faction.getUUID().equals(playersFaction.getUUID())) {
            return;
        }

        List<Gate> gatesForTrigger = this.dataService.getGatesForFactionsTriggerBlock(faction.getUUID(), clickedBlock);
        if (gatesForTrigger.isEmpty()) {
            return;
        }

        for (Gate g : gatesForTrigger) {
            BlockData blockData = clickedBlock.getBlockData();
            Powerable powerable = (Powerable) blockData;
            if (gatesForTrigger.get(0).isReady()) {
                if (powerable.isPowered()) this.open(g);
                else this.close(g);
                return;
            }
            event.setCancelled(true);
            this.dataService.getPlayerRecord(player.getUniqueId()).alert("PlayerNotice.GatePleaseWait", g.getStatus().toString().toLowerCase());
        }
    }

    public void handlePotentialGateInteraction(Block block, BlockRedstoneEvent event) {
        ClaimedChunk claim = this.dataService.getClaimedChunk(block.getChunk());
        if (claim != null) {
            List<Gate> gates = this.dataService.getGatesForFactionsTriggerBlock(claim.getHolder(), block);
            if (gates.size() == 0) return;
            for (Gate g : gates) {
                BlockData blockData = block.getBlockData();
                Powerable powerable = (Powerable) blockData;
                if (gates.get(0).isReady()) {
                    if (powerable.isPowered()) this.open(g);
                    else this.close(g);
                    return;
                }
            }
        }
    }

    public void handleCreatingGate(Block clickedBlock, Player player, PlayerInteractEvent event) {
        PlayerRecord member = this.dataService.getPlayerRecord(player.getUniqueId());

        if (! this.dataService.isChunkClaimed(clickedBlock.getChunk())) {
            member.error("Error.Gate.ClaimedTerritory");
            return;
        } else {
            ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(clickedBlock.getChunk());
            Faction faction = this.dataService.getFaction(claimedChunk.getHolder());
            if (claimedChunk != null) {
                if (! faction.isMember(player.getUniqueId())) {
                    member.error("Error.Gate.NotMember", faction.getName());
                    return;
                }
                if (! faction.isOwner(player.getUniqueId())
                            && ! faction.isOfficer(player.getUniqueId())) {
                    member.error("Error.Gate.NotOfficer", faction.getName());
                    return;
                }
            }
        }

        if (player.hasPermission("mf.gate")) {
            // TODO: Check if a gate already exists here, and if it does, print out some info of that existing gate instead of trying to create a new one.
            InteractionContext context = this.ephemeralData.getPlayersPendingInteraction().get(event.getPlayer().getUniqueId());
            if (context == null) return;
            boolean removeFromCreatingPlayers = true;
            String confirmedMessage = null;
            String nextMessage = null;
            String errorMessage = null;
            Integer stepNumber = null;

            if (
                context.isGateCreating() &&
                context.getGate().getCoord1() == null
            ) {
                stepNumber = 1;
                nextMessage = "PlayerNotice.Gate.ClickSecondPoint";
                removeFromCreatingPlayers = true;
            } else if (context.isGateCreating()
                    && context.getGate().getCoord1() != null
                    && context.getGate().getCoord2() == null
                    && context.getGate().getTrigger() == null
                ) {
                if (! context.getGate().getCoord1().getBlock().equals(clickedBlock)) {
                    stepNumber = 2;
                    nextMessage = "PlayerNotice.Gate.ClickTrigger";
                    removeFromCreatingPlayers = false;
                }
            } else if (context.getGate().getCoord2() != null
                    && context.getGate().getTrigger() == null
                    && ! context.getGate().getCoord2().getBlock().equals(clickedBlock)) {
                if (! (clickedBlock.getBlockData() instanceof Powerable)) {
                    // error & return
                    member.error("Error.Trigger.Unpowerable");
                    this.ephemeralData.getPlayersPendingInteraction().remove(event.getPlayer().getUniqueId());
                    return;
                }
                ClaimedChunk chunk = this.dataService.getClaimedChunk(clickedBlock.getChunk());
                if (chunk == null) {
                    // error & return
                    member.error("Error.Trigger.ClaimedTerritory");
                    this.ephemeralData.getPlayersPendingInteraction().remove(event.getPlayer().getUniqueId());
                    return;
                }
                stepNumber = 3;
                nextMessage = "GateCreated";
            }
            if (stepNumber == null) return;
            ErrorCodeAddCoord errorCode = this.addCoord(context.getGate(), clickedBlock);
            if (stepNumber < 3) {
                switch(errorCode) {
                    case None:
                        confirmedMessage = "PlayerNotice.Gate.PointPlaced";
                        break;
                    case MaterialMismatch:
                        errorMessage = "Error.Gate.MaterialMismatch";
                        break;
                    case NoCuboids:
                        errorMessage = "Error.Gate.CuboidDisallowed";
                        break;
                    case WorldMismatch:
                        errorMessage = "Error.Gate.WorldMismatch";
                        break;
                    case LessThanThreeHigh:
                        errorMessage = "Error.Gate.HeightRequirement";
                        break;
                    case Oversized:
                        errorMessage = "Error.Gate.TooBig";
                        break;
                    default:
                        errorMessage = "Error.Gate.Cancelled";
                        break;
                }
            } else {
                ClaimedChunk chunk = this.dataService.getClaimedChunk(clickedBlock.getChunk());
                switch(errorCode) {
                    case None:
                        context.getGate().setFaction(chunk.getHolder());
                        this.dataService.getGateRepository().create(context.getGate());
                        confirmedMessage = "PlayerNotice.Gate.TriggerPlaced";
                        break;
                    default:
                        errorMessage = "Error.Gate.LinkError";
                        break;
                }
            }
            if (errorMessage != null) member.error(errorMessage, stepNumber);
            if (nextMessage != null) member.alert(nextMessage, stepNumber);
            if (confirmedMessage != null) member.success(confirmedMessage, stepNumber);
            if (removeFromCreatingPlayers == true) this.ephemeralData.getPlayersPendingInteraction().remove(event.getPlayer().getUniqueId());
            return;
        }
        member.error("Error.PermissionDenied", "mf.gate");
    }

    public void open(Gate gate) {
        if (gate.isOpen() || ! gate.getStatus().equals(GateStatus.Ready)) {
            return;
        }

        gate.setOpen(true);
        gate.setStatus(GateStatus.Opening);

        // For vertical we only need to iterate over x/y
        if (gate.isVertical()) {
            if (gate.isParallelToX()) {
                int topY = gate.getCoord1().getY();
                int _bottomY = gate.getCoord2().getY();
                if (gate.getCoord2().getY() > gate.getCoord1().getY()) {
                    topY = gate.getCoord2().getY();
                    _bottomY = gate.getCoord1().getY();
                }
                final int bottomY = _bottomY;

                int _leftX = gate.getCoord1().getX();
                int _rightX = gate.getCoord2().getX();
                if (gate.getCoord2().getX() < gate.getCoord1().getX()) {
                    _leftX = gate.getCoord2().getX();
                    _rightX = gate.getCoord1().getX();
                }

                final int leftX = _leftX;
                final int rightX = _rightX;

                int c = 0;
                for (int y = bottomY; y <= topY; y++) {
                    c++;
                    final int blockY = y;
                    Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                        Block b;

                        @Override
                        public void run() {
                            for (int x = leftX; x <= rightX; x++) {
                                b = gate.getWorld().getBlockAt(x, blockY, gate.getCoord1().getZ());
                                b.setType(Material.AIR);
                                gate.getWorld().playSound(b.getLocation(), gate.getSoundEffect(), 0.1f, 0.1f);
                            }
                        }
                    }, c * 10L);
                }
                Bukkit.getScheduler().runTaskLater(factionsPlusPlus, new Runnable() {
                    @Override
                    public void run() {
                        gate.setStatus(GateStatus.Ready);
                        gate.setOpen(true);
                    }
                }, (topY - bottomY + 2) * 10L);
            } else if (gate.isParallelToZ()) {
                int topY = gate.getCoord1().getY();
                int _bottomY = gate.getCoord2().getY();
                if (gate.getCoord2().getY() > gate.getCoord1().getY()) {
                    topY = gate.getCoord2().getY();
                    _bottomY = gate.getCoord1().getY();
                }
                final int bottomY = _bottomY;
                int _leftZ = gate.getCoord1().getZ();
                int _rightZ = gate.getCoord2().getZ();
                if (gate.getCoord2().getZ() < gate.getCoord1().getZ()) {
                    _leftZ = gate.getCoord2().getZ();
                    _rightZ = gate.getCoord1().getZ();
                }

                final int leftZ = _leftZ;
                final int rightZ = _rightZ;

                int c = 0;
                for (int y = bottomY; y <= topY; y++) {
                    c++;
                    final int blockY = y;
                    Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                        Block b;

                        @Override
                        public void run() {
                            for (int z = leftZ; z <= rightZ; z++) {
                                b = gate.getWorld().getBlockAt(gate.getCoord1().getX(), blockY, z);
                                b.setType(Material.AIR);
                                gate.getWorld().playSound(b.getLocation(), gate.getSoundEffect(), 0.1f, 0.1f);
                            }
                        }
                    }, c * 10L);
                }
                Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                    @Override
                    public void run() {
                        gate.setStatus(GateStatus.Ready);
                        gate.setOpen(true);
                    }
                }, (topY - bottomY + 2) * 10L);
            }
        }
    }

    public void close(Gate gate) {
        if (! gate.isOpen() || ! gate.getStatus().equals(GateStatus.Ready)) {
            return;
        }

        gate.setOpen(false);
        gate.setStatus(GateStatus.Closing);

        // For vertical we only need to iterate over x/y
        if (gate.isVertical()) {
            if (gate.isParallelToX()) {
                int topY = gate.getCoord1().getY();
                int _bottomY = gate.getCoord2().getY();
                if (gate.getCoord2().getY() > gate.getCoord1().getY()) {
                    topY = gate.getCoord2().getY();
                    _bottomY = gate.getCoord1().getY();
                }
                final int bottomY = _bottomY;
                int _leftX = gate.getCoord1().getX();
                int _rightX = gate.getCoord2().getX();
                if (gate.getCoord2().getX() < gate.getCoord1().getX()) {
                    _leftX = gate.getCoord2().getX();
                    _rightX = gate.getCoord1().getX();
                }

                final int leftX = _leftX;
                final int rightX = _rightX;

                int c = 0;
                for (int y = topY; y >= bottomY; y--) {
                    c++;
                    final int blockY = y;
                    Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                        Block b;

                        @Override
                        public void run() {
                            for (int x = leftX; x <= rightX; x++) {
                                b = gate.getWorld().getBlockAt(x, blockY, gate.getCoord1().getZ());
                                b.setType(gate.getMaterial());
                                gate.getWorld().playSound(b.getLocation(), gate.getSoundEffect(), 0.1f, 0.1f);
                            }
                        }
                    }, c * 10L);
                }
                Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                    @Override
                    public void run() {
                        gate.setStatus(GateStatus.Ready);
                        gate.setOpen(false);
                    }
                }, (topY - bottomY + 2) * 10L);
            } else if (gate.isParallelToZ()) {
                int topY = gate.getCoord1().getY();
                int _bottomY = gate.getCoord2().getY();
                if (gate.getCoord2().getY() > gate.getCoord1().getY()) {
                    topY = gate.getCoord2().getY();
                    _bottomY = gate.getCoord1().getY();
                }
                final int bottomY = _bottomY;
                int _leftZ = gate.getCoord1().getZ();
                int _rightZ = gate.getCoord2().getZ();

                if (gate.getCoord2().getZ() < gate.getCoord1().getZ()) {
                    _leftZ = gate.getCoord2().getZ();
                    _rightZ = gate.getCoord1().getZ();
                }
                final int leftZ = _leftZ;
                final int rightZ = _rightZ;

                int c = 0;
                for (int y = topY; y >= bottomY; y--) {
                    c++;
                    final int blockY = y;
                    Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                        Block b;

                        @Override
                        public void run() {
                            for (int z = leftZ; z <= rightZ; z++) {
                                b = gate.getWorld().getBlockAt(gate.getCoord1().getX(), blockY, z);
                                b.setType(gate.getMaterial());
                                b.getState().update(true);
                                gate.getWorld().playSound(b.getLocation(), gate.getSoundEffect(), 0.1f, 0.1f);
                            }
                        }
                    }, c * 10L);
                }
                Bukkit.getScheduler().runTaskLater(this.factionsPlusPlus, new Runnable() {
                    @Override
                    public void run() {
                        gate.setStatus(GateStatus.Ready);
                        gate.setOpen(false);
                    }
                }, (topY - bottomY + 2) * 10L);
            }
        }
    }

    public ErrorCodeAddCoord addCoord(Gate gate, Block clickedBlock) {
        if (gate.getCoord1() == null) {
            gate.setWorld(clickedBlock.getWorld().getUID());
            gate.setCoord1(new LocationData(clickedBlock));
            gate.setMaterial(clickedBlock.getType());
        } else if (gate.getCoord2() == null) {
            if (! gate.getCoord1().getWorld().equals(clickedBlock.getWorld().getUID())) {
                return ErrorCodeAddCoord.WorldMismatch;
            }
            if (! clickedBlock.getType().equals(gate.getMaterial())) {
                return ErrorCodeAddCoord.MaterialMismatch;
            }
            // GetDim methods use coord2 object.
            gate.setCoord2(new LocationData(clickedBlock));
            if (gate.getDimX() > 1 && gate.getDimY() > 1 && gate.getDimZ() > 1) {
                // No cuboids.
                gate.setCoord2(null);
                return ErrorCodeAddCoord.NoCuboids;
            }
            if (gate.getDimY() <= 2) {
                gate.setCoord2(null);
                return ErrorCodeAddCoord.LessThanThreeHigh;
            }

            if (gate.isParallelToX() && gate.getDimY() > 1) {
                gate.setVertical(true);
            } else {
                gate.setVertical(gate.isParallelToZ() && gate.getDimY() > 1);
            }

            int area = 0;
            if (gate.isParallelToX()) {
                area = gate.getDimX() * gate.getDimY();
            } else if (gate.isParallelToZ()) {
                area = gate.getDimZ() * gate.getDimY();
            }

            if (area > this.configService.getInt("factionMaxGateArea")) {
                // Gate size exceeds config limit.
                gate.setCoord2(null);
                return ErrorCodeAddCoord.Oversized;
            }
            if (! gate.gateBlocksMatch(gate.getMaterial())) {
                gate.setCoord2(null);
                return ErrorCodeAddCoord.MaterialMismatch;
            }
        } else {
            gate.setTrigger(new LocationData(clickedBlock));
        }
        return ErrorCodeAddCoord.None;
    }
}
