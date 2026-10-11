import java.util.HashSet;

public class MaintenanceTeam {

    String teamId;
    String currentLocation;

    boolean available;

    HashSet<String> equipment;

    MaintenanceTeam(String teamId, String currentLocation) {

        this.teamId = teamId;
        this.currentLocation = currentLocation;

        available = true;

        equipment = new HashSet<>();
    }

    // Add equipment
    void addEquipment(String equipmentName) {

        equipment.add(equipmentName);
    }

    // Remove equipment 
    boolean removeEquipment(String equipmentName) {

    return equipment.remove(equipmentName);
    }

    
    // Check required equipment
    boolean hasEquipment(String requiredEquipment) {

        return equipment.contains(requiredEquipment);
    }

    // Assign team to a repair
    void assignJob() {

        available = false;
    }

    // Complete repair
    void completeJob(String newLocation) {

        currentLocation = newLocation;
        available = true;
    }
}