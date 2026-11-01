package cli;

import core.Main;
import java.util.Scanner;

/**
 * Handles console input parsing and validation.
 */
public final class ConsoleHelper {

    private ConsoleHelper() {
    }

    public static int getMenuChoice() {
        while (!Main.sc.hasNextInt()) {
            if (!Main.sc.hasNext()) {
                throw new java.util.NoSuchElementException("EOF reached on console input");
            }
            System.out.println("\nInvalid input. Please enter a number.");
            Main.sc.nextLine();
            System.out.print("Enter your choice: ");
        }
        return Main.sc.nextInt();
    }

    public static int readRating(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!Main.sc.hasNext()) {
                throw new java.util.NoSuchElementException("EOF reached on console input");
            }
            if (Main.sc.hasNextInt()) {
                int value = Main.sc.nextInt();
                if (value >= 1 && value <= 3) {
                    return value;
                }
                System.out.println("Please enter a value from 1 to 3.");
            } else {
                System.out.println("Invalid input. Please enter a number from 1 to 3.");
            }
            Main.sc.nextLine();
        }
    }
}
