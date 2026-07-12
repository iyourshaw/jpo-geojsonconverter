package us.dot.its.jpo.geojsonconverter.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.MapData.*;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.*;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.standards.MapStandard;
import us.dot.its.jpo.geojsonconverter.standards.SpatStandard;

public class CTI4501ValidatorTest {

    // ============================================
    // SPAT Validation Tests
    // ============================================

    @Test
    public void testSpatValidation_MissingTimeStamp() {
        SPAT spat = getSpat().withoutSpatTimestamp().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("SPAT 'timeStamp'")));
    }

    @Test
    public void testSpatValidation_PresentTimeStamp() {
        SPAT spat = getSpat().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, not(hasItem(containsString("SPAT 'timeStamp'"))));
    }

    @Test
    public void testSpatValidation_MissingIntersectionRegion_V1() {
        SPAT spat = getSpat().withoutRegion().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("id.region")));
    }

    @Test
    public void testSpatValidation_PresentIntersectionRegion_V2_Deprecated() {
        SPAT spat = getSpat().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V2_DRAFT));
        assertThat(messages, hasItem(containsString("deprecated")));
    }

    @Test
    public void testSpatValidation_V2_NoRegion_NoDeprecatedMessage() {
        SPAT spat = getSpat().withoutRegion().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V2_DRAFT));
        assertThat(messages, not(hasItem(containsString("deprecated"))));
    }

    @Test
    public void testSpatValidation_MissingIntersectionTimeStamp() {
        SPAT spat = getSpat().withoutIntersectionTimestamp().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("intersections 'timeStamp'")));
    }

    @Test
    public void testSpatValidation_MissingTimingField() {
        SPAT spat = getSpat().withoutTiming().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("'timing' DF_TimeChangeDetails is missing")));
    }

    @Test
    public void testSpatValidation_MissingStartTime() {
        SPAT spat = getSpat().withoutStartTime().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("timing.startTime")));
    }

    @Test
    public void testSpatValidation_MissingMaxEndTime() {
        SPAT spat = getSpat().withoutMaxEndTime().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("timing.maxEndTime")));
    }

    @Test
    public void testSpatValidation_MissingMaxEndTime_DedupedAcrossEvents() {
        SPAT spat = getSpat().withoutMaxEndTime().build();

        MovementEvent secondEvent = new MovementEvent();
        TimeChangeDetails secondTiming = new TimeChangeDetails();
        secondTiming.setStartTime(new TimeMark(10));
        secondTiming.setMaxEndTime(null);
        secondTiming.setNextTime(new TimeMark(11));
        secondEvent.setTiming(secondTiming);
        spat.getIntersections().getFirst().getStates().getFirst().getState_time_speed().add(secondEvent);

        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));

        assertThat(countContaining(messages, "timing.maxEndTime"), equalTo(1L));
    }

    @Test
    public void testSpatValidation_MissingTiming_DedupedAcrossEvents() {
        SPAT spat = getSpat().withoutTiming().build();

        MovementEvent secondEvent = new MovementEvent();
        secondEvent.setTiming(null);
        spat.getIntersections().getFirst().getStates().getFirst().getState_time_speed().add(secondEvent);

        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));

        assertThat(countContaining(messages, "'timing' DF_TimeChangeDetails is missing"), equalTo(1L));
    }

    @Test
    public void testSpatValidation_MissingNextTime() {
        SPAT spat = getSpat().withoutNextTime().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("timing.nextTime")));
    }

    @Test
    public void testSpatValidation_AllFieldsPresent() {
        SPAT spat = getSpat().build();
        List<String> messages = toMessages(CTI4501Validator.spatValidation(spat, SpatStandard.CTI4501_V1));
        assertThat(messages, empty());
    }

    // ============================================
    // MAP Validation Tests
    // ============================================

    @Test
    public void testMapValidation_MinimalMapData() {
        MapData mapData = getMap().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, empty());
    }

    @Test
    public void testMapValidation_MissingRegion_V1() {
        MapData mapData = getMap().withoutRegion().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("id.region")));
    }

    @Test
    public void testMapValidation_MissingElevation() {
        MapData mapData = getMap().withoutElevation().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("refPoint.elevation")));
    }

    @Test
    public void testMapValidation_MissingLaneWidth() {
        MapData mapData = getMap().withoutLaneWidth().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("laneWidth")));
    }

    @Test
    public void testMapValidation_MissingSpeedLimits() {
        MapData mapData = getMap().withoutSpeedLimits().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("speedLimits")));
    }

    @Test
    public void testMapValidation_MissingSpeedLimitType() {
        MapData mapData = getMap().withoutSpeedLimitType().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("speedLimits 'type'")));
    }

    @Test
    public void testMapValidation_MissingSpeedLimitSpeed() {
        MapData mapData = getMap().build();

        RegulatorySpeedLimit speedLimit = new RegulatorySpeedLimit();
        speedLimit.setType(SpeedLimitType.VEHICLEMAXSPEED);
        speedLimit.setSpeed(null);
        SpeedLimitList speedLimits = new SpeedLimitList();
        speedLimits.add(speedLimit);
        mapData.getIntersections().getFirst().setSpeedLimits(speedLimits);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("speedLimits 'speed'")));
    }

    @Test
    public void testMapValidation_IngressVehicleLane_MissingConnectsTo() {
        MapData mapData = getMap().withoutConnectsTo().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("connectsTo")));
    }

    @Test
    public void testMapValidation_EgressVehicleLane_NoConnectsToRequired() {
        MapData mapData = getMap().egressVehicleLane().withoutConnectsTo().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, not(hasItem(containsString("DF_ConnectsToList is missing"))));
    }

    @Test
    public void testMapValidation_RegionPresent_V2_Deprecated() {
        MapData mapData = getMap().build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V2_DRAFT));
        assertThat(messages, hasItem(containsString("deprecated in CTI-4501 v2")));
    }

    @Test
    public void testMapValidation_NodeXY1_DeduplicatesRepeatedMissingXYMessages() {
        MapData mapData = getMap().build();

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(nodeXY1(false, false));
        nodes.add(nodeXY1(false, false));
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("node-XY1.x")));
        assertThat(messages, hasItem(containsString("node-XY1.y")));
        assertThat(countContaining(messages, "node-XY1.x"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY1.y"), equalTo(1L));
    }

    @Test
    public void testMapValidation_AllNodeXYTypes_MissingXY() {
        MapData mapData = getMap().build();

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(nodeXY2MissingXY());
        nodes.add(nodeXY3MissingXY());
        nodes.add(nodeXY4MissingXY());
        nodes.add(nodeXY5MissingXY());
        nodes.add(nodeXY6MissingXY());
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("node-XY2.x")));
        assertThat(messages, hasItem(containsString("node-XY2.y")));
        assertThat(messages, hasItem(containsString("node-XY3.x")));
        assertThat(messages, hasItem(containsString("node-XY3.y")));
        assertThat(messages, hasItem(containsString("node-XY4.x")));
        assertThat(messages, hasItem(containsString("node-XY4.y")));
        assertThat(messages, hasItem(containsString("node-XY5.x")));
        assertThat(messages, hasItem(containsString("node-XY5.y")));
        assertThat(messages, hasItem(containsString("node-XY6.x")));
        assertThat(messages, hasItem(containsString("node-XY6.y")));
    }

    @Test
    public void testMapValidation_NodeXY2To6_DeduplicateRepeatedMissingXYMessages() {
        MapData mapData = getMap().build();

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(nodeXY2MissingXY());
        nodes.add(nodeXY2MissingXY());
        nodes.add(nodeXY3MissingXY());
        nodes.add(nodeXY3MissingXY());
        nodes.add(nodeXY4MissingXY());
        nodes.add(nodeXY4MissingXY());
        nodes.add(nodeXY5MissingXY());
        nodes.add(nodeXY5MissingXY());
        nodes.add(nodeXY6MissingXY());
        nodes.add(nodeXY6MissingXY());
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(countContaining(messages, "node-XY2.x"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY2.y"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY3.x"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY3.y"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY4.x"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY4.y"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY5.x"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY5.y"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY6.x"), equalTo(1L));
        assertThat(countContaining(messages, "node-XY6.y"), equalTo(1L));
    }

    @Test
    public void testMapValidation_NodeAttributes_MissingSpeedLimitTypeAndSpeed_Deduped() {
        MapData mapData = getMap().build();

        LaneDataAttribute attribute1 = laneDataAttributeWithSpeedLimit(null, null);
        LaneDataAttribute attribute2 = laneDataAttributeWithSpeedLimit(null, null);
        NodeXY node = nodeXY1WithAttributes(attribute1, attribute2);

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(node);
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("data.speedLimits.type")));
        assertThat(messages, hasItem(containsString("data.speedLimits.speed")));
        assertThat(countContaining(messages, "data.speedLimits.type"), equalTo(1L));
        assertThat(countContaining(messages, "data.speedLimits.speed"), equalTo(1L));
    }

    @Test
    public void testMapValidation_NodeAttributes_MissingSpeedLimitsList() {
        MapData mapData = getMap().build();

        LaneDataAttribute attribute = laneDataAttributeWithSpeedLimits(null);
        NodeXY node = nodeXY1WithAttributes(attribute);

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(node);
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("data.speedLimits' DF_SpeedLimitList is missing")));
    }

    @Test
    public void testMapValidation_NodeAttributes_NonNullTypeAndSpeed_NoAttributeSpeedMessages() {
        MapData mapData = getMap().build();

        LaneDataAttribute attribute = laneDataAttributeWithSpeedLimit(SpeedLimitType.VEHICLEMAXSPEED, new Velocity(200));
        NodeXY node = nodeXY1WithAttributes(attribute);

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(node);
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, not(hasItem(containsString("attributes 'data.speedLimits.type'"))));
        assertThat(messages, not(hasItem(containsString("attributes 'data.speedLimits.speed'"))));
    }

    @Test
    public void testMapValidation_NodeAttributes_MissingSpeedLimitsList_DedupedAcrossAttributes() {
        MapData mapData = getMap().build();

        LaneDataAttribute attribute1 = laneDataAttributeWithSpeedLimits(null);
        LaneDataAttribute attribute2 = laneDataAttributeWithSpeedLimits(null);
        NodeXY node = nodeXY1WithAttributes(attribute1, attribute2);

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(node);
        setLaneNodes(getFirstLane(mapData), nodes);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(countContaining(messages, "data.speedLimits' DF_SpeedLimitList is missing"), equalTo(1L));
    }

    @Test
    public void testMapValidation_ComputedLane_MissingReferenceAndOffsets() {
        MapData mapData = getMap().build();

        GenericLane lane = getFirstLane(mapData);
        lane.setNodeList(computedNodeListMissingReferenceAndOffsets());

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("computed 'referenceLaneId'")));
        assertThat(messages, hasItem(containsString("computed 'offsetXaxis'")));
        assertThat(messages, hasItem(containsString("computed 'offsetYaxis'")));
    }

    @Test
    public void testMapValidation_ComputedLane_DedupedAcrossLanes() {
        MapData mapData = getMap().build();

        GenericLane lane1 = getFirstLane(mapData);
        lane1.setNodeList(computedNodeListMissingReferenceAndOffsets());

        GenericLane lane2 = new GenericLane();
        lane2.setLaneID(new LaneID(2));
        lane2.setManeuvers(new AllowedManeuvers());
        lane2.setLaneAttributes(lane1.getLaneAttributes());
        lane2.setConnectsTo(lane1.getConnectsTo());
        lane2.setNodeList(computedNodeListMissingReferenceAndOffsets());
        mapData.getIntersections().getFirst().getLaneSet().add(lane2);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(countContaining(messages, "computed 'referenceLaneId'"), equalTo(1L));
        assertThat(countContaining(messages, "computed 'offsetXaxis'"), equalTo(1L));
        assertThat(countContaining(messages, "computed 'offsetYaxis'"), equalTo(1L));
    }

    @Test
    public void testMapValidation_ConnectsTo_MissingConnectingLaneFields() {
        MapData mapData = getMap().build();

        getFirstLane(mapData).setConnectsTo(connectsTo(connection(connectingLane(null, null), new SignalGroupID(1))));

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("connectingLane.lane")));
        assertThat(messages, hasItem(containsString("connectingLane.maneuver")));
    }

    @Test
    public void testMapValidation_ConnectsTo_MissingConnectingLaneFields_DedupedAcrossConnections() {
        MapData mapData = getMap().build();

        Connection connection1 = connection(connectingLane(null, null), new SignalGroupID(1));
        Connection connection2 = connection(connectingLane(null, null), new SignalGroupID(2));
        getFirstLane(mapData).setConnectsTo(connectsTo(connection1, connection2));

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(countContaining(messages, "connectingLane.lane"), equalTo(1L));
        assertThat(countContaining(messages, "connectingLane.maneuver"), equalTo(1L));
    }

    @Test
    public void testMapValidation_ConnectsTo_MissingSignalGroup() {
        MapData mapData = getMap().build();

        Connection connection = connection(connectingLane(new LaneID(2), new AllowedManeuvers()), null);
        getFirstLane(mapData).setConnectsTo(connectsTo(connection));

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("connectsTo 'signalGroup'")));
    }

    @Test
    public void testMapValidation_IngressBikeLane_MissingConnectsTo() {
        MapData mapData = getMap().withoutConnectsTo().build();

        LaneTypeAttributes laneType = new LaneTypeAttributes();
        laneType.setBikeLane(new LaneAttributes_Bike());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneType);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("lane type: bike lane")));
    }

    @Test
    public void testMapValidation_IngressSidewalkLane_MissingConnectsTo() {
        MapData mapData = getMap().withoutConnectsTo().build();

        LaneTypeAttributes laneType = new LaneTypeAttributes();
        laneType.setSidewalk(new LaneAttributes_Sidewalk());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneType);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("lane type: sidewalk")));
    }

    @Test
    public void testMapValidation_CrosswalkParkingMedianStripingAndUnknown_NoConnectsToMessage() {
        MapData mapData = getMap().withoutConnectsTo().build();

        // Crosswalk
        LaneTypeAttributes laneTypeCrosswalk = new LaneTypeAttributes();
        laneTypeCrosswalk.setCrosswalk(new LaneAttributes_Crosswalk());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneTypeCrosswalk);
        List<String> crosswalkMessages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(crosswalkMessages, not(hasItem(containsString("lane type: crosswalk"))));

        // Parking
        LaneTypeAttributes laneTypeParking = new LaneTypeAttributes();
        laneTypeParking.setParking(new LaneAttributes_Parking());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneTypeParking);
        List<String> parkingMessages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(parkingMessages, not(hasItem(containsString("lane type: parking"))));

        // Median
        LaneTypeAttributes laneTypeMedian = new LaneTypeAttributes();
        laneTypeMedian.setMedian(new LaneAttributes_Barrier());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneTypeMedian);
        List<String> medianMessages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(medianMessages, not(hasItem(containsString("lane type: median"))));

        // Striping
        LaneTypeAttributes laneTypeStriping = new LaneTypeAttributes();
        laneTypeStriping.setStriping(new LaneAttributes_Striping());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneTypeStriping);
        List<String> stripingMessages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(stripingMessages, not(hasItem(containsString("lane type: striping"))));

        // Unknown lane type (null laneType attributes)
        getFirstLane(mapData).getLaneAttributes().setLaneType(null);
        List<String> unknownMessages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(unknownMessages, not(hasItem(containsString("lane type: unknown"))));
    }

    @Test
    public void testMapValidation_NullLaneIdAndNullLaneAttributes_NoConnectsToMessage() {
        MapData mapData = getMap().withoutConnectsTo().build();

        GenericLane lane = getFirstLane(mapData);
        lane.setLaneID(null);
        lane.setLaneAttributes(null);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, not(hasItem(containsString("DF_ConnectsToList is missing for lane ID"))));
    }

    @Test
    public void testMapValidation_IngressTrackedVehicleLane_MissingConnectsTo() {
        MapData mapData = getMap().withoutConnectsTo().build();

        LaneTypeAttributes laneType = new LaneTypeAttributes();
        laneType.setTrackedVehicle(new LaneAttributes_TrackedVehicle());
        getFirstLane(mapData).getLaneAttributes().setLaneType(laneType);

        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));

        assertThat(messages, hasItem(containsString("lane type: tracked vehicle lane")));
    }

    // ============================================
    // Helper methods
    // ============================================

    private List<String> toMessages(List<ProcessedValidationMessage> messages) {
        return messages.stream().map(ProcessedValidationMessage::getMessage).collect(Collectors.toList());
    }

    private long countContaining(List<String> messages, String fragment) {
        return messages.stream().filter(m -> m.contains(fragment)).count();
    }

    private ConnectingLane connectingLane(LaneID laneId, AllowedManeuvers maneuver) {
        ConnectingLane connectingLane = new ConnectingLane();
        connectingLane.setLane(laneId);
        connectingLane.setManeuver(maneuver);
        return connectingLane;
    }

    private Connection connection(ConnectingLane connectingLane, SignalGroupID signalGroupID) {
        Connection connection = new Connection();
        connection.setConnectingLane(connectingLane);
        connection.setSignalGroup(signalGroupID);
        return connection;
    }

    private ConnectsToList connectsTo(Connection... connections) {
        ConnectsToList connectsToList = new ConnectsToList();
        connectsToList.addAll(Arrays.asList(connections));
        return connectsToList;
    }

    private NodeListXY computedNodeListMissingReferenceAndOffsets() {
        ComputedLane computedLane = new ComputedLane();
        computedLane.setReferenceLaneId(null);
        computedLane.setOffsetXaxis(null);
        computedLane.setOffsetYaxis(null);
        NodeListXY nodeListXY = new NodeListXY();
        nodeListXY.setComputed(computedLane);
        return nodeListXY;
    }

    private GenericLane getFirstLane(MapData mapData) {
        return mapData.getIntersections().getFirst().getLaneSet().getFirst();
    }

    private void setLaneNodes(GenericLane lane, NodeSetXY nodes) {
        NodeListXY nodeListXY = new NodeListXY();
        nodeListXY.setNodes(nodes);
        lane.setNodeList(nodeListXY);
    }

    private NodeXY nodeXY1(boolean includeX, boolean includeY) {
        Node_XY_20b point = new Node_XY_20b();
        point.setX(includeX ? new Offset_B10(1) : null);
        point.setY(includeY ? new Offset_B10(2) : null);
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_XY1(point);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }

    private NodeXY nodeXY2MissingXY() {
        Node_XY_22b point = new Node_XY_22b();
        point.setX(null);
        point.setY(null);
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_XY2(point);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }

    private NodeXY nodeXY3MissingXY() {
        Node_XY_24b point = new Node_XY_24b();
        point.setX(null);
        point.setY(null);
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_XY3(point);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }

    private NodeXY nodeXY4MissingXY() {
        Node_XY_26b point = new Node_XY_26b();
        point.setX(null);
        point.setY(null);
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_XY4(point);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }

    private NodeXY nodeXY5MissingXY() {
        Node_XY_28b point = new Node_XY_28b();
        point.setX(null);
        point.setY(null);
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_XY5(point);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }

    private NodeXY nodeXY6MissingXY() {
        Node_XY_32b point = new Node_XY_32b();
        point.setX(null);
        point.setY(null);
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_XY6(point);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }

    private NodeXY nodeXY1WithAttributes(LaneDataAttribute... attributes) {
        NodeXY node = nodeXY1(true, true);
        NodeAttributeSetXY attrs = new NodeAttributeSetXY();
        LaneDataAttributeList dataList = new LaneDataAttributeList();
        dataList.addAll(List.of(attributes));
        attrs.setData(dataList);
        node.setAttributes(attrs);
        return node;
    }

    private LaneDataAttribute laneDataAttributeWithSpeedLimit(SpeedLimitType type, Velocity speed) {
        return laneDataAttributeWithSpeedLimits(speedLimitList(regulatorySpeedLimit(type, speed)));
    }

    private LaneDataAttribute laneDataAttributeWithSpeedLimits(SpeedLimitList speedLimits) {
        LaneDataAttribute attribute = new LaneDataAttribute();
        attribute.setSpeedLimits(speedLimits);
        return attribute;
    }

    private RegulatorySpeedLimit regulatorySpeedLimit(SpeedLimitType type, Velocity speed) {
        RegulatorySpeedLimit speedLimit = new RegulatorySpeedLimit();
        speedLimit.setType(type);
        speedLimit.setSpeed(speed);
        return speedLimit;
    }

    private SpeedLimitList speedLimitList(RegulatorySpeedLimit... speedLimits) {
        SpeedLimitList speedLimitList = new SpeedLimitList();
        speedLimitList.addAll(Arrays.asList(speedLimits));
        return speedLimitList;
    }

    private SpatBuilder getSpat() {
        return new SpatBuilder();
    }

    private static final class SpatBuilder {
        private boolean includeSpatTimestamp = true;
        private boolean includeRegion = true;
        private boolean includeIntersectionTimestamp = true;
        private boolean includeTiming = true;
        private boolean includeStartTime = true;
        private boolean includeMaxEndTime = true;
        private boolean includeNextTime = true;

        private SpatBuilder withoutSpatTimestamp() {
            this.includeSpatTimestamp = false;
            return this;
        }

        private SpatBuilder withoutRegion() {
            this.includeRegion = false;
            return this;
        }

        private SpatBuilder withoutIntersectionTimestamp() {
            this.includeIntersectionTimestamp = false;
            return this;
        }

        private SpatBuilder withoutTiming() {
            this.includeTiming = false;
            return this;
        }

        private SpatBuilder withoutStartTime() {
            this.includeStartTime = false;
            return this;
        }

        private SpatBuilder withoutMaxEndTime() {
            this.includeMaxEndTime = false;
            return this;
        }

        private SpatBuilder withoutNextTime() {
            this.includeNextTime = false;
            return this;
        }

        private SPAT build() {
            SPAT spat = new SPAT();
            spat.setTimeStamp(includeSpatTimestamp ? new MinuteOfTheYear(1200) : null);

            IntersectionReferenceID id = new IntersectionReferenceID();
            id.setId(new IntersectionID(100));
            id.setRegion(includeRegion ? new RoadRegulatorID(20) : null);

            IntersectionState intersection = new IntersectionState();
            intersection.setId(id);
            intersection.setTimeStamp(includeIntersectionTimestamp ? new DSecond(100) : null);

            MovementEvent movementEvent = new MovementEvent();
            if (includeTiming) {
                TimeChangeDetails timing = new TimeChangeDetails();
                timing.setStartTime(includeStartTime ? new TimeMark(1) : null);
                timing.setMaxEndTime(includeMaxEndTime ? new TimeMark(2) : null);
                timing.setNextTime(includeNextTime ? new TimeMark(3) : null);
                movementEvent.setTiming(timing);
            } else {
                movementEvent.setTiming(null);
            }

            MovementEventList movementEvents = new MovementEventList();
            movementEvents.add(movementEvent);
            MovementState movementState = new MovementState();
            movementState.setState_time_speed(movementEvents);
            MovementList movementStates = new MovementList();
            movementStates.add(movementState);
            intersection.setStates(movementStates);

            IntersectionStateList intersections = new IntersectionStateList();
            intersections.add(intersection);
            spat.setIntersections(intersections);

            return spat;
        }
    }

    private MapBuilder getMap() {
        return new MapBuilder();
    }

    private static final class MapBuilder {
        private boolean includeRegion = true;
        private boolean includeElevation = true;
        private boolean includeLaneWidth = true;
        private boolean includeSpeedLimits = true;
        private boolean includeSpeedLimitType = true;
        private boolean isIngressVehicleLane = true;
        private boolean includeConnectsTo = true;

        private MapBuilder withoutRegion() {
            this.includeRegion = false;
            return this;
        }

        private MapBuilder withoutElevation() {
            this.includeElevation = false;
            return this;
        }

        private MapBuilder withoutLaneWidth() {
            this.includeLaneWidth = false;
            return this;
        }

        private MapBuilder withoutSpeedLimits() {
            this.includeSpeedLimits = false;
            return this;
        }

        private MapBuilder withoutSpeedLimitType() {
            this.includeSpeedLimitType = false;
            return this;
        }

        private MapBuilder egressVehicleLane() {
            this.isIngressVehicleLane = false;
            return this;
        }

        private MapBuilder withoutConnectsTo() {
            this.includeConnectsTo = false;
            return this;
        }

        private MapData build() {
            MapData mapData = new MapData();

            IntersectionGeometry intersection = new IntersectionGeometry();
            IntersectionReferenceID id = new IntersectionReferenceID();
            id.setId(new IntersectionID(100));
            id.setRegion(includeRegion ? new RoadRegulatorID(20) : null);
            intersection.setId(id);

            Position3D refPoint = new Position3D();
            refPoint.setLat(new Latitude(400000000));
            refPoint.setLong_(new Longitude(-1050000000));
            refPoint.setElevation(includeElevation ? new Elevation(100) : null);
            intersection.setRefPoint(refPoint);
            intersection.setLaneWidth(includeLaneWidth ? new LaneWidth(300) : null);

            if (includeSpeedLimits) {
                SpeedLimitList speedLimits = new SpeedLimitList();
                if (!includeSpeedLimitType) {
                    RegulatorySpeedLimit speedLimit = new RegulatorySpeedLimit();
                    speedLimit.setType(null);
                    speedLimit.setSpeed(new Velocity(200));
                    speedLimits.add(speedLimit);
                }
                intersection.setSpeedLimits(speedLimits);
            } else {
                intersection.setSpeedLimits(null);
            }

            GenericLane lane = new GenericLane();
            lane.setLaneID(new LaneID(1));
            lane.setManeuvers(new AllowedManeuvers());

            NodeListXY nodeListXY = new NodeListXY();
            nodeListXY.setNodes(new NodeSetXY());
            lane.setNodeList(nodeListXY);

            LaneAttributes laneAttributes = new LaneAttributes();
            LaneDirection laneDirection = new LaneDirection();
            laneDirection.setIngressPath(isIngressVehicleLane);
            laneAttributes.setDirectionalUse(laneDirection);

            LaneTypeAttributes laneTypeAttributes = new LaneTypeAttributes();
            laneTypeAttributes.setVehicle(new LaneAttributes_Vehicle());
            laneAttributes.setLaneType(laneTypeAttributes);
            lane.setLaneAttributes(laneAttributes);

            if (includeConnectsTo) {
                Connection connection = new Connection();
                // A non-null signalGroup is enough to avoid connectsTo validation failures.
                connection.setConnectingLane(null);
                connection.setSignalGroup(new SignalGroupID(1));

                ConnectsToList connectsTo = new ConnectsToList();
                connectsTo.add(connection);
                lane.setConnectsTo(connectsTo);
            } else {
                lane.setConnectsTo(null);
            }

            LaneList laneSet = new LaneList();
            laneSet.add(lane);
            intersection.setLaneSet(laneSet);

            IntersectionGeometryList intersections = new IntersectionGeometryList();
            intersections.add(intersection);
            mapData.setIntersections(intersections);

            return mapData;
        }
    }


}

