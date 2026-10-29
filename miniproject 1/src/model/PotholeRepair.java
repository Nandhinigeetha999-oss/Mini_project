package model;

public class PotholeRepair extends RepairJob {

    public PotholeRepair(RoadDamage damage) {
        super(damage);
    }

    @Override
    public String getRequiredEquipment() {
        return "Pothole Kit";
    }
}
