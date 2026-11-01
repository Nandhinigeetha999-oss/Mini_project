/**
 * Top-level entry point bridging to core.Main for seamless classpath execution.
 */
public class Main {

    public static java.util.Scanner sc = core.Main.sc;
    public static java.util.ArrayList<model.RoadDamage> reports = core.Main.reports;
    public static java.util.ArrayList<model.MaintenanceTeam> teams = core.Main.teams;
    public static java.util.HashMap<String, model.RoadDamage> reportMap = core.Main.reportMap;
    public static java.util.HashMap<String, model.MaintenanceTeam> teamMap = core.Main.teamMap;
    public static routing.RoadNetwork roadNetwork = core.Main.roadNetwork;
    public static java.util.ArrayList<model.RepairJob> repairJobs = core.Main.repairJobs;
    public static int reportOrder = core.Main.reportOrder;

    public static void main(String[] args) {
        core.Main.main(args);
    }

    public static synchronized void addReportToMemory(model.RoadDamage report) {
        core.Main.addReportToMemory(report);
    }

    public static void engineeringMenu() {
        core.Main.engineeringMenu();
    }

    public static void wardOfficerMenu() {
        core.Main.wardOfficerMenu();
    }

    public static void submitReport() {
        core.Main.submitReport();
    }

    public static void viewReports() {
        core.Main.viewReports();
    }

    public static int readRating(String prompt) {
        return core.Main.readRating(prompt);
    }

    public static int getMenuChoice() {
        return core.Main.getMenuChoice();
    }

    public static void registerMaintenanceTeam() {
        core.Main.registerMaintenanceTeam();
    }

    public static void addEquipmentToTeam() {
        core.Main.addEquipmentToTeam();
    }

    public static void removeEquipmentFromTeam() {
        core.Main.removeEquipmentFromTeam();
    }

    public static void removeEquipment() {
        core.Main.removeEquipment();
    }

    public static model.MaintenanceTeam findTeam(String teamId) {
        return core.Main.findTeam(teamId);
    }

    public static void viewTeams() {
        core.Main.viewTeams();
    }

    public static void setTeamAvailability() {
        core.Main.setTeamAvailability();
    }

    public static void addRoad() {
        core.Main.addRoad();
    }

    public static void markRoadClosure() {
        core.Main.markRoadClosure();
    }

    public static void processPendingRepairs() {
        core.Main.processPendingRepairs();
    }

    public static model.MaintenanceTeam findCompatibleTeam(model.RoadDamage report) {
        return core.Main.findCompatibleTeam(report);
    }

    public static void updateRepairCompletion() {
        core.Main.updateRepairCompletion();
    }

    public static void updateDamageReport() {
        core.Main.updateDamageReport();
    }

    public static void viewRepairJobs() {
        core.Main.viewRepairJobs();
    }

    public static void testDijkstra() {
        core.Main.testDijkstra();
    }
}
