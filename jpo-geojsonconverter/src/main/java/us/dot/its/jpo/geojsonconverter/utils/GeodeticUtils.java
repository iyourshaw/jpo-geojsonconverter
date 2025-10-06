package us.dot.its.jpo.geojsonconverter.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for geodetic calculations and coordinate transformations. Provides accurate methods for converting
 * between different coordinate systems and calculating distances and offsets on the Earth's surface.
 */
@Slf4j
public class GeodeticUtils {

    // Shared GeometryFactory instance for creating JTS geometries
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    // Earth's radius in meters (WGS84)
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    // Earth's semi-major axis (WGS84)
    private static final double EARTH_SEMI_MAJOR_AXIS = 6378137.0;

    // First eccentricity squared (WGS84)
    private static final double ECCENTRICITY_SQUARED = 0.00669437999014;

    /**
     * Get the shared GeometryFactory instance for creating JTS geometries.
     * 
     * @return The shared GeometryFactory instance
     */
    public static GeometryFactory getGeometryFactory() {
        return GEOMETRY_FACTORY;
    }

    /**
     * Convert meter offsets to latitude/longitude coordinates using proper geodetic calculations. This method accounts
     * for the Earth's curvature and provides accurate results using ellipsoidal calculations.
     * 
     * @param longitude The reference longitude in decimal degrees (must be finite and valid)
     * @param latitude The reference latitude in decimal degrees (must be finite and valid)
     * @param offsetXMeters The offset in meters along the X-axis (East-West, positive = East)
     * @param offsetYMeters The offset in meters along the Y-axis (North-South, positive = North)
     * @return A Coordinate object with the new longitude and latitude
     * @throws IllegalArgumentException if input parameters are invalid (NaN, infinite, or out of valid ranges)
     */
    public static Coordinate offsetCoordinate(double longitude, double latitude, double offsetXMeters,
            double offsetYMeters) {
        // Validate input parameters
        validateCoordinate(longitude, latitude);
        validateOffset(offsetXMeters, offsetYMeters);

        // Convert to radians
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);

        // Calculate the radius of curvature in the meridian (North-South direction)
        double radiusMeridian = calculateRadiusOfCurvatureMeridian(latRad);

        // Calculate the radius of curvature in the prime vertical (East-West direction)
        double radiusPrimeVertical = calculateRadiusOfCurvaturePrimeVertical(latRad);

        // Convert meter offsets to angular offsets
        double latOffsetRad = offsetYMeters / radiusMeridian;
        double lonOffsetRad = offsetXMeters / (radiusPrimeVertical * Math.cos(latRad));

        // Calculate new coordinates
        double newLatRad = latRad + latOffsetRad;
        double newLonRad = lonRad + lonOffsetRad;

        // Convert back to degrees
        double newLatitude = Math.toDegrees(newLatRad);
        double newLongitude = Math.toDegrees(newLonRad);

        // Normalize longitude to [-180, 180]
        newLongitude = normalizeLongitude(newLongitude);

        return new Coordinate(newLongitude, newLatitude);
    }

    /**
     * Validate coordinate parameters for validity and reasonable ranges.
     * 
     * @param longitude The longitude in decimal degrees
     * @param latitude The latitude in decimal degrees
     * @throws IllegalArgumentException if coordinates are invalid
     */
    private static void validateCoordinate(double longitude, double latitude) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
            throw new IllegalArgumentException("Longitude and latitude must be finite numbers");
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }

        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }

        // Check for extreme latitudes that could cause numerical issues
        if (Math.abs(latitude) > 89.0) {
            throw new IllegalArgumentException(
                    "Latitude too close to poles (|latitude| > 89°), may cause numerical instability");
        }
    }

    /**
     * Validate offset parameters for validity and reasonable ranges.
     * 
     * @param offsetXMeters The X offset in meters
     * @param offsetYMeters The Y offset in meters
     * @throws IllegalArgumentException if offsets are invalid
     */
    private static void validateOffset(double offsetXMeters, double offsetYMeters) {
        if (!Double.isFinite(offsetXMeters) || !Double.isFinite(offsetYMeters)) {
            throw new IllegalArgumentException("Offset values must be finite numbers");
        }

        // Check for unreasonably large offsets that could cause numerical issues
        double maxOffset = 1000000.0; // 1000 km
        if (Math.abs(offsetXMeters) > maxOffset || Math.abs(offsetYMeters) > maxOffset) {
            throw new IllegalArgumentException("Offset values too large (>1000km), may cause numerical instability");
        }
    }

    /**
     * Calculate the radius of curvature in the meridian (North-South direction). This accounts for the Earth's
     * ellipsoidal shape.
     */
    private static double calculateRadiusOfCurvatureMeridian(double latRad) {
        double sinLat = Math.sin(latRad);
        double denominator = Math.sqrt(1 - ECCENTRICITY_SQUARED * sinLat * sinLat);
        return EARTH_SEMI_MAJOR_AXIS * (1 - ECCENTRICITY_SQUARED) / Math.pow(denominator, 3);
    }

    /**
     * Calculate the radius of curvature in the prime vertical (East-West direction). This accounts for the Earth's
     * ellipsoidal shape.
     */
    private static double calculateRadiusOfCurvaturePrimeVertical(double latRad) {
        double sinLat = Math.sin(latRad);
        double denominator = Math.sqrt(1 - ECCENTRICITY_SQUARED * sinLat * sinLat);
        return EARTH_SEMI_MAJOR_AXIS / denominator;
    }

    /**
     * Normalize longitude to the range [-180, 180] degrees.
     */
    private static double normalizeLongitude(double longitude) {
        while (longitude > 180.0) {
            longitude -= 360.0;
        }
        while (longitude < -180.0) {
            longitude += 360.0;
        }
        return longitude;
    }

    /**
     * Calculate the distance between two points on the Earth's surface using the Haversine formula.
     * 
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @return Distance in meters
     */
    public static double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calculate the bearing (direction) from one point to another.
     * 
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @return Bearing in degrees (0-360, where 0 is North, 90 is East, etc.)
     */
    public static double calculateBearing(double lon1, double lat1, double lon2, double lat2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x =
                Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);

        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);

        // Normalize to 0-360 degrees
        return (bearingDeg + 360) % 360;
    }

    /**
     * Calculate a destination point given a starting point, bearing, and distance.
     * 
     * @param longitude Starting longitude in decimal degrees
     * @param latitude Starting latitude in decimal degrees
     * @param bearing Bearing in degrees (0-360, where 0 is North)
     * @param distanceMeters Distance in meters
     * @return A Coordinate object with the destination longitude and latitude
     */
    public static Coordinate calculateDestination(double longitude, double latitude, double bearing,
            double distanceMeters) {
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        double bearingRad = Math.toRadians(bearing);
        double angularDistance = distanceMeters / EARTH_RADIUS_METERS;

        double newLatRad = Math.asin(Math.sin(latRad) * Math.cos(angularDistance)
                + Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(bearingRad));

        double newLonRad = lonRad + Math.atan2(Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(latRad),
                Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(newLatRad));

        double newLatitude = Math.toDegrees(newLatRad);
        double newLongitude = Math.toDegrees(newLonRad);

        // Normalize longitude to [-180, 180]
        newLongitude = normalizeLongitude(newLongitude);

        return new Coordinate(newLongitude, newLatitude);
    }

    /**
     * Calculate the center location from a list of coordinate points.
     * 
     * @param coordinates List of coordinate points [longitude, latitude]
     * @return Point representing the center location, or null if no coordinates provided
     */
    public static Point calculateCenterLocation(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        List<Double> allLatitudes = new ArrayList<>();
        List<Double> allLongitudes = new ArrayList<>();

        for (List<Double> coordinate : coordinates) {
            if (coordinate != null && coordinate.size() >= 2) {
                allLongitudes.add(coordinate.get(0)); // longitude
                allLatitudes.add(coordinate.get(1)); // latitude
            }
        }

        if (allLatitudes.isEmpty()) {
            return null;
        }

        // Calculate center point (simple average)
        double centerLat = allLatitudes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double centerLon = allLongitudes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Create Point geometry using shared GeometryFactory
        return GEOMETRY_FACTORY.createPoint(new Coordinate(centerLon, centerLat));
    }
}
