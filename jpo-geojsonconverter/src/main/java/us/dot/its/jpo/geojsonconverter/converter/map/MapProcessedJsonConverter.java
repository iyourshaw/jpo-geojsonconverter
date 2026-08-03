package us.dot.its.jpo.geojsonconverter.converter.map;

import com.networknt.schema.Error;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.MapData.*;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.*;
import us.dot.its.jpo.geojsonconverter.standards.MapStandard;
import us.dot.its.jpo.geojsonconverter.utils.BitstringUtils;
import us.dot.its.jpo.geojsonconverter.utils.ProcessedSchemaVersions;
import us.dot.its.jpo.geojsonconverter.validator.CTI4501Validator;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

@Slf4j
public class MapProcessedJsonConverter
        implements Transformer<Void, DeserializedRawMap, KeyValue<RsuIntersectionKey, ProcessedMap<LineString>>> {
    private static final Logger logger = LoggerFactory.getLogger(MapProcessedJsonConverter.class);

    private final MapStandard mapStandardVersion;

    public MapProcessedJsonConverter(MapStandard mapStandardVersion) {
        this.mapStandardVersion = mapStandardVersion;
    }

    @Override
    public void init(ProcessorContext arg0) {}

    /**
     * Transform an ODE MAP POJO to MAP GeoJSON POJO.
     *
     * @param rawKey - Void type because ODE topics have no specified key
     * @param rawMap - The raw POJO
     * @return A key value pair: the key an {@link RsuIntersectionKey} containing the RSU IP address and Intersection ID
     *         and the value is the GeoJSON FeatureCollection POJO
     */
    @Override
    public KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> transform(Void rawKey, DeserializedRawMap rawMap) {
        try {
            if (!rawMap.isValidationFailure()) {
                OdeMessageFrameData rawValue = new OdeMessageFrameData();
                rawValue.setMetadata(rawMap.getOdeMapMessageFrameData().getMetadata());
                OdeMessageFrameMetadata mapMetadata = rawValue.getMetadata();

                rawValue.setPayload(rawMap.getOdeMapMessageFrameData().getPayload());
                MapDataMessageFrame mapMessageFrame = (MapDataMessageFrame) rawValue.getPayload().getData();
                IntersectionGeometry intersection = mapMessageFrame.getValue().getIntersections().get(0);

                MapSharedProperties sharedProps = createProperties(mapMessageFrame.getValue(), mapMetadata,
                        intersection, rawMap.getValidatorResults());

                // Set the schema version
                sharedProps.setSchemaVersion(ProcessedSchemaVersions.PROCESSED_MAP_SCHEMA_VERSION);

                MapFeatureCollection<LineString> mapFeatureCollection = createFeatureCollection(intersection);
                ConnectingLanesFeatureCollection<LineString> connectingLanesFeatureCollection =
                        createConnectingLanesFeatureCollection(mapMetadata, intersection);

                ProcessedMap<LineString> processedMapObject = new ProcessedMap<LineString>();
                processedMapObject.setMapFeatureCollection(mapFeatureCollection);
                processedMapObject.setConnectingLanesFeatureCollection(connectingLanesFeatureCollection);
                processedMapObject.setProperties(sharedProps);

                var key = new RsuIntersectionKey();
                key.setRsuId(mapMetadata.getOriginIp());
                key.setIntersectionReferenceID(intersection.getId());

                logger.debug("Successfully created MAP GeoJSON for {}", key);
                return KeyValue.pair(key, processedMapObject);
            } else {
                ProcessedMap<LineString> processedMapObject =
                        createFailureProcessedMap(rawMap.getValidatorResults(), rawMap.getFailedMessage());

                var key = new RsuIntersectionKey();
                key.setRsuId("ERROR");

                return KeyValue.pair(key, processedMapObject);
            }
        } catch (Exception e) {
            String errMsg = String.format("Exception converting ODE MAP to GeoJSON! Message: %s", e.getMessage());
            logger.error(errMsg, e);
            // KafkaStreams knows to remove null responses before allowing further steps from occurring
            var key = new RsuIntersectionKey();
            key.setRsuId("ERROR");
            return KeyValue.pair(key, null);
        }
    }

    @Override
    public void close() {
        // Nothing to do here
    }

    public MapSharedProperties createProperties(MapData mapData, OdeMessageFrameMetadata metadata,
            IntersectionGeometry intersection, JsonValidatorResult validationMessages) {
        // Save for geometry calculations
        MapRefPoint refPoint = new MapRefPoint();
        refPoint.setFromPosition3D(convertPosition3D(intersection.getRefPoint()));

        String odeTimestamp = metadata.getOdeReceivedAt();
        ZonedDateTime odeDate = Instant.parse(odeTimestamp).atZone(ZoneId.of("UTC"));

        // Handle validation messages for the J2735 and CTI-4501 SPaT conformance validation
        List<ProcessedValidationMessage> processedSpatValidationMessages = new ArrayList<ProcessedValidationMessage>();
        for (Exception exception : validationMessages.getExceptions()) {
            ProcessedValidationMessage object = new ProcessedValidationMessage();
            object.setMessage(exception.getMessage());
            object.setException(Arrays.toString(exception.getStackTrace()));
            processedSpatValidationMessages.add(object);
        }
        for (Error vm : validationMessages.getValidationMessages()) {
            ProcessedValidationMessage object = new ProcessedValidationMessage();
            object.setMessage(vm.getMessage());
            final var schemaLocation = vm.getSchemaLocation();
            if (schemaLocation != null) {
                object.setSchemaPath(schemaLocation.toString());
            } else {
                log.warn("validationMessage.schemaLocation is null");
            }
            final var evaluationPath = vm.getEvaluationPath();
            if (evaluationPath != null) {
                object.setJsonPath(vm.getEvaluationPath().toString());
            }

            processedSpatValidationMessages.add(object);
        }
        processedSpatValidationMessages.addAll(CTI4501Validator.mapValidation(mapData, mapStandardVersion));

        // Build the MapSharedProperties object
        MapSharedProperties sharedProps = new MapSharedProperties();

        sharedProps.setOriginIp(metadata.getOriginIp());
        sharedProps.setAsn1(metadata.getAsn1());
        sharedProps.setOdeReceivedAt(odeDate);
        sharedProps.setIntersectionName(intersection.getName() != null ? intersection.getName().getValue() : null);
        sharedProps.setIntersectionReferenceID(convertIntersectionReferenceID(intersection.getId()));
        sharedProps.setMsgIssueRevision(
                mapData.getMsgIssueRevision() != null ? (int) mapData.getMsgIssueRevision().getValue() : null);
        sharedProps
                .setRevision(intersection.getRevision() != null ? (int) intersection.getRevision().getValue() : null);
        sharedProps.setRefPoint(refPoint);
        sharedProps.setLaneWidth(
                intersection.getLaneWidth() != null ? (int) intersection.getLaneWidth().getValue() : null);
        sharedProps.setSpeedLimits(convertSpeedLimitList(intersection.getSpeedLimits()));
        sharedProps.setMapSource(metadata.getSource());
        sharedProps.setTimeStamp(generateUTCTimestamp(mapData.getTimeStamp(), odeDate));
        // Setting validation fields
        sharedProps.setValidationMessages(processedSpatValidationMessages);
        sharedProps.setCti4501Conformant(sharedProps.getValidationMessages().size() == 0);

        return sharedProps;
    }

    private ProcessedPosition3D convertPosition3D(Position3D p) {
        if (p == null) return null;
        ProcessedPosition3D processed = new ProcessedPosition3D();
        processed.setLat(p.getLat() != null ? (int)p.getLat().getValue() : null);
        processed.setLong_(p.getLong_() != null ? (int)p.getLong_().getValue() : null);
        processed.setElevation(p.getElevation() != null ? (int)p.getElevation().getValue() : null);
        return processed;
    }

    private ProcessedIntersectionReferenceID convertIntersectionReferenceID(IntersectionReferenceID intersectionReferenceID) {
        if (intersectionReferenceID == null) return null;
        ProcessedIntersectionReferenceID processed = new ProcessedIntersectionReferenceID();
        processed.setId(intersectionReferenceID.getId() != null ? (int)intersectionReferenceID.getId().getValue() : null);
        processed.setRegion(intersectionReferenceID.getRegion() != null ? (int)intersectionReferenceID.getRegion().getValue() : null);
        return processed;
    }

    private ProcessedSpeedLimitList convertSpeedLimitList(SpeedLimitList speedLimitList) {
        if (speedLimitList == null) return null;
        ProcessedSpeedLimitList processed = new ProcessedSpeedLimitList();
        for (RegulatorySpeedLimit speedLimit : speedLimitList) {
            var processedSpeedLimit = new ProcessedRegulatorySpeedLimit();
            processedSpeedLimit.setSpeed(speedLimit.getSpeed() != null ? (int)speedLimit.getSpeed().getValue() : null);
            if (speedLimit.getType() != null) {
                ProcessedSpeedLimitType processedSpeedLimitType = ProcessedSpeedLimitType.fromName(speedLimit.getType().getName());
                processedSpeedLimit.setType(processedSpeedLimitType);
            }
            processed.add(processedSpeedLimit);
        }
        return processed;
    }

    @SuppressWarnings("unchecked")
    public MapFeatureCollection<LineString> createFeatureCollection(IntersectionGeometry intersection) {
        // Save for geometry calculations
        MapRefPoint refPoint = new MapRefPoint();
        refPoint.setFromPosition3D(convertPosition3D(intersection.getRefPoint()));

        List<MapFeature<LineString>> mapFeatures = new ArrayList<>();
        for (GenericLane lane : intersection.getLaneSet()) {
            // Create MAP properties
            MapProperties mapProps = new MapProperties();
            if (lane.getNodeList().getNodes() != null) {
                mapProps.setNodes(nodeConversionList(lane.getNodeList().getNodes()));
            }
            mapProps.setLaneId(lane.getLaneID() != null ? (int) lane.getLaneID().getValue() : null);
            mapProps.setLaneName(lane.getName() != null ? lane.getName().getValue() : null);
            LaneAttributes laneAttributes = lane.getLaneAttributes();
            if (laneAttributes != null) {
                mapProps.setLaneType(convertLaneTypeAttributes(laneAttributes.getLaneType()));
                ProcessedLaneSharing processedLaneSharing = new ProcessedLaneSharing();
                BitstringUtils.processBitstring(processedLaneSharing, laneAttributes.getSharedWith());
                mapProps.setIngressPath(lane.getLaneAttributes().getDirectionalUse().isIngressPath());
                mapProps.setEgressPath(lane.getLaneAttributes().getDirectionalUse().isEgressPath());
            }

            mapProps.setIngressApproach(
                    lane.getIngressApproach() != null ? (int) lane.getIngressApproach().getValue() : 0);
            mapProps.setEgressApproach(
                    lane.getEgressApproach() != null ? (int) lane.getEgressApproach().getValue() : 0);
            ProcessedAllowedManeuvers processedManeuvers = new ProcessedAllowedManeuvers();
            BitstringUtils.processBitstring(processedManeuvers, lane.getManeuvers());
            mapProps.setManeuvers(processedManeuvers);
            mapProps.setConnectsTo(convertConnectsToList(lane.getConnectsTo()));

            // Create MAP geometry
            LineString geometry = createGeometry(lane, refPoint);

            // Create MAP feature and add it to the feature list
            mapFeatures.add(new MapFeature<LineString>(mapProps.getLaneId(), geometry, mapProps));
        }

        return new MapFeatureCollection<LineString>(mapFeatures.toArray(new MapFeature[0]));
    }

    private ProcessedLaneTypeAttributes convertLaneTypeAttributes(LaneTypeAttributes laneTypeAttributes) {
        if (laneTypeAttributes == null) return null;
        ProcessedLaneTypeAttributes processed = new ProcessedLaneTypeAttributes();
        if (laneTypeAttributes.getVehicle() != null) {
            var processedVehicle = new ProcessedLaneAttributes_Vehicle();
            BitstringUtils.processBitstring(processedVehicle, laneTypeAttributes.getVehicle());
            processed.setVehicle(processedVehicle);
        } else if (laneTypeAttributes.getCrosswalk() != null) {
            var processedCrosswalk = new ProcessedLaneAttributes_Crosswalk();
            BitstringUtils.processBitstring(processedCrosswalk, laneTypeAttributes.getCrosswalk());
            processed.setCrosswalk(processedCrosswalk);
        } else if (laneTypeAttributes.getBikeLane() != null) {
            var processedBike = new ProcessedLaneAttributes_Bike();
            BitstringUtils.processBitstring(processedBike, laneTypeAttributes.getBikeLane());
            processed.setBikeLane(processedBike);
        } else if (laneTypeAttributes.getSidewalk() != null) {
            var processedSidewalk = new ProcessedLaneAttributes_Sidewalk();
            BitstringUtils.processBitstring(processedSidewalk, laneTypeAttributes.getSidewalk());
            processed.setSidewalk(processedSidewalk);
        } else if (laneTypeAttributes.getMedian() != null) {
            var processedMedian = new ProcessedLaneAttributes_Barrier();
            BitstringUtils.processBitstring(processedMedian, laneTypeAttributes.getMedian());
            processed.setMedian(processedMedian);
        } else if (laneTypeAttributes.getStriping() != null) {
            var processedStriping = new ProcessedLaneAttributes_Striping();
            BitstringUtils.processBitstring(processedStriping, laneTypeAttributes.getStriping());
            processed.setStriping(processedStriping);
        } else if (laneTypeAttributes.getTrackedVehicle() != null) {
            var processedTrackedVehicle = new ProcessedLaneAttributes_TrackedVehicle();
            BitstringUtils.processBitstring(processedTrackedVehicle, laneTypeAttributes.getTrackedVehicle());
            processed.setTrackedVehicle(processedTrackedVehicle);
        } else if (laneTypeAttributes.getParking() != null) {
            var processedParking = new ProcessedLaneAttributes_Parking();
            BitstringUtils.processBitstring(processedParking, laneTypeAttributes.getParking());
            processed.setParking(processedParking);
        }
        return processed;
    }

    private ProcessedConnectsToList convertConnectsToList(ConnectsToList connectsToList) {
        if (connectsToList == null) return null;
        ProcessedConnectsToList processedConnectsToList = new ProcessedConnectsToList();
        for (Connection connection : connectsToList) {
            ProcessedConnection processedConnection = new ProcessedConnection();
            processedConnection.setSignalGroup(connection.getSignalGroup() != null ? (int) connection.getSignalGroup().getValue() : null);
            processedConnection.setUserClass(connection.getUserClass() != null ? (int) connection.getUserClass().getValue() : null);
            processedConnection.setConnectionID(connection.getConnectionID() != null ? (int) connection.getConnectionID().getValue() : null);
            processedConnection.setRemoteIntersection(convertIntersectionReferenceID(connection.getRemoteIntersection()));
            processedConnection.setConnectingLane(convertConnectingLane(connection.getConnectingLane()));
            processedConnectsToList.add(processedConnection);
        }
        return processedConnectsToList;
    }

    private ProcessedConnectingLane convertConnectingLane(ConnectingLane connectingLane) {
        if (connectingLane == null) return null;
        ProcessedConnectingLane processedConnectingLane = new ProcessedConnectingLane();
        processedConnectingLane.setLane(connectingLane.getLane() != null ?  (int) connectingLane.getLane().getValue() : null);
        ProcessedAllowedManeuvers processedManeuvers = new ProcessedAllowedManeuvers();
        BitstringUtils.processBitstring(processedManeuvers, connectingLane.getManeuver());
        processedConnectingLane.setManeuver(processedManeuvers);
        return processedConnectingLane;
    }

    @SuppressWarnings("unchecked")
    public ConnectingLanesFeatureCollection<LineString> createConnectingLanesFeatureCollection(
            OdeMessageFrameMetadata metadata, IntersectionGeometry intersection) {
        // Save for geometry calculations
        MapRefPoint refPoint = new MapRefPoint();
        refPoint.setFromPosition3D(convertPosition3D(intersection.getRefPoint()));

        HashMap<Integer, Coordinate> laneFirstPoints = new HashMap<Integer, Coordinate>();
        HashMap<Integer, Coordinate> laneLastPoints = new HashMap<Integer, Coordinate>();
        HashMap<Integer, GenericLane> laneById = new HashMap<Integer, GenericLane>();
        for (GenericLane lane : intersection.getLaneSet()) {
            int laneId = (int) lane.getLaneID().getValue();
            if (!laneById.containsKey(laneId)) {
                LineString laneGeometry = createGeometry(lane, refPoint);
                double[][] laneGeometryCoordinates = laneGeometry.getCoordinates();
                laneFirstPoints.put(laneId, new Coordinate(laneGeometryCoordinates[0][0], laneGeometryCoordinates[0][1]));
                double[] lastCoordinate = laneGeometryCoordinates[laneGeometryCoordinates.length - 1];
                laneLastPoints.put(laneId, new Coordinate(lastCoordinate[0], lastCoordinate[1]));
                laneById.put(laneId, lane);
            }
        }

        // Tracks (fromLaneId, toLaneId) pairs already emitted, so a connection redundantly declared
        // from both lanes isn't rendered twice. Only the reverse direction is ever checked, so multiple
        // connections declared in the same direction (e.g. different signal groups) are unaffected.
        Set<Pair<Integer, Integer>> emittedConnections = new HashSet<>();

        List<ConnectingLanesFeature<LineString>> lanesFeatures = new ArrayList<>();
        for (GenericLane lane : intersection.getLaneSet()) {
            boolean isIngress = lane.getLaneAttributes().getDirectionalUse().isIngressPath();
            boolean isEgress = lane.getLaneAttributes().getDirectionalUse().isEgressPath();
            // Cover case where a no-travel lane has a connection.
            // But still ignore the case of egress-only lanes with connections, in case implementors
            // have redundant connections on ingress and egress.
            boolean isNeither = !isIngress && !isEgress;
            if (isIngress || isNeither) {
                int laneId = (int) lane.getLaneID().getValue();
                Coordinate laneFirstPoint = laneFirstPoints.get(laneId);
                Coordinate laneLastPoint = laneLastPoints.get(laneId);
                if (lane.getConnectsTo() == null)
                    continue;

                for (Connection connection : lane.getConnectsTo()) {
                    ConnectingLanesProperties laneProps = new ConnectingLanesProperties();
                    laneProps.setIngressLaneId(lane.getLaneID() != null ? (int) lane.getLaneID().getValue() : null);
                    laneProps.setEgressLaneId(connection.getConnectingLane().getLane() != null
                            ? (int) connection.getConnectingLane().getLane().getValue()
                            : null);
                    laneProps.setSignalGroupId(
                            connection.getSignalGroup() != null ? (int) connection.getSignalGroup().getValue() : null);

                    if (connection.getConnectingLane() == null || connection.getConnectingLane().getLane() == null) {
                        // skip if connecting lane is null, avoid NPE
                        continue;
                    }

                    int targetLaneId = (int) connection.getConnectingLane().getLane().getValue();
                    GenericLane targetLane = laneById.get(targetLaneId);
                    if (targetLane == null) {
                        // skip if target lane is null, avoid NPE
                        continue;
                    }
                    boolean targetIsIngress = targetLane.getLaneAttributes().getDirectionalUse().isIngressPath();
                    boolean targetIsEgress = targetLane.getLaneAttributes().getDirectionalUse().isEgressPath();
                    boolean targetIsNeither = !targetIsIngress && !targetIsEgress;

                    // Skip if the target lane's own connectsTo list already declared this connection in reverse.
                    boolean targetCouldAlsoBeCurrent = targetIsIngress || targetIsNeither;
                    if (targetCouldAlsoBeCurrent && emittedConnections.contains(Pair.of(targetLaneId, laneId))) {
                        continue;
                    }
                    emittedConnections.add(Pair.of(laneId, targetLaneId));

                    Coordinate targetFirstPoint = laneFirstPoints.get(targetLaneId);
                    Coordinate targetLastPoint = laneLastPoints.get(targetLaneId);

                    // The normal case: an exclusively-ingress lane connecting to an exclusively-egress lane.
                    // Both lanes' node lists start at the point nearest the intersection, so the first point
                    // of each is the correct connection point.
                    boolean isIngressToEgress = isIngress && !isEgress && targetIsEgress && !targetIsIngress;

                    Coordinate connectionStart;
                    Coordinate connectionEnd;
                    if (isIngressToEgress) {
                        connectionStart = laneFirstPoint;
                        connectionEnd = targetFirstPoint;
                    } else {
                        // Either lane has both directional flags set, or neither set, so it's not safe to
                        // assume the first point of each lane is the near-intersection end. Instead, connect
                        // whichever pair of endpoints (first/last of each lane) are geodetically closest.
                        Coordinate[] nearestEndpoints = findNearestEndpoints(
                                laneFirstPoint, laneLastPoint, targetFirstPoint, targetLastPoint);
                        connectionStart = nearestEndpoints[0];
                        connectionEnd = nearestEndpoints[1];
                    }

                    double[][] coordinates = new double[][] {
                            {connectionStart.x, connectionStart.y}, {connectionEnd.x, connectionEnd.y}};
                    LineString geometry = new LineString(coordinates);

                    String id = String.format("%s-%s", laneProps.getIngressLaneId(), laneProps.getEgressLaneId());
                    lanesFeatures.add(new ConnectingLanesFeature<LineString>(id, geometry, laneProps));
                }
            }
        }

        return new ConnectingLanesFeatureCollection<LineString>(lanesFeatures.toArray(new ConnectingLanesFeature[0]));
    }

    /**
     * Given both endpoints of two lanes, find which pair of endpoints (one from each lane) are
     * geodetically closest together.
     *
     * @return a two-element array: {@code [0]} is the chosen endpoint of the first lane, {@code [1]}
     *         is the chosen endpoint of the second lane.
     */
    private Coordinate[] findNearestEndpoints(
            Coordinate thisFirst, Coordinate thisLast, Coordinate targetFirst, Coordinate targetLast) {
        Coordinate[][] candidatePairs = new Coordinate[][] {
                {thisFirst, targetFirst},
                {thisFirst, targetLast},
                {thisLast, targetFirst},
                {thisLast, targetLast}
        };

        Coordinate[] nearestPair = candidatePairs[0];
        double nearestDistance = geodeticDistanceMeters(nearestPair[0], nearestPair[1]);
        for (int i = 1; i < candidatePairs.length; i++) {
            double distance = geodeticDistanceMeters(candidatePairs[i][0], candidatePairs[i][1]);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPair = candidatePairs[i];
            }
        }
        return nearestPair;
    }

    private double geodeticDistanceMeters(Coordinate a, Coordinate b) {
        GeodeticCalculator calculator = new GeodeticCalculator();
        calculator.setStartingGeographicPoint(a.x, a.y);
        calculator.setDestinationGeographicPoint(b.x, b.y);
        return calculator.getOrthodromicDistance();
    }

    public LineString createGeometry(GenericLane lane, MapRefPoint refPoint) {
        // Calculate coordinates from reference point
        Double anchorLat = refPoint.getLatitude();
        Double anchorLong = refPoint.getLongitude();
        List<List<Double>> coordinatesList = new ArrayList<>();
        for (NodeXY nodeXY : lane.getNodeList().getNodes()) {
            NodeOffsetPointXY nodeOffset = nodeXY.getDelta();

            if (nodeOffset.getNode_LatLon() != null) {
                Node_LLmD_64b nodeLatLong = nodeOffset.getNode_LatLon();
                Double lat = FieldConversions.convertLat(nodeLatLong.getLat().getValue());
                Double lon = FieldConversions.convertLong(nodeLatLong.getLon().getValue());

                List<Double> coordinate = new ArrayList<>();
                coordinate.add(lon);
                coordinate.add(lat);
                coordinatesList.add(coordinate);

                // Reset the anchor point for following offset nodes
                // J2735 is not clear if only one of these nodelatlon types is allowed in the lane path nodes
                anchorLat = lat;
                anchorLong = lon;
            } else {
                // Get the NodeXY object or skip node if entirely null
                Double offsetX = null;
                Double offsetY = null;
                if (nodeOffset.getNode_XY1() != null) {
                    offsetX = (double) nodeOffset.getNode_XY1().getX().getValue();
                    offsetY = (double) nodeOffset.getNode_XY1().getY().getValue();
                } else if (nodeOffset.getNode_XY2() != null) {
                    offsetX = (double) nodeOffset.getNode_XY2().getX().getValue();
                    offsetY = (double) nodeOffset.getNode_XY2().getY().getValue();
                } else if (nodeOffset.getNode_XY3() != null) {
                    offsetX = (double) nodeOffset.getNode_XY3().getX().getValue();
                    offsetY = (double) nodeOffset.getNode_XY3().getY().getValue();
                } else if (nodeOffset.getNode_XY4() != null) {
                    offsetX = (double) nodeOffset.getNode_XY4().getX().getValue();
                    offsetY = (double) nodeOffset.getNode_XY4().getY().getValue();
                } else if (nodeOffset.getNode_XY5() != null) {
                    offsetX = (double) nodeOffset.getNode_XY5().getX().getValue();
                    offsetY = (double) nodeOffset.getNode_XY5().getY().getValue();
                } else if (nodeOffset.getNode_XY6() != null) {
                    offsetX = (double) nodeOffset.getNode_XY6().getX().getValue();
                    offsetY = (double) nodeOffset.getNode_XY6().getY().getValue();
                } else {
                    continue;
                }

                // Calculate offset lon,lat values
                // Equations may become less accurate the further N/S the coordinate is
                // (offsetX * 0.01) / (math.cos((Math.PI / 180.0) * anchorLat) * 111111.0)
                // Step 1. (offsetX * 0.01)
                // Step 2. (math.cos((Math.PI/180.0) * anchorLat) * 111111.0)
                // Step 3. Step 1 / Step 2
                double offsetX_step1 = offsetX * 0.01;
                double offsetX_step2 = Math.cos(((double) (Math.PI / 180.0)) * anchorLat) * 111111.0;
                double offsetXDegrees = offsetX_step1 / offsetX_step2;

                // (offsetY * 0.01) / 111111.0
                double offsetYDegrees = (offsetY * 0.01) / 111111.0;

                // return (reference_point[0] + dx_deg, reference_point[1] + dy_deg)
                double offsetLong = anchorLong + offsetXDegrees;
                double offsetLat = anchorLat + offsetYDegrees;

                List<Double> coordinate = new ArrayList<>();
                coordinate.add(offsetLong);
                coordinate.add(offsetLat);
                coordinatesList.add(coordinate);

                // Reset the anchor point for following offset nodes
                anchorLat = offsetLat;
                anchorLong = offsetLong;
            }
        }

        double[][] coordinatesArray = coordinatesList.stream()
                .map(l -> l.stream().mapToDouble(Double::doubleValue).toArray()).toArray(double[][]::new);

        return new LineString(coordinatesArray);
    }

    public ProcessedMap<LineString> createFailureProcessedMap(JsonValidatorResult validatorResult, String message) {
        ProcessedMap<LineString> processedMapObject = new ProcessedMap<LineString>();

        MapSharedProperties processedMapProps = new MapSharedProperties();
        ProcessedValidationMessage object = new ProcessedValidationMessage();
        List<ProcessedValidationMessage> processedMapValidationMessages = new ArrayList<ProcessedValidationMessage>();

        ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);

        object.setMessage(message);
        object.setException(ExceptionUtils.getStackTrace(validatorResult.getExceptions().get(0)));

        processedMapValidationMessages.add(object);
        processedMapProps.setValidationMessages(processedMapValidationMessages);
        processedMapProps.setTimeStamp(utcDateTime);

        processedMapObject.setProperties(processedMapProps);

        return processedMapObject;
    }

    public ZonedDateTime generateUTCTimestamp(MinuteOfTheYear moy, ZonedDateTime odeDate) { // 2022-10-31T15:40:26.687292Z
        ZonedDateTime date = null;
        try {
            int year = odeDate.getYear();
            String dateString;
            long minutes;
            if (moy != null) {
                minutes = moy.getValue(); // minutes from beginning of year
                dateString = String.format("%d-01-01T00:00:00.00Z", year);
                date = Instant.parse(dateString).atZone(ZoneId.of("UTC"));
                date = date.plusMinutes(minutes);
            } else {
                date = odeDate;
            }

        } catch (Exception e) {
            String errMsg = String.format("Failed to generateUTCTimestamp - SpatProcessedJsonConverter. Message: %s",
                    e.getMessage());
            logger.error(errMsg, e);
        }

        return date;
    }

    public List<MapNode> nodeConversionList(NodeSetXY nodeXYs) { // 2022-10-31T15:40:26.687292Z
        List<MapNode> mapNodes = new ArrayList<MapNode>();
        try {
            for (NodeXY nodeXy : nodeXYs) {
                MapNode mapNode = new MapNode();
                if (nodeXy.getAttributes() != null) {
                    mapNode.setDWidth(nodeXy.getAttributes().getDWidth() != null
                            ? (int) nodeXy.getAttributes().getDWidth().getValue()
                            : null);
                    mapNode.setDElevation(nodeXy.getAttributes().getDElevation() != null
                            ? (int) nodeXy.getAttributes().getDElevation().getValue()
                            : null);
                }

                Integer offsetX = null;
                Integer offsetY = null;
                NodeOffsetPointXY nodeOffset = nodeXy.getDelta();
                if (nodeOffset.getNode_XY1() != null) {
                    offsetX = (int) nodeOffset.getNode_XY1().getX().getValue();
                    offsetY = (int) nodeOffset.getNode_XY1().getY().getValue();
                } else if (nodeOffset.getNode_XY2() != null) {
                    offsetX = (int) nodeOffset.getNode_XY2().getX().getValue();
                    offsetY = (int) nodeOffset.getNode_XY2().getY().getValue();
                } else if (nodeOffset.getNode_XY3() != null) {
                    offsetX = (int) nodeOffset.getNode_XY3().getX().getValue();
                    offsetY = (int) nodeOffset.getNode_XY3().getY().getValue();
                } else if (nodeOffset.getNode_XY4() != null) {
                    offsetX = (int) nodeOffset.getNode_XY4().getX().getValue();
                    offsetY = (int) nodeOffset.getNode_XY4().getY().getValue();
                } else if (nodeOffset.getNode_XY5() != null) {
                    offsetX = (int) nodeOffset.getNode_XY5().getX().getValue();
                    offsetY = (int) nodeOffset.getNode_XY5().getY().getValue();
                } else if (nodeOffset.getNode_XY6() != null) {
                    offsetX = (int) nodeOffset.getNode_XY6().getX().getValue();
                    offsetY = (int) nodeOffset.getNode_XY6().getY().getValue();
                } else if (nodeOffset.getNode_LatLon() != null) {
                    offsetX = FieldConversions.convertLong(nodeOffset.getNode_LatLon().getLon().getValue()).intValue();
                    offsetY = FieldConversions.convertLat(nodeOffset.getNode_LatLon().getLat().getValue()).intValue();
                } else {
                    continue;
                }

                Integer[] delta = {offsetX, offsetY};
                mapNode.setDelta(delta);
                mapNodes.add(mapNode);
            }
        } catch (Exception e) {
            String errMsg = String.format("Failed to nodeConversionList - SpatProcessedJsonConverter. Message: %s",
                    e.getMessage());
            logger.error(errMsg, e);
        }
        return mapNodes;
    }
}
