public class PotholeRepair extends RepairJob {

    PotholeRepair(RoadDamage damage) {

        super(damage);
    }

    @Override
    String getRequiredEquipment() {

        return "Pothole Kit";
    }
}