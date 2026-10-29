package cli;

import core.Main;
import routing.Dijkstra;
import service.RepairManager;
import service.ReportManager;
import service.TeamManager;

/**
 * Handles the Engineering Section menu and operations.
 */
public class EngineeringSection {

    public static void showMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("          ENGINEERING SECTION");
            System.out.println("==========================================");

            System.out.println("1. Register Maintenance Team");
            System.out.println("2. Add Equipment to Team");
            System.out.println("3. Remove Equipment from Team");
            System.out.println("4. View Teams");
            System.out.println("5. Set Team Availability");
            System.out.println("6. Add Road");
            System.out.println("7. View Road Network");
            System.out.println("8. Mark Road Closure");
            System.out.println("9. View Road Damage Reports");
            System.out.println("10. Auto-Allot Teams & Tools to Pending Repairs");
            System.out.println("11. Update Repair Completion");
            System.out.println("12. Update Damage Report");
            System.out.println("13. View Repair Jobs");
            System.out.println("14. Find fastest route");
            System.out.println("15. Back");

            System.out.print("Enter your choice: ");
            int choice = ConsoleHelper.getMenuChoice();

            switch (choice) {
                case 1:
                    TeamManager.registerMaintenanceTeam();
                    break;

                case 2:
                    TeamManager.addEquipmentToTeam();
                    break;

                case 3:
                    TeamManager.removeEquipmentFromTeam();
                    break;

                case 4:
                    TeamManager.viewTeams();
                    break;

                case 5:
                    TeamManager.setTeamAvailability();
                    break;

                case 6:
                    addRoad();
                    break;

                case 7:
                    Main.roadNetwork.viewRoads();
                    break;

                case 8:
                    markRoadClosure();
                    break;

                case 9:
                    ReportManager.viewReports();
                    break;

                case 10:
                    RepairManager.processPendingRepairs();
                    break;

                case 11:
                    RepairManager.updateRepairCompletion();
                    break;

                case 12:
                    RepairManager.updateDamageReport();
                    break;

                case 13:
                    RepairManager.viewRepairJobs();
                    break;

                case 14:
                    testDijkstra();
                    break;

                case 15:
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    public static void addRoad() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("               ADD ROAD");
        System.out.println("==========================================");

        System.out.print("Enter Road ID: ");
        String roadId = Main.sc.nextLine();

        System.out.print("Enter Starting Location: ");
        String from = Main.sc.nextLine();

        System.out.print("Enter Ending Location: ");
        String to = Main.sc.nextLine();

        System.out.print("Enter Travel Time (minutes): ");
        int travelTime = Main.sc.nextInt();

        boolean added = Main.roadNetwork.addRoad(
                roadId,
                from,
                to,
                travelTime
        );

        if (added) {
            System.out.println("\nRoad added successfully and saved to the database.");
        }
    }

    public static void markRoadClosure() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("             ROAD CLOSURE");
        System.out.println("==========================================");

        System.out.print("Enter Road ID to close: ");
        String roadId = Main.sc.nextLine();

        Main.roadNetwork.markRoadClosed(roadId);
    }

    public static void testDijkstra() {
        System.out.println("\n===== FIND FASTEST ROUTE =====");

        System.out.print("Enter starting location: ");
        String start = Main.sc.nextLine();

        while (start.trim().isEmpty()) {
            start = Main.sc.nextLine();
        }

        System.out.print("Enter destination: ");
        String destination = Main.sc.nextLine();

        while (destination.trim().isEmpty()) {
            destination = Main.sc.nextLine();
        }

        int shortestTime = Dijkstra.findShortestDistance(
                Main.roadNetwork,
                start,
                destination
        );

        String route = Dijkstra.findShortestRoute(
                Main.roadNetwork,
                start,
                destination
        );

        if (shortestTime == -1 || route == null) {
            System.out.println("No reachable route found.");
        } else {
            System.out.println("\nFastest Route:");
            System.out.println(route);

            System.out.println(
                    "Travel Time: "
                    + shortestTime + " minutes"
            );
        }
    }
}
