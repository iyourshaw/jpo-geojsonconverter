package us.dot.its.jpo.geojsonconverter.converter.tim;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath.DescriptionChoice;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetSystem.OffsetChoice;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame.ContentChoice;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.*;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.*;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;
import us.dot.its.jpo.geojsonconverter.utils.*;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import org.locationtech.jts.geom.Coordinate;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * Converts ODE TIM messages to Processed TIM GeoJSON format.
 * 
 * This converter processes Traveler Information Messages (TIM) from ASN.1 format and generates GeoJSON features with
 * appropriate geometries: - Path regions become LineString or MultiLineString - Circle/closed regions become Polygon or
 * MultiPolygon - Multiple regions are combined into MultiLineString or MultiPolygon as appropriate
 */
@Slf4j
public class TimProcessedJsonConverter
        implements Transformer<Void, DeserializedRawTim, KeyValue<RsuTimKey, ProcessedTim>> {

    // Constants
    private static final String UTC_ZONE_ID = "UTC";
    private static final String ERROR_RSU_ID = "ERROR";
    private static final int CIRCLE_APPROXIMATION_POINTS = 16;
    private static final double DEFAULT_PADDING_DEGREES = 0.005;
    private static final int INFINITE_DURATION_VALUE = 32000;
    private static final int CENTIMETERS_TO_METERS_DIVISOR = 100;
    private static final int PACKET_ID_GNIS_LENGTH = 6;
    private static final int PACKET_ID_GNIS_RADIX = 16;
    private static final ZonedDateTime INFINITE_VALIDITY_PERIOD =
            ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of(UTC_ZONE_ID));

    // Frame type constants
    private static final String FRAME_TYPE_ADVISORY = "advisory";
    private static final String FRAME_TYPE_ROAD_SIGNAGE = "roadSignage";
    private static final String FRAME_TYPE_COMMERCIAL_SIGNAGE = "commercialSignage";

    @Override
    public void init(ProcessorContext context) {
        // No initialization required
    }

    /**
     * Transform an ODE TIM POJO to Processed TIM POJO.
     *
     * @param rawKey Void type because ODE topics have no specified key
     * @param rawTim The raw POJO containing TIM data
     * @return A key-value pair: the key is an {@link RsuTimKey} containing the RSU IP address, packet ID, and message
     *         count, and the value is the ProcessedTim POJO
     */
    @Override
    public KeyValue<RsuTimKey, ProcessedTim> transform(Void rawKey, DeserializedRawTim rawTim) {
        try {
            if (!rawTim.isValidationFailure()) {
                return processValidTim(rawTim);
            } else {
                return processInvalidTim(rawTim);
            }
        } catch (Exception e) {
            log.error("Exception converting ODE TIM to Processed TIM: {}", e.getMessage(), e);
            return createErrorKeyValuePair();
        }
    }

    /**
     * Process a valid TIM message.
     *
     * @param rawTim The valid TIM data
     * @return Key-value pair with processed TIM
     */
    private KeyValue<RsuTimKey, ProcessedTim> processValidTim(DeserializedRawTim rawTim) {
        OdeMessageFrameData rawValue = new OdeMessageFrameData();
        rawValue.setMetadata(rawTim.getOdeTimMessageFrameData().getMetadata());
        OdeMessageFrameMetadata timMetadata = rawValue.getMetadata();

        rawValue.setPayload(rawTim.getOdeTimMessageFrameData().getPayload());
        TravelerInformationMessageFrame travelerInfoMessageFrame =
                (TravelerInformationMessageFrame) rawValue.getPayload().getData();

        ProcessedTim processedTim =
                createProcessedTim(travelerInfoMessageFrame.getValue(), timMetadata, rawTim.getValidatorResults());
        processedTim.setSchemaVersion(ProcessedSchemaVersions.PROCESSED_TIM_SCHEMA_VERSION);

        // Create key with TIM-specific data
        TravelerInformation travelerInfo = travelerInfoMessageFrame.getValue();
        String packetId = null;
        Integer msgCnt = null;

        if (travelerInfo.getPacketID() != null) {
            packetId = travelerInfo.getPacketID().getValue();
        }
        if (travelerInfo.getMsgCnt() != null) {
            msgCnt = (int) travelerInfo.getMsgCnt().getValue();
        }

        RsuTimKey key = createRsuTimKey(timMetadata.getOriginIp(), packetId, msgCnt);
        return KeyValue.pair(key, processedTim);
    }

    /**
     * Process an invalid TIM message.
     *
     * @param rawTim The invalid TIM data
     * @return Key-value pair with failure information
     */
    private KeyValue<RsuTimKey, ProcessedTim> processInvalidTim(DeserializedRawTim rawTim) {
        ProcessedTim processedTim = createFailureProcessedTim(rawTim.getValidatorResults(), rawTim.getFailedMessage());
        RsuTimKey key = createRsuTimKey(ERROR_RSU_ID);
        return KeyValue.pair(key, processedTim);
    }

    /**
     * Create an error key-value pair for exception handling.
     *
     * @return Key-value pair with error key and null value
     */
    private KeyValue<RsuTimKey, ProcessedTim> createErrorKeyValuePair() {
        RsuTimKey key = createRsuTimKey(ERROR_RSU_ID);
        return KeyValue.pair(key, null);
    }

    /**
     * Create an RSU TIM key.
     *
     * @param rsuId The RSU ID
     * @return Configured RSU TIM key
     */
    private RsuTimKey createRsuTimKey(String rsuId) {
        return new RsuTimKey(rsuId);
    }

    /**
     * Create an RSU TIM key with TIM-specific data.
     *
     * @param rsuId The RSU ID
     * @param packetId The packet ID
     * @param msgCnt The message count
     * @return Configured RSU TIM key
     */
    private RsuTimKey createRsuTimKey(String rsuId, String packetId, Integer msgCnt) {
        return new RsuTimKey(rsuId, packetId, msgCnt);
    }

    @Override
    public void close() {
        // No cleanup required
    }

    // ============================================================================
    // MAIN PROCESSING METHODS
    // ============================================================================

    /**
     * Create a processed TIM object from ASN.1 data.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @param metadata The ODE message frame metadata
     * @param validationMessages List of validation messages
     * @return Processed TIM object
     */
    public ProcessedTim createProcessedTim(TravelerInformation travelerInfo, OdeMessageFrameMetadata metadata,
            List<ProcessedValidationMessage> validationMessages) {
        ZonedDateTime odeDate = Instant.parse(metadata.getOdeReceivedAt()).atZone(ZoneId.of(UTC_ZONE_ID));
        ProcessedTim processedTim = initializeProcessedTim(metadata, travelerInfo, odeDate);

        setComplianceInformation(processedTim, validationMessages);
        setBasicTimProperties(processedTim, travelerInfo);
        setFeatureCollection(processedTim, travelerInfo, odeDate);
        setLocation(processedTim, travelerInfo);

        return processedTim;
    }

    /**
     * Initialize the basic ProcessedTim object with metadata and timestamp.
     *
     * @param metadata The ODE message frame metadata
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @param odeDate The parsed ODE date
     * @return Initialized ProcessedTim object
     */
    private ProcessedTim initializeProcessedTim(OdeMessageFrameMetadata metadata, TravelerInformation travelerInfo,
            ZonedDateTime odeDate) {
        ProcessedTim processedTim = new ProcessedTim();
        processedTim.setOdeReceivedAt(metadata.getOdeReceivedAt());
        processedTim.setOriginIp(metadata.getOriginIp());
        processedTim.setAsn1(metadata.getAsn1());

        ZonedDateTime creationTimestamp =
                J2735DateTimeConverter.generateUTCTimestamp(travelerInfo.getTimeStamp(), odeDate);
        processedTim.setTimeStamp(creationTimestamp);

        return processedTim;
    }

    /**
     * Set compliance information for the processed TIM.
     *
     * @param processedTim The ProcessedTim object to update
     * @param validationMessages List of validation messages
     */
    private void setComplianceInformation(ProcessedTim processedTim,
            List<ProcessedValidationMessage> validationMessages) {
        List<ProcessedCompliance> complianceList = new ArrayList<>();
        ProcessedCompliance compliance = new ProcessedCompliance();
        compliance.setStandard(ProcessedCompliance.Standard.ITWG);
        compliance.setCompliant(validationMessages.isEmpty());
        compliance.setValidationMessages(validationMessages);
        complianceList.add(compliance);
        processedTim.setCompliance(complianceList);
    }

    /**
     * Set basic TIM properties from ASN.1 object.
     *
     * @param processedTim The ProcessedTim object to update
     * @param travelerInfo The ASN.1 TravelerInformation object
     */
    private void setBasicTimProperties(ProcessedTim processedTim, TravelerInformation travelerInfo) {
        if (travelerInfo.getMsgCnt() != null) {
            processedTim.setMsgCnt((int) travelerInfo.getMsgCnt().getValue());
        }

        if (travelerInfo.getPacketID() != null) {
            String packetId = travelerInfo.getPacketID().getValue();
            processedTim.setPacketId(packetId);
            // GNIS code is the first 3 bytes of the packet ID converted from hex to decimal
            processedTim.setGnisRegionId(
                    Integer.parseInt(packetId.substring(0, PACKET_ID_GNIS_LENGTH), PACKET_ID_GNIS_RADIX));
        }
    }

    /**
     * Set the feature collection for the processed TIM.
     *
     * @param processedTim The ProcessedTim object to update
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @param odeDate The parsed ODE date
     */
    private void setFeatureCollection(ProcessedTim processedTim, TravelerInformation travelerInfo,
            ZonedDateTime odeDate) {
        try {
            ProcessedTimFeatureCollection featureCollection = new ProcessedTimFeatureCollection();
            List<ProcessedTimFeature<?>> features = processDataFrames(travelerInfo, odeDate);
            featureCollection.setFeatures(features);
            processedTim.setRegionFeatureCollection(featureCollection);
        } catch (Exception e) {
            log.error("Error processing TIM ASN.1 data: {}", e.getMessage(), e);
            // Create empty feature collection if processing fails
            ProcessedTimFeatureCollection featureCollection = new ProcessedTimFeatureCollection();
            featureCollection.setFeatures(new ArrayList<>());
            processedTim.setRegionFeatureCollection(featureCollection);
        }
    }

    /**
     * Set the location field for MongoDB 2D sphere indexing.
     *
     * @param processedTim The ProcessedTim object to update
     * @param travelerInfo The ASN.1 TravelerInformation object
     */
    private void setLocation(ProcessedTim processedTim, TravelerInformation travelerInfo) {
        try {
            org.locationtech.jts.geom.Point jtsPoint = calculateCenterLocationFromRegions(travelerInfo);
            if (jtsPoint != null) {
                // Convert JTS Point to GeoJSON Point
                Point geoJsonPoint = new Point(jtsPoint.getX(), jtsPoint.getY());
                processedTim.setLocation(geoJsonPoint);
            }
        } catch (Exception e) {
            log.error("Error calculating center location: {}", e.getMessage(), e);
            // Location will remain null if calculation fails
        }
    }

    /**
     * Process data frames from the traveler information.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @param odeDate The parsed ODE date
     * @return List of processed TIM features
     */
    private List<ProcessedTimFeature<?>> processDataFrames(TravelerInformation travelerInfo, ZonedDateTime odeDate) {
        List<ProcessedTimFeature<?>> features = new ArrayList<>();

        if (travelerInfo.getDataFrames() != null) {
            for (int i = 0; i < travelerInfo.getDataFrames().size(); i++) {
                TravelerDataFrame dataFrame = travelerInfo.getDataFrames().get(i);
                ProcessedTimFeature<?> feature = createProcessedTimFeatureFromAsnData(odeDate, dataFrame, i);
                if (feature != null) {
                    features.add(feature);
                }
            }
        }

        return features;
    }

    /**
     * Calculate the center location from all regions in the TIM message.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @return JTS Point representing the center location, or null if no regions found
     */
    private org.locationtech.jts.geom.Point calculateCenterLocationFromRegions(TravelerInformation travelerInfo) {
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

    // ============================================================================
    // GEOMETRY PROCESSING METHODS
    // ============================================================================

    /**
     * Convert TIM region to appropriate GeoJSON geometry based on region type.
     *
     * @param region The geographical path region
     * @return Appropriate GeoJSON geometry or null if processing fails
     */
    private Geometry createGeometryFromRegion(GeographicalPath region) {
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
     * Create geometry based on the region type.
     *
     * @param coordinates The processed coordinates
     * @param regionType The type of region
     * @return Appropriate geometry object
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
     * Process offset path coordinates
     */
    private List<List<Double>> processOffsetPath(GeographicalPath region, OffsetSystem path) {
        List<List<Double>> coordinates = new ArrayList<>();
        Position3D anchor = region.getAnchor();
        if (anchor == null) {
            return coordinates;
        }

        // Convert anchor to decimal degrees
        double anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
        double anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());

        List<List<Double>> points = new ArrayList<>();
        List<Double> prevPoint = Arrays.asList(anchorLon, anchorLat);

        // Get scale factor (zoom) - defaults to 0 (1:1 zoom) if not present
        int scale = 0;
        if (path.getScale() != null) {
            scale = (int) path.getScale().getValue();
        }

        OffsetChoice choice = path.getOffset();
        if (choice != null) {
            if (choice.getXy() != null) {
                // Handle XY offset points
                NodeListXY nodes = choice.getXy();
                for (NodeXY node : nodes.getNodes()) {
                    List<Double> nextPoint = convertNodeToCoordinate(node.getDelta(), prevPoint, scale);
                    if (nextPoint != null) {
                        points.add(nextPoint);
                        prevPoint = nextPoint;
                    }
                }
            } else if (choice.getLl() != null) {
                // Handle LL (latitude/longitude) points
                NodeListLL nodes = choice.getLl();
                for (NodeLL node : nodes.getNodes()) {
                    List<Double> nextPoint = convertLLNodeToCoordinate(node.getDelta(), prevPoint, scale);
                    if (nextPoint != null) {
                        points.add(nextPoint);
                        prevPoint = nextPoint;
                    }
                }
            }
        }

        coordinates.addAll(points);
        return coordinates;
    }

    /**
     * Convert XY node to coordinate
     */
    private List<Double> convertNodeToCoordinate(NodeOffsetPointXY node, List<Double> prevPoint, int scale) {
        if (node.getNode_XY1() != null) {
            Node_XY_20b xy = node.getNode_XY1();
            return offsetCoordinate(prevPoint, xy.getX().getValue(), xy.getY().getValue(), scale);
        } else if (node.getNode_XY2() != null) {
            Node_XY_22b xy = node.getNode_XY2();
            return offsetCoordinate(prevPoint, xy.getX().getValue(), xy.getY().getValue(), scale);
        } else if (node.getNode_XY3() != null) {
            Node_XY_24b xy = node.getNode_XY3();
            return offsetCoordinate(prevPoint, xy.getX().getValue(), xy.getY().getValue(), scale);
        } else if (node.getNode_XY4() != null) {
            Node_XY_26b xy = node.getNode_XY4();
            return offsetCoordinate(prevPoint, xy.getX().getValue(), xy.getY().getValue(), scale);
        } else if (node.getNode_XY5() != null) {
            Node_XY_28b xy = node.getNode_XY5();
            return offsetCoordinate(prevPoint, xy.getX().getValue(), xy.getY().getValue(), scale);
        } else if (node.getNode_XY6() != null) {
            Node_XY_32b xy = node.getNode_XY6();
            return offsetCoordinate(prevPoint, xy.getX().getValue(), xy.getY().getValue(), scale);
        } else if (node.getNode_LatLon() != null) {
            Node_LLmD_64b ll = node.getNode_LatLon();
            return Arrays.asList(FieldConversions.convertLong(ll.getLon().getValue()),
                    FieldConversions.convertLat(ll.getLat().getValue()));
        }
        return null;
    }

    /**
     * Convert LL node to coordinate
     */
    private List<Double> convertLLNodeToCoordinate(NodeOffsetPointLL node, List<Double> prevPoint, int scale) {
        if (node == null) {
            return null;
        }

        if (node.getNode_LL1() != null) {
            Node_LL_24B ll = node.getNode_LL1();
            return offsetLatLonCoordinate(prevPoint, ll.getLon().getValue(), ll.getLat().getValue(), scale);
        } else if (node.getNode_LL2() != null) {
            Node_LL_28B ll = node.getNode_LL2();
            return offsetLatLonCoordinate(prevPoint, ll.getLon().getValue(), ll.getLat().getValue(), scale);
        } else if (node.getNode_LL3() != null) {
            Node_LL_32B ll = node.getNode_LL3();
            return offsetLatLonCoordinate(prevPoint, ll.getLon().getValue(), ll.getLat().getValue(), scale);
        } else if (node.getNode_LL4() != null) {
            Node_LL_36B ll = node.getNode_LL4();
            return offsetLatLonCoordinate(prevPoint, ll.getLon().getValue(), ll.getLat().getValue(), scale);
        } else if (node.getNode_LL5() != null) {
            Node_LL_44B ll = node.getNode_LL5();
            return offsetLatLonCoordinate(prevPoint, ll.getLon().getValue(), ll.getLat().getValue(), scale);
        } else if (node.getNode_LL6() != null) {
            Node_LL_48B ll = node.getNode_LL6();
            return offsetLatLonCoordinate(prevPoint, ll.getLon().getValue(), ll.getLat().getValue(), scale);
        } else if (node.getNode_LatLon() != null) {
            Node_LLmD_64b ll = node.getNode_LatLon();
            return Arrays.asList(FieldConversions.convertLong(ll.getLon().getValue()),
                    FieldConversions.convertLat(ll.getLat().getValue()));
        }
        return null;
    }

    /**
     * Offset coordinate using XY offsets (centimeters) with proper geodetic calculations.
     *
     * @param prevPoint The previous coordinate point [longitude, latitude]
     * @param offsetX X offset in centimeters
     * @param offsetY Y offset in centimeters
     * @param scale Scale factor (zoom) applied as 2^N
     * @return New coordinate point [longitude, latitude]
     */
    private List<Double> offsetCoordinate(List<Double> prevPoint, long offsetX, long offsetY, int scale) {
        double prevLon = prevPoint.get(0);
        double prevLat = prevPoint.get(1);

        // Apply scale factor: multiply by 2^scale
        double scaleFactor = Math.pow(2, scale);
        double scaledOffsetX = offsetX * scaleFactor;
        double scaledOffsetY = offsetY * scaleFactor;

        // Convert centimeter offsets to meters
        double offsetXMeters = scaledOffsetX / CENTIMETERS_TO_METERS_DIVISOR;
        double offsetYMeters = scaledOffsetY / CENTIMETERS_TO_METERS_DIVISOR;

        // Use proper geodetic calculations for accurate coordinate transformation
        Coordinate newCoord = GeodeticUtils.offsetCoordinate(prevLon, prevLat, offsetXMeters, offsetYMeters);

        return Arrays.asList(newCoord.x, newCoord.y);
    }

    /**
     * Offset coordinate using lat/lon offsets
     */
    private List<Double> offsetLatLonCoordinate(List<Double> prevPoint, long offsetLon, long offsetLat, int scale) {
        double prevLon = prevPoint.get(0);
        double prevLat = prevPoint.get(1);

        // Apply scale factor: multiply by 2^scale
        double scaleFactor = Math.pow(2, scale);
        double scaledOffsetLon = offsetLon * scaleFactor;
        double scaledOffsetLat = offsetLat * scaleFactor;

        // Convert scaled offsets to decimal degrees
        double latOffset = FieldConversions.convertLat((long) scaledOffsetLat);
        double lonOffset = FieldConversions.convertLong((long) scaledOffsetLon);

        return Arrays.asList(prevLon + lonOffset, prevLat + latOffset);
    }

    /**
     * Process geometry (circle)
     */
    private List<List<Double>> processGeometry(GeographicalPath region, GeometricProjection geometry) {
        if (geometry.getCircle() != null) {
            return processCircle(geometry.getCircle());
        }
        return new ArrayList<>();
    }

    /**
     * Process circle geometry by creating an approximation using multiple points.
     *
     * @param circle The circle geometry to process
     * @return List of coordinate points forming the circle approximation
     */
    private List<List<Double>> processCircle(Circle circle) {
        Position3D center = circle.getCenter();
        double centerLat = FieldConversions.convertLat(center.getLat().getValue());
        double centerLon = FieldConversions.convertLong(center.getLong_().getValue());
        double radius = circle.getRadius().getValue();

        return createCircleApproximation(centerLon, centerLat, radius);
    }

    /**
     * Create a circle approximation using multiple points.
     *
     * @param centerLon Center longitude
     * @param centerLat Center latitude
     * @param radius Radius in meters
     * @return List of coordinate points forming the circle
     */
    private List<List<Double>> createCircleApproximation(double centerLon, double centerLat, double radius) {
        List<List<Double>> coordinates = new ArrayList<>();

        for (int i = 0; i <= CIRCLE_APPROXIMATION_POINTS; i++) {
            double angle = 360.0 * i / CIRCLE_APPROXIMATION_POINTS; // Angle in degrees
            List<Double> point = calculateDestinationPoint(centerLon, centerLat, angle, radius);
            coordinates.add(point);
        }

        return coordinates;
    }

    /**
     * Calculate destination point given bearing and distance using proper geodetic calculations
     */
    private List<Double> calculateDestinationPoint(double lon, double lat, double bearing, double distanceMeters) {
        Coordinate destCoord = GeodeticUtils.calculateDestination(lon, lat, bearing, distanceMeters);
        return Arrays.asList(destCoord.x, destCoord.y);
    }

    /**
     * Create a rectangle from an anchor point with default padding.
     *
     * @param anchor The anchor position
     * @return List of coordinate points forming a rectangle
     */
    private List<List<Double>> createRectangleFromAnchor(Position3D anchor) {
        double lat = FieldConversions.convertLat(anchor.getLat().getValue());
        double lon = FieldConversions.convertLong(anchor.getLong_().getValue());

        return createRectangleCoordinates(lon, lat, DEFAULT_PADDING_DEGREES);
    }

    /**
     * Create rectangle coordinates around a center point.
     *
     * @param centerLon Center longitude
     * @param centerLat Center latitude
     * @param padding Padding in degrees
     * @return List of coordinate points forming a rectangle
     */
    private List<List<Double>> createRectangleCoordinates(double centerLon, double centerLat, double padding) {
        List<List<Double>> coordinates = new ArrayList<>();

        coordinates.add(Arrays.asList(centerLon - padding, centerLat - padding));
        coordinates.add(Arrays.asList(centerLon + padding, centerLat - padding));
        coordinates.add(Arrays.asList(centerLon + padding, centerLat + padding));
        coordinates.add(Arrays.asList(centerLon - padding, centerLat + padding));
        coordinates.add(Arrays.asList(centerLon - padding, centerLat - padding)); // Close the rectangle

        return coordinates;
    }

    /**
     * Create LineString from coordinates
     */
    private LineString createLineStringFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates.isEmpty()) {
            return null;
        }

        double[][] coords =
                coordinates.stream().map(point -> new double[] {point.get(0), point.get(1)}).toArray(double[][]::new);

        return new LineString(coords);
    }

    /**
     * Create Polygon from coordinates
     */
    private Polygon createPolygonFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates.isEmpty()) {
            return null;
        }

        // Ensure the polygon is closed
        List<List<Double>> closedCoords = new ArrayList<>(coordinates);
        if (closedCoords.size() > 1 && !closedCoords.get(0).equals(closedCoords.get(closedCoords.size() - 1))) {
            closedCoords.add(new ArrayList<>(closedCoords.get(0)));
        }

        double[][][] coords = new double[1][][];
        coords[0] =
                closedCoords.stream().map(point -> new double[] {point.get(0), point.get(1)}).toArray(double[][]::new);

        return new Polygon(coords);
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
     * Create MultiLineString from coordinates
     */
    private MultiLineString createMultiLineStringFromCoordinates(List<List<List<Double>>> allCoordinates) {
        double[][][] coords = allCoordinates
                .stream().map(coordinates -> coordinates.stream()
                        .map(point -> new double[] {point.get(0), point.get(1)}).toArray(double[][]::new))
                .toArray(double[][][]::new);

        return new MultiLineString(coords);
    }

    /**
     * Create MultiPolygon from coordinates
     */
    private MultiPolygon createMultiPolygonFromCoordinates(List<List<List<Double>>> allCoordinates) {
        double[][][][] coords = allCoordinates.stream().map(coordinates -> {
            // Ensure each polygon is closed
            List<List<Double>> closedCoords = new ArrayList<>(coordinates);
            if (closedCoords.size() > 1 && !closedCoords.get(0).equals(closedCoords.get(closedCoords.size() - 1))) {
                closedCoords.add(new ArrayList<>(closedCoords.get(0)));
            }

            double[][][] polygon = new double[1][][];
            polygon[0] = closedCoords.stream().map(point -> new double[] {point.get(0), point.get(1)})
                    .toArray(double[][]::new);
            return polygon;
        }).toArray(double[][][][]::new);

        return new MultiPolygon(coords);
    }

    // ============================================================================
    // ASN.1 DATA PROCESSING METHODS
    // ============================================================================

    /**
     * Create a processed TIM feature from ASN.1 data.
     *
     * @param odeDate The ODE date
     * @param dataFrame The traveler data frame
     * @param featureId The feature ID
     * @return Processed TIM feature or null if processing fails
     */
    private ProcessedTimFeature<?> createProcessedTimFeatureFromAsnData(ZonedDateTime odeDate,
            TravelerDataFrame dataFrame, int featureId) {
        try {
            ProcessedTimProperties properties = createProcessedTimProperties(dataFrame, odeDate);
            Geometry geometry = createGeometryFromDataFrame(dataFrame);

            return new ProcessedTimFeature<>(featureId, geometry, properties);
        } catch (Exception e) {
            log.error("Error creating TIM feature from ASN.1 data: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create processed TIM properties from ASN.1 data.
     *
     * @param dataFrame The traveler data frame
     * @param odeDate The ODE date
     * @return Processed TIM properties
     */
    private ProcessedTimProperties createProcessedTimProperties(TravelerDataFrame dataFrame, ZonedDateTime odeDate) {
        ProcessedTimProperties properties = new ProcessedTimProperties();

        setDeploymentAgency(properties, dataFrame);
        setValidityPeriod(properties, dataFrame, odeDate);
        setPriority(properties, dataFrame);
        setRegionAndDirectionInfo(properties, dataFrame);
        setContent(properties, dataFrame);

        return properties;
    }

    /**
     * Set deployment agency based on frame type.
     *
     * @param properties The properties object to update
     * @param dataFrame The data frame containing frame type information
     */
    private void setDeploymentAgency(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getFrameType() != null) {
            TravelerInfoType frameType = dataFrame.getFrameType();
            ProcessedDeploymentAgency agency = ProcessedDeploymentAgency.fromValue(frameType);
            properties.setDeploymentAgencyType(agency);
        }
    }

    /**
     * Set validity period for the TIM feature.
     *
     * @param properties The properties object to update
     * @param dataFrame The data frame containing timing information
     * @param odeDate The ODE date
     */
    private void setValidityPeriod(ProcessedTimProperties properties, TravelerDataFrame dataFrame,
            ZonedDateTime odeDate) {
        ProcessedValidityPeriod validityPeriod = new ProcessedValidityPeriod();
        ZonedDateTime startDateTime = odeDate;

        if (dataFrame.getStartYear() != null && dataFrame.getStartTime() != null) {
            int startYear = (int) dataFrame.getStartYear().getValue();
            MinuteOfTheYear startTimeMoy = dataFrame.getStartTime();
            startDateTime = J2735DateTimeConverter.generateUTCTimestamp(startTimeMoy, null, odeDate, startYear);
            validityPeriod.setStartTime(startDateTime);
        }

        if (dataFrame.getDurationTime() != null) {
            int duration = (int) dataFrame.getDurationTime().getValue();

            if (duration != INFINITE_DURATION_VALUE) {
                validityPeriod.setInfinite(false);
                validityPeriod.setEndTime(startDateTime.plusMinutes(duration));
            } else {
                validityPeriod.setInfinite(true);
                validityPeriod.setEndTime(INFINITE_VALIDITY_PERIOD);
            }
        } else {
            validityPeriod.setInfinite(true);
        }

        properties.setValidityPeriod(validityPeriod);
    }

    /**
     * Set priority from data frame.
     *
     * @param properties The properties object to update
     * @param dataFrame The data frame containing priority information
     */
    private void setPriority(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getPriority() != null) {
            properties.setPriority((int) dataFrame.getPriority().getValue());
        }
    }

    /**
     * Set region and direction information.
     *
     * @param properties The properties object to update
     * @param dataFrame The data frame containing region information
     */
    private void setRegionAndDirectionInfo(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getRegions() != null && !dataFrame.getRegions().isEmpty()) {
            List<ProcessedRegionInfoBase> regionInfoList = new ArrayList<>();

            // Create one region info object for each region
            for (GeographicalPath region : dataFrame.getRegions()) {
                ProcessedRegionInfoBase regionInfo = createProcessedRegionInfoFromAsnData(region);
                regionInfoList.add(regionInfo);
            }

            properties.setRegionInfoList(regionInfoList);
        }
    }

    /**
     * Set content information.
     *
     * @param properties The properties object to update
     * @param dataFrame The data frame containing content information
     */
    private void setContent(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getContent() != null) {
            ProcessedTimContent content = createProcessedTimContentFromAsnData(dataFrame.getContent());
            properties.setContent(content);
        }
    }

    /**
     * Create geometry from data frame regions.
     *
     * @param dataFrame The data frame containing region information
     * @return Appropriate geometry object or null
     */
    private Geometry createGeometryFromDataFrame(TravelerDataFrame dataFrame) {
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
     * Create processed region info from ASN.1 data.
     *
     * @param region The geographical path region
     * @return Processed region info
     */
    private ProcessedRegionInfoBase createProcessedRegionInfoFromAsnData(GeographicalPath region) {
        ProcessedRegionType regionType = determineRegionType(region);
        ProcessedElevationProfile elevationProfile = new ProcessedElevationProfile();

        // Set anchor point and elevation from anchor
        setAnchorPointAndElevation(region, elevationProfile);

        // Create the appropriate region info object based on type
        ProcessedRegionInfoBase regionInfo = createRegionInfoByType(regionType, region, elevationProfile);

        // Set direction info for this specific region
        ProcessedDirectionInfoBase directionInfo = createProcessedDirectionInfoFromAsnData(region, regionType);
        regionInfo.setDirectionInfo(directionInfo);

        return regionInfo;
    }

    /**
     * Determine the region type from ASN.1 data.
     *
     * @param region The geographical path region
     * @return The determined region type
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
     * Set anchor point and elevation from region data.
     *
     * @param region The geographical path region
     * @param elevationProfile The elevation profile to update
     * @return The created anchor point
     */
    private ProcessedAnchorPoint setAnchorPointAndElevation(GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        if (region.getAnchor() != null) {
            return createAnchorPoint(region.getAnchor(), elevationProfile);
        }
        return null;
    }

    /**
     * Create the appropriate region info object based on type.
     *
     * @param regionType The region type
     * @param region The geographical path region
     * @param elevationProfile The elevation profile
     * @return The appropriate region info object
     */
    private ProcessedRegionInfoBase createRegionInfoByType(ProcessedRegionType regionType, GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        ProcessedRegionInfoBase regionInfo;

        switch (regionType) {
            case PATH:
                regionInfo = new ProcessedPathRegionInfo();
                setPathRegionInfo((ProcessedPathRegionInfo) regionInfo, region, elevationProfile);
                break;
            case POLYGON:
                regionInfo = new ProcessedPolygonRegionInfo();
                setPolygonRegionInfo((ProcessedPolygonRegionInfo) regionInfo, region, elevationProfile);
                break;
            case CIRCLE:
                regionInfo = new ProcessedCircleRegionInfo();
                setCircleRegionInfo((ProcessedCircleRegionInfo) regionInfo, region, elevationProfile);
                break;
            case UNKNOWN:
            default:
                regionInfo = new ProcessedUnknownRegionInfo();
                setUnknownRegionInfo((ProcessedUnknownRegionInfo) regionInfo, region, elevationProfile);
                break;
        }

        // Set common fields
        regionInfo.setRegionType(regionType);

        // Set anchor point and elevation from anchor
        ProcessedAnchorPoint anchorPoint = setAnchorPointAndElevation(region, elevationProfile);
        regionInfo.setAnchorPoint(anchorPoint);

        // Only set elevation profile if there's elevation data in the anchor point
        if (anchorPoint != null && anchorPoint.getElevationMeters() != null) {
            regionInfo.setElevationProfile(elevationProfile);
        }

        return regionInfo;
    }

    /**
     * Set path region info with lane width profile.
     *
     * @param regionInfo The path region info to update
     * @param region The geographical path region
     * @param elevationProfile The elevation profile
     */
    private void setPathRegionInfo(ProcessedPathRegionInfo regionInfo, GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        ProcessedLaneWidthProfile laneWidthProfile = new ProcessedLaneWidthProfile();
        processPathProfiles(region, elevationProfile, laneWidthProfile);
        regionInfo.setLaneWidthProfile(laneWidthProfile);
    }

    /**
     * Set polygon region info (no lane width profile).
     *
     * @param regionInfo The polygon region info to update
     * @param region The geographical path region
     * @param elevationProfile The elevation profile
     */
    private void setPolygonRegionInfo(ProcessedPolygonRegionInfo regionInfo, GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        // Polygon regions don't have lane width profiles
        // Process elevation profile for polygon regions
        processPolygonElevationProfile(region, elevationProfile);
    }

    /**
     * Set circle region info with radius (no lane width profile).
     *
     * @param regionInfo The circle region info to update
     * @param region The geographical path region
     * @param elevationProfile The elevation profile
     */
    private void setCircleRegionInfo(ProcessedCircleRegionInfo regionInfo, GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        // Set radius for circle regions
        if (region.getDescription() != null && region.getDescription().getGeometry() != null
                && region.getDescription().getGeometry().getCircle() != null) {
            Circle circle = region.getDescription().getGeometry().getCircle();
            if (circle.getRadius() != null) {
                regionInfo.setRadius((int) circle.getRadius().getValue());
            }
        }

        // Only set elevation segments if there's a default elevation from the anchor
        // Circles don't have corresponding nodes, so always set empty array if default elevation exists
        if (elevationProfile.getDefaultElevationMeters() != null) {
            elevationProfile.setNodeElevationMeters(new ArrayList<>());
        }
    }

    /**
     * Set unknown region info (may have lane width profile).
     *
     * @param regionInfo The unknown region info to update
     * @param region The geographical path region
     * @param elevationProfile The elevation profile
     */
    private void setUnknownRegionInfo(ProcessedUnknownRegionInfo regionInfo, GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        // Unknown regions may have lane width if available
        if (region.getLaneWidth() != null) {
            ProcessedLaneWidthProfile laneWidthProfile = new ProcessedLaneWidthProfile();
            int laneWidth = (int) region.getLaneWidth().getValue();
            laneWidthProfile.setDefaultWidthMeters(FieldConversions.convertLaneWidth(laneWidth));
            regionInfo.setLaneWidthProfile(laneWidthProfile);
        }

        // Only set elevation segments if there's a default elevation from the anchor
        List<Double> segmentsElevation = new ArrayList<>();
        Double currentElevation = elevationProfile.getDefaultElevationMeters();
        if (currentElevation != null) {
            // Process elevation for all path nodes
            if (region.getDescription() != null && region.getDescription().getPath() != null) {
                OffsetSystem path = region.getDescription().getPath();
                if (path != null && path.getOffset() != null) {
                    if (path.getOffset().getXy() != null) {
                        processXYNodesForElevation(path.getOffset().getXy().getNodes(), segmentsElevation,
                                currentElevation, 0); // Default scale for elevation processing
                    } else if (path.getOffset().getLl() != null) {
                        processLLNodesForElevation(path.getOffset().getLl().getNodes(), segmentsElevation,
                                currentElevation, 0); // Default scale for elevation processing
                    }
                }
            }

            // Always set elevation segments when there's a default elevation
            elevationProfile.setNodeElevationMeters(segmentsElevation);
        }
    }


    /**
     * Process elevation profile for polygon regions.
     *
     * @param region The geographical path region
     * @param elevationProfile The elevation profile to update
     */
    private void processPolygonElevationProfile(GeographicalPath region, ProcessedElevationProfile elevationProfile) {
        List<Double> segmentsElevation = new ArrayList<>();
        Double currentElevation = elevationProfile.getDefaultElevationMeters();

        // Only process elevation if there's a default elevation from the anchor
        if (currentElevation != null) {
            // For polygon regions, process elevation for all path nodes
            if (region.getDescription() != null && region.getDescription().getPath() != null) {
                OffsetSystem path = region.getDescription().getPath();
                if (path != null && path.getOffset() != null) {
                    if (path.getOffset().getXy() != null) {
                        processXYNodesForElevation(path.getOffset().getXy().getNodes(), segmentsElevation,
                                currentElevation, 0); // Default scale for elevation processing
                    } else if (path.getOffset().getLl() != null) {
                        processLLNodesForElevation(path.getOffset().getLl().getNodes(), segmentsElevation,
                                currentElevation, 0); // Default scale for elevation processing
                    }
                }
            }

            // Always set elevation segments for polygon regions when there's a default elevation
            elevationProfile.setNodeElevationMeters(segmentsElevation);
        }
    }

    /**
     * Create anchor point from position data.
     *
     * @param anchor The 3D position anchor
     * @param elevationProfile The elevation profile to update
     * @return Processed anchor point
     */
    private ProcessedAnchorPoint createAnchorPoint(Position3D anchor, ProcessedElevationProfile elevationProfile) {
        ProcessedAnchorPoint anchorPoint = new ProcessedAnchorPoint();

        if (anchor.getLat() != null) {
            anchorPoint.setLatitude(FieldConversions.convertLat(anchor.getLat().getValue()));
        }
        if (anchor.getLong_() != null) {
            anchorPoint.setLongitude(FieldConversions.convertLong(anchor.getLong_().getValue()));
        }
        if (anchor.getElevation() != null) {
            anchorPoint.setElevationMeters(FieldConversions.convertElevation(anchor.getElevation().getValue()));
            elevationProfile
                    .setDefaultElevationMeters(FieldConversions.convertElevation(anchor.getElevation().getValue()));
        }

        return anchorPoint;
    }


    /**
     * Process XY nodes for elevation only (for polygon regions).
     *
     * @param nodes List of XY nodes
     * @param segmentsElevation List to store segment elevations
     * @param currentElevation Current elevation
     * @param scale Scale factor (zoom) applied as 2^N
     */
    private void processXYNodesForElevation(List<NodeXY> nodes, List<Double> segmentsElevation, Double currentElevation,
            int scale) {
        for (NodeXY segment : nodes) {
            NodeAttributeSetXY attributes = segment.getAttributes();
            if (attributes != null) {
                currentElevation = processElevationAttribute(attributes.getDElevation(), currentElevation,
                        segmentsElevation, new ArrayList<>());
            } else {
                // Always add elevation for each node, even without attributes
                segmentsElevation.add(currentElevation);
            }
        }
    }

    /**
     * Process LL nodes for elevation only (for polygon regions).
     *
     * @param nodes List of LL nodes
     * @param segmentsElevation List to store segment elevations
     * @param currentElevation Current elevation
     * @param scale Scale factor (zoom) applied as 2^N
     */
    private void processLLNodesForElevation(List<NodeLL> nodes, List<Double> segmentsElevation, Double currentElevation,
            int scale) {
        for (NodeLL segment : nodes) {
            NodeAttributeSetLL attributes = segment.getAttributes();
            if (attributes != null) {
                currentElevation = processElevationAttribute(attributes.getDElevation(), currentElevation,
                        segmentsElevation, new ArrayList<>());
            } else {
                // Always add elevation for each node, even without attributes
                segmentsElevation.add(currentElevation);
            }
        }
    }


    /**
     * Process path profiles for lane width and elevation.
     *
     * @param region The geographical path region
     * @param elevationProfile The elevation profile to update
     * @param laneWidthProfile The lane width profile to update
     */
    private void processPathProfiles(GeographicalPath region, ProcessedElevationProfile elevationProfile,
            ProcessedLaneWidthProfile laneWidthProfile) {
        // Always set the default lane width
        if (region.getLaneWidth() != null) {
            int laneWidth = (int) region.getLaneWidth().getValue();
            laneWidthProfile.setDefaultWidthMeters(FieldConversions.convertLaneWidth(laneWidth));
        }

        List<Double> segmentsMeters = new ArrayList<>();
        List<Double> segmentsElevation = new ArrayList<>();

        Double currentLaneWidth = laneWidthProfile.getDefaultWidthMeters();
        Double currentElevation = elevationProfile.getDefaultElevationMeters();

        // Process offset nodes if they exist
        OffsetSystem path = region.getDescription().getPath();
        if (path != null && path.getOffset() != null) {
            // Get scale factor (zoom) - defaults to 0 (1:1 zoom) if not present
            int scale = 0;
            if (path.getScale() != null) {
                scale = (int) path.getScale().getValue();
            }

            if (path.getOffset().getXy() != null) {
                processXYNodes(path.getOffset().getXy().getNodes(), segmentsMeters, segmentsElevation, currentLaneWidth,
                        currentElevation, scale);
            } else if (path.getOffset().getLl() != null) {
                processLLNodes(path.getOffset().getLl().getNodes(), segmentsMeters, segmentsElevation, currentLaneWidth,
                        currentElevation, scale);
            }
        }

        // Always set the lane width segments list, even if empty
        laneWidthProfile.setNodeLaneWidthMeters(segmentsMeters);

        // Only set elevation segments if there's actual elevation data
        if (!segmentsElevation.isEmpty()) {
            elevationProfile.setNodeElevationMeters(segmentsElevation);
        }
    }

    /**
     * Process XY nodes for path profiles.
     *
     * @param nodes List of XY nodes
     * @param segmentsMeters List to store segment meters
     * @param segmentsElevation List to store segment elevations
     * @param currentLaneWidth Current lane width
     * @param currentElevation Current elevation
     * @param scale Scale factor (zoom) applied as 2^N
     */
    private void processXYNodes(List<NodeXY> nodes, List<Double> segmentsMeters, List<Double> segmentsElevation,
            Double currentLaneWidth, Double currentElevation, int scale) {
        for (NodeXY segment : nodes) {
            NodeAttributeSetXY attributes = segment.getAttributes();
            if (attributes != null) {
                currentLaneWidth = processLaneWidthAttribute(attributes.getDWidth(), currentLaneWidth, segmentsMeters);
                currentElevation = processElevationAttribute(attributes.getDElevation(), currentElevation,
                        segmentsElevation, segmentsMeters);
            } else {
                segmentsMeters.add(currentLaneWidth);
                if (currentElevation != null) {
                    segmentsElevation.add(currentElevation);
                }
            }
        }
    }

    /**
     * Process LL nodes for path profiles.
     *
     * @param nodes List of LL nodes
     * @param segmentsMeters List to store segment meters
     * @param segmentsElevation List to store segment elevations
     * @param currentLaneWidth Current lane width
     * @param currentElevation Current elevation
     * @param scale Scale factor (zoom) applied as 2^N
     */
    private void processLLNodes(List<NodeLL> nodes, List<Double> segmentsMeters, List<Double> segmentsElevation,
            Double currentLaneWidth, Double currentElevation, int scale) {
        for (NodeLL segment : nodes) {
            NodeAttributeSetLL attributes = segment.getAttributes();
            if (attributes != null) {
                currentLaneWidth = processLaneWidthAttribute(attributes.getDWidth(), currentLaneWidth, segmentsMeters);
                currentElevation = processElevationAttribute(attributes.getDElevation(), currentElevation,
                        segmentsElevation, segmentsMeters);
            } else {
                segmentsMeters.add(currentLaneWidth);
                if (currentElevation != null) {
                    segmentsElevation.add(currentElevation);
                }
            }
        }
    }

    /**
     * Process lane width attribute.
     *
     * @param dWidth The width delta object
     * @param currentLaneWidth Current lane width
     * @param segmentsMeters List to store segment meters
     * @return Updated current lane width
     */
    private Double processLaneWidthAttribute(Object dWidth, Double currentLaneWidth, List<Double> segmentsMeters) {
        if (dWidth != null) {
            try {
                Object value = dWidth.getClass().getMethod("getValue").invoke(dWidth);
                long widthValue;

                // Handle both Integer and Long types
                if (value instanceof Long) {
                    widthValue = (Long) value;
                } else if (value instanceof Integer) {
                    widthValue = ((Integer) value).longValue();
                } else {
                    // Try to convert to long using Number interface
                    widthValue = ((Number) value).longValue();
                }

                double newWidth = FieldConversions.calculateLaneWidthOffset(currentLaneWidth, widthValue);
                segmentsMeters.add(newWidth);
                return newWidth;
            } catch (Exception e) {
                log.warn("Error processing lane width attribute: {}", e.getMessage());
                segmentsMeters.add(currentLaneWidth);
                return currentLaneWidth;
            }
        } else {
            segmentsMeters.add(currentLaneWidth);
            return currentLaneWidth;
        }
    }

    /**
     * Process elevation attribute.
     *
     * @param dElevation The elevation delta object of type Offset_B10
     * @param currentElevation Current elevation
     * @param segmentsElevation List to store segment elevations
     * @param segmentsMeters List of segment meters for reference
     * @return Updated current elevation
     */
    private Double processElevationAttribute(Offset_B10 dElevation, Double currentElevation,
            List<Double> segmentsElevation, List<Double> segmentsMeters) {
        if (dElevation != null && currentElevation != null) {
            try {
                long elevationValue = dElevation.getValue();

                double newElevation = FieldConversions.calculateElevationOffset(currentElevation, elevationValue);
                segmentsElevation.add(newElevation);
                return newElevation;
            } catch (Exception e) {
                log.warn("Error processing elevation attribute: {}", e.getMessage());
                segmentsElevation.add(currentElevation);
                return currentElevation;
            }
        } else {
            segmentsElevation.add(currentElevation);
            return currentElevation;
        }
    }


    /**
     * Create processed direction info from ASN.1 data.
     *
     * @param region The geographical path region
     * @param regionType The region type to determine appropriate direction info type
     * @return Processed direction info
     */
    private ProcessedDirectionInfoBase createProcessedDirectionInfoFromAsnData(GeographicalPath region,
            ProcessedRegionType regionType) {
        ProcessedDirectionInfoBase directionInfo;

        // For polygon and circle regions, always use heading-based direction info
        if (regionType == ProcessedRegionType.POLYGON || regionType == ProcessedRegionType.CIRCLE) {
            directionInfo = new ProcessedHeadingDirectionInfo();
            directionInfo.setDirectionType(ProcessedDirectionType.HEADING);
            processHeadingDirectionInfo((ProcessedHeadingDirectionInfo) directionInfo, region);
        } else {
            // For other region types, determine based on available data
            if (region.getDirection() != null) {
                directionInfo = new ProcessedHeadingDirectionInfo();
                directionInfo.setDirectionType(ProcessedDirectionType.HEADING);
                processHeadingDirectionInfo((ProcessedHeadingDirectionInfo) directionInfo, region);
            } else {
                directionInfo = new ProcessedDirectionalityDirectionInfo();
                directionInfo.setDirectionType(ProcessedDirectionType.DIRECTIONALITY);
            }
        }

        // Set directionality only for directionality-type direction info
        if (directionInfo instanceof ProcessedDirectionalityDirectionInfo) {
            setDirectionality((ProcessedDirectionalityDirectionInfo) directionInfo, region);
        }

        return directionInfo;
    }

    /**
     * Process heading direction info with heading list.
     *
     * @param directionInfo The heading direction info to update
     * @param region The geographical path region
     */
    private void processHeadingDirectionInfo(ProcessedHeadingDirectionInfo directionInfo, GeographicalPath region) {
        // Process heading list from direction data
        List<ProcessedHeading> headingList = processHeadingList(region);
        directionInfo.setHeadingList(headingList);
    }

    /**
     * Set directionality from region data.
     *
     * @param directionInfo The direction info to update
     * @param region The geographical path region
     */
    private void setDirectionality(ProcessedDirectionalityDirectionInfo directionInfo, GeographicalPath region) {
        if (region.getDirectionality() != null) {
            String directionality = region.getDirectionality().getName();
            try {
                directionInfo.setDirectionality(ProcessedDirectionality.fromValue(directionality));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown directionality value: {}", directionality);
                directionInfo.setDirectionality(ProcessedDirectionality.UNKNOWN);
            }
        } else {
            directionInfo.setDirectionality(ProcessedDirectionality.UNKNOWN);
        }
    }

    /**
     * Process heading list from direction data.
     *
     * @param region The geographical path region
     * @return List of processed headings
     */
    private List<ProcessedHeading> processHeadingList(GeographicalPath region) {
        List<ProcessedHeading> headingList = new ArrayList<>();

        if (region.getDirection() != null) {
            try {
                // The direction field contains hex-encoded heading information
                // Example: "E0E0" represents heading data
                String directionHex = FieldConversions.extractDirectionValue(region.getDirection());
                if (directionHex != null) {
                    List<ProcessedHeading> headings = parseHeadingFromHex(directionHex);
                    headingList.addAll(headings);
                }
            } catch (Exception e) {
                log.warn("Error processing direction data: {}", e.getMessage());
            }
        }

        return headingList;
    }


    /**
     * Parse heading information from hex-encoded direction string and combine adjacent sectors.
     * 
     * The direction field uses a bitmap representation where each bit represents a 22.5-degree sector starting from
     * North (0°) and moving clockwise. Adjacent active sectors are combined into single heading ranges.
     * 
     * @param directionHex The hex-encoded direction string (e.g., "E0E0")
     * @return List of processed headings with combined adjacent sectors
     */
    private List<ProcessedHeading> parseHeadingFromHex(String directionHex) {
        List<ProcessedHeading> headings = new ArrayList<>();

        if (directionHex == null || directionHex.length() < 4) {
            return headings;
        }

        try {
            // Get active sectors using FieldConversions
            int[] activeSectors = FieldConversions.parseHeadingSectors(directionHex);

            log.debug("Direction hex '{}': found {} active sectors", directionHex, activeSectors.length);

            if (activeSectors.length == 0) {
                return headings;
            }

            // Combine adjacent sectors
            List<int[]> combinedRanges = combineAdjacentSectors(activeSectors);

            // Create ProcessedHeading objects for each combined range
            for (int[] range : combinedRanges) {
                int startSector = range[0];
                int endSector = range[1];

                // Calculate center heading and total range
                double startHeading = FieldConversions.sectorBitToHeading(startSector);
                double endHeading = FieldConversions.sectorBitToHeading(endSector);
                double centerHeading = (startHeading + endHeading) / 2.0;
                double totalRange = (endSector - startSector + 1) * FieldConversions.getHeadingSectorRange();

                ProcessedHeading processedHeading = new ProcessedHeading();
                processedHeading.setHeading(centerHeading);
                processedHeading.setRange(totalRange);

                headings.add(processedHeading);

                log.debug("Combined sectors {}-{}: center={}°, range={}°", startSector, endSector, centerHeading,
                        totalRange);
            }

            log.debug("Parsed direction hex '{}': created {} combined heading ranges", directionHex, headings.size());

        } catch (Exception e) {
            log.warn("Error parsing direction hex '{}': {}", directionHex, e.getMessage());
        }

        return headings;
    }

    /**
     * Combine adjacent active sectors into ranges.
     * 
     * @param activeSectors Array of active sector bit positions
     * @return List of ranges [startSector, endSector]
     */
    private List<int[]> combineAdjacentSectors(int[] activeSectors) {
        List<int[]> ranges = new ArrayList<>();

        if (activeSectors.length == 0) {
            return ranges;
        }

        // Sort sectors to ensure proper ordering
        java.util.Arrays.sort(activeSectors);

        int rangeStart = activeSectors[0];
        int rangeEnd = activeSectors[0];

        for (int i = 1; i < activeSectors.length; i++) {
            int currentSector = activeSectors[i];

            // Check if this sector is adjacent to the current range
            if (currentSector == rangeEnd + 1) {
                // Extend the current range
                rangeEnd = currentSector;
            } else {
                // End the current range and start a new one
                ranges.add(new int[] {rangeStart, rangeEnd});
                rangeStart = currentSector;
                rangeEnd = currentSector;
            }
        }

        // Add the final range
        ranges.add(new int[] {rangeStart, rangeEnd});

        return ranges;
    }


    /**
     * Create processed TIM content from ASN.1 data.
     *
     * @param content The content choice from ASN.1
     * @return Processed TIM content
     */
    private ProcessedTimContent createProcessedTimContentFromAsnData(ContentChoice content) {
        ProcessedTimContent timContent = new ProcessedTimContent();
        List<ProcessedTimContentItem> contentItems = new ArrayList<>();

        if (content != null) {
            if (content.getAdvisory() != null) {
                timContent.setType(ProcessedContentType.ADVISORY);
                processAdvisoryContent(content.getAdvisory(), contentItems);
            } else if (content.getSpeedLimit() != null) {
                timContent.setType(ProcessedContentType.ROAD_SIGNAGE);
                processSpeedLimitContent(content.getSpeedLimit(), contentItems);
            } else if (content.getWorkZone() != null) {
                timContent.setType(ProcessedContentType.COMMERCIAL_SIGNAGE);
                processWorkZoneContent(content.getWorkZone(), contentItems);
            }
        }

        // Set the ordered content items
        timContent.setContentItems(contentItems);

        // Create combined message from content items in order
        String combinedMessage = contentItems.stream().map(ProcessedTimContentItem::getItisPhrase)
                .filter(text -> text != null && !text.trim().isEmpty()).collect(Collectors.joining(" "));
        timContent.setSentence(combinedMessage);

        return timContent;
    }

    /**
     * Process advisory content.
     *
     * @param advisoryList List of advisory sequences
     * @param contentItems List to store ordered content items
     */
    private void processAdvisoryContent(List<ITIScodesAndTextSequence> advisoryList,
            List<ProcessedTimContentItem> contentItems) {
        for (ITIScodesAndTextSequence advisory : advisoryList) {
            processItisAndTextSequence(advisory.getItem(), contentItems);
        }
    }

    /**
     * Process speed limit content.
     *
     * @param speedLimitList List of speed limit sequences
     * @param contentItems List to store ordered content items
     */
    private void processSpeedLimitContent(List<SpeedLimitSequence> speedLimitList,
            List<ProcessedTimContentItem> contentItems) {
        for (SpeedLimitSequence speedLimit : speedLimitList) {
            processItisAndTextSequence(speedLimit.getItem(), contentItems);
        }
    }

    /**
     * Process work zone content.
     *
     * @param workZoneList List of work zone sequences
     * @param contentItems List to store ordered content items
     */
    private void processWorkZoneContent(List<WorkZoneSequence> workZoneList,
            List<ProcessedTimContentItem> contentItems) {
        for (WorkZoneSequence workZone : workZoneList) {
            processItisAndTextSequence(workZone.getItem(), contentItems);
        }
    }

    /**
     * Process ITIS and text sequence from various content types.
     *
     * @param item The item choice containing ITIS and text data
     * @param contentItems List to store ordered content items
     */
    private void processItisAndTextSequence(Object item, List<ProcessedTimContentItem> contentItems) {
        if (item == null) {
            return;
        }

        // Handle different item types using reflection-like approach
        try {
            if (item instanceof ITIScodesAndTextSequence.ItemChoice) {
                ITIScodesAndTextSequence.ItemChoice itemChoice = (ITIScodesAndTextSequence.ItemChoice) item;
                processItisAndTextItem(itemChoice.getItis(), itemChoice.getText(), contentItems);
            } else if (item instanceof SpeedLimitSequence.ItemChoice) {
                SpeedLimitSequence.ItemChoice itemChoice = (SpeedLimitSequence.ItemChoice) item;
                processItisAndTextItem(itemChoice.getItis(), itemChoice.getText(), contentItems);
            } else if (item instanceof WorkZoneSequence.ItemChoice) {
                WorkZoneSequence.ItemChoice itemChoice = (WorkZoneSequence.ItemChoice) item;
                processItisAndTextItem(itemChoice.getItis(), itemChoice.getText(), contentItems);
            }
        } catch (Exception e) {
            log.warn("Error processing ITIS and text sequence: {}", e.getMessage());
        }
    }

    /**
     * Process ITIS code and text from item, maintaining order in content items.
     *
     * @param itisCode The ITIS code
     * @param text The text message
     * @param contentItems List to store ordered content items
     */
    private void processItisAndTextItem(ITIScodes itisCode, Object text, List<ProcessedTimContentItem> contentItems) {
        if (itisCode != null) {
            try {
                long itisValue = itisCode.getValue();
                String itisPhrase = ItisCodeLookup.lookupItisCode(itisValue);

                ProcessedTimContentItem itisItem = new ProcessedTimContentItem(itisValue, itisPhrase);
                contentItems.add(itisItem);
            } catch (Exception e) {
                log.warn("Error extracting ITIS code: {}", e.getMessage());
            }
        }

        if (text != null) {
            try {
                String textValue = (String) text.getClass().getMethod("getValue").invoke(text);
                if (textValue != null && !textValue.trim().isEmpty()) {
                    ProcessedTimContentItem textItem = ProcessedTimContentItem.createPlainTextItem(textValue);
                    contentItems.add(textItem);
                }
            } catch (Exception e) {
                log.warn("Error extracting text: {}", e.getMessage());
            }
        }
    }


    // ============================================================================
    // FAILURE HANDLING METHODS
    // ============================================================================

    /**
     * Create a failure ProcessedTim object for validation failures.
     *
     * @param validatorResults List of validation messages
     * @param message The failure message
     * @return ProcessedTim object indicating failure
     */
    public ProcessedTim createFailureProcessedTim(List<ProcessedValidationMessage> validatorResults, String message) {
        ProcessedTim processedTim = new ProcessedTim();

        setFailureCompliance(processedTim, validatorResults);
        processedTim.setTimeStamp(ZonedDateTime.now(ZoneOffset.UTC));

        return processedTim;
    }

    /**
     * Set compliance information for failure cases.
     *
     * @param processedTim The ProcessedTim object to update
     * @param validatorResults List of validation messages
     */
    private void setFailureCompliance(ProcessedTim processedTim, List<ProcessedValidationMessage> validatorResults) {
        List<ProcessedCompliance> complianceList = new ArrayList<>();
        ProcessedCompliance compliance = new ProcessedCompliance();
        compliance.setStandard(ProcessedCompliance.Standard.ITWG);
        compliance.setCompliant(false);
        compliance.setValidationMessages(validatorResults);
        complianceList.add(compliance);
        processedTim.setCompliance(complianceList);
    }
}
