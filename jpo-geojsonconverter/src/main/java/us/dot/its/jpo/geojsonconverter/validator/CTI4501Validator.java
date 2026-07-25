package us.dot.its.jpo.geojsonconverter.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import us.dot.its.jpo.asn.j2735.r2024.Common.LaneDataAttribute;
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
import static us.dot.its.jpo.geojsonconverter.validator.CTI4501Validator.LaneType.*;

public class CTI4501Validator {

    // String constants used for validation
    private static final String INTERSECTION_ID_REGION = "intersection.id.region";
    private static final String LANE_MANEUVERS = "laneSet.GenericLane.maneuvers";

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
                validationMap.put("intersection.id.region",
                        createValidationMessage(spatStandardVersion,
                                "The intersections 'id.region' DE_RoadRegulatorID is missing"));
            }
        } else if (spatStandardVersion == SpatStandard.CTI4501_V2_DRAFT) {
            if (intersection.getId().getRegion() != null) {
                validationMap.put("intersection.id.region",
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
                    if (mEvent.getTiming().getStartTime() == null && !validationMap.containsKey("timing.startTime")) {
                        validationMap.put("timing.startTime", createValidationMessage(spatStandardVersion,
                                "The state-time-speed 'timing.startTime' DE_TimeMark is missing"));
                    }
                    if (mEvent.getTiming().getMaxEndTime() == null && !validationMap.containsKey("timing.maxEndTime")) {
                        validationMap.put("timing.maxEndTime", createValidationMessage(spatStandardVersion,
                                "The state-time-speed 'timing.maxEndTime' DE_TimeMark is missing"));
                    }
                    if (mEvent.getTiming().getNextTime() == null && !validationMap.containsKey("timing.nextTime")) {
                        validationMap.put("timing.nextTime", createValidationMessage(spatStandardVersion,
                                "The state-time-speed 'timing.nextTime' DE_TimeMark is missing"));
                    }
                } else if (!validationMap.containsKey("timing")) {
                    validationMap.put("timing",
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
     * CTI-4501 Document: https://www.ite.org/ITEORG/assets/File/Standards/CTI%204501v0101.pdf
     * <p>
     * Notes on the logic for deciding whether a lane must have a 'connectsTo':
     * <p>
     * Summary:
     * vehicle lanes, bike lanes, and sidewalks that are ingress lanes should have connections.
     * <p>
     * Reasoning:
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
     *
     * @param mapData The MapData object to be validated for CTI-4501 conformance.
     * @return a list of validation messages describing CTI-4501 conformance issues, or an empty list if conformant.
     */
    @SuppressWarnings("java:S3776") // Ignore Sonar 'cognitive complexity' warning
    public static List<ProcessedValidationMessage> mapValidation(MapData mapData, MapStandard mapStandardVersion) {
        HashMap<String, ProcessedValidationMessage> validationMap = new HashMap<>();

        // Get the first intersection from the Map object
        IntersectionGeometry intersection = mapData.getIntersections().get(0);

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
        } else if (mapStandardVersion == MapStandard.CTI4501_V2_DRAFT) {
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
                if (speedLimit.getType() == null && !validationMap.containsKey("speedLimits.type")) {
                    validationMap.put("speedLimits.type",
                            createValidationMessage(mapStandardVersion,
                                    "The speedLimits 'type' DE_SpeedLimitType is missing"));
                }
                if (speedLimit.getSpeed() == null && !validationMap.containsKey("speedLimits.speed")) {
                    validationMap.put("speedLimits.speed",
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
            LaneDescription laneDesc = getLaneDescription(lane);
            if (lane.getManeuvers() == null && !validationMap.containsKey(LANE_MANEUVERS)) {
                validationMap.put(LANE_MANEUVERS,
                        createValidationMessage(mapStandardVersion,
                                "The '%s' DE_AllowedManeuvers element is missing for %s lane ID %s",
                                LANE_MANEUVERS, laneDesc.ingressOrEgress(), laneDesc.laneId()));
            }

            // Check for conditional nodes field requirements
            if (lane.getNodeList().getNodes() != null) {
                for (NodeXY nodeXY : lane.getNodeList().getNodes()) {
                    if (nodeXY.getDelta().getNode_XY1() != null) {
                        if (nodeXY.getDelta().getNode_XY1().getX() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY1.x")) {
                            validationMap.put("nodeXY.delta.node-XY1.x", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY1.x' DE_Offset_B10 is missing but 'delta.node-XY1' DF_Node_XY_20b is present"));
                        }
                        if (nodeXY.getDelta().getNode_XY1().getY() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY1.y")) {
                            validationMap.put("nodeXY.delta.node-XY1.y", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY1.y' DE_Offset_B10 is missing but 'delta.node-XY1' DF_Node_XY_20b is present"));
                        }
                    } else if (nodeXY.getDelta().getNode_XY2() != null) {
                        if (nodeXY.getDelta().getNode_XY2().getX() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY2.x")) {
                            validationMap.put("nodeXY.delta.node-XY2.x", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY2.x' DE_Offset_B11 is missing but 'delta.node-XY2' DF_Node_XY_22b is present"));
                        }
                        if (nodeXY.getDelta().getNode_XY2().getY() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY2.y")) {
                            validationMap.put("nodeXY.delta.node-XY2.y", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY2.y' DE_Offset_B11 is missing but 'delta.node-XY2' DF_Node_XY_22b is present"));
                        }
                    } else if (nodeXY.getDelta().getNode_XY3() != null) {
                        if (nodeXY.getDelta().getNode_XY3().getX() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY3.x")) {
                            validationMap.put("nodeXY.delta.node-XY3.x", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY3.x' DE_Offset_B12 is missing but 'delta.node-XY3' DF_Node_XY_24b is present"));
                        }
                        if (nodeXY.getDelta().getNode_XY3().getY() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY3.y")) {
                            validationMap.put("nodeXY.delta.node-XY3.y", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY3.y' DE_Offset_B12 is missing but 'delta.node-XY3' DF_Node_XY_24b is present"));
                        }
                    } else if (nodeXY.getDelta().getNode_XY4() != null) {
                        if (nodeXY.getDelta().getNode_XY4().getX() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY4.x")) {
                            validationMap.put("nodeXY.delta.node-XY4.x", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY4.x' DE_Offset_B13 is missing but 'delta.node-XY4' DF_Node_XY_26b is present"));
                        }
                        if (nodeXY.getDelta().getNode_XY4().getY() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY4.y")) {
                            validationMap.put("nodeXY.delta.node-XY4.y", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY4.y' DE_Offset_B13 is missing but 'delta.node-XY4' DF_Node_XY_26b is present"));
                        }
                    } else if (nodeXY.getDelta().getNode_XY5() != null) {
                        if (nodeXY.getDelta().getNode_XY5().getX() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY5.x")) {
                            validationMap.put("nodeXY.delta.node-XY5.x", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY5.x' DE_Offset_B14 is missing but 'delta.node-XY5' DF_Node_XY_28b is present"));
                        }
                        if (nodeXY.getDelta().getNode_XY5().getY() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY5.y")) {
                            validationMap.put("nodeXY.delta.node-XY5.y", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY5.y' DE_Offset_B14 is missing but 'delta.node-XY5' DF_Node_XY_28b is present"));
                        }
                    } else if (nodeXY.getDelta().getNode_XY6() != null) {
                        if (nodeXY.getDelta().getNode_XY6().getX() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY6.x")) {
                            validationMap.put("nodeXY.delta.node-XY6.x", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY6.x' DE_Offset_B16 is missing but 'delta.node-XY6' DF_Node_XY_32b is present"));
                        }
                        if (nodeXY.getDelta().getNode_XY6().getY() == null
                                && !validationMap.containsKey("nodeXY.delta.node-XY6.y")) {
                            validationMap.put("nodeXY.delta.node-XY6.y", createValidationMessage(
                                    mapStandardVersion,
                                    "The nodeXY 'delta.node-XY6.y' DE_Offset_B16 is missing but 'delta.node-XY6' DF_Node_XY_32b is present"));
                        }
                    }

                    // Check for conditional node attributes
                    if (nodeXY.getAttributes() != null) {
                        if (nodeXY.getAttributes().getData() != null) {
                            for (LaneDataAttribute data : nodeXY.getAttributes().getData()) {
                                if (data.getSpeedLimits() != null) {
                                    for (RegulatorySpeedLimit speedLimit : data.getSpeedLimits()) {
                                        if (speedLimit.getType() == null
                                                && !validationMap.containsKey("attributes.data.speedLimits.type")) {
                                            validationMap.put("attributes.data.speedLimits.type",
                                                    createValidationMessage(mapStandardVersion,
                                                            "The attributes 'data.speedLimits.type' DE_SpeedLimitType is missing"));
                                        }
                                        if (speedLimit.getSpeed() == null
                                                && !validationMap.containsKey("attributes.data.speedLimits.speed")) {
                                            validationMap.put("attributes.data.speedLimits.speed",
                                                    createValidationMessage(mapStandardVersion,
                                                            "The attributes 'data.speedLimits.speed' DE_Velocity is missing"));
                                        }
                                    }
                                } else if (!validationMap.containsKey("attributes.data.speedLimits")) {
                                    validationMap.put("attributes.data.speedLimits", createValidationMessage(
                                            mapStandardVersion,
                                            "The attributes 'data.speedLimits' DF_SpeedLimitList is missing but 'attributes.data' DF_LaneDataAttributeList is present"));
                                }
                            }
                        }
                    }
                }
            } else if (lane.getNodeList().getComputed() != null) {
                if (lane.getNodeList().getComputed().getReferenceLaneId() == null
                        && !validationMap.containsKey("computed.referenceLaneId")) {
                    validationMap.put("computed.referenceLaneId",
                            createValidationMessage(mapStandardVersion,
                                    "The computed 'referenceLaneId' DE_LaneID is missing"));
                }
                if (lane.getNodeList().getComputed().getOffsetXaxis() == null
                        && !validationMap.containsKey("computed.offsetXaxis")) {
                    validationMap.put("computed.offsetXaxis", createValidationMessage(mapStandardVersion,
                            "The computed 'offsetXaxis' DE_DrivenLineOffsetSmall or DE_DrivenLineOffsetLarge is missing"));
                }
                if (lane.getNodeList().getComputed().getOffsetYaxis() == null
                        && !validationMap.containsKey("computed.offsetYaxis")) {
                    validationMap.put("computed.offsetYaxis", createValidationMessage(mapStandardVersion,
                            "The computed 'offsetYaxis' DE_DrivenLineOffsetSmall or DE_DrivenLineOffsetLarge is missing"));
                }
            }

            // Check for connectsTo field and its nested fields
            if (lane.getConnectsTo() != null) {
                for (Connection connection : lane.getConnectsTo()) {
                    if (connection.getConnectingLane() != null) {
                        if (connection.getConnectingLane().getLane() == null
                                && !validationMap.containsKey("connectsTo.connectingLane.lane")) {
                            validationMap.put("connectsTo.connectingLane.lane", createValidationMessage(
                                    mapStandardVersion,
                                    "The connectsTo 'connectingLane.lane' DE_LaneID is missing"));
                        }
                        if (connection.getConnectingLane().getManeuver() == null
                                && !validationMap.containsKey("connectsTo.connectingLane.maneuver")) {
                            validationMap.put("connectsTo.connectingLane.maneuver", createValidationMessage(
                                    mapStandardVersion,
                                    "The connectsTo 'connectingLane.maneuver' DE_AllowedManeuver is missing"));
                        }
                    }
                    if (connection.getSignalGroup() == null && !validationMap.containsKey("connectsTo.signalGroup")) {
                        validationMap.put("connectsTo.signalGroup",
                                createValidationMessage(mapStandardVersion,
                                        "The connectsTo 'signalGroup' DE_SignalGroupID is missing"));
                    }
                }
            } else {
                LaneDescription laneDesc = getLaneDescription(lane);
                boolean shouldHaveConnection = laneDesc.isVehicleLane() || laneDesc.isBikeLane()
                        || laneDesc.isTrackedVehicleLane() || laneDesc.isSidewalk();
                if (laneDesc.isIngress() && shouldHaveConnection) {
                    String validationKey = "laneSet.connectsTo." + laneDesc.laneId();
                    validationMap.put(validationKey,
                            createValidationMessage(mapStandardVersion,
                                    String.format("The laneSet 'connectsTo' DF_ConnectsToList is missing for lane ID %s, " +
                                            "lane type: %s " ,
                                            laneDesc.laneId(), laneDesc.laneType().getDescription())));

                } else {
                    // Ignore egress or sidewalk/crosswalk lanes without connections
                }
            }
        }

        // Convert HashMap values to List
        List<ProcessedValidationMessage> validationMessages = new ArrayList<>(validationMap.values());
        return validationMessages;
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

    public enum LaneType{
        BIKE_LANE,
        VEHICLE_LANE,
        TRACKED_VEHICLE_LANE,
        CROSSWALK,
        SIDEWALK,
        PARKING,
        MEDIAN,
        STRIPING,
        UNKNOWN;
        public String getDescription() {
            return this.name().toLowerCase().replace('_', ' ');
        }
    }


    public record LaneDescription(
            Long laneId,
            LaneType laneType,
            boolean isIngress
    ){
        public String ingressOrEgress() {
            return isIngress ? "ingress" : "egress";
        }
        public boolean isBikeLane(){
            return laneType == BIKE_LANE;
        }
        public boolean isVehicleLane(){
            return laneType == VEHICLE_LANE;
        }
        public boolean isTrackedVehicleLane(){
            return laneType == TRACKED_VEHICLE_LANE;
        }
        public boolean isCrosswalk(){
            return laneType == CROSSWALK;
        }
        public boolean isSidewalk(){
            return laneType == SIDEWALK;
        }
        public boolean isParking(){
            return laneType == PARKING;
        }
        public boolean isStriping(){
            return laneType == STRIPING;
        }
        public boolean isMedian(){
            return laneType == MEDIAN;
        }
    }


}
