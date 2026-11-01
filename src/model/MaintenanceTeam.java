package model;

import java.util.HashSet;

public class MaintenanceTeam {

    public String teamId;
    public String currentLocation;
    public boolean available;
    public HashSet<String> equipment;

    public MaintenanceTeam(String teamId, String currentLocation) {
        this.teamId = teamId;
        this.currentLocation = currentLocation;
        this.available = true;
        this.equipment = new HashSet<>();
    }

    public void addEquipment(String equipmentName) {
        equipment.add(equipmentName);
    }

    public boolean removeEquipment(String equipmentName) {
        return equipment.remove(equipmentName);
    }

    public boolean hasEquipment(String requiredEquipment) {
        return equipment.contains(requiredEquipment);
    }

    public void assignJob() {
        available = false;
    }

    public void completeJob(String newLocation) {
        currentLocation = newLocation;
        available = true;
    }
}
