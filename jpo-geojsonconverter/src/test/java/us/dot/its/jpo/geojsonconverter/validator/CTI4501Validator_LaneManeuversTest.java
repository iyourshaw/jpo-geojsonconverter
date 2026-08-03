package us.dot.its.jpo.geojsonconverter.validator;

import org.junit.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.MapData.*;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.standards.MapStandard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Tests for the DE_AllowedManeuvers validation rule described in
 * {@link CTI4501Validator#mapValidation}: only ingress lanes are required to have a
 * 'maneuvers' element, egress lanes are not.
 */
public class CTI4501Validator_LaneManeuversTest {

    private static final String MANEUVERS_MISSING = "DE_AllowedManeuvers element is missing";

    // ============================================
    // Only ingress lanes
    // ============================================

    @Test
    public void testOnlyIngressLanes_WithManeuvers_NoValidationMessage() {
        MapData mapData = getMap().ingressLane(true).build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, not(hasItem(containsString(MANEUVERS_MISSING))));
    }

    @Test
    public void testOnlyIngressLanes_WithoutManeuvers_ValidationMessage() {
        MapData mapData = getMap().ingressLane(false).build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString(MANEUVERS_MISSING)));
        assertThat(messages, hasItem(containsString("ingress lane ID 1")));
    }

    @Test
    public void testOnlyIngressLanes_MultipleLanesMissingManeuvers_DedupedMessage() {
        MapData mapData = getMap().ingressLane(1, false).ingressLane(2, false).build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(countContaining(messages, MANEUVERS_MISSING), equalTo(1L));
    }

    // ============================================
    // Only egress lanes
    // ============================================

    @Test
    public void testOnlyEgressLanes_WithoutManeuvers_NoValidationMessage() {
        // Egress lanes are not required to define maneuvers, so a missing element here is fine.
        MapData mapData = getMap().egressLane(1, false).build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, not(hasItem(containsString(MANEUVERS_MISSING))));
    }

    // ============================================
    // Mixed ingress and egress lanes
    // ============================================

    @Test
    public void testMixedIngressAndEgressLanes_IngressMissingManeuvers_EgressPresent_ValidationMessage() {
        MapData mapData = getMap().ingressLane(false).egressLane(2, true).build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, hasItem(containsString("ingress lane ID 1")));
    }

    @Test
    public void testMixedIngressAndEgressLanes_EgressMissingManeuvers_IngressPresent_NoValidationMessage() {
        MapData mapData = getMap().ingressLane(true).egressLane(2, false).build();
        List<String> messages = toMessages(CTI4501Validator.mapValidation(mapData, MapStandard.CTI4501_V1));
        assertThat(messages, not(hasItem(containsString(MANEUVERS_MISSING))));
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

    private MapBuilder getMap() {
        return new MapBuilder();
    }

    /**
     * Builds a minimal, otherwise-conformant MapData with one or more configurable lanes, so
     * that tests can isolate messages related to the 'maneuvers' element.
     */
    private static final class MapBuilder {
        private final List<LaneSpec> laneSpecs = new ArrayList<>();

        private MapBuilder ingressLane(boolean withManeuvers) {
            return ingressLane(1, withManeuvers);
        }

        private MapBuilder ingressLane(long laneId, boolean withManeuvers) {
            laneSpecs.add(new LaneSpec(laneId, true, withManeuvers));
            return this;
        }

        private MapBuilder egressLane(long laneId, boolean withManeuvers) {
            laneSpecs.add(new LaneSpec(laneId, false, withManeuvers));
            return this;
        }

        private MapData build() {
            MapData mapData = new MapData();

            IntersectionGeometry intersection = new IntersectionGeometry();
            IntersectionReferenceID id = new IntersectionReferenceID();
            id.setId(new IntersectionID(100));
            id.setRegion(new RoadRegulatorID(20));
            intersection.setId(id);

            Position3D refPoint = new Position3D();
            refPoint.setLat(new Latitude(400000000));
            refPoint.setLong_(new Longitude(-1050000000));
            refPoint.setElevation(new Elevation(100));
            intersection.setRefPoint(refPoint);
            intersection.setLaneWidth(new LaneWidth(300));
            intersection.setSpeedLimits(new SpeedLimitList());

            LaneList laneSet = new LaneList();
            for (LaneSpec spec : laneSpecs) {
                laneSet.add(buildLane(spec));
            }
            intersection.setLaneSet(laneSet);

            IntersectionGeometryList intersections = new IntersectionGeometryList();
            intersections.add(intersection);
            mapData.setIntersections(intersections);

            return mapData;
        }

        private GenericLane buildLane(LaneSpec spec) {
            GenericLane lane = new GenericLane();
            lane.setLaneID(new LaneID(spec.laneId));
            lane.setManeuvers(spec.withManeuvers ? new AllowedManeuvers() : null);

            NodeListXY nodeListXY = new NodeListXY();
            nodeListXY.setNodes(new NodeSetXY());
            lane.setNodeList(nodeListXY);

            LaneAttributes laneAttributes = new LaneAttributes();
            LaneDirection laneDirection = new LaneDirection();
            laneDirection.setIngressPath(spec.isIngress);
            laneAttributes.setDirectionalUse(laneDirection);

            LaneTypeAttributes laneTypeAttributes = new LaneTypeAttributes();
            laneTypeAttributes.setVehicle(new LaneAttributes_Vehicle());
            laneAttributes.setLaneType(laneTypeAttributes);
            lane.setLaneAttributes(laneAttributes);

            // Every lane gets a connectsTo entry so the 'connectsTo' rule stays out of these results.
            Connection connection = new Connection();
            connection.setConnectingLane(null);
            connection.setSignalGroup(new SignalGroupID(1));
            ConnectsToList connectsTo = new ConnectsToList();
            connectsTo.add(connection);
            lane.setConnectsTo(connectsTo);

            return lane;
        }

        private record LaneSpec(long laneId, boolean isIngress, boolean withManeuvers) {
        }
    }
}
