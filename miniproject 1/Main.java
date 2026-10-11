import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;

public class Main {

    static Scanner sc = new Scanner(System.in);

    // Stores all road damage reports
    static ArrayList<RoadDamage> reports = new ArrayList<>();

    // Stores all maintenance teams
    static ArrayList<MaintenanceTeam> teams = new ArrayList<>();

    // Stores road damage reports using Report ID
    static HashMap<String, RoadDamage> reportMap = new HashMap<>();

    // Stores maintenance teams using Team ID
    static HashMap<String, MaintenanceTeam> teamMap = new HashMap<>();

    // Road network
    static RoadNetwork roadNetwork = new RoadNetwork();

    // Stores repair jobs
    static ArrayList<RepairJob> repairJobs = new ArrayList<>();

    // Automatically gives each report an order
    static int reportOrder = 1;


    // ==========================================
    // MAIN METHOD
    // ==========================================

    public static void main(String[] args) {
        RoadApiServer.start(8080);
           
        while (true) {

            System.out.println("\n==========================================");
            System.out.println(" SMART MUNICIPAL ROAD MAINTENANCE SYSTEM");
            System.out.println("          SENTHAMIZH NAGAR");
            System.out.println("==========================================");

            System.out.println("1. Engineering Section");
            System.out.println("2. Ward Officer");
            System.out.println("3. Exit");

            System.out.print("Enter your choice: ");
             int choice = getMenuChoice();
            
            switch (choice) {

                case 1:
                    engineeringMenu();
                    break;

                case 2:
                    wardOfficerMenu();
                    break;

                case 3:
                    System.out.println("\nApplication closed.");
                    sc.close();
                    return;

                default:
                    System.out.println("\nInvalid choice.");
            }
        }
    }


    // ==========================================
    // ENGINEERING SECTION MENU
    // ==========================================

    static void engineeringMenu() {

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
            System.out.println("10. Process Pending Repairs");
            System.out.println("11. Update Repair Completion");
            System.out.println("12. Update Damage Report");
            System.out.println("13. View Repair Jobs");
            System.out.println("14. Find fastest route");
            System.out.println("15. Back");

            System.out.print("Enter your choice: ");
            int choice = getMenuChoice();

            switch (choice) {

    case 1:
        registerMaintenanceTeam();
        break;

    case 2:
        addEquipmentToTeam();
        break;

    case 3:
        removeEquipmentFromTeam();
        break;

    case 4:
        viewTeams();
        break;

    case 5:
        setTeamAvailability();
        break;

    case 6:
        addRoad();
        break;

    case 7:
        roadNetwork.viewRoads();
        break;

    case 8:
        markRoadClosure();
        break;

    case 9:
        viewReports();
        break;

    case 10:
        processPendingRepairs();
        break;

    case 11:
        updateRepairCompletion();
        break;

    case 12:
        updateDamageReport();
        break;

    case 13:
        viewRepairJobs();
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


    // ==========================================
    // WARD OFFICER MENU
    // ==========================================

    static void wardOfficerMenu() {

        while (true) {

            System.out.println("\n==========================================");
            System.out.println("              WARD OFFICER");
            System.out.println("==========================================");

            System.out.println("1. Submit Road Damage Report");
            System.out.println("2. View Submitted Reports");
            System.out.println("3. Back");

            System.out.print("Enter your choice: ");
            int choice = getMenuChoice();

            switch (choice) {

                case 1:
                    submitReport();
                    break;

                case 2:
                    viewReports();
                    break;

                case 3:
                    return;

                default:
                    System.out.println("\nInvalid choice.");
            }
        }
    }


    // ==========================================
    // SUBMIT ROAD DAMAGE REPORT
    // ==========================================

    static void submitReport() {

        sc.nextLine(); // Clear the newline left by the previous menu choice.

        System.out.println("\n==========================================");
        System.out.println("        SUBMIT ROAD DAMAGE REPORT");
        System.out.println("==========================================");

        if (roadNetwork.roads.isEmpty()) {
            System.out.println("No roads are registered yet.");
            System.out.println("Ask the Engineering Section to add roads first.");
            return;
        }

        roadNetwork.viewRoads();
        System.out.print("\nEnter Road ID: ");
        String roadId = sc.nextLine().trim();

        Road selectedRoad = roadNetwork.findRoadById(roadId);
        if (selectedRoad == null) {
            System.out.println("Road ID not found. Please select an existing Road ID.");
            return;
        }

        // Report ID is generated automatically; the Ward Officer does not type it.
        String reportId;
        do {
            reportId = String.format("DR%03d", reportOrder);
            if (reportMap.containsKey(reportId)) {
                reportOrder++;
            } else {
                break;
            }
        } while (true);

        String location = selectedRoad.from + " - " + selectedRoad.to;

        System.out.println("\nRoad details fetched from the stored road network:");
        System.out.println("Road ID       : " + selectedRoad.roadId);
        System.out.println("Starting Point: " + selectedRoad.from);
        System.out.println("Ending Point  : " + selectedRoad.to);
        System.out.println("Travel Time   : " + selectedRoad.travelTime + " minutes");

        System.out.print("\nEnter Damage Type: ");
        String damageType = sc.nextLine().trim();
        if (damageType.isEmpty()) {
            System.out.println("Damage type cannot be empty.");
            return;
        }

        // These four values are retained because the existing priority policy uses them.
        // They cannot be reliably inferred from road endpoints or travel time alone.
        int severity = readRating("Enter Severity (1-3): ");
        int accidentRisk = readRating("Enter Accident Risk (1-3): ");
        int roadImportance = readRating("Enter Road Importance (1-3): ");
        int trafficRank = readRating("Enter Traffic Rank (1-3): ");

        RoadDamage report = new RoadDamage(
                reportId,
                selectedRoad.roadId,
                location,
                selectedRoad.from,
                selectedRoad.to,
                selectedRoad.travelTime,
                damageType,
                severity,
                accidentRisk,
                roadImportance,
                trafficRank,
                reportOrder
        );

        reports.add(report);
        reportMap.put(reportId, report);
        reportOrder++;

        System.out.println("\n------------------------------------------");
        System.out.println("Road damage report submitted successfully.");
        System.out.println("Report ID       : " + report.reportId);
        System.out.println("Road ID         : " + report.roadId);
        System.out.println("Location        : " + report.location);
        System.out.println("Starting Point  : " + report.startLocation);
        System.out.println("Ending Point    : " + report.endLocation);
        System.out.println("Road Travel Time: " + report.roadTravelTime + " minutes");
        System.out.println("Damage Type     : " + report.damageType);
        System.out.println("Submitted On    : " + report.getFormattedSubmissionTime());
        System.out.println("Status          : " + report.status);
        System.out.println("------------------------------------------");
    }

    static int readRating(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (sc.hasNextInt()) {
                int value = sc.nextInt();
                if (value >= 1 && value <= 3) {
                    return value;
                }
                System.out.println("Please enter a value from 1 to 3.");
            } else {
                System.out.println("Invalid input. Please enter a number from 1 to 3.");
            }
            sc.nextLine();
        }
    }

    // ==========================================
    // VIEW ROAD DAMAGE REPORTS
    // ==========================================

    static void viewReports() {

        System.out.println("\n==========================================");
        System.out.println("           ROAD DAMAGE REPORTS");
        System.out.println("==========================================");

        if (reports.isEmpty()) {

            System.out.println("No road damage reports available.");
            return;
        }


        for (RoadDamage report : reports) {

            System.out.println("\n------------------------------------------");

            System.out.println("Report ID       : " + report.reportId);
            System.out.println("Road ID         : " + report.roadId);
            System.out.println("Location        : " + report.location);
            System.out.println("Starting Point  : " + report.startLocation);
            System.out.println("Ending Point    : " + report.endLocation);
            System.out.println("Road Travel Time: " + report.roadTravelTime + " minutes");
            System.out.println("Damage Type     : " + report.damageType);
            System.out.println("Submitted On    : " + report.getFormattedSubmissionTime());
            System.out.println("Severity        : " + report.severity);
            System.out.println("Accident Risk   : " + report.accidentRisk);
            System.out.println("Road Importance : " + report.roadImportance);
            System.out.println("Traffic Rank    : " + report.trafficRank);
            System.out.println("Report Order    : " + report.reportOrder);
            System.out.println("Status          : " + report.status);
        }

        System.out.println("------------------------------------------");
    }


    // ==========================================
    // REGISTER MAINTENANCE TEAM
    // ==========================================

    static void registerMaintenanceTeam() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("       REGISTER MAINTENANCE TEAM");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = sc.nextLine();

        if (teamMap.containsKey(teamId)) {

        System.out.println("Team ID already exists.");
        return;
        }

        System.out.print("Enter Current Location: ");
        String location = sc.nextLine();


        MaintenanceTeam team =
                new MaintenanceTeam(teamId, location);

        teams.add(team);
        teamMap.put(teamId, team);

        System.out.println("\nMaintenance team registered successfully.");
        System.out.println("Team ID      : " + teamId);
        System.out.println("Location     : " + location);
        System.out.println("Availability : AVAILABLE");
    }


    // ==========================================
    // ADD EQUIPMENT TO TEAM
    // ==========================================

    static void addEquipmentToTeam() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("             ADD EQUIPMENT");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = sc.nextLine();


        MaintenanceTeam selectedTeam =
                findTeam(teamId);


        if (selectedTeam == null) {

            System.out.println("Team not found.");
            return;
        }


        System.out.print("Enter Equipment Name: ");
        String equipmentName = sc.nextLine();


        selectedTeam.addEquipment(equipmentName);


        System.out.println("\nEquipment added successfully.");
        System.out.println("Team ID    : " + teamId);
        System.out.println("Equipment  : " + equipmentName);
    }


    // ==========================================
    // FIND TEAM
    // ==========================================

    static MaintenanceTeam findTeam(String teamId) {

         return teamMap.get(teamId);
    }


    // ==========================================
    // VIEW MAINTENANCE TEAMS
    // ==========================================

    static void viewTeams() {

        System.out.println("\n==========================================");
        System.out.println("          MAINTENANCE TEAMS");
        System.out.println("==========================================");


        if (teams.isEmpty()) {

            System.out.println("No maintenance teams registered.");
            return;
        }


        for (MaintenanceTeam team : teams) {

            System.out.println("\n------------------------------------------");

            System.out.println("Team ID          : " + team.teamId);
            System.out.println("Current Location : " + team.currentLocation);

            if (team.available) {

                System.out.println("Availability     : AVAILABLE");

            } else {

                System.out.println("Availability     : BUSY");
            }

            System.out.println("Equipment        : " + team.equipment);
        }

        System.out.println("------------------------------------------");
    }


    // ==========================================
    // SET TEAM AVAILABILITY
    // ==========================================

    static void setTeamAvailability() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("         SET TEAM AVAILABILITY");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = sc.nextLine();


        MaintenanceTeam team =
                findTeam(teamId);


        if (team == null) {

            System.out.println("Team not found.");
            return;
        }


        System.out.println("1. Available");
        System.out.println("2. Busy");

        System.out.print("Enter choice: ");
        int choice = sc.nextInt();


        if (choice == 1) {

            team.available = true;

            System.out.println("Team marked as AVAILABLE.");

        } else if (choice == 2) {

            team.available = false;

            System.out.println("Team marked as BUSY.");

        } else {

            System.out.println("Invalid choice.");
        }
    }


    // ==========================================
    // ADD ROAD
    // ==========================================

    static void addRoad() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("               ADD ROAD");
        System.out.println("==========================================");

        System.out.print("Enter Road ID: ");
        String roadId = sc.nextLine();

        System.out.print("Enter Starting Location: ");
        String from = sc.nextLine();

        System.out.print("Enter Ending Location: ");
        String to = sc.nextLine();

        System.out.print("Enter Travel Time (minutes): ");
        int travelTime = sc.nextInt();


        boolean added = roadNetwork.addRoad(
                roadId,
                from,
                to,
                travelTime
        );

        if (added) {
            System.out.println("\nRoad added successfully and saved to the database.");
        }
    }


    // ==========================================
    // MARK ROAD CLOSURE
    // ==========================================

    static void markRoadClosure() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("             ROAD CLOSURE");
        System.out.println("==========================================");

        System.out.print("Enter Road ID to close: ");
        String roadId = sc.nextLine();


        roadNetwork.markRoadClosed(roadId);
    }


    // ==========================================
    // PROCESS PENDING REPAIRS
    // ==========================================

    static void processPendingRepairs() {

        System.out.println("\n==========================================");
        System.out.println("          PROCESS PENDING REPAIRS");
        System.out.println("==========================================");


        PriorityQueue<RoadDamage> queue =
                new PriorityQueue<>(
                        RepairPriorityPolicy.priorityComparator
                );


        // Add only waiting reports
        for (RoadDamage report : reports) {

            if (report.status.equals("PENDING")) {

                queue.add(report);
            }
        }


        if (queue.isEmpty()) {

            System.out.println("No pending repairs.");
            return;
        }


        System.out.println("\nPriority order:");

        while (!queue.isEmpty()) {

            RoadDamage report = queue.poll();


            System.out.println(
                    "\nReport " + report.reportId +
                    " | Severity: " + report.severity +
                    " | Risk: " + report.accidentRisk
            );


            MaintenanceTeam selectedTeam =
                    findCompatibleTeam(report);


            if (selectedTeam == null) {

                System.out.println(
                        "No compatible available team."
                );

                System.out.println(
                        "Report remains PENDING."
                );

                continue;
            }


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

            job.assignTeam(selectedTeam);

            repairJobs.add(job);


            System.out.println(
                    "Team " + selectedTeam.teamId +
                    " assigned successfully."
            );

            System.out.println(
                    "Required Equipment: " +
                    job.requiredEquipment
            );
        }
    }


    // ==========================================
// FIND COMPATIBLE TEAM
// ==========================================

static MaintenanceTeam findCompatibleTeam(
        RoadDamage report) {

    RepairJob temporaryJob;

    // Create the correct repair type
    if (report.damageType.equalsIgnoreCase("Pothole")) {

        temporaryJob = new PotholeRepair(report);

    } else if (report.damageType.equalsIgnoreCase("Resurfacing")) {

        temporaryJob = new ResurfacingRepair(report);

    } else if (report.damageType.equalsIgnoreCase("Road Crack")) {

        temporaryJob = new RoadCrackRepair(report);

    } else {

        temporaryJob = new RepairJob(report);
    }

    String requiredEquipment =
            temporaryJob.requiredEquipment;

    MaintenanceTeam bestTeam = null;

    int shortestTime = Integer.MAX_VALUE;

    // Check all teams
    for (MaintenanceTeam team : teams) {

        // Check availability and equipment
        if (team.available &&
            team.hasEquipment(requiredEquipment)) {

            // Find fastest route using Dijkstra
            int travelTime =
                    Dijkstra.findShortestDistance(
                            roadNetwork,
                            team.currentLocation,
                            report.location
                    );

            // No reachable route
            if (travelTime == -1) {
                continue;
            }

            // Choose team with shortest travel time
            if (travelTime < shortestTime) {

                shortestTime = travelTime;

                bestTeam = team;
            }
        }
    }

    return bestTeam;
}


    // ==========================================
    // COMPLETE REPAIR
    // ==========================================

    static void updateRepairCompletion() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("          COMPLETE REPAIR");
        System.out.println("==========================================");

        System.out.print("Enter Report ID: ");
        String reportId = sc.nextLine();


        for (RepairJob job : repairJobs) {

            if (job.damage.reportId.equals(reportId) &&
                job.status.equals("ACTIVE")) {


                job.completeRepair();


                System.out.println(
                        "\nRepair completed successfully."
                );

                System.out.println(
                        "Report Status : COMPLETED"
                );

                System.out.println(
                        "Team " +
                        job.assignedTeam.teamId +
                        " is now AVAILABLE."
                );

                return;
            }
        }


        System.out.println(
                "Active repair job not found."
        );
    }


    // ==========================================
    // UPDATE DAMAGE REPORT
    // ==========================================

    static void updateDamageReport() {

        sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("          UPDATE DAMAGE REPORT");
        System.out.println("==========================================");

        System.out.print("Enter Report ID: ");
        String reportId = sc.nextLine();


        for (RoadDamage report : reports) {

            if (report.reportId.equals(reportId)) {


                // Do not modify completed/active jobs
                if (!report.status.equals("PENDING")) {

                    System.out.println(
                            "Only PENDING reports can be updated."
                    );

                    return;
                }


                System.out.print(
                        "Enter New Severity (1-3): "
                );

                int severity = sc.nextInt();


                System.out.print(
                        "Enter New Accident Risk (1-3): "
                );

                int risk = sc.nextInt();


                System.out.print(
                        "Enter New Road Importance (1-3): "
                );

                int importance = sc.nextInt();


                System.out.print(
                        "Enter New Traffic Rank (1-3): "
                );

                int traffic = sc.nextInt();


                report.updateDamage(
                        severity,
                        risk,
                        importance,
                        traffic
                );


                System.out.println(
                        "\nDamage report updated successfully."
                );


                System.out.println(
                        "The report will receive its new priority "
                        + "when pending repairs are processed."
                );

                return;
            }
        }


        System.out.println("Report not found.");
    }


    // ==========================================
    // VIEW REPAIR JOBS
    // ==========================================

    static void viewRepairJobs() {

        System.out.println("\n==========================================");
        System.out.println("             REPAIR JOBS");
        System.out.println("==========================================");


        if (repairJobs.isEmpty()) {

            System.out.println("No repair jobs created.");
            return;
        }


        for (RepairJob job : repairJobs) {

            System.out.println("\n------------------------------------------");

            System.out.println(
                    "Report ID          : " +
                    job.damage.reportId
            );

            System.out.println(
                    "Location           : " +
                    job.damage.location
            );

            System.out.println(
                    "Damage Type        : " +
                    job.damage.damageType
            );

            System.out.println(
                    "Required Equipment : " +
                    job.requiredEquipment
            );


            if (job.assignedTeam != null) {

                System.out.println(
                        "Assigned Team      : " +
                        job.assignedTeam.teamId
                );

            } else {

                System.out.println(
                        "Assigned Team      : NONE"
                );
            }


            System.out.println(
                    "Job Status         : " +
                    job.status
            );
        }

        System.out.println("------------------------------------------");
    }

    static void removeEquipmentFromTeam() {

    sc.nextLine();

    System.out.println("\n==========================================");
    System.out.println("           REMOVE EQUIPMENT");
    System.out.println("==========================================");

    System.out.print("Enter Team ID: ");
    String teamId = sc.nextLine();

    MaintenanceTeam selectedTeam = findTeam(teamId);

    if (selectedTeam == null) {

        System.out.println("Team not found.");
        return;
    }

    System.out.println(
            "Current Equipment: " + selectedTeam.equipment
    );

    System.out.print("Enter Equipment Name to Remove: ");
    String equipmentName = sc.nextLine();

    selectedTeam.removeEquipment(equipmentName);
}

static int getMenuChoice() {

    while (!sc.hasNextInt()) {

        System.out.println("\nInvalid input. Please enter a number.");
        sc.nextLine();

        System.out.print("Enter your choice: ");
    }

    return sc.nextInt();
}

static void removeEquipment() {

    System.out.print("\nEnter Team ID: ");
    String teamId = sc.next();

    MaintenanceTeam team = findTeam(teamId);

    if (team == null) {

        System.out.println("Team not found.");
        return;
    }

    System.out.print("Enter Equipment Name to Remove: ");
    sc.nextLine();
    String equipmentName = sc.nextLine();

    boolean removed = team.removeEquipment(equipmentName);

    if (removed) {

        System.out.println(
                equipmentName + " removed from Team " + teamId + "."
        );

    } else {

        System.out.println(
                equipmentName + " is not assigned to Team " + teamId + "."
        );
    }
}

static void testDijkstra() {

    System.out.println("\n===== FIND FASTEST ROUTE =====");

    System.out.print("Enter starting location: ");
    String start = sc.nextLine();
    
    while (start.trim().isEmpty()) {
    start = sc.nextLine();
    }

    System.out.print("Enter destination: ");
    String destination = sc.nextLine();

     while (destination.trim().isEmpty()) {
        destination = sc.nextLine();
    }

    int shortestTime = Dijkstra.findShortestDistance(
            roadNetwork,
            start,
            destination
    );

    String route =
            Dijkstra.findShortestRoute(
                    roadNetwork,
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
