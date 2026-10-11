public class RepairJob {

    RoadDamage damage;
    MaintenanceTeam assignedTeam;

    String requiredEquipment;
    String status;

    RepairJob(RoadDamage damage) {

        this.damage = damage;

        assignedTeam = null;

        requiredEquipment = getRequiredEquipment();

        status = "WAITING";
    }

    String getRequiredEquipment() {

        return "General Equipment";
    }

    void assignTeam(MaintenanceTeam team) {

        assignedTeam = team;

        status = "ACTIVE";

        damage.status = "ACTIVE";

        team.assignJob();
    }

    void completeRepair() {

        status = "COMPLETED";

        damage.status = "COMPLETED";

        if (assignedTeam != null) {

            assignedTeam.completeJob(damage.location);
        }
    }
}