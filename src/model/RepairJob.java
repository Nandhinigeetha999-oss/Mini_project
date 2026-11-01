package model;

public class RepairJob {

    public RoadDamage damage;
    public MaintenanceTeam assignedTeam;
    public String requiredEquipment;
    public String status;

    public RepairJob(RoadDamage damage) {
        this.damage = damage;
        this.assignedTeam = null;
        this.requiredEquipment = getRequiredEquipment();
        this.status = "WAITING";
    }

    public String getRequiredEquipment() {
        return "General Equipment";
    }

    public void assignTeam(MaintenanceTeam team) {
        assignedTeam = team;
        status = "ACTIVE";
        damage.status = "ACTIVE";
        team.assignJob();
    }

    public void completeRepair() {
        status = "COMPLETED";
        damage.status = "COMPLETED";
        if (assignedTeam != null) {
            assignedTeam.completeJob(damage.location);
        }
    }
}
