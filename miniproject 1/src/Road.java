public class Road {

    String roadId;
    String from;
    String to;
    int travelTime;
    boolean closed;

    Road(String roadId, String from, String to, int travelTime) {

        this.roadId = roadId;
        this.from = from;
        this.to = to;
        this.travelTime = travelTime;

        closed = false;
    }

    void closeRoad() {

        closed = true;
    }

    void openRoad() {

        closed = false;
    }
}