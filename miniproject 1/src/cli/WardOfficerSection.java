package cli;

import service.ReportManager;

/**
 * Handles the Ward Officer menu and operations.
 */
public class WardOfficerSection {

    public static void showMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("              WARD OFFICER");
            System.out.println("==========================================");

            System.out.println("1. Submit Road Damage Report");
            System.out.println("2. View Submitted Reports");
            System.out.println("3. Back");

            System.out.print("Enter your choice: ");
            int choice = ConsoleHelper.getMenuChoice();

            switch (choice) {
                case 1:
                    ReportManager.submitReport();
                    break;

                case 2:
                    ReportManager.viewReports();
                    break;

                case 3:
                    return;

                default:
                    System.out.println("\nInvalid choice.");
            }
        }
    }
}
