package us.dot.its.jpo.geojsonconverter.converter.tim;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath.DescriptionChoice;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.*;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;
import us.dot.its.jpo.geojsonconverter.utils.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles geometry processing for TIM regions. This class encapsulates all geometry-related conversions and
 * calculations.
 */
@Slf4j
@Component
public class TimGeometryProcessor {

    // Constants
    private static final int CIRCLE_APPROXIMATION_POINTS = 16;
    private static final double DEFAULT_PADDING_DEGREES = 0.005;

    /**
     * Convert TIM region to appropriate GeoJSON geometry based on region type.
     *
     * @param region The geographical path region
     * @return Appropriate GeoJSON geometry or null if processing fails
     */
    public Geometry createGeometryFromRegion(GeographicalPath region) {
        try {
            ProcessedRegionType regionType = determineRegionType(region);
            List<List<Double>> coordinates = processRegionCoordinates(region);

            if (coordinates.isEmpty()) {
                return null;
            }

            return createGeometryByType(coordinates, regionType);
        } catch (Exception e) {
            log.error("Error creating geometry from region: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create geometry from data frame regions.
     *
     * @param dataFrame The data frame containing region information
     * @return Appropriate geometry object or null
     */
    public Geometry createGeometryFromDataFrame(TravelerDataFrame dataFrame) {
        if (dataFrame.getRegions() == null || dataFrame.getRegions().isEmpty()) {
            return null;
        }

        if (dataFrame.getRegions().size() == 1) {
            return createGeometryFromRegion(dataFrame.getRegions().get(0));
        } else {
            return createMultiGeometryFromRegions(dataFrame.getRegions());
        }
    }

    /**
     * Calculate the center location from all regions in the TIM message.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @return JTS Point representing the center location, or null if no regions found
     */
    public Point calculateCenterLocationFromRegions(TravelerInformation travelerInfo) {
        List<List<Double>> coordinates = new ArrayList<>();

        if (travelerInfo.getDataFrames() != null) {
            for (TravelerDataFrame dataFrame : travelerInfo.getDataFrames()) {
                if (dataFrame.getRegions() != null) {
                    for (GeographicalPath region : dataFrame.getRegions()) {
                        // Get anchor point coordinates
                        if (region.getAnchor() != null) {
                            double lat = FieldConversions.convertLat(region.getAnchor().getLat().getValue());
                            double lon = FieldConversions.convertLong(region.getAnchor().getLong_().getValue());
                            coordinates.add(Arrays.asList(lon, lat)); // [longitude, latitude]
                        }
                    }
                }
            }
        }

        // Use GeodeticUtils to calculate center location
        return GeodeticUtils.calculateCenterLocation(coordinates);
    }

    /**
     * Determine the region type from ASN.1 data.
     */
    private ProcessedRegionType determineRegionType(GeographicalPath region) {
        if (region.getDescription() == null) {
            return ProcessedRegionType.UNKNOWN;
        }

        boolean isClosedPath = region.getClosedPath() != null && region.getClosedPath().getValue();
        boolean isCircle = region.getDescription().getGeometry() != null
                && region.getDescription().getGeometry().getCircle() != null;
        boolean hasPath = region.getDescription().getPath() != null;

        if (isCircle) {
            return ProcessedRegionType.CIRCLE;
        } else if (isClosedPath) {
            return ProcessedRegionType.POLYGON;
        } else if (hasPath) {
            return ProcessedRegionType.PATH;
        } else {
            return ProcessedRegionType.UNKNOWN;
        }
    }

    /**
     * Process region coordinates from ASN.1 data
     */
    private List<List<Double>> processRegionCoordinates(GeographicalPath region) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Get coordinates from the description
        DescriptionChoice description = region.getDescription();
        if (description != null) {
            if (description.getPath() != null) {
                coordinates.addAll(processOffsetPath(region, description.getPath()));
            } else if (description.getGeometry() != null) {
                coordinates.addAll(processGeometry(region, description.getGeometry()));
            }
        }

        // If no description, try to use anchor point with lane width
        if (coordinates.isEmpty() && region.getAnchor() != null) {
            Position3D anchor = region.getAnchor();
            coordinates.addAll(createRectangleFromAnchor(anchor));
        }

        return coordinates;
    }

    /**
     * Create geometry based on the region type.
     */
    private Geometry createGeometryByType(List<List<Double>> coordinates, ProcessedRegionType regionType) {
        switch (regionType) {
            case CIRCLE:
            case POLYGON:
                return createPolygonFromCoordinates(coordinates);
            case PATH:
            case UNKNOWN:
            default:
                return createLineStringFromCoordinates(coordinates);
        }
    }

    /**
     * Create MultiLineString or MultiPolygon from multiple regions
     */
    private Geometry createMultiGeometryFromRegions(List<GeographicalPath> regions) {
        List<List<List<Double>>> allCoordinates = new ArrayList<>();
        boolean hasPolygons = false;

        // Process each region and determine geometry types
        for (GeographicalPath region : regions) {
            List<List<Double>> coordinates = processRegionCoordinates(region);
            if (!coordinates.isEmpty()) {
                allCoordinates.add(coordinates);

                // Determine if this region should be a polygon or linestring
                boolean isClosedPath = region.getClosedPath() != null && region.getClosedPath().getValue();
                boolean isCircle = region.getDescription() != null && region.getDescription().getGeometry() != null
                        && region.getDescription().getGeometry().getCircle() != null;

                if (isCircle || isClosedPath) {
                    hasPolygons = true;
                }
            }
        }

        if (allCoordinates.isEmpty()) {
            return null;
        }

        // If we have polygons, use MultiPolygon, otherwise use MultiLineString
        if (hasPolygons) {
            return createMultiPolygonFromCoordinates(allCoordinates);
        } else {
            return createMultiLineStringFromCoordinates(allCoordinates);
        }
    }

    private List<List<Double>> processOffsetPath(GeographicalPath region, OffsetSystem path) {
        if (path == null || region.getAnchor() == null) {
            return new ArrayList<>();
        }

        Position3D anchor = region.getAnchor();
        double anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
        double anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());

        List<List<Double>> coordinates = new ArrayList<>();

        // Add anchor point as first coordinate
        coordinates.add(Arrays.asList(anchorLon, anchorLat));

        return coordinates;
    }

    private List<List<Double>> processGeometry(GeographicalPath region, GeometricProjection geometry) {
        if (geometry == null || region.getAnchor() == null) {
            return new ArrayList<>();
        }

        Position3D anchor = region.getAnchor();
        double anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
        double anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());

        List<List<Double>> coordinates = new ArrayList<>();

        // Handle circle geometry
        if (geometry.getCircle() != null) {
            Circle circle = geometry.getCircle();
            if (circle.getRadius() != null) {
                int radius = (int) circle.getRadius().getValue();

                // Create circle points using accurate geodetic calculations
                coordinates.addAll(createCirclePoints(anchorLon, anchorLat, radius));
            }
        }

        return coordinates;
    }

    /**
     * Create circle points using accurate geodetic calculations.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param radiusMeters Radius in meters
     * @return List of coordinate points forming a circle
     */
    private List<List<Double>> createCirclePoints(double centerLon, double centerLat, int radiusMeters) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Create points around the circle using GeodeticUtils for accurate calculations
        for (int i = 0; i < CIRCLE_APPROXIMATION_POINTS; i++) {
            double bearing = (360.0 / CIRCLE_APPROXIMATION_POINTS) * i;

            // Use GeodeticUtils to calculate the point at this bearing and distance
            Coordinate point = GeodeticUtils.calculateDestination(centerLon, centerLat, bearing, radiusMeters);
            coordinates.add(Arrays.asList(point.x, point.y));
        }

        return coordinates;
    }

    private List<List<Double>> createRectangleFromAnchor(Position3D anchor) {
        if (anchor == null) {
            return new ArrayList<>();
        }

        double lat = FieldConversions.convertLat(anchor.getLat().getValue());
        double lon = FieldConversions.convertLong(anchor.getLong_().getValue());

        // Create rectangle using GeodeticUtils for accurate coordinate calculations
        return createRectanglePoints(lon, lat, DEFAULT_PADDING_DEGREES);
    }

    /**
     * Create rectangle points using accurate geodetic calculations.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param paddingDegrees Padding in degrees (half-width and half-height)
     * @return List of coordinate points forming a rectangle
     */
    private List<List<Double>> createRectanglePoints(double centerLon, double centerLat, double paddingDegrees) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Convert padding from degrees to approximate meters for GeodeticUtils
        // This is a rough conversion for the default padding
        double paddingMeters = paddingDegrees * 111000; // approximately 111km per degree

        // Create rectangle corners using GeodeticUtils
        // North-East corner
        Coordinate ne = GeodeticUtils.offsetCoordinate(centerLon, centerLat, paddingMeters, paddingMeters);
        coordinates.add(Arrays.asList(ne.x, ne.y));

        // South-East corner
        Coordinate se = GeodeticUtils.offsetCoordinate(centerLon, centerLat, paddingMeters, -paddingMeters);
        coordinates.add(Arrays.asList(se.x, se.y));

        // South-West corner
        Coordinate sw = GeodeticUtils.offsetCoordinate(centerLon, centerLat, -paddingMeters, -paddingMeters);
        coordinates.add(Arrays.asList(sw.x, sw.y));

        // North-West corner
        Coordinate nw = GeodeticUtils.offsetCoordinate(centerLon, centerLat, -paddingMeters, paddingMeters);
        coordinates.add(Arrays.asList(nw.x, nw.y));

        // Close the rectangle by adding the first point again
        coordinates.add(Arrays.asList(ne.x, ne.y));

        return coordinates;
    }

    private LineString createLineStringFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        double[][] coordinateArray = new double[coordinates.size()][2];
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> coord = coordinates.get(i);
            if (coord.size() >= 2) {
                coordinateArray[i][0] = coord.get(0); // longitude
                coordinateArray[i][1] = coord.get(1); // latitude
            }
        }

        return new LineString(coordinateArray);
    }

    private Polygon createPolygonFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        // Ensure the polygon is closed (first and last coordinates are the same)
        List<List<Double>> closedCoordinates = new ArrayList<>(coordinates);
        if (closedCoordinates.size() > 2) {
            List<Double> first = closedCoordinates.get(0);
            List<Double> last = closedCoordinates.get(closedCoordinates.size() - 1);
            if (!first.equals(last)) {
                closedCoordinates.add(new ArrayList<>(first));
            }
        }

        double[][][] coordinateArray = new double[1][closedCoordinates.size()][2];
        for (int i = 0; i < closedCoordinates.size(); i++) {
            List<Double> coord = closedCoordinates.get(i);
            if (coord.size() >= 2) {
                coordinateArray[0][i][0] = coord.get(0); // longitude
                coordinateArray[0][i][1] = coord.get(1); // latitude
            }
        }

        return new Polygon(coordinateArray);
    }

    private MultiLineString createMultiLineStringFromCoordinates(List<List<List<Double>>> allCoordinates) {
        if (allCoordinates == null || allCoordinates.isEmpty()) {
            return null;
        }

        double[][][] coordinateArray = new double[allCoordinates.size()][][];
        for (int i = 0; i < allCoordinates.size(); i++) {
            List<List<Double>> lineString = allCoordinates.get(i);
            if (lineString != null && !lineString.isEmpty()) {
                coordinateArray[i] = new double[lineString.size()][2];
                for (int j = 0; j < lineString.size(); j++) {
                    List<Double> coord = lineString.get(j);
                    if (coord.size() >= 2) {
                        coordinateArray[i][j][0] = coord.get(0); // longitude
                        coordinateArray[i][j][1] = coord.get(1); // latitude
                    }
                }
            }
        }

        return new MultiLineString(coordinateArray);
    }

    private MultiPolygon createMultiPolygonFromCoordinates(List<List<List<Double>>> allCoordinates) {
        if (allCoordinates == null || allCoordinates.isEmpty()) {
            return null;
        }

        double[][][][] coordinateArray = new double[allCoordinates.size()][][][];
        for (int i = 0; i < allCoordinates.size(); i++) {
            List<List<Double>> polygon = allCoordinates.get(i);
            if (polygon != null && !polygon.isEmpty()) {
                // Ensure the polygon is closed
                List<List<Double>> closedPolygon = new ArrayList<>(polygon);
                if (closedPolygon.size() > 2) {
                    List<Double> first = closedPolygon.get(0);
                    List<Double> last = closedPolygon.get(closedPolygon.size() - 1);
                    if (!first.equals(last)) {
                        closedPolygon.add(new ArrayList<>(first));
                    }
                }

                coordinateArray[i] = new double[1][closedPolygon.size()][2];
                for (int j = 0; j < closedPolygon.size(); j++) {
                    List<Double> coord = closedPolygon.get(j);
                    if (coord.size() >= 2) {
                        coordinateArray[i][0][j][0] = coord.get(0); // longitude
                        coordinateArray[i][0][j][1] = coord.get(1); // latitude
                    }
                }
            }
        }

        return new MultiPolygon(coordinateArray);
    }
}
