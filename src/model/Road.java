package model;

public class Road {

    public String roadId;
    public String from;
    public String to;
    public int travelTime;
    public boolean closed;

    public Road(String roadId, String from, String to, int travelTime) {
        this(roadId, from, to, travelTime, false);
    }

    public Road(String roadId, String from, String to, int travelTime, boolean closed) {
        this.roadId = roadId;
        this.from = from;
        this.to = to;
        this.travelTime = travelTime;
        this.closed = closed;
    }

    public void closeRoad() {
        closed = true;
    }

    public void openRoad() {
        closed = false;
    }
}
