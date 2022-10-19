package factionsplusplus.integrators;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.MedievalFactions;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.objects.helper.ChunkFlags;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.LocaleService;
import factionsplusplus.utils.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.*;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Caibinus
 */
@Singleton
public class DynmapIntegrator {
    public static boolean dynmapInitialized = false;
    private final Logger logger;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final PersistentData persistentData;
    private final FactionService factionService;
    private final DataService dataService;

    // Claims/factions markers
    private final Map<String, AreaMarker> resAreas = new HashMap<>();
    private final Map<String, Marker> resMark = new HashMap<>();

    // Dynmap integration related members
    // Realms markers
    private final Map<String, AreaMarker> realmsAreas = new HashMap<>();
    private final Map<String, Marker> realmsMark = new HashMap<>();
    private final Plugin dynmap;
    private DynmapCommonAPI dynmapAPI;
    private boolean updateClaimsAreaMarkers = false;
    private MarkerSet claims;
    private MarkerSet realms;

    @Inject
    public DynmapIntegrator(
        Logger logger,
        LocaleService localeService,
        MedievalFactions medievalFactions,
        PersistentData persistentData,
        FactionService factionService,
        DataService dataService
    ) {
        this.logger = logger;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.persistentData = persistentData;
        this.factionService = factionService;
        this.dataService = dataService;
        PluginManager pm = getServer().getPluginManager();

        /* Get dynmap */
        dynmap = pm.getPlugin("dynmap");

        if (dynmap == null) {
            this.logger.debug(this.localeService.get("ConsoleAlerts.CannotFindDynmap"));
        } else {
            try {
                this.dynmapAPI = (DynmapCommonAPI) dynmap; /* Get API */
                if (this.dynmapAPI == null) {
                    this.logger.error("Instantiated DynmapCommonAPI object was null. DynmapIntegrator construction cannot continue.");
                    return;
                }
                this.initializeMarkerSets();
                this.logger.debug(this.localeService.get("ConsoleAlerts.DynmapIntegrationSuccessful"));
            } catch (Exception e) {
                this.logger.debug(this.localeService.get("ConsoleAlerts.ErrorIntegratingWithDynmap").replace("#error#", e.getMessage()));
            }
        }
    }

    public static boolean hasDynmap() {
        return dynmapInitialized;
    }

    /***
     * Scheduled task that checks to see if there are changes to the claims that need
     * to be rendered on dynmap.
     * @param interval Number of ticks before the scheduled task executes again.
     */
    public void scheduleClaimsUpdate(long interval) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isDynmapMissing()) {
                    return;
                }
                if (updateClaimsAreaMarkers) {
                    if (realms != null) {
                        realms.deleteMarkerSet();
                        claims.deleteMarkerSet();
                    }
                    initializeMarkerSets();
                    dynmapUpdateFactions();
                    dynmapUpdateRealms();
                    updateClaimsAreaMarkers = false;
                }
            }
        }.runTaskTimer(this.medievalFactions, 40, interval);
    }

    /***
     * Tell the scheduled task that we have made changes and it should update the
     * area markers.
     */
    public void updateClaims() {
        if (this.isDynmapMissing()) {
            return;
        }

        if (DynmapIntegrator.hasDynmap()) {
            updateClaimsAreaMarkers = true;
        }
    }

    private void initializeMarkerSets() {
        claims = this.getMarkerAPI().getMarkerSet(getDynmapPluginSetId("claims"));
        claims = this.initializeMarkerSet(this.claims, "Claims");

        realms = this.getMarkerAPI().getMarkerSet(getDynmapPluginSetId("realms"));
        realms = this.initializeMarkerSet(this.realms, "Realms");
    }

    private MarkerSet initializeMarkerSet(MarkerSet set, String markerLabel) {
        if (set == null) {
            set = this.getMarkerAPI().createMarkerSet(this.getDynmapPluginSetId(markerLabel), this.getDynmapPluginLayer(), null, false);
            if (set == null) {
                this.logger.debug(this.localeService.get("ConsoleAlerts.ErrorCreatingMarkerSet").replace("#error#", ": markerLabel = " + markerLabel));
                return null;
            }
        }
        set.setMarkerSetLabel(markerLabel);
        return set;
    }

    private boolean isDynmapMissing() {
        this.logger.debug("Is dynmap missing? " + (dynmap == null));
        return (dynmap == null);
    }

    private MarkerAPI getMarkerAPI() {
        return this.dynmapAPI.getMarkerAPI();
    }

    /**
     * Find all contiguous blocks, set in target and clear in source
     */
    private void floodFillTarget(ChunkFlags src, ChunkFlags dest, int x, int y) {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if (src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false);   /* Clear source */
                dest.setFlag(x, y, true);   /* Set in destination */
                if (src.getFlag(x + 1, y))
                    stack.push(new int[]{x + 1, y});
                if (src.getFlag(x - 1, y))
                    stack.push(new int[]{x - 1, y});
                if (src.getFlag(x, y + 1))
                    stack.push(new int[]{x, y + 1});
                if (src.getFlag(x, y - 1))
                    stack.push(new int[]{x, y - 1});
            }
        }
    }

    /* Update Realm information */
    private void dynmapUpdateRealms() {
        // Realms Layer

        Map<String, AreaMarker> newMap = new HashMap<>(); /* Build new map */
        Map<String, Marker> newMark = new HashMap<>(); /* Build new map */

        /* Loop through realms and build area markers coloured in the same colour
            as each faction's liege's colour. */
        for (Faction f : this.persistentData.getFactions()) {
            UUID liegeID = this.factionService.getTopLiege(f);
            Faction liege = this.persistentData.getFactionByID(liegeID);
            String liegeName = liege.getName();
            String liegeColor;
            String popupText;
            // If there's no liege, then f is the liege.
            if (liege != null) {
                liegeColor = liege.getFlag("dynmapTerritoryColor").toString();
                popupText = this.buildNationPopupText(liege);
            } else {
                liegeColor = f.getFlag("dynmapTerritoryColor").toString();
                liegeName = f.getName() + "__parent";
                popupText = this.buildNationPopupText(f);
            }
            this.dynmapUpdateFaction(f, this.realms, newMap, "realm", liegeName + "__" + getClass().getName(), popupText, liegeColor, newMap);
        }

        /* Now, review old map - anything left is gone */
        for (AreaMarker oldm : this.realmsAreas.values()) {
            oldm.deleteMarker();
        }
        for (Marker oldm : this.realmsMark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        this.realmsAreas.putAll(newMap);
        this.realmsMark.putAll(newMark);
    }

    /* Update Faction information */
    private void dynmapUpdateFactions() {
        // Claims Layer

        Map<String, AreaMarker> newmap = new HashMap<>(); /* Build new map */
        Map<String, Marker> newmark = new HashMap<>(); /* Build new map */

        /* Loop through factions and build coloured faction area markers. */
        for (Faction f : this.persistentData.getFactions()) {
            this.dynmapUpdateFaction(f, this.claims, newmap, "claims", f.getName(), buildNationPopupText(f), f.getFlag("dynmapTerritoryColor").toString(), newmap);
        }

        /* Now, review old map - anything left is gone */
        for (AreaMarker oldm : this.resAreas.values()) {
            oldm.deleteMarker();
        }
        for (Marker oldm : this.resMark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        this.resAreas.putAll(newmap);
        this.resMark.putAll(newmark);
    }

    private String buildNationPopupText(Faction f) {
        UUIDChecker uuidChecker = new UUIDChecker();
        String message = "<h4>" + f.getName() + "</h4>" +
                "Owner: " + uuidChecker.findPlayerNameBasedOnUUID(f.getOwner()) + "<br/>" +
                "Description: " + f.getDescription() + "<br/>" +
                "<div style='display: inline;' title='" + f.getMemberListSeparatedByCommas() + "'>Population: " + f.getMemberList().size() + "</div><br/>";

        if (f.hasLiege()) {
            message += "Liege: " + this.dataService.getFactionByID(f.getLiege()).getName() + "<br/>";
        }
        if (f.isLiege()) {
            message += "Vassals: " + f.getVassalsSeparatedByCommas() + "<br/>";
        }
        message += "Allied With: " + f.getAlliesSeparatedByCommas() + "<br/>" +
                "At War With: " + f.getEnemiesSeparatedByCommas() + "<br/>" +
                "Power Level: " + this.factionService.getCumulativePowerLevel(f) + "<br/>" +
                "Demesne Size: " + String.format("%d/%d",
                this.persistentData.getChunkDataAccessor().getChunksClaimedByFaction(f.getID()),
                this.factionService.getCumulativePowerLevel(f));
        return message;
    }

    private void dynmapUpdateFaction(Faction faction, MarkerSet markerSet, Map<String, AreaMarker> areaMarkers, String type, String name, String popupDescription, String colorCode, Map<String, AreaMarker> newmap) {
        double[] x;
        double[] z;
        int poly_index = 0; /* Index of polygon for given town */

        /* Handle areas */
        List<ClaimedChunk> blocks = this.dataService.getClaimedChunksForFaction(faction);
        if (blocks.isEmpty())
            return;
        HashMap<String, ChunkFlags> blkmaps = new HashMap<>();
        LinkedList<ClaimedChunk> nodevals = new LinkedList<>();
        String currentWorld = null;
        ChunkFlags curblks = null;

        /* Loop through blocks: set flags on blockmaps for worlds */
        for (ClaimedChunk b : blocks) {
            if (!b.getWorldName().equalsIgnoreCase(currentWorld)) { /* Not same world */
                String wname = b.getWorldName();
                currentWorld = b.getWorldName();
                curblks = blkmaps.get(wname);
                if (curblks == null) {
                    curblks = new ChunkFlags();
                    blkmaps.put(wname, curblks);
                }
            }
            curblks.setFlag(b.getChunk().getX(), b.getChunk().getZ(), true);
            nodevals.addLast(b);
        }
        /* Loop through until we don't find more areas */
        while (nodevals != null) {
            LinkedList<ClaimedChunk> ournodes = null;
            LinkedList<ClaimedChunk> newlist = null;
            ChunkFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for (ClaimedChunk node : nodevals) {
                int nodex = node.getChunk().getX();
                int nodez = node.getChunk().getZ();
                if (ourblks == null) {   /* If not started, switch to world for this block first */
                    if (!node.getWorldName().equalsIgnoreCase(currentWorld)) {
                        currentWorld = node.getWorldName();
                        curblks = blkmaps.get(currentWorld);
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
                if ((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new ChunkFlags();  /* Create map for shape */
                    ournodes = new LinkedList<>();
                    this.floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex;
                    minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if ((ourblks != null) && (node.getWorldName().equalsIgnoreCase(currentWorld)) &&
                        (ourblks.getFlag(nodex, nodez))) {
                    ournodes.add(node);
                    if (nodex < minx) {
                        minx = nodex;
                        minz = nodez;
                    }
                    else if ((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                } else {  /* Else, keep it in the list for the next polygon */
                    if (newlist == null) {
                        newlist = new LinkedList<>();
                    }
                    newlist.add(node);
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */
            if (ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<>();
                linelist.add(new int[]{minx, minz}); // Add start point
                while ((cur_x != minx) || (cur_z != minz) || (dir != direction.ZMINUS)) {
                    switch (dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if (!ourblks.getFlag(cur_x + 1, cur_z)) { /* Right turn? */
                                linelist.add(new int[]{cur_x + 1, cur_z}); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x + 1, cur_z - 1)) {  /* Straight? */
                                cur_x++;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x + 1, cur_z}); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++;
                                cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if (!ourblks.getFlag(cur_x, cur_z + 1)) { /* Right turn? */
                                linelist.add(new int[]{cur_x + 1, cur_z + 1}); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x + 1, cur_z + 1)) {  /* Straight? */
                                cur_z++;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x + 1, cur_z + 1}); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++;
                                cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if (!ourblks.getFlag(cur_x - 1, cur_z)) { /* Right turn? */
                                linelist.add(new int[]{cur_x, cur_z + 1}); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x - 1, cur_z + 1)) {  /* Straight? */
                                cur_x--;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x, cur_z + 1}); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--;
                                cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if (!ourblks.getFlag(cur_x, cur_z - 1)) { /* Right turn? */
                                linelist.add(new int[]{cur_x, cur_z}); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x - 1, cur_z - 1)) {  /* Straight? */
                                cur_z--;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x, cur_z}); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--;
                                cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = name + "__" + type + "__" + poly_index;
                int csize = 16;
                int sz = linelist.size();
                x = new double[sz];
                z = new double[sz];
                for (int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double) line[0] * (double) csize;
                    z[i] = (double) line[1] * (double) csize;
                }
                /* Find existing one */
                AreaMarker m = areaMarkers.remove(polyid); /* Existing area? */
                if (m == null) {
                    m = markerSet.createAreaMarker(polyid, name, false, currentWorld, x, z, false);
                    if (m == null) {
                        this.logger.debug(this.localeService.get("ConsoleAlerts.ErrorAddingAreaMarker".replace("#error#", String.valueOf(polyid))));
                        return;
                    }
                } else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(name);   /* Update label */
                }
                try {
                    int colrCode = Integer.decode(colorCode);
                    if (type.equalsIgnoreCase("realm")) {
                        m.setLineStyle(4, 1.0, colrCode);
                        m.setFillStyle(0.0, colrCode);
                    } else {
                        m.setLineStyle(1, 1.0, colrCode);
                        m.setFillStyle(0.3, colrCode);
                    }
                } catch (Exception e) {
                    this.logger.debug(this.localeService.get("ConsoleAlerts.ErrorSettingAreaMarkerColor".replace("#color#", String.valueOf(colorCode))));
                }
                m.setDescription(popupDescription); /* Set popup */

                /* Add to map */
                newmap.put(polyid, m);
                poly_index++;
            }
        }
    }

    /***
     * Dynmap marker set id (prefix used for other ids/layer ids)
     */
    private String getDynmapPluginSetId(String type) {
        return "mf.faction." + type;
    }

    /***
     * @return Dynmap set Id for faction.
     */
    private String getDynmapFactionSetId(String holder) {
        return this.getDynmapPluginSetId("holder") + "." + holder;
    }

    /***
     * @return Dynmap layer Id for faction.
     */
    private String getDynmapPluginLayer() {
        return this.getDynmapPluginSetId("layer") + "_layer";
    }

    /***
     * @return Dynmap polygon Id corresponding to these chunk
     * coordinates.
     */

    @SuppressWarnings("unused")
    private String getDynmapChunkPolyId(String worldName, int x, int z) {
        // return getDynmapFactionSetId() + "_" + String.format("%d-%d", chunk.getX(), chunk.getZ());
        return this.getDynmapPluginSetId("poly") + "_" + String.format("%d-%d", x, z);
    }

    /***
     *
     * Refreshes the Dynmap Player List for the nation that owns the current chunk.
     */

    @SuppressWarnings("unused")
    private void dynmapUpdateNationPlayerLists(String holder) {
        try {
            String setid = this.getDynmapFactionSetId(holder);
            MarkerAPI markerapi = this.getMarkerAPI();
            Set<String> plids = new HashSet<>();
            Faction f = this.persistentData.getFaction(holder);
            if (f != null) {
                for (PlayerRecord record : this.persistentData.getPlayerRecords()) {
                    Faction pf = this.persistentData.getPlayersFaction(record.getPlayerUUID());
                    if (pf != null && pf.getName().equalsIgnoreCase(holder)) {
                        UUIDChecker uuidChecker = new UUIDChecker();
                        plids.add(uuidChecker.findPlayerNameBasedOnUUID(record.getPlayerUUID()));
                    }
                }
            }
            PlayerSet set = markerapi.getPlayerSet(setid);  /* See if set exists */
            if (set == null) {
                markerapi.createPlayerSet(setid, true, plids, false);
            } else {
                set.setPlayers(plids);
            }
        } catch (Exception e) {
            logger.error("Something went wrong updating a nation's player lists.");
        }
    }

    enum direction {XPLUS, ZPLUS, XMINUS, ZMINUS}
}