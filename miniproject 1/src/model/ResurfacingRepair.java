package model;

public class ResurfacingRepair extends RepairJob {

    public ResurfacingRepair(RoadDamage damage) {
        super(damage);
    }

    @Override
    public String getRequiredEquipment() {
        return "Resurfacing Machine";
    }
}
