import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Dijkstra {

    static class Node implements Comparable<Node> {

        String location;
        int distance;

        Node(String location, int distance) {
            this.location = location;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(
                    this.distance,
                    other.distance
            );
        }
    }

    static int findShortestDistance(
            RoadNetwork network,
            String start,
            String destination) {

        HashMap<String, Integer> distance = new HashMap<>();

        for (String location : network.graph.keySet()) {
            distance.put(location, Integer.MAX_VALUE);
        }

        distance.put(start, 0);

        PriorityQueue<Node> queue =
                new PriorityQueue<>();

        queue.add(new Node(start, 0));

        while (!queue.isEmpty()) {

            Node current = queue.poll();

            if (current.location.equals(destination)) {
                return current.distance;
            }
            if (!network.graph.containsKey(current.location)) {
                 continue;
            }
            for (GraphEdge edge :
                    network.graph.get(current.location)) {

                // Check whether this connection is closed
                boolean roadClosed = false;

                for (Road road : network.roads) {

                    boolean sameRoad =
                            (road.from.equals(current.location)
                            && road.to.equals(edge.destination))
                            ||
                            (road.to.equals(current.location)
                            && road.from.equals(edge.destination));

                    if (sameRoad && road.closed) {
                        roadClosed = true;
                        break;
                    }
                }

                // Do not use a closed road
                if (roadClosed) {
                    continue;
                }

                int newDistance =
                        current.distance + edge.travelTime;

                if (newDistance <
                        distance.get(edge.destination)) {

                    distance.put(
                            edge.destination,
                            newDistance
                    );

                    queue.add(
                            new Node(
                                    edge.destination,
                                    newDistance
                            )
                    );
                }
            }
        }

        return -1;
    }

    static String findShortestRoute(
        RoadNetwork network,
        String start,
        String destination) {

    HashMap<String, Integer> distance = new HashMap<>();
    HashMap<String, String> previous = new HashMap<>();

    for (String location : network.graph.keySet()) {
        distance.put(location, Integer.MAX_VALUE);
    }

    distance.put(start, 0);

    PriorityQueue<Node> queue = new PriorityQueue<>();
    queue.add(new Node(start, 0));

    while (!queue.isEmpty()) {

        Node current = queue.poll();

        if (current.distance > distance.get(current.location)) {
            continue;
        }

        for (GraphEdge edge :
                network.graph.get(current.location)) {

            boolean roadClosed = false;

            for (Road road : network.roads) {

                boolean sameRoad =
                        (road.from.equals(current.location)
                        && road.to.equals(edge.destination))
                        ||
                        (road.to.equals(current.location)
                        && road.from.equals(edge.destination));

                if (sameRoad && road.closed) {
                    roadClosed = true;
                    break;
                }
            }

            if (roadClosed) {
                continue;
            }

            int newDistance =
                    current.distance + edge.travelTime;

            if (newDistance <
                    distance.get(edge.destination)) {

                distance.put(
                        edge.destination,
                        newDistance
                );

                previous.put(
                        edge.destination,
                        current.location
                );

                queue.add(
                        new Node(
                                edge.destination,
                                newDistance
                        )
                );
            }
        }
    }

    if (!distance.containsKey(destination)
            || distance.get(destination) == Integer.MAX_VALUE) {

        return null;
    }

    ArrayList<String> path = new ArrayList<>();

    String current = destination;

    while (current != null) {

        path.add(current);

        if (current.equals(start)) {
            break;
        }

        current = previous.get(current);
    }

    if (!path.get(path.size() - 1).equals(start)) {
        return null;
    }

    String route = "";

    for (int i = path.size() - 1; i >= 0; i--) {

        route += path.get(i);

        if (i != 0) {
            route += " -> ";
        }
    }

    return route;
}
}
