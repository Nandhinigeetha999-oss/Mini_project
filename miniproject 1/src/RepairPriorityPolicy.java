import java.util.Comparator;

public class RepairPriorityPolicy {

    static Comparator<RoadDamage> priorityComparator =
            new Comparator<RoadDamage>() {

        @Override
        public int compare(RoadDamage a, RoadDamage b) {

            if (a.severity != b.severity) {

                return Integer.compare(b.severity, a.severity);
            }

            if (a.accidentRisk != b.accidentRisk) {

                return Integer.compare(
                        b.accidentRisk,
                        a.accidentRisk
                );
            }

            if (a.roadImportance != b.roadImportance) {

                return Integer.compare(
                        b.roadImportance,
                        a.roadImportance
                );
            }

            if (a.trafficRank != b.trafficRank) {

                return Integer.compare(
                        b.trafficRank,
                        a.trafficRank
                );
            }

            return Integer.compare(
                    a.reportOrder,
                    b.reportOrder
            );
        }
    };
}