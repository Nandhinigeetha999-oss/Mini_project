package model;

public class GraphEdge {

    public String destination;
    public int travelTime;

    public GraphEdge(String destination, int travelTime) {
        this.destination = destination;
        this.travelTime = travelTime;
    }
}
