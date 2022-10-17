/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.constants.GateStatus;
import dansplugins.factionsystem.constants.ErrorCodeAddCoord;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.Gate;
import dansplugins.factionsystem.objects.helper.GateCoord;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;
import javax.inject.Provider;

/**
 * @author Caibinus
 */
@Singleton
public class GateService {
    private final Provider<PersistentData> persistentData;
    private final LocaleService localeService;
    private final EphemeralData ephemeralData;
    private final PlayerService playerService;
    private final MessageService messageService;
    private final MedievalFactions medievalFactions;
    private final ConfigService configService;

    @Inject
    public GateService(
        Provider<PersistentData> persistentData,
        LocaleService localeService,
        EphemeralData ephemeralData,
        PlayerService playerService,
        MessageService messageService,
        MedievalFactions medievalFactions,
        ConfigService configService
    ) {
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.ephemeralData = ephemeralData;
        this.playerService = playerService;
        this.messageService = messageService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
    }

    public void handlePotentialGateInteraction(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!persistentData.get().getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
            return;
        }

        ClaimedChunk claim = persistentData.get().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
        Faction faction = persistentData.get().getFaction(claim.getHolder());
        Faction playersFaction = persistentData.get().getPlayersFaction(player.getUniqueId());

        if (!faction.getName().equals(playersFaction.getName())) {
            return;
        }

        if (!faction.hasGateTrigger(clickedBlock)) {
            return;
        }

        for (Gate g : faction.getGatesForTrigger(clickedBlock)) {
            BlockData blockData = clickedBlock.getBlockData();
            Powerable powerable = (Powerable) blockData;
            if (powerable.isPowered()) {
                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                    this.open(g);
                } else {
                    event.setCancelled(true);
                    playerService.sendMessage(player, ChatColor.RED + String.format(localeService.get("PleaseWaitGate"), g.getStatus().toString().toLowerCase())
                            , Objects.requireNonNull(messageService.getLanguage().getString("PleaseWaitGate")).replace("#status#", g.getStatus().toString().toLowerCase()), true);
                    return;
                }
            } else {
                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                    this.close(g);
                } else {
                    event.setCancelled(true);
                    playerService.sendMessage(player, ChatColor.RED + String.format(localeService.get("PleaseWaitGate"), g.getStatus().toString().toLowerCase())
                            , Objects.requireNonNull(messageService.getLanguage().getString("PleaseWaitGate")).replace("#status#", g.getStatus().toString().toLowerCase()), true);
                    return;
                }
            }
        }
    }

    public void handlePotentialGateInteraction(Block block, BlockRedstoneEvent event) {
        if (persistentData.get().getChunkDataAccessor().isClaimed(block.getChunk())) {
            ClaimedChunk claim = persistentData.get().getChunkDataAccessor().getClaimedChunk(block.getChunk());
            Faction faction = persistentData.get().getFaction(claim.getHolder());

            if (faction.hasGateTrigger(block)) {
                for (Gate g : faction.getGatesForTrigger(block)) {
                    BlockData blockData = block.getBlockData();
                    Powerable powerable = (Powerable) blockData;
                    if (powerable.isPowered()) {
                        if (faction.getGatesForTrigger(block).get(0).isReady()) {
                            this.open(g);
                        } else {
                            return;
                        }
                    } else {
                        if (faction.getGatesForTrigger(block).get(0).isReady()) {
                            this.close(g);
                        } else {
                            return;
                        }
                    }
                }
            }
        }
    }

    public void handleCreatingGate(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!persistentData.get().getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
            playerService.sendMessage(player, ChatColor.RED + localeService.get("CanOnlyCreateGatesInClaimedTerritory"), "CanOnlyCreateGatesInClaimedTerritory", false);
            return;
        } else {
            ClaimedChunk claimedChunk = persistentData.get().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
            if (claimedChunk != null) {
                if (!persistentData.get().getFaction(claimedChunk.getHolder()).isMember(player.getUniqueId())) {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("AlertMustBeMemberToCreateGate"), "AlertMustBeMemberToCreateGate", false);

                    return;
                } else {
                    if (!persistentData.get().getFaction(claimedChunk.getHolder()).isOwner(player.getUniqueId())
                            && !persistentData.get().getFaction(claimedChunk.getHolder()).isOfficer(player.getUniqueId())) {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("AlertMustBeOwnerOrOfficerToCreateGate"), "AlertMustBeOwnerOrOfficerToCreateGate", false);
                        return;
                    }
                }
            }
        }

        if (player.hasPermission("mf.gate")) {
            // TODO: Check if a gate already exists here, and if it does, print out some info of that existing gate instead of trying to create a new one.
            if (ephemeralData.getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() == null) {
                Gate g = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId());
                ErrorCodeAddCoord e = this.addCoord(g, clickedBlock);
                if (e.equals(ErrorCodeAddCoord.None)) {
                    playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + localeService.get("Point1PlacementSuccessful")
                            , "Point1PlacementSuccessful", false);
                    playerService.sendMessage(event.getPlayer(), ChatColor.YELLOW + localeService.get("ClickToPlaceSecondCorner")
                            , "ClickToPlaceSecondCorner", false);
                } else if (e.equals(ErrorCodeAddCoord.MaterialMismatch)) {
                    playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("MaterialsMismatch1")
                            , "MaterialsMismatch1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(ErrorCodeAddCoord.WorldMismatch)) {
                    playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("WorldsMismatch1")
                            , "WorldsMismatch1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(ErrorCodeAddCoord.NoCuboids)) {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("CuboidDisallowed1")
                            , "CuboidDisallowed1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("CancelledGatePlacement1")
                            , "CancelledGatePlacement1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            } else if (ephemeralData.getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null) {
                if (!ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock)) {
                    Gate g = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId());
                    ErrorCodeAddCoord e = this.addCoord(g, clickedBlock);
                    if (e.equals(ErrorCodeAddCoord.None)) {
                        playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + localeService.get("Point2PlacementSuccessful")
                                , "Point2PlacementSuccessful", false);
                        playerService.sendMessage(event.getPlayer(), ChatColor.YELLOW + "Click on the trigger block..."
                                , "ClickTBlock", false);
                    } else if (e.equals(ErrorCodeAddCoord.MaterialMismatch)) {
                        playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("MaterialsMismatch2")
                                , "MaterialsMismatch2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(ErrorCodeAddCoord.WorldMismatch)) {
                        playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("WorldsMismatch2")
                                , "WorldsMismatch2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(ErrorCodeAddCoord.NoCuboids)) {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("CuboidDisallowed2")
                                , "CuboidDisallowed2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(ErrorCodeAddCoord.LessThanThreeHigh)) {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("ThreeBlockRequirement")
                                , "ThreeBlockRequirement", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("CancelledGatePlacement2")
                                , "CancelledGatePlacement2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                }
            } else if (ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() != null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null
                    && !ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock)) {
                if (clickedBlock.getBlockData() instanceof Powerable) {
                    if (persistentData.get().getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
                        Gate g = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId());
                        ErrorCodeAddCoord e = this.addCoord(g, clickedBlock);
                        if (e.equals(ErrorCodeAddCoord.None)) {
                            ClaimedChunk claim = persistentData.get().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
                            this.persistentData.get().getFaction(claim.getHolder()).addGate(g);
                            ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + "Creating Gate 4/4: Trigger successfully linked."
                                    , "Point4TriggeredSuccessfully", false);
                            playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + localeService.get("GateCreated")
                                    , "GateCreated", false);
                        } else {
                            playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("CancelledGatePlacementErrorLinking")
                                    , "CancelledGatePlacementErrorLinking", false);
                            ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        }
                    } else {
                        playerService.sendMessage(event.getPlayer(), ChatColor.RED + "Error: Can only use triggers in claimed territory."
                                , "CanOnlyTrigger", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                } else {
                    playerService.sendMessage(event.getPlayer(), ChatColor.RED + "Trigger block was not powerable. Cancelled gate placement."
                            , "TriggerBlockNotPowerable", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            }
        } else {
            playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("PermissionGate")
                    , Objects.requireNonNull(messageService.getLanguage().getString("PermissionNeeded")).replace("#permission#", "mf.gate"), true);
        }
    }

    public void open(Gate gate) {
        if (gate.isOpen() || !gate.getStatus().equals(GateStatus.Ready)) {
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
                    Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
                Bukkit.getScheduler().runTaskLater(medievalFactions, new Runnable() {
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
                    Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
                Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
        if (!gate.isOpen() || !gate.getStatus().equals(GateStatus.Ready)) {
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
                    Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
                Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
                    Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
                Bukkit.getScheduler().runTaskLater(this.medievalFactions, new Runnable() {
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
            gate.setWorld(clickedBlock.getWorld().getName());
            gate.setCoord1(new GateCoord(clickedBlock));
            gate.setMaterial(clickedBlock.getType());
        } else if (gate.getCoord2() == null) {
            if (!gate.getCoord1().getWorld().equalsIgnoreCase(clickedBlock.getWorld().getName())) {
                return ErrorCodeAddCoord.WorldMismatch;
            }
            if (!clickedBlock.getType().equals(gate.getMaterial())) {
                return ErrorCodeAddCoord.MaterialMismatch;
            }
            // GetDim methods use coord2 object.
            gate.setCoord2(new GateCoord(clickedBlock));
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
            if (!gate.gateBlocksMatch(gate.getMaterial())) {
                gate.setCoord2(null);
                return ErrorCodeAddCoord.MaterialMismatch;
            }
        } else {
            gate.setTrigger(new GateCoord(clickedBlock));
        }
        return ErrorCodeAddCoord.None;
    }
}
