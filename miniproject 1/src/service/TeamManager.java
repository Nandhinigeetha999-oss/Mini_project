package service;

import core.Main;
import model.MaintenanceTeam;

/**
 * Handles maintenance teams registration, equipment assignment, and availability.
 */
public class TeamManager {

    public static MaintenanceTeam findTeam(String teamId) {
        return Main.teamMap.get(teamId);
    }

    public static void registerMaintenanceTeam() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("       REGISTER MAINTENANCE TEAM");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = Main.sc.nextLine();

        if (Main.teamMap.containsKey(teamId)) {
            System.out.println("Team ID already exists.");
            return;
        }

        System.out.print("Enter Current Location: ");
        String location = Main.sc.nextLine();

        MaintenanceTeam team = new MaintenanceTeam(teamId, location);

        Main.teams.add(team);
        Main.teamMap.put(teamId, team);

        System.out.println("\nMaintenance team registered successfully.");
        System.out.println("Team ID      : " + teamId);
        System.out.println("Location     : " + location);
        System.out.println("Availability : AVAILABLE");
    }

    public static void addEquipmentToTeam() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("             ADD EQUIPMENT");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = Main.sc.nextLine();

        MaintenanceTeam selectedTeam = findTeam(teamId);

        if (selectedTeam == null) {
            System.out.println("Team not found.");
            return;
        }

        System.out.print("Enter Equipment Name: ");
        String equipmentName = Main.sc.nextLine();

        selectedTeam.addEquipment(equipmentName);

        System.out.println("\nEquipment added successfully.");
        System.out.println("Team ID    : " + teamId);
        System.out.println("Equipment  : " + equipmentName);
    }

    public static void removeEquipmentFromTeam() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("           REMOVE EQUIPMENT");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = Main.sc.nextLine();

        MaintenanceTeam selectedTeam = findTeam(teamId);

        if (selectedTeam == null) {
            System.out.println("Team not found.");
            return;
        }

        System.out.println("Current Equipment: " + selectedTeam.equipment);

        System.out.print("Enter Equipment Name to Remove: ");
        String equipmentName = Main.sc.nextLine();

        selectedTeam.removeEquipment(equipmentName);
    }

    public static void removeEquipment() {
        System.out.print("\nEnter Team ID: ");
        String teamId = Main.sc.next();

        MaintenanceTeam team = findTeam(teamId);

        if (team == null) {
            System.out.println("Team not found.");
            return;
        }

        System.out.print("Enter Equipment Name to Remove: ");
        Main.sc.nextLine();
        String equipmentName = Main.sc.nextLine();

        boolean removed = team.removeEquipment(equipmentName);

        if (removed) {
            System.out.println(equipmentName + " removed from Team " + teamId + ".");
        } else {
            System.out.println(equipmentName + " is not assigned to Team " + teamId + ".");
        }
    }

    public static void viewTeams() {
        System.out.println("\n==========================================");
        System.out.println("          MAINTENANCE TEAMS");
        System.out.println("==========================================");

        if (Main.teams.isEmpty()) {
            System.out.println("No maintenance teams registered.");
            return;
        }

        for (MaintenanceTeam team : Main.teams) {
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

    public static void setTeamAvailability() {
        Main.sc.nextLine();

        System.out.println("\n==========================================");
        System.out.println("         SET TEAM AVAILABILITY");
        System.out.println("==========================================");

        System.out.print("Enter Team ID: ");
        String teamId = Main.sc.nextLine();

        MaintenanceTeam team = findTeam(teamId);

        if (team == null) {
            System.out.println("Team not found.");
            return;
        }

        System.out.println("1. Available");
        System.out.println("2. Busy");

        System.out.print("Enter choice: ");
        int choice = Main.sc.nextInt();

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
}
