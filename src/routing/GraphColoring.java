package routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import model.Road;

/**
 * Implements Graph Coloring algorithms for the Municipal Road Network:
 * 1. Road Maintenance Phasing (Line Graph Coloring):
 *    Partitions road repairs into conflict-free phases (time slots) where
 *    no two roads undergoing repair share an intersection junction.
 * 2. Location Zone Coloring (Vertex Coloring):
 *    Colors adjacent municipal locations so neighboring junctions belong
 *    to distinct monitoring/patrol zones.
 */
public class GraphColoring {

    public static final String[] COLOR_PALETTE = {
        "#10b981", // Phase 1: Emerald Green
        "#3b82f6", // Phase 2: Vibrant Blue
        "#f59e0b", // Phase 3: Amber Orange
        "#8b5cf6", // Phase 4: Violet Purple
        "#ec4899", // Phase 5: Rose Pink
        "#06b6d4", // Phase 6: Cyan Teal
        "#e11d48", // Phase 7: Crimson Red
        "#84cc16"  // Phase 8: Lime Green
    };

    public static String getColorHex(int colorIndex) {
        if (colorIndex <= 0) return "#94a3b8";
        int idx = (colorIndex - 1) % COLOR_PALETTE.length;
        return COLOR_PALETTE[idx];
    }

    // ==========================================
    // RESULT CONTAINERS
    // ==========================================

    public static class ScheduleResult {
        public int chromaticNumber;
        public Map<String, Integer> roadToPhase = new LinkedHashMap<>();
        public Map<String, String> roadToColorHex = new LinkedHashMap<>();
        public Map<Integer, List<Road>> phaseGroups = new TreeMap<>();
        public Map<String, List<String>> conflictAdjacency = new LinkedHashMap<>();
    }

    public static class LocationColoringResult {
        public int chromaticNumber;
        public Map<String, Integer> locationToZone = new LinkedHashMap<>();
        public Map<String, String> locationToColorHex = new LinkedHashMap<>();
        public Map<Integer, List<String>> zoneGroups = new TreeMap<>();
    }

    // ==========================================
    // ROAD CONFLICT-FREE REPAIR PHASING (LINE GRAPH)
    // ==========================================

    /**
     * Schedules roads into conflict-free maintenance phases using the
     * Welsh-Powell Graph Coloring algorithm on the conflict/intersection graph.
     * Two roads conflict if they share either junction (from or to).
     */
    public static ScheduleResult scheduleRoadRepairs(List<Road> roads) {
        ScheduleResult result = new ScheduleResult();
        if (roads == null || roads.isEmpty()) {
            return result;
        }

        // Map for quick road lookup by ID
        Map<String, Road> roadLookup = new HashMap<>();
        for (Road r : roads) {
            roadLookup.put(r.roadId, r);
        }

        // 1. Build Conflict Graph (Adjacency List)
        // Two roads are adjacent if they meet at any common location
        Map<String, Set<String>> conflictGraph = new LinkedHashMap<>();
        for (Road r : roads) {
            conflictGraph.put(r.roadId, new HashSet<>());
        }

        for (int i = 0; i < roads.size(); i++) {
            Road r1 = roads.get(i);
            for (int j = i + 1; j < roads.size(); j++) {
                Road r2 = roads.get(j);
                if (r1.from.equalsIgnoreCase(r2.from) || r1.from.equalsIgnoreCase(r2.to) ||
                    r1.to.equalsIgnoreCase(r2.from)   || r1.to.equalsIgnoreCase(r2.to)) {
                    conflictGraph.get(r1.roadId).add(r2.roadId);
                    conflictGraph.get(r2.roadId).add(r1.roadId);
                }
            }
        }

        // Store conflicts as sorted lists for JSON/display
        for (Map.Entry<String, Set<String>> entry : conflictGraph.entrySet()) {
            List<String> sortedConflicts = new ArrayList<>(entry.getValue());
            Collections.sort(sortedConflicts);
            result.conflictAdjacency.put(entry.getKey(), sortedConflicts);
        }

        // 2. Welsh-Powell: Sort vertices by degree in descending order
        List<String> sortedRoads = new ArrayList<>(conflictGraph.keySet());
        sortedRoads.sort((a, b) -> {
            int degCompare = Integer.compare(conflictGraph.get(b).size(), conflictGraph.get(a).size());
            if (degCompare != 0) return degCompare;
            return a.compareTo(b);
        });

        // 3. Assign colors sequentially (First-Fit heuristic with Welsh-Powell ordering)
        int maxColor = 0;
        for (String roadId : sortedRoads) {
            Set<Integer> neighborColors = new HashSet<>();
            for (String neighborId : conflictGraph.get(roadId)) {
                if (result.roadToPhase.containsKey(neighborId)) {
                    neighborColors.add(result.roadToPhase.get(neighborId));
                }
            }

            int assignedPhase = 1;
            while (neighborColors.contains(assignedPhase)) {
                assignedPhase++;
            }

            result.roadToPhase.put(roadId, assignedPhase);
            result.roadToColorHex.put(roadId, getColorHex(assignedPhase));

            Road r = roadLookup.get(roadId);
            result.phaseGroups.computeIfAbsent(assignedPhase, k -> new ArrayList<>()).add(r);
            maxColor = Math.max(maxColor, assignedPhase);
        }

        result.chromaticNumber = maxColor;
        return result;
    }

    // ==========================================
    // LOCATION / JUNCTION ZONE COLORING (VERTEX GRAPH)
    // ==========================================

    /**
     * Colors adjacent municipal locations (junctions) so no two connected
     * locations share the same monitoring/patrol zone.
     */
    public static LocationColoringResult colorLocations(List<Road> roads) {
        LocationColoringResult result = new LocationColoringResult();
        if (roads == null || roads.isEmpty()) {
            return result;
        }

        // 1. Build Location Adjacency Graph
        Map<String, Set<String>> locationGraph = new LinkedHashMap<>();
        for (Road road : roads) {
            locationGraph.putIfAbsent(road.from, new HashSet<>());
            locationGraph.putIfAbsent(road.to, new HashSet<>());
            locationGraph.get(road.from).add(road.to);
            locationGraph.get(road.to).add(road.from);
        }

        // 2. Welsh-Powell: Sort locations by degree in descending order
        List<String> sortedLocations = new ArrayList<>(locationGraph.keySet());
        sortedLocations.sort((a, b) -> {
            int degCompare = Integer.compare(locationGraph.get(b).size(), locationGraph.get(a).size());
            if (degCompare != 0) return degCompare;
            return a.compareTo(b);
        });

        // 3. Assign zones/colors
        int maxZone = 0;
        for (String location : sortedLocations) {
            Set<Integer> neighborZones = new HashSet<>();
            for (String neighbor : locationGraph.get(location)) {
                if (result.locationToZone.containsKey(neighbor)) {
                    neighborZones.add(result.locationToZone.get(neighbor));
                }
            }

            int assignedZone = 1;
            while (neighborZones.contains(assignedZone)) {
                assignedZone++;
            }

            result.locationToZone.put(location, assignedZone);
            result.locationToColorHex.put(location, getColorHex(assignedZone));
            result.zoneGroups.computeIfAbsent(assignedZone, k -> new ArrayList<>()).add(location);
            maxZone = Math.max(maxZone, assignedZone);
        }

        result.chromaticNumber = maxZone;
        return result;
    }
}
