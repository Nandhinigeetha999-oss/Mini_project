/** A named municipal place with the coordinates stored in the database. */
public class Location {

    String name;
    double latitude;
    double longitude;

    Location(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
