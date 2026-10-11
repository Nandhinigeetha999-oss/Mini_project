public class ResurfacingRepair extends RepairJob {

    ResurfacingRepair(RoadDamage damage) {

        super(damage);
    }

    @Override
    String getRequiredEquipment() {

        return "Resurfacing Machine";
    }
}