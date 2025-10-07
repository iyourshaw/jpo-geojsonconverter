package us.dot.its.jpo.geojsonconverter.utils;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for coordinate projection and transformation operations. Provides methods for converting between
 * different coordinate reference systems using Proj4J.
 */
@Slf4j
public class ProjectionUtils {

    /**
     * Transform a single coordinate from one coordinate reference system to another.
     * 
     * @param sourceCrsCode Source CRS code (e.g., "EPSG:4326")
     * @param targetCrsCode Target CRS code (e.g., "EPSG:32633")
     * @param longitude Source longitude
     * @param latitude Source latitude
     * @return ProjCoordinate in target CRS
     */
    public static ProjCoordinate transformCoordinate(String sourceCrsCode, String targetCrsCode, double longitude,
            double latitude) {
        try {
            CRSFactory crsFactory = new CRSFactory();
            CoordinateReferenceSystem sourceCRS = crsFactory.createFromName(sourceCrsCode);
            CoordinateReferenceSystem targetCRS = crsFactory.createFromName(targetCrsCode);

            CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
            CoordinateTransform transform = ctFactory.createTransform(sourceCRS, targetCRS);

            ProjCoordinate result = new ProjCoordinate();
            transform.transform(new ProjCoordinate(longitude, latitude), result);

            return result;
        } catch (Exception e) {
            log.error("Error transforming coordinate from {} to {}: {}", sourceCrsCode, targetCrsCode, e.getMessage(),
                    e);
            return new ProjCoordinate(longitude, latitude); // Return original coordinates on error
        }
    }

    /**
     * Create a coordinate transform between two coordinate reference systems.
     * 
     * @param sourceCrsCode Source CRS code (e.g., "EPSG:4326")
     * @param targetCrsCode Target CRS code (e.g., "EPSG:32633")
     * @return CoordinateTransform for the specified CRS pair
     */
    public static CoordinateTransform createTransform(String sourceCrsCode, String targetCrsCode) {
        try {
            CRSFactory crsFactory = new CRSFactory();
            CoordinateReferenceSystem sourceCRS = crsFactory.createFromName(sourceCrsCode);
            CoordinateReferenceSystem targetCRS = crsFactory.createFromName(targetCrsCode);

            CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
            return ctFactory.createTransform(sourceCRS, targetCRS);
        } catch (Exception e) {
            log.error("Error creating transform from {} to {}: {}", sourceCrsCode, targetCrsCode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Transform coordinates from one coordinate reference system to another.
     * 
     * @param sourceCrsCode Source CRS code (e.g., "EPSG:4326")
     * @param targetCrsCode Target CRS code (e.g., "EPSG:32633")
     * @param longitude Source longitude
     * @param latitude Source latitude
     * @return Array containing [longitude, latitude] in target CRS
     */
    public static double[] transformCoordinates(String sourceCrsCode, String targetCrsCode, double longitude,
            double latitude) {
        try {
            CRSFactory crsFactory = new CRSFactory();
            CoordinateReferenceSystem sourceCRS = crsFactory.createFromName(sourceCrsCode);
            CoordinateReferenceSystem targetCRS = crsFactory.createFromName(targetCrsCode);

            CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
            CoordinateTransform transform = ctFactory.createTransform(sourceCRS, targetCRS);

            ProjCoordinate result = new ProjCoordinate();
            transform.transform(new ProjCoordinate(longitude, latitude), result);

            return new double[] {result.x, result.y};
        } catch (Exception e) {
            log.error("Error transforming coordinates from {} to {}: {}", sourceCrsCode, targetCrsCode, e.getMessage(),
                    e);
            return new double[] {longitude, latitude}; // Return original coordinates on error
        }
    }

    /**
     * Determine the appropriate UTM zone for a given longitude.
     * 
     * @param longitude Longitude in degrees
     * @return UTM zone number (1-60)
     */
    public static int getUtmZone(double longitude) {
        return (int) Math.floor((longitude + 180) / 6) + 1;
    }

    /**
     * Get the UTM CRS code for a given longitude and latitude.
     * 
     * @param longitude Longitude in degrees
     * @param latitude Latitude in degrees
     * @return UTM CRS code (e.g., "EPSG:32633" for northern hemisphere, "EPSG:32733" for southern)
     */
    public static String getUtmCrsCode(double longitude, double latitude) {
        int utmZone = getUtmZone(longitude);
        return (latitude >= 0) ? "EPSG:" + (32600 + utmZone) // northern hemisphere
                : "EPSG:" + (32700 + utmZone); // southern hemisphere
    }

}
