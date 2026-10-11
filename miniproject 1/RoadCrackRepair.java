public class RoadCrackRepair extends RepairJob {

    RoadCrackRepair(RoadDamage damage) {

        super(damage);
    }

    @Override
    String getRequiredEquipment() {

        return "Road Cutter";
    }
}