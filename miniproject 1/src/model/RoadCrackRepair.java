package model;

public class RoadCrackRepair extends RepairJob {

    public RoadCrackRepair(RoadDamage damage) {
        super(damage);
    }

    @Override
    public String getRequiredEquipment() {
        return "Road Cutter";
    }
}
