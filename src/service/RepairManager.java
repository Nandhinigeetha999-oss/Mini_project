package service;

import core.Main;
import db.DatabaseManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import model.MaintenanceTeam;
import model.PotholeRepair;
import model.RepairJob;
import model.ResurfacingRepair;
import model.RoadCrackRepair;
import model.Road;
import model.RoadDamage;
import policy.RepairPriorityPolicy;
import routing.Dijkstra;
import routing.GraphColoring;
import java.util.List;
import java.util.Map;

/**
 * Handles automated repair prioritization, automatic team matching with Dijkstra,
 * automatic tool assignment, and job lifecycle management.
 */
public class RepairManager {

    /**
     * Determines the required primary tool and related equipment for a damage type.
     */
    public static String determineRequiredEquipment(String damageType) {
        if (damageType == null) {
            return "General Equipment";
        }
        String normalized = damageType.trim().toLowerCase();
        if (normalized.contains("pothole")) {
            return "Pothole Kit";
        } else if (normalized.contains("crack")) {
            return "Road Cutter";
        } else if (normalized.contains("resurfacing")) {
            return "Resurfacing Machine";
        }
        return "General Equipment";
    }

    /**
     * Automatically fetches details, selects the closest available maintenance team via Dijkstra,
     * equips them with the necessary tools, and allots the job.
     */
    public static class AllotmentSummary {
        public int allottedCount = 0;
        public int pendingCount = 0;
        public List<String> allottedDetails = new ArrayList<>();
        public String message = "";
    }

    public static boolean isTeamReallyAvailable(MaintenanceTeam team) {
        if (team == null || !team.available) {
            return false;
        }
        String tid = team.teamId.trim().toUpperCase(Locale.ROOT);
        for (RoadDamage r : Main.reports) {
            if ("ACTIVE".equalsIgnoreCase(r.status) && r.assignedTeam != null && tid.equalsIgnoreCase(r.assignedTeam.trim().replaceAll("(?i)^Team\\s*", ""))) {
                return false;
            }
        }
        for (RepairJob job : Main.repairJobs) {
            if ("ACTIVE".equalsIgnoreCase(job.status) && job.assignedTeam != null && tid.equalsIgnoreCase(job.assignedTeam.teamId.trim().replaceAll("(?i)^Team\\s*", ""))) {
                return false;
            }
        }
        return true;
    }

    public static synchronized void reconcileTeamAssignments() {
        reconcileAndAutoAllot();
    }

    public static synchronized AllotmentSummary reconcileAndAutoAllot() {
        try {
            ArrayList<RoadDamage> dbReports = DatabaseManager.loadDamageReports();
            Main.reports.clear();
            Main.reportMap.clear();
            for (RoadDamage r : dbReports) {
                Main.reports.add(r);
                Main.reportMap.put(r.reportId, r);
            }
            ArrayList<MaintenanceTeam> dbTeams = DatabaseManager.loadMaintenanceTeams();
            if (!dbTeams.isEmpty()) {
                Main.teams.clear();
                Main.teamMap.clear();
                for (MaintenanceTeam t : dbTeams) {
                    Main.teams.add(t);
                    Main.teamMap.put(t.teamId.trim().toUpperCase(Locale.ROOT), t);
                }
            }
        } catch (SQLException ignored) { }

        ensureTeamsInitialized();

        Set<String> activeAllottedTeams = new HashSet<>();
        List<RoadDamage> duplicateReports = new ArrayList<>();

        for (RoadDamage report : Main.reports) {
            if ("ACTIVE".equalsIgnoreCase(report.status) && report.assignedTeam != null && !report.assignedTeam.toLowerCase().contains("unassigned")) {
                String tid = report.assignedTeam.trim().replaceAll("(?i)^Team\\s*", "").toUpperCase(Locale.ROOT);
                if (activeAllottedTeams.contains(tid)) {
                    duplicateReports.add(report);
                } else {
                    activeAllottedTeams.add(tid);
                }
            }
        }

        for (RoadDamage dup : duplicateReports) {
            dup.status = "PENDING";
            dup.assignedTeam = "Unassigned (Queued)";
            try {
                DatabaseManager.updateDamageReportAssignment(dup.reportId, "Unassigned (Queued)", dup.assignedEquipment, "PENDING");
            } catch (SQLException ignored) { }
        }

        for (MaintenanceTeam team : Main.teams) {
            String tid = team.teamId.trim().toUpperCase(Locale.ROOT);
            boolean isAssigned = activeAllottedTeams.contains(tid);
            team.available = !isAssigned;
            try {
                DatabaseManager.saveOrUpdateMaintenanceTeam(team);
            } catch (SQLException ignored) { }
        }

        Main.repairJobs.clear();
        for (RoadDamage report : Main.reports) {
            String tid = (report.assignedTeam != null && !report.assignedTeam.toLowerCase().contains("unassigned"))
                ? report.assignedTeam.trim().replaceAll("(?i)^Team\\s*", "").toUpperCase(Locale.ROOT)
                : null;
            MaintenanceTeam team = tid != null ? Main.teamMap.get(tid) : null;
            RepairJob job;
            if (report.damageType.equalsIgnoreCase("Pothole")) {
                job = new PotholeRepair(report);
            } else if (report.damageType.equalsIgnoreCase("Resurfacing")) {
                job = new ResurfacingRepair(report);
            } else if (report.damageType.equalsIgnoreCase("Road Crack")) {
                job = new RoadCrackRepair(report);
            } else {
                job = new RepairJob(report);
            }
            if (team != null) {
                job.assignTeam(team);
            }
            job.status = report.status;
            job.requiredEquipment = report.assignedEquipment;
            Main.repairJobs.add(job);
        }

        AllotmentSummary summary = new AllotmentSummary();
        PriorityQueue<RoadDamage> queue = new PriorityQueue<>(RepairPriorityPolicy.priorityComparator);

        for (RoadDamage report : Main.reports) {
            if (!"COMPLETED".equalsIgnoreCase(report.status)) {
                boolean isUnassigned = report.assignedTeam == null
                        || report.assignedTeam.trim().isEmpty()
                        || report.assignedTeam.toLowerCase().contains("unassigned")
                        || "PENDING".equalsIgnoreCase(report.status);
                if (isUnassigned) {
                    queue.add(report);
                }
            }
        }

        summary.pendingCount = queue.size();

        if (queue.isEmpty()) {
            summary.message = "No pending repairs waiting for allotment. All tasks are active or completed.";
            return summary;
        }

        while (!queue.isEmpty()) {
            RoadDamage report = queue.poll();
            RepairJob job = autoAllotTeamAndTools(report);
            if (job != null && job.assignedTeam != null) {
                summary.allottedCount++;
                summary.allottedDetails.add("Team " + job.assignedTeam.teamId + " -> " + report.reportId + " (" + report.damageType + ")");
            }
        }

        if (summary.allottedCount > 0) {
            summary.message = "Auto-allotted " + summary.allottedCount + " team(s): " + String.join(", ", summary.allottedDetails);
        } else {
            summary.message = "All maintenance teams are currently busy. " + summary.pendingCount + " pending repair(s) queued for next available team.";
        }

        return summary;
    }

    public static synchronized RepairJob autoAllotTeamAndTools(RoadDamage report) {
        if (report == null) {
            return null;
        }

        ensureTeamsInitialized();

        String requiredEquipment = determineRequiredEquipment(report.damageType);
        MaintenanceTeam bestTeam = null;
        int shortestTime = Integer.MAX_VALUE;

        // Find the best available team using shortest Dijkstra travel time
        for (MaintenanceTeam team : Main.teams) {
            if (!isTeamReallyAvailable(team)) {
                continue;
            }

            int travelTime = calculateTravelTime(team.currentLocation, report);
            if (travelTime >= 0 && travelTime < shortestTime) {
                shortestTime = travelTime;
                bestTeam = team;
            } else if (bestTeam == null) {
                // Fallback available team if no network path found
                bestTeam = team;
            }
        }

        if (bestTeam == null) {
            report.status = "PENDING";
            report.assignedTeam = "Unassigned (Queued)";
            report.assignedEquipment = requiredEquipment;
            try {
                DatabaseManager.updateDamageReportAssignment(
                        report.reportId, "Unassigned (Queued)", requiredEquipment, "PENDING");
            } catch (SQLException ignored) { }
            return null;
        }

        // 1. Automatically allot tools to the team
        bestTeam.addEquipment(requiredEquipment);

        // Fetch additional catalog tools for this damage type and equip the team
        try {
            ArrayList<String> catalogTools = DatabaseManager.loadEquipmentCatalog(report.damageType);
            for (String tool : catalogTools) {
                bestTeam.addEquipment(tool);
            }
        } catch (SQLException ignored) { }

        // 2. Create the appropriate repair job
        RepairJob job;
        if (report.damageType.equalsIgnoreCase("Pothole")) {
            job = new PotholeRepair(report);
        } else if (report.damageType.equalsIgnoreCase("Resurfacing")) {
            job = new ResurfacingRepair(report);
        } else if (report.damageType.equalsIgnoreCase("Road Crack")) {
            job = new RoadCrackRepair(report);
        } else {
            job = new RepairJob(report);
        }

        // 3. Automatically allot the team and update status
        job.assignTeam(bestTeam);
        report.status = "ACTIVE";
        report.assignedTeam = bestTeam.teamId;
        report.assignedEquipment = requiredEquipment;

        // Ensure job is tracked in memory
        Main.repairJobs.removeIf(j -> j.damage.reportId.equalsIgnoreCase(report.reportId));
        Main.repairJobs.add(job);

        // 4. Persist allotments in SQLite database
        try {
            DatabaseManager.updateDamageReportAssignment(
                    report.reportId, bestTeam.teamId, requiredEquipment, "ACTIVE");
            DatabaseManager.saveOrUpdateMaintenanceTeam(bestTeam);
        } catch (SQLException exception) {
            System.err.println("Could not persist automatic allotment: " + exception.getMessage());
        }

        return job;
    }

    /**
     * Automatically processes and allots all pending damage reports to available teams.
     */
    public static synchronized void autoAllotAllPendingRepairs() {
        ensureTeamsInitialized();
        PriorityQueue<RoadDamage> queue = new PriorityQueue<>(RepairPriorityPolicy.priorityComparator);

        for (RoadDamage report : Main.reports) {
            if (!"COMPLETED".equalsIgnoreCase(report.status)) {
                boolean isUnassigned = report.assignedTeam == null
                        || report.assignedTeam.trim().isEmpty()
                        || report.assignedTeam.toLowerCase().contains("unassigned")
                        || "PENDING".equalsIgnoreCase(report.status);
                if (isUnassigned) {
                    queue.add(report);
                }
            }
        }

        while (!queue.isEmpty()) {
            RoadDamage report = queue.poll();
            autoAllotTeamAndTools(report);
        }
    }

    public static void processPendingRepairs() {
        System.out.println("\n==========================================");
        System.out.println("       AUTOMATED PENDING REPAIR ALLOTMENT");
        System.out.println("==========================================");

        PriorityQueue<RoadDamage> queue =
                new PriorityQueue<>(RepairPriorityPolicy.priorityComparator);

        for (RoadDamage report : Main.reports) {
            if (!"COMPLETED".equalsIgnoreCase(report.status)) {
                boolean isUnassigned = report.assignedTeam == null
                        || report.assignedTeam.trim().isEmpty()
                        || report.assignedTeam.toLowerCase().contains("unassigned")
                        || "PENDING".equalsIgnoreCase(report.status);
                if (isUnassigned) {
                    queue.add(report);
                }
            }
        }

        if (queue.isEmpty()) {
            System.out.println("No pending repairs waiting for allotment.");
            return;
        }

        System.out.println("Processing " + queue.size() + " pending report(s) by priority:");

        while (!queue.isEmpty()) {
            RoadDamage report = queue.poll();
            System.out.println("\nProcessing Report: " + report.reportId + " (" + report.damageType + ")");
            System.out.println("  Location   : " + report.location);
            System.out.println("  Severity   : " + report.severity + " | Risk: " + report.accidentRisk);

            RepairJob job = autoAllotTeamAndTools(report);
            if (job == null) {
                System.out.println("  Status     : All maintenance teams are currently busy. Queued for next team.");
            } else {
                System.out.println("  -> Allotted Team  : Team " + job.assignedTeam.teamId);
                System.out.println("  -> Allotted Tools : " + job.requiredEquipment);
                System.out.println("  -> Job Status     : ACTIVE");
            }
        }
    }

    public static MaintenanceTeam findCompatibleTeam(RoadDamage report) {
        String requiredEquipment = determineRequiredEquipment(report.damageType);
        MaintenanceTeam bestTeam = null;
        int shortestTime = Integer.MAX_VALUE;

        for (MaintenanceTeam team : Main.teams) {
            if (isTeamReallyAvailable(team)) {
                int travelTime = calculateTravelTime(team.currentLocation, report);
                if (travelTime >= 0 && travelTime < shortestTime) {
                    shortestTime = travelTime;
                    bestTeam = team;
                }
            }
        }

        return bestTeam;
    }

    private static int calculateTravelTime(String fromLocation, RoadDamage report) {
        if (fromLocation == null || report == null) {
            return -1;
        }

        fromLocation = fromLocation.trim();
        if (report.location != null && fromLocation.equalsIgnoreCase(report.location.trim())) {
            return 0;
        }
        if (report.startLocation != null && fromLocation.equalsIgnoreCase(report.startLocation.trim())) {
            return 0;
        }
        if (report.endLocation != null && fromLocation.equalsIgnoreCase(report.endLocation.trim())) {
            return 0;
        }

        if (fromLocation.contains(" - ")) {
            String[] parts = fromLocation.split(" - ", 2);
            String p1 = parts[0].trim();
            String p2 = parts[1].trim();
            int t1 = Dijkstra.findShortestDistance(Main.roadNetwork, p1, report.startLocation);
            int t2 = Dijkstra.findShortestDistance(Main.roadNetwork, p2, report.startLocation);
            if (t1 >= 0 && t2 >= 0) return Math.min(t1, t2);
            if (t1 >= 0) return t1;
            if (t2 >= 0) return t2;
        }

        int travelTime = Dijkstra.findShortestDistance(Main.roadNetwork, fromLocation, report.startLocation);
        if (travelTime == -1 && report.endLocation != null) {
            travelTime = Dijkstra.findShortestDistance(Main.roadNetwork, fromLocation, report.endLocation);
        }
        if (travelTime == -1 && report.location != null) {
            travelTime = Dijkstra.findShortestDistance(Main.roadNetwork, fromLocation, report.location);
        }
        return travelTime;
    }

    public static synchronized void updateJobStatus(String reportId, String status) {
        if (reportId == null || status == null) return;
        String normStatus = status.trim().toUpperCase(Locale.ROOT);

        if ("COMPLETED".equals(normStatus)) {
            completeRepair(reportId);
            return;
        }

        RoadDamage targetReport = null;
        for (RoadDamage r : Main.reports) {
            if (r.reportId.equalsIgnoreCase(reportId.trim())) {
                targetReport = r;
                break;
            }
        }

        if (targetReport != null) {
            targetReport.status = normStatus;
            if ("PENDING".equals(normStatus)) {
                targetReport.assignedTeam = "Unassigned (Queued)";
                try {
                    DatabaseManager.updateDamageReportAssignment(
                        targetReport.reportId, "Unassigned (Queued)", targetReport.assignedEquipment, "PENDING");
                } catch (SQLException ignored) { }
            } else {
                try {
                    DatabaseManager.updateDamageReportStatus(targetReport.reportId, normStatus);
                } catch (SQLException ignored) { }
            }
        } else {
            try {
                DatabaseManager.updateDamageReportStatus(reportId, normStatus);
            } catch (SQLException ignored) { }
        }

        for (RepairJob job : Main.repairJobs) {
            if (job.damage.reportId.equalsIgnoreCase(reportId.trim())) {
                job.status = normStatus;
                job.damage.status = normStatus;
                if ("PENDING".equals(normStatus) && job.assignedTeam != null) {
                    job.assignedTeam.available = true;
                } else if ("ACTIVE".equals(normStatus) && job.assignedTeam != null) {
                    job.assignedTeam.available = false;
                }
                break;
            }
        }

        if ("ACTIVE".equals(normStatus)) {
            if (targetReport != null && (targetReport.assignedTeam == null || targetReport.assignedTeam.toLowerCase().contains("unassigned"))) {
                autoAllotTeamAndTools(targetReport);
            }
        } else if ("PENDING".equals(normStatus)) {
            reconcileAndAutoAllot();
        }
    }

    public static synchronized void completeRepair(String reportId) {
        if (reportId == null) return;

        RoadDamage targetReport = null;
        for (RoadDamage r : Main.reports) {
            if (r.reportId.equalsIgnoreCase(reportId.trim())) {
                targetReport = r;
                break;
            }
        }

        for (RepairJob job : Main.repairJobs) {
            if (job.damage.reportId.equalsIgnoreCase(reportId.trim())) {
                job.completeRepair();
                if (job.assignedTeam != null) {
                    try {
                        DatabaseManager.saveOrUpdateMaintenanceTeam(job.assignedTeam);
                    } catch (SQLException ignored) { }
                }
                break;
            }
        }

        if (targetReport != null) {
            targetReport.status = "COMPLETED";
            try {
                DatabaseManager.updateDamageReportStatus(targetReport.reportId, "COMPLETED");
            } catch (SQLException ignored) { }
        }

        // When a team completes a job and becomes free, automatically allot next queued pending repair!
        autoAllotAllPendingRepairs();
    }

    public static void updateRepairCompletion() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("          COMPLETE REPAIR");
        System.out.println("==========================================");

        System.out.print("Enter Report ID: ");
        String reportId = Main.sc.nextLine();

        completeRepair(reportId);
        System.out.println("\nRepair marked as COMPLETED. Team is now available and queued reports were re-checked.");
    }

    public static void updateDamageReport() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("          UPDATE DAMAGE REPORT");
        System.out.println("==========================================");

        System.out.print("Enter Report ID: ");
        String reportId = Main.sc.nextLine();

        for (RoadDamage report : Main.reports) {
            if (report.reportId.equalsIgnoreCase(reportId)) {

                if (!"PENDING".equalsIgnoreCase(report.status)) {
                    System.out.println("Only PENDING reports can be updated.");
                    return;
                }

                System.out.print("Enter New Severity (1-3): ");
                int severity = Main.sc.nextInt();

                System.out.print("Enter New Accident Risk (1-3): ");
                int risk = Main.sc.nextInt();

                System.out.print("Enter New Road Importance (1-3): ");
                int importance = Main.sc.nextInt();

                System.out.print("Enter New Traffic Rank (1-3): ");
                int traffic = Main.sc.nextInt();

                report.updateDamage(severity, risk, importance, traffic);

                System.out.println("\nDamage report updated successfully.");
                System.out.println("The report will receive its new priority when pending repairs are processed.");
                return;
            }
        }

        System.out.println("Report not found.");
    }

    public static void viewRepairJobs() {
        System.out.println("\n==========================================");
        System.out.println("             REPAIR JOBS");
        System.out.println("==========================================");

        if (Main.repairJobs.isEmpty()) {
            System.out.println("No repair jobs created.");
            return;
        }

        for (RepairJob job : Main.repairJobs) {
            System.out.println("\n------------------------------------------");
            System.out.println("Report ID          : " + job.damage.reportId);
            System.out.println("Location           : " + job.damage.location);
            System.out.println("Damage Type        : " + job.damage.damageType);
            System.out.println("Allotted Equipment : " + job.requiredEquipment);

            if (job.assignedTeam != null) {
                System.out.println("Allotted Team      : " + job.assignedTeam.teamId
                        + " (Base: " + job.assignedTeam.currentLocation + ")");
            } else {
                System.out.println("Allotted Team      : NONE");
            }

            System.out.println("Job Status         : " + job.status);
        }

        System.out.println("------------------------------------------");
    }

    private static void ensureTeamsInitialized() {
        if (Main.teams.isEmpty()) {
            try {
                ArrayList<MaintenanceTeam> dbTeams = DatabaseManager.loadMaintenanceTeams();
                for (MaintenanceTeam team : dbTeams) {
                    Main.teams.add(team);
                    Main.teamMap.put(team.teamId, team);
                }
            } catch (SQLException ignored) { }

            if (Main.teams.isEmpty()) {
                registerTeam("T1", "City Hospital", "Pothole Kit", "Road Cutter");
                registerTeam("T2", "Market Junction", "Resurfacing Machine");
                registerTeam("T3", "Sports Ground", "Road Cutter", "Pothole Kit");
                registerTeam("T4", "Bus Stand", "General Equipment", "Pothole Kit");
            }
        }
    }

    private static void registerTeam(String teamId, String location, String... equipment) {
        MaintenanceTeam team = new MaintenanceTeam(teamId, location);
        for (String eq : equipment) {
            team.addEquipment(eq);
        }
        Main.teams.add(team);
        Main.teamMap.put(teamId, team);
        try {
            DatabaseManager.saveOrUpdateMaintenanceTeam(team);
        } catch (SQLException ignored) { }
    }

    /**
     * Calculates the conflict-free maintenance schedule for all roads using Graph Coloring.
     */
    public static GraphColoring.ScheduleResult getConflictFreeSchedule() {
        return GraphColoring.scheduleRoadRepairs(Main.roadNetwork.roads);
    }

    /**
     * Displays the conflict-free maintenance phases in the console.
     */
    public static void displayConflictFreePhases() {
        GraphColoring.ScheduleResult result = getConflictFreeSchedule();

        System.out.println("\n=======================================================");
        System.out.println(" CONFLICT-FREE ROAD MAINTENANCE PHASING (GRAPH COLORING)");
        System.out.println("=======================================================");
        System.out.println("Total Roads Analyzed    : " + Main.roadNetwork.roads.size());
        System.out.println("Chromatic Number (Chi)  : " + result.chromaticNumber + " Distinct Phases");
        System.out.println("Algorithm Heuristic     : Welsh-Powell (First-Fit on Degree Ordering)");
        System.out.println("Constraint              : No two roads sharing an intersection");
        System.out.println("                          are scheduled in the same phase.");

        for (Map.Entry<Integer, List<Road>> entry : result.phaseGroups.entrySet()) {
            int phase = entry.getKey();
            List<Road> roadsInPhase = entry.getValue();
            String colorHex = GraphColoring.getColorHex(phase);

            System.out.println("\n-------------------------------------------------------");
            System.out.println(" PHASE " + phase + " [Color: " + colorHex + "] - " + roadsInPhase.size() + " Road(s) Concurrent");
            System.out.println("-------------------------------------------------------");

            for (Road r : roadsInPhase) {
                System.out.println(String.format("  • %-6s : %-20s <---> %-20s (%d mins)",
                        r.roadId, r.from, r.to, r.travelTime));
            }
        }
        System.out.println("=======================================================");
    }
}
