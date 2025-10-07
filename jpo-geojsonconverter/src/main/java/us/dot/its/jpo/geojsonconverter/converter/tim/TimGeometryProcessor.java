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
import org.locationtech.jts.util.GeometricShapeFactory;

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
    // https://en.wikipedia.org/wiki/Geographic_coordinate_system
    // 1 degree of latitude is ranges from 110.6 to 111.6 km
    // 1 degree of longitude is 111.3 km
    private static final double METERS_PER_DEGREE_APPROXIMATION = 111000.0;


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

    /**
     * Calculate zoom factor from path scale.
     * 
     * @param path The OffsetSystem path
     * @return Zoom factor (2^zoom)
     */
    private double calculateZoomFactor(OffsetSystem path) {
        if (path.getScale() != null) {
            // Zoom is applied as 2^zoom for coordinate scaling
            // A value of 0 is 1:1 zoom (no zoom), 1 is 2:1 zoom, 2 is 4:1 zoom, etc.
            return Math.pow(2, path.getScale().getValue());
        }
        return 1.0;
    }

    /**
     * Process LL (Latitude/Longitude) node and update current coordinates.
     * 
     * @param node The node to process
     * @param zoomFactor Zoom scaling factor
     * @param currentCoords Current coordinates [lon, lat] to update
     */
    private void processLLNode(NodeOffsetPointLL node, double zoomFactor, double[] currentCoords) {
        double currentLon = currentCoords[0];
        double currentLat = currentCoords[1];

        // Process different LL node types
        if (node.getNode_LL1() != null) {
            var nodeLL1 = node.getNode_LL1();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL1.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL1.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL2() != null) {
            var nodeLL2 = node.getNode_LL2();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL2.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL2.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL3() != null) {
            var nodeLL3 = node.getNode_LL3();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL3.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL3.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL4() != null) {
            var nodeLL4 = node.getNode_LL4();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL4.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL4.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL5() != null) {
            var nodeLL5 = node.getNode_LL5();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL5.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL5.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL6() != null) {
            var nodeLL6 = node.getNode_LL6();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL6.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL6.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LatLon() != null) {
            var nodeLatLon = node.getNode_LatLon();
            // node_LatLon contains absolute coordinates, not offsets
            Double absLon = FieldConversions.convertLong(nodeLatLon.getLon().getValue());
            Double absLat = FieldConversions.convertLat(nodeLatLon.getLat().getValue());
            if (absLon != null)
                currentLon = absLon;
            if (absLat != null)
                currentLat = absLat;
        }

        // Update coordinates array
        currentCoords[0] = currentLon;
        currentCoords[1] = currentLat;
    }

    /**
     * Process XY (Cartesian) node and update current coordinates.
     * 
     * @param node The node to process
     * @param zoomFactor Zoom scaling factor
     * @param currentCoords Current coordinates [lon, lat] to update
     */
    private void processXYNode(NodeOffsetPointXY node, double zoomFactor, double[] currentCoords) {
        double currentLon = currentCoords[0];
        double currentLat = currentCoords[1];

        // Process different XY node types
        if (node.getNode_XY1() != null) {
            var nodeXY1 = node.getNode_XY1();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY1.getX().getValue(), nodeXY1.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY2() != null) {
            var nodeXY2 = node.getNode_XY2();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY2.getX().getValue(), nodeXY2.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY3() != null) {
            var nodeXY3 = node.getNode_XY3();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY3.getX().getValue(), nodeXY3.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY4() != null) {
            var nodeXY4 = node.getNode_XY4();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY4.getX().getValue(), nodeXY4.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY5() != null) {
            var nodeXY5 = node.getNode_XY5();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY5.getX().getValue(), nodeXY5.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY6() != null) {
            var nodeXY6 = node.getNode_XY6();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY6.getX().getValue(), nodeXY6.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        }

        // Update coordinates array
        currentCoords[0] = currentLon;
        currentCoords[1] = currentLat;
    }

    private List<List<Double>> processOffsetPath(GeographicalPath region, OffsetSystem path) {
        if (path == null || region.getAnchor() == null) {
            return new ArrayList<>();
        }

        Position3D anchor = region.getAnchor();
        double anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
        double anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());

        List<List<Double>> coordinates = new ArrayList<>();
        coordinates.add(Arrays.asList(anchorLon, anchorLat));

        if (path.getOffset() != null) {
            double[] currentCoords = {anchorLon, anchorLat};
            double zoomFactor = calculateZoomFactor(path);

            // Handle LL (Latitude/Longitude) coordinates
            if (path.getOffset().getLl() != null && path.getOffset().getLl().getNodes() != null) {
                for (var node : path.getOffset().getLl().getNodes()) {
                    if (node.getDelta() != null) {
                        processLLNode(node.getDelta(), zoomFactor, currentCoords);
                        coordinates.add(Arrays.asList(currentCoords[0], currentCoords[1]));
                    }
                }
            }
            // Handle XY (Cartesian) coordinates
            else if (path.getOffset().getXy() != null && path.getOffset().getXy().getNodes() != null) {
                for (var node : path.getOffset().getXy().getNodes()) {
                    if (node.getDelta() != null) {
                        processXYNode(node.getDelta(), zoomFactor, currentCoords);
                        coordinates.add(Arrays.asList(currentCoords[0], currentCoords[1]));
                    }
                }
            }
        }

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
     * Create circle points using JTS GeometricShapeFactory.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param radiusMeters Radius in meters
     * @return List of coordinate points forming a circle
     */
    private List<List<Double>> createCirclePoints(double centerLon, double centerLat, int radiusMeters) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Convert radius from meters to degrees (approximate)
        double radiusDegrees = radiusMeters / METERS_PER_DEGREE_APPROXIMATION;

        // Create circle using JTS GeometricShapeFactory
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setCentre(new Coordinate(centerLon, centerLat));
        shapeFactory.setSize(radiusDegrees * 2); // diameter
        shapeFactory.setNumPoints(CIRCLE_APPROXIMATION_POINTS);

        org.locationtech.jts.geom.Polygon circle = shapeFactory.createCircle();
        Coordinate[] circleCoords = circle.getExteriorRing().getCoordinates();

        for (Coordinate coord : circleCoords) {
            coordinates.add(Arrays.asList(coord.x, coord.y));
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
     * Create rectangle points using JTS GeometricShapeFactory.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param paddingDegrees Padding in degrees (half-width and half-height)
     * @return List of coordinate points forming a rectangle
     */
    private List<List<Double>> createRectanglePoints(double centerLon, double centerLat, double paddingDegrees) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Create rectangle using JTS GeometricShapeFactory
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setCentre(new Coordinate(centerLon, centerLat));
        shapeFactory.setWidth(paddingDegrees * 2); // total width
        shapeFactory.setHeight(paddingDegrees * 2); // total height

        org.locationtech.jts.geom.Polygon rectangle = shapeFactory.createRectangle();
        Coordinate[] rectCoords = rectangle.getExteriorRing().getCoordinates();

        for (Coordinate coord : rectCoords) {
            coordinates.add(Arrays.asList(coord.x, coord.y));
        }

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
