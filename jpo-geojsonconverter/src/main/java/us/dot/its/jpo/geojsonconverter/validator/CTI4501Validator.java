package us.dot.its.jpo.geojsonconverter.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.dot.its.jpo.asn.j2735.r2024.Common.ComputedLane;
import us.dot.its.jpo.asn.j2735.r2024.Common.LaneDataAttribute;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeListXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeOffsetPointXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.RegulatorySpeedLimit;
import us.dot.its.jpo.asn.j2735.r2024.MapData.*;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.IntersectionState;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.MovementEvent;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.MovementState;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.SPAT;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.standards.MapStandard;
import us.dot.its.jpo.geojsonconverter.standards.SpatStandard;
import static us.dot.its.jpo.geojsonconverter.validator.LaneType.*;

public class CTI4501Validator {

    // String constants used for validation
    private static final String INTERSECTION_ID_REGION = "intersection.id.region";
    private static final String LANE_MANEUVERS = "laneSet.GenericLane.maneuvers";
    private static final String TIMING_START_TIME = "timing.startTime";
    private static final String TIMING_MAX_END_TIME = "timing.maxEndTime";
    private static final String TIMING_NEXT_TIME = "timing.nextTime";
    private static final String TIMING = "timing";
    private static final String SPEED_LIMITS_TYPE = "speedLimits.type";
    private static final String SPEED_LIMITS_SPEED = "speedLimits.speed";
    private static final String NODE_XY1_X = "nodeXY.delta.node-XY1.x";
    private static final String NODE_XY1_Y = "nodeXY.delta.node-XY1.y";
    private static final String NODE_XY2_X = "nodeXY.delta.node-XY2.x";
    private static final String NODE_XY2_Y = "nodeXY.delta.node-XY2.y";
    private static final String NODE_XY3_X = "nodeXY.delta.node-XY3.x";
    private static final String NODE_XY3_Y = "nodeXY.delta.node-XY3.y";
    private static final String NODE_XY4_X = "nodeXY.delta.node-XY4.x";
    private static final String NODE_XY4_Y = "nodeXY.delta.node-XY4.y";
    private static final String NODE_XY5_X = "nodeXY.delta.node-XY5.x";
    private static final String NODE_XY5_Y = "nodeXY.delta.node-XY5.y";
    private static final String NODE_XY6_X = "nodeXY.delta.node-XY6.x";
    private static final String NODE_XY6_Y = "nodeXY.delta.node-XY6.y";
    private static final String ATTRIBUTES_DATA_SPEED_LIMITS_TYPE = "attributes.data.speedLimits.type";
    private static final String ATTRIBUTES_DATA_SPEED_LIMITS_SPEED = "attributes.data.speedLimits.speed";
    private static final String ATTRIBUTES_DATA_SPEED_LIMITS = "attributes.data.speedLimits";
    private static final String COMPUTED_REFERENCE_LANE_ID = "computed.referenceLaneId";
    private static final String COMPUTED_OFFSET_X_AXIS = "computed.offsetXaxis";
    private static final String COMPUTED_OFFSET_Y_AXIS = "computed.offsetYaxis";
    private static final String CONNECTS_TO_CONNECTING_LANE_LANE = "connectsTo.connectingLane.lane";
    private static final String CONNECTS_TO_CONNECTING_LANE_MANEUVER = "connectsTo.connectingLane.maneuver";
    private static final String CONNECTS_TO_SIGNAL_GROUP = "connectsTo.signalGroup";

    /**
     * Checks if the provided SPAT (Signal Phase and Timing) object conforms to the CTI-4501 specification. This method
     * validates the presence of CTI-4501 required fields in the SPAT object and assumes only 1 intersection is defined.
     * <p>
     * Checks for the following fields:
     * <p>
     * SPAT.timeStamp
     * <p>
     * SPAT.intersections[0].id.region
     * <p>
     * SPAT.intersections[0].timeStamp
     * <p>
     * SPAT.intersections[0].states[*].state_time_speed[*].timing
     * <p>
     * SPAT.intersections[0].states[*].state_time_speed[*].timing.startTime
     * <p>
     * SPAT.intersections[0].states[*].state_time_speed[*].timing.maxEndTime
     * <p>
     * SPAT.intersections[0].states[*].state_time_speed[*].timing.nextTime
     *
     * @param spat The SPAT object to be validated for CTI-4501 conformance.
     * @return a list of validation messages describing CTI-4501 conformance issues, or an empty list if conformant.
     */
    @SuppressWarnings("java:S3776") // Ignore Sonar 'cognitive complexity' warning
    public static List<ProcessedValidationMessage> spatValidation(SPAT spat, SpatStandard spatStandardVersion) {
        HashMap<String, ProcessedValidationMessage> validationMap = new HashMap<>();

        // Check SPAT timestamp
        if (spat.getTimeStamp() == null) {
            validationMap.put("spat.timeStamp",
                    createValidationMessage(spatStandardVersion,
                            "The SPAT 'timeStamp' DE_MinuteOfTheYear is missing"));
        }

        // Get the first intersection from the SPAT object
        IntersectionState intersection = spat.getIntersections().get(0);

        // Check intersection fields
        if (spatStandardVersion == SpatStandard.CTI4501_V1) {
            if (intersection.getId().getRegion() == null) {
                validationMap.put(INTERSECTION_ID_REGION,
                        createValidationMessage(spatStandardVersion,
                                "The intersections 'id.region' DE_RoadRegulatorID is missing"));
            }
        } else {
            // spatStandardVersion == SpatStandard.CTI4501_V2_DRAFT
            if (intersection.getId().getRegion() != null) {
                validationMap.put(INTERSECTION_ID_REGION,
                        createValidationMessage(spatStandardVersion,
                                "The intersections 'id.region' DE_RoadRegulatorID is present. Its use is deprecated in CTI-4501 v2."));
            }
        }
        if (intersection.getTimeStamp() == null) {
            validationMap.put("intersection.timeStamp",
                    createValidationMessage(spatStandardVersion,
                            "The intersections 'timeStamp' DE_Dsecond is missing"));
        }

        // Iterate through each movement state and check for CTI-4501 required fields
        for (MovementState mState : intersection.getStates()) {
            for (MovementEvent mEvent : mState.getState_time_speed()) {
                if (mEvent.getTiming() != null) {
                    if (mEvent.getTiming().getStartTime() == null && !validationMap.containsKey(TIMING_START_TIME)) {
                        validationMap.put(TIMING_START_TIME, createValidationMessage(spatStandardVersion,
                                "The state-time-speed 'timing.startTime' DE_TimeMark is missing"));
                    }
                    if (mEvent.getTiming().getMaxEndTime() == null && !validationMap.containsKey(TIMING_MAX_END_TIME)) {
                        validationMap.put(TIMING_MAX_END_TIME, createValidationMessage(spatStandardVersion,
                                "The state-time-speed 'timing.maxEndTime' DE_TimeMark is missing"));
                    }
                    if (mEvent.getTiming().getNextTime() == null && !validationMap.containsKey(TIMING_NEXT_TIME)) {
                        validationMap.put(TIMING_NEXT_TIME, createValidationMessage(spatStandardVersion,
                                "The state-time-speed 'timing.nextTime' DE_TimeMark is missing"));
                    }
                } else if (!validationMap.containsKey(TIMING)) {
                    validationMap.put(TIMING,
                            createValidationMessage(spatStandardVersion,
                                    "The state-time-speed 'timing' DF_TimeChangeDetails is missing"));
                }
            }
        }

        // Convert HashMap values to List
        List<ProcessedValidationMessage> validationMessages = new ArrayList<>(validationMap.values());
        return validationMessages;
    }



    /**
     * Checks if the provided MapData object conforms to the CTI-4501 specification. This method validates the presence
     * of CTI-4501 required fields in the MapData object and assumes only 1 intersection is defined.
     * <p>
     * Checks for all strictly and conditionally mandatory fields defined in CTI-4501 (page 132)
     * <p>
     * <a href='https://www.ite.org/ITEORG/assets/File/Standards/CTI%204501v0101.pdf'>CTI-4501 Document</a>
     * <h3>
     * Notes on the logic for deciding whether a lane must have a 'connectsTo':</h3>
     * <h4>
     * Summary:</h4>
     * <p>vehicle lanes, bike lanes, and sidewalks that are ingress lanes should have connections.
     * <h4>
     * Reasoning:</h4>
     * <p>
     * Here we interpret cti-4501 (v1) such that all lanes which vehicles or VRUs travel on, and which are
     * ingress lanes that pass through the intersection, should have connections to either an egress lane
     * or a crosswalk.
     * <p>
     * Some points are not clear-cut though.
     * <p>
     * We count ingress lanes, but ignore egress lanes because it would be redundant to include the
     * "connectsTo" data structure for both ingress and egress, and CTI-4501 specifically mentions ingress
     * lanes in this context.
     * <p>
     *  We ignore medians, striping, and parking lanes because vehicles don't travel on them.
     * <p>
     *  We ignore crosswalks because of the difficulty of defining if they are "ingress" or "egress".
     * <p>
     *  We hope that future editions of CTI-4501 will clarify these issues more explicitly.
     *  <h4>Notes on logic for deciding whether a lane should have DE_AllowedManeuvers
     *  <h5>Summary</h5>
     *  <p>We only require ingress lanes to have a DE_AllowedManeuvers element.  Egress lanes
     *  are not required to have a 'maneuvers' element.  This check is same for both CTI-4501 v1 and v2,
     *  despite some ambiguity in the v1 specification.
     *  <h5>Reasoning</h5>
     *  <p>CTI-4501 v1, section 3.3.3.4.3 states "A connected intersection shall identify for a lane each maneuver
     *  that is allowed for that lane at the stop line for ingress lanes and at the first node point for the
     *  downstream lane, as defined by DE_AllowedManeuvers in SAE J2735_202007."
     *  <p>This is not very clear.  The bit about the 'first node point for the downstream lane'
     *  seems to suggest that maneuvers should be defined for both the
     *  ingress and egress lanes, but it doesn't clearly state that.  Common sense would dictate that maneuvers should
     *  not be required for egress lanes because it would be redundant information that would bloat the MAP
     *  message size.
     *  <p>The CTI-4501 version 2 draft standard has language that clarifies the situation, making it clear
     *  that 'maneuvers' only needs to be defined 'for each ingress lane' instead of the ambiguous v1 language
     *  'for a lane'.
     * @param mapData The MapData object to be validated for CTI-4501 conformance.
     * @return a list of validation messages describing CTI-4501 conformance issues, or an empty list if conformant.
     */
    public static List<ProcessedValidationMessage> mapValidation(MapData mapData, MapStandard mapStandardVersion) {
        HashMap<String, ProcessedValidationMessage> validationMap = new HashMap<>();

        // Get the first intersection from the Map object
        IntersectionGeometry intersection = mapData.getIntersections().getFirst();

        // Check intersection fields

        // RoadRegulatorID required in CTI-4501 v1, not used in CTI-4501 v2.
        // Noncompliant if missing from v1
        // Noncompliant if present in v2
        if (mapStandardVersion == MapStandard.CTI4501_V1) {
            if (intersection.getId().getRegion() == null) {
                validationMap.put(INTERSECTION_ID_REGION,
                        createValidationMessage(mapStandardVersion,
                                "The intersections 'id.region' DE_RoadRegulatorID is missing"));
            }
        } else {
            // mapStandardVersion == MapStandard.CTI4501_V2_DRAFT
            if (intersection.getId().getRegion() != null) {
                validationMap.put(INTERSECTION_ID_REGION,
                        createValidationMessage(mapStandardVersion,
                                "The intersections 'id.region' DE_RoadRegulatorID is present. Its use is deprecated in CTI-4501 v2."));
            }
        }

        if (intersection.getRefPoint().getElevation() == null) {
            validationMap.put("intersection.refPoint.elevation",
                    createValidationMessage(mapStandardVersion,
                            "The intersections 'refPoint.elevation' DE_Elevation is missing"));
        }
        if (intersection.getLaneWidth() == null) {
            validationMap.put("intersection.laneWidth",
                    createValidationMessage(mapStandardVersion,
                            "The intersections 'laneWidth' DE_LaneWidth is missing"));
        }

        // Check speedLimits
        if (intersection.getSpeedLimits() != null) {
            for (RegulatorySpeedLimit speedLimit : intersection.getSpeedLimits()) {
                if (speedLimit.getType() == null && !validationMap.containsKey(SPEED_LIMITS_TYPE)) {
                    validationMap.put(SPEED_LIMITS_TYPE,
                            createValidationMessage(mapStandardVersion,
                                    "The speedLimits 'type' DE_SpeedLimitType is missing"));
                }
                if (speedLimit.getSpeed() == null && !validationMap.containsKey(SPEED_LIMITS_SPEED)) {
                    validationMap.put(SPEED_LIMITS_SPEED,
                            createValidationMessage(mapStandardVersion,
                                    "The speedLimits 'speed' DE_Velocity is missing"));
                }
            }
        } else {
            validationMap.put("intersection.speedLimits",
                    createValidationMessage(mapStandardVersion,
                            "The intersections 'speedLimits' DF_SpeedLimitList is missing"));
        }

        // Check GenericLane fields
        for (GenericLane lane : intersection.getLaneSet()) {
            laneValidation(validationMap, lane, mapStandardVersion);
        }

        return new ArrayList<>(validationMap.values());
    }

    private static void laneValidation(Map<String, ProcessedValidationMessage> validationMap, GenericLane lane,
                                       MapStandard mapStandardVersion) {
        final LaneDescription laneDesc = getLaneDescription(lane);
        checkLaneManeuvers(validationMap, lane, laneDesc, mapStandardVersion);
        checkNodeList(validationMap, lane.getNodeList(), laneDesc, mapStandardVersion);
        checkConnectsTo(validationMap, lane, laneDesc, mapStandardVersion);
    }

    // Only check for maneuvers for ingress lanes
    private static void checkLaneManeuvers(Map<String, ProcessedValidationMessage> validationMap, GenericLane lane,
                                           LaneDescription laneDesc, MapStandard mapStandardVersion) {
        if (!laneDesc.isIngress()) {
            return;
        }
        if (lane.getManeuvers() == null && !validationMap.containsKey(LANE_MANEUVERS)) {
            validationMap.put(LANE_MANEUVERS,
                    createValidationMessage(mapStandardVersion,
                            "The '%s' DE_AllowedManeuvers element is missing for %s lane ID %s",
                            LANE_MANEUVERS, laneDesc.ingressOrEgress(), laneDesc.laneId()));
        }
    }

    // Check for conditional nodes field requirements
    private static void checkNodeList(Map<String, ProcessedValidationMessage> validationMap, NodeListXY nodeList,
                                      LaneDescription laneDesc, MapStandard mapStandardVersion) {
        if (nodeList.getNodes() != null) {
            for (NodeXY nodeXY : nodeList.getNodes()) {
                checkNodeXYDelta(validationMap, nodeXY, laneDesc, mapStandardVersion);
                checkNodeAttributes(validationMap, nodeXY, laneDesc, mapStandardVersion);
            }
        } else if (nodeList.getComputed() != null) {
            checkComputedLane(validationMap, nodeList.getComputed(), laneDesc, mapStandardVersion);
        }
    }

    private static void checkNodeXYDelta(Map<String, ProcessedValidationMessage> validationMap, NodeXY nodeXY,
                                         LaneDescription laneDesc, MapStandard mapStandardVersion) {
        NodeOffsetPointXY delta = nodeXY.getDelta();
        if (delta.getNode_XY1() != null) {
            checkNodeOffset(validationMap, delta.getNode_XY1().getX(), delta.getNode_XY1().getY(),
                    NODE_XY1_X, NODE_XY1_Y, "node-XY1", "DE_Offset_B10", "DF_Node_XY_20b", laneDesc, mapStandardVersion);
        } else if (delta.getNode_XY2() != null) {
            checkNodeOffset(validationMap, delta.getNode_XY2().getX(), delta.getNode_XY2().getY(),
                    NODE_XY2_X, NODE_XY2_Y, "node-XY2", "DE_Offset_B11", "DF_Node_XY_22b", laneDesc, mapStandardVersion);
        } else if (delta.getNode_XY3() != null) {
            checkNodeOffset(validationMap, delta.getNode_XY3().getX(), delta.getNode_XY3().getY(),
                    NODE_XY3_X, NODE_XY3_Y, "node-XY3", "DE_Offset_B12", "DF_Node_XY_24b", laneDesc, mapStandardVersion);
        } else if (delta.getNode_XY4() != null) {
            checkNodeOffset(validationMap, delta.getNode_XY4().getX(), delta.getNode_XY4().getY(),
                    NODE_XY4_X, NODE_XY4_Y, "node-XY4", "DE_Offset_B13", "DF_Node_XY_26b", laneDesc, mapStandardVersion);
        } else if (delta.getNode_XY5() != null) {
            checkNodeOffset(validationMap, delta.getNode_XY5().getX(), delta.getNode_XY5().getY(),
                    NODE_XY5_X, NODE_XY5_Y, "node-XY5", "DE_Offset_B14", "DF_Node_XY_28b", laneDesc, mapStandardVersion);
        } else if (delta.getNode_XY6() != null) {
            checkNodeOffset(validationMap, delta.getNode_XY6().getX(), delta.getNode_XY6().getY(),
                    NODE_XY6_X, NODE_XY6_Y, "node-XY6", "DE_Offset_B16", "DF_Node_XY_32b", laneDesc, mapStandardVersion);
        }
    }

    private static void checkNodeOffset(Map<String, ProcessedValidationMessage> validationMap, Object x, Object y,
                                        String xKey, String yKey, String nodeName, String deType, String dfType,
                                        LaneDescription laneDesc, MapStandard mapStandardVersion) {
        if (x == null && !validationMap.containsKey(xKey)) {
            validationMap.put(xKey, createValidationMessage(
                    mapStandardVersion,
                    "The nodeXY 'delta.%s.x' %s is missing but 'delta.%s' %s is present, lane ID %s",
                    nodeName, deType, nodeName, dfType, laneDesc.laneId()));
        }
        if (y == null && !validationMap.containsKey(yKey)) {
            validationMap.put(yKey, createValidationMessage(
                    mapStandardVersion,
                    "The nodeXY 'delta.%s.y' %s is missing but 'delta.%s' %s is present, lane ID %s",
                    nodeName, deType, nodeName, dfType, laneDesc.laneId()));
        }
    }

    // Check for conditional node attributes
    private static void checkNodeAttributes(Map<String, ProcessedValidationMessage> validationMap, NodeXY nodeXY,
                                            LaneDescription laneDesc, MapStandard mapStandardVersion) {
        if (nodeXY.getAttributes() == null || nodeXY.getAttributes().getData() == null) {
            return;
        }
        for (LaneDataAttribute data : nodeXY.getAttributes().getData()) {
            checkNodeSpeedLimits(validationMap, data, laneDesc, mapStandardVersion);
        }
    }

    private static void checkNodeSpeedLimits(Map<String, ProcessedValidationMessage> validationMap,
                                             LaneDataAttribute data, LaneDescription laneDesc,
                                             MapStandard mapStandardVersion) {
        if (data.getSpeedLimits() != null) {
            for (RegulatorySpeedLimit speedLimit : data.getSpeedLimits()) {
                if (speedLimit.getType() == null
                        && !validationMap.containsKey(ATTRIBUTES_DATA_SPEED_LIMITS_TYPE)) {
                    validationMap.put(ATTRIBUTES_DATA_SPEED_LIMITS_TYPE,
                            createValidationMessage(mapStandardVersion,
                                    "The attributes 'data.speedLimits.type' DE_SpeedLimitType is missing, lane ID %s",
                                    laneDesc.laneId()));
                }
                if (speedLimit.getSpeed() == null
                        && !validationMap.containsKey(ATTRIBUTES_DATA_SPEED_LIMITS_SPEED)) {
                    validationMap.put(ATTRIBUTES_DATA_SPEED_LIMITS_SPEED,
                            createValidationMessage(mapStandardVersion,
                                    "The attributes 'data.speedLimits.speed' DE_Velocity is missing, lane ID %s",
                                    laneDesc.laneId()));
                }
            }
        } else if (!validationMap.containsKey(ATTRIBUTES_DATA_SPEED_LIMITS)) {
            validationMap.put(ATTRIBUTES_DATA_SPEED_LIMITS, createValidationMessage(
                    mapStandardVersion,
                    "The attributes 'data.speedLimits' DF_SpeedLimitList is missing but 'attributes.data' DF_LaneDataAttributeList is present, lane ID %s",
                    laneDesc.laneId()));
        }
    }

    private static void checkComputedLane(Map<String, ProcessedValidationMessage> validationMap,
                                          ComputedLane computed, LaneDescription laneDesc,
                                          MapStandard mapStandardVersion) {
        if (computed.getReferenceLaneId() == null
                && !validationMap.containsKey(COMPUTED_REFERENCE_LANE_ID)) {
            validationMap.put(COMPUTED_REFERENCE_LANE_ID,
                    createValidationMessage(mapStandardVersion,
                            "The computed 'referenceLaneId' DE_LaneID is missing, lane ID %s",
                            laneDesc.laneId()));
        }
        if (computed.getOffsetXaxis() == null
                && !validationMap.containsKey(COMPUTED_OFFSET_X_AXIS)) {
            validationMap.put(COMPUTED_OFFSET_X_AXIS, createValidationMessage(mapStandardVersion,
                    "The computed 'offsetXaxis' DE_DrivenLineOffsetSmall or DE_DrivenLineOffsetLarge is missing, lane ID %s",
                    laneDesc.laneId()));
        }
        if (computed.getOffsetYaxis() == null
                && !validationMap.containsKey(COMPUTED_OFFSET_Y_AXIS)) {
            validationMap.put(COMPUTED_OFFSET_Y_AXIS, createValidationMessage(mapStandardVersion,
                    "The computed 'offsetYaxis' DE_DrivenLineOffsetSmall or DE_DrivenLineOffsetLarge is missing, lane ID %s",
                    laneDesc.laneId()));
        }
    }

    // Check for connectsTo field and its nested fields
    private static void checkConnectsTo(Map<String, ProcessedValidationMessage> validationMap, GenericLane lane,
                                        LaneDescription laneDesc, MapStandard mapStandardVersion) {
        if (lane.getConnectsTo() != null) {
            for (Connection connection : lane.getConnectsTo()) {
                checkConnectingLane(validationMap, connection, laneDesc, mapStandardVersion);
                if (connection.getSignalGroup() == null && !validationMap.containsKey(CONNECTS_TO_SIGNAL_GROUP)) {
                    validationMap.put(CONNECTS_TO_SIGNAL_GROUP,
                            createValidationMessage(mapStandardVersion,
                                    "The connectsTo 'signalGroup' DE_SignalGroupID is missing, lane ID %s",
                                    laneDesc.laneId()));
                }
            }
        } else {
            checkMissingConnectsTo(validationMap, laneDesc, mapStandardVersion);
        }
    }

    private static void checkConnectingLane(Map<String, ProcessedValidationMessage> validationMap,
                                            Connection connection, LaneDescription laneDesc,
                                            MapStandard mapStandardVersion) {
        if (connection.getConnectingLane() == null) {
            return;
        }
        if (connection.getConnectingLane().getLane() == null
                && !validationMap.containsKey(CONNECTS_TO_CONNECTING_LANE_LANE)) {
            validationMap.put(CONNECTS_TO_CONNECTING_LANE_LANE, createValidationMessage(
                    mapStandardVersion,
                    "The connectsTo 'connectingLane.lane' DE_LaneID is missing, lane ID %s",
                    laneDesc.laneId()));
        }
        if (connection.getConnectingLane().getManeuver() == null
                && !validationMap.containsKey(CONNECTS_TO_CONNECTING_LANE_MANEUVER)) {
            validationMap.put(CONNECTS_TO_CONNECTING_LANE_MANEUVER, createValidationMessage(
                    mapStandardVersion,
                    "The connectsTo 'connectingLane.maneuver' DE_AllowedManeuver is missing, lane ID %s",
                    laneDesc.laneId()));
        }
    }

    // Ingress vehicle, bike, tracked-vehicle, and sidewalk lanes without connections are noncompliant.
    // Egress or sidewalk/crosswalk lanes without connections are ignored.
    private static void checkMissingConnectsTo(Map<String, ProcessedValidationMessage> validationMap,
                                               LaneDescription laneDesc, MapStandard mapStandardVersion) {
        boolean shouldHaveConnection = laneDesc.isVehicleLane() || laneDesc.isBikeLane()
                || laneDesc.isTrackedVehicleLane() || laneDesc.isSidewalk();
        if (laneDesc.isIngress() && shouldHaveConnection) {
            String validationKey = "laneSet.connectsTo." + laneDesc.laneId();
            validationMap.put(validationKey,
                    createValidationMessage(mapStandardVersion,
                            "The laneSet 'connectsTo' DF_ConnectsToList is missing for lane ID %s, lane type: %s" ,
                                    laneDesc.laneId(), laneDesc.laneType().getDescription()));
        }
    }

    // Helper method to create a validation message
    private static ProcessedValidationMessage createValidationMessage(SpatStandard spatVersion, String message,
                                                                      Object...args) {
        return createValidationMessage(spatVersion.getShortName(), message, args);
    }

    private static ProcessedValidationMessage createValidationMessage(MapStandard mapVersion, String message,
                                                                      Object...args) {
        return createValidationMessage(mapVersion.getShortName(), message, args);
    }

    private static ProcessedValidationMessage createValidationMessage(String standardVersion, String message,
                                                                      Object...args) {
        ProcessedValidationMessage validationMessage = new ProcessedValidationMessage();
        String formattedMessage = standardVersion + " conformance issue: " + String.format(message, args);
        validationMessage.setMessage(formattedMessage);
        return validationMessage;
    }

    public static LaneDescription getLaneDescription(GenericLane lane) {
        Long laneId = lane.getLaneID() != null ? lane.getLaneID().getValue() : null;
        LaneAttributes laneAttribs = lane.getLaneAttributes();
        LaneDirection directionalUse = laneAttribs != null ? laneAttribs.getDirectionalUse() : null;
        boolean isIngress = directionalUse != null && directionalUse.isIngressPath();
        LaneTypeAttributes laneTypeAttribs = laneAttribs != null ? laneAttribs.getLaneType() : null;
        boolean isBikeLane = false;
        boolean isVehicleLane = false;
        boolean isCrosswalk = false;
        boolean isSidewalk = false;
        boolean isTrackedVehicleLane = false;
        boolean isParking = false;
        boolean isMedian = false;
        boolean isStriping = false;
        if (laneTypeAttribs != null) {
            isBikeLane = laneTypeAttribs.getBikeLane() != null;
            isVehicleLane = laneTypeAttribs.getVehicle() != null;
            isCrosswalk = laneTypeAttribs.getCrosswalk() != null;
            isSidewalk = laneTypeAttribs.getSidewalk() != null;
            isMedian = laneTypeAttribs.getMedian() != null;
            isParking = laneTypeAttribs.getParking() != null;
            isStriping = laneTypeAttribs.getStriping() != null;
            isTrackedVehicleLane = laneTypeAttribs.getTrackedVehicle() != null;
        }
        LaneType laneType = isBikeLane ? BIKE_LANE : isVehicleLane ? VEHICLE_LANE : isTrackedVehicleLane
                ? TRACKED_VEHICLE_LANE : isCrosswalk ? CROSSWALK : isSidewalk ? SIDEWALK : isParking
                ? PARKING : isMedian ? MEDIAN : isStriping ? STRIPING : UNKNOWN;

        return new LaneDescription(laneId, laneType, isIngress);
    }




}
