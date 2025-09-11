package us.dot.its.jpo.geojsonconverter.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

public class GeodeticUtilsTest {

    @Test
    public void testOffsetCoordinate() {
        // Test offsetting from a known point
        double longitude = -104.9903; // Denver, CO
        double latitude = 39.7392;

        // Offset 1000 meters North and 1000 meters East
        double offsetXMeters = 1000.0; // East
        double offsetYMeters = 1000.0; // North

        Coordinate result = GeodeticUtils.offsetCoordinate(longitude, latitude, offsetXMeters, offsetYMeters);

        assertNotNull(result);
        // The new point should be northeast of the original
        assertEquals("New longitude should be greater (more east)", true, result.x > longitude);
        assertEquals("New latitude should be greater (more north)", true, result.y > latitude);
    }

    @Test
    public void testCalculateDistance() {
        // Test distance calculation between two known points
        double lon1 = -104.9903; // Denver, CO
        double lat1 = 39.7392;
        double lon2 = -105.0178; // Boulder, CO (approximately)
        double lat2 = 40.0150;

        double distance = GeodeticUtils.calculateDistance(lon1, lat1, lon2, lat2);

        // Distance between Denver and Boulder should be approximately 30-40 km
        assertEquals("Distance should be reasonable", true, distance > 30000 && distance < 50000);
    }

    @Test
    public void testCalculateBearing() {
        // Test bearing calculation
        double lon1 = -104.9903; // Denver, CO
        double lat1 = 39.7392;
        double lon2 = -105.0178; // Boulder, CO (northwest of Denver)
        double lat2 = 40.0150;

        double bearing = GeodeticUtils.calculateBearing(lon1, lat1, lon2, lat2);

        // Bearing should be roughly northwest (around 315 degrees)
        assertEquals("Bearing should be reasonable", true, bearing > 300 && bearing < 360);
    }

    @Test
    public void testCalculateDestination() {
        // Test destination calculation
        double longitude = -104.9903; // Denver, CO
        double latitude = 39.7392;
        double bearing = 45.0; // Northeast
        double distanceMeters = 1000.0; // 1 km

        Coordinate destination = GeodeticUtils.calculateDestination(longitude, latitude, bearing, distanceMeters);

        assertNotNull(destination);
        // The destination should be northeast of the starting point
        assertEquals("Destination longitude should be greater (more east)", true, destination.x > longitude);
        assertEquals("Destination latitude should be greater (more north)", true, destination.y > latitude);
    }

    @Test
    public void testNormalizeLongitude() {
        // Test longitude normalization
        Coordinate result1 = GeodeticUtils.offsetCoordinate(0, 0, 0, 0);
        assertEquals("Longitude should be normalized", true, result1.x >= -180 && result1.x <= 180);

        // Test with a longitude that would normally be > 180
        Coordinate result2 = GeodeticUtils.calculateDestination(179.0, 0, 90.0, 100000);
        assertEquals("Longitude should be normalized", true, result2.x >= -180 && result2.x <= 180);
    }
}
