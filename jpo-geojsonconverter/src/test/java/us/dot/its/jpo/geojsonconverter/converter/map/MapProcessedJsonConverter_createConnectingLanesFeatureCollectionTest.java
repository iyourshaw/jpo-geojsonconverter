package us.dot.its.jpo.geojsonconverter.converter.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Before;
import org.junit.Test;

import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.MapData.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.ConnectingLanesFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.ConnectingLanesFeatureCollection;
import us.dot.its.jpo.geojsonconverter.standards.MapStandard;

/**
 * Tests for {@link MapProcessedJsonConverter#createConnectingLanesFeatureCollection}'s endpoint
 * selection: the exclusive-ingress-to-exclusive-egress case must keep connecting the first point of
 * both lanes, while any other combination of directional flags must fall back to connecting whichever
 * pair of endpoints (first/last of each lane) is geodetically nearest.
 */
public class MapProcessedJsonConverter_createConnectingLanesFeatureCollectionTest {

    private MapProcessedJsonConverter converter;

    @Before
    public void setup() {
        converter = new MapProcessedJsonConverter(MapStandard.CTI4501_V1);
    }

    @Test
    public void testExclusiveIngressToExclusiveEgress_ConnectsFirstPoints() {
        GenericLane laneA = lane(1, true, false, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 2);
        GenericLane laneB = lane(2, false, true, new double[] {39.0, -104.0001}, new double[] {39.0, -105.0002}, null);

        ConnectingLanesFeature<LineString> feature = getOnlyFeature(laneA, laneB);
        assertEquals(1, feature.getProperties().getIngressLaneId());
        assertEquals(2, feature.getProperties().getEgressLaneId());
        assertCoordinatesEqual(new double[][] {{-105.0, 39.0}, {-104.0001, 39.0}}, feature.getGeometry().getCoordinates());
    }

    @Test
    public void testCurrentLaneNeitherFlag_ConnectsNearestPointPair() {
        GenericLane laneC = lane(3, false, false, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 4);
        GenericLane laneD = lane(4, false, true, new double[] {39.0, -104.0001}, new double[] {39.0, -106.0}, null);

        ConnectingLanesFeature<LineString> feature = getOnlyFeature(laneC, laneD);
        assertEquals(3, feature.getProperties().getIngressLaneId());
        assertEquals(4, feature.getProperties().getEgressLaneId());
        assertCoordinatesEqual(new double[][] {{-104.0, 39.0}, {-104.0001, 39.0}}, feature.getGeometry().getCoordinates());
    }

    @Test
    public void testCurrentLaneBothFlags_ConnectsNearestPointPair() {
        GenericLane laneE = lane(5, true, true, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 6);
        GenericLane laneF = lane(6, false, true, new double[] {39.0, -104.0001}, new double[] {39.0, -108.0}, null);

        ConnectingLanesFeature<LineString> feature = getOnlyFeature(laneE, laneF);
        assertEquals(5, feature.getProperties().getIngressLaneId());
        assertEquals(6, feature.getProperties().getEgressLaneId());
        assertCoordinatesEqual(new double[][] {{-104.0, 39.0}, {-104.0001, 39.0}}, feature.getGeometry().getCoordinates());
    }

    @Test
    public void testTargetLaneBothFlags_ConnectsNearestPointPair() {
        GenericLane laneG = lane(7, true, false, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 8);
        GenericLane laneH = lane(8, true, true, new double[] {39.0, -104.0001}, new double[] {39.0, -109.0}, null);

        ConnectingLanesFeature<LineString> feature = getOnlyFeature(laneG, laneH);
        assertEquals(7, feature.getProperties().getIngressLaneId());
        assertEquals(8, feature.getProperties().getEgressLaneId());
        assertCoordinatesEqual(new double[][] {{-104.0, 39.0}, {-104.0001, 39.0}}, feature.getGeometry().getCoordinates());
    }

    @Test
    public void testMutualNeitherFlagLanes_EmitsSingleConnection() {
        // Both lanes redundantly declare the connection to each other; only one feature should result.
        GenericLane laneI = lane(9, false, false, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 10);
        GenericLane laneJ = lane(10, false, false, new double[] {39.0, -104.0001}, new double[] {39.0, -106.0}, 9);

        ConnectingLanesFeature<LineString> feature = getOnlyFeature(laneI, laneJ);
        assertEquals(9, feature.getProperties().getIngressLaneId());
        assertEquals(10, feature.getProperties().getEgressLaneId());
    }

    @Test
    public void testMutualIngressOnlyLanes_EmitsSingleConnection() {
        // Same as above, but both lanes are ingress-only rather than "neither".
        GenericLane laneK = lane(11, true, false, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 12);
        GenericLane laneL = lane(12, true, false, new double[] {39.0, -104.0001}, new double[] {39.0, -106.0}, 11);

        ConnectingLanesFeature<LineString> feature = getOnlyFeature(laneK, laneL);
        assertEquals(11, feature.getProperties().getIngressLaneId());
        assertEquals(12, feature.getProperties().getEgressLaneId());
    }

    @Test
    public void testMultipleConnectionsSameDirection_AllEmitted() {
        // Two distinct connections in the same direction (e.g. different signal groups) must both survive.
        GenericLane laneM = lane(13, true, false, new double[] {39.0, -105.0}, new double[] {39.0, -104.0}, 14, 14);
        GenericLane laneN = lane(14, false, true, new double[] {39.0, -104.0001}, new double[] {39.0, -106.0});

        ConnectingLanesFeatureCollection<LineString> collection =
                converter.createConnectingLanesFeatureCollection(null, buildIntersection(laneM, laneN));
        assertEquals(2, collection.getFeatures().length);
    }

    // ============================================
    // Helper methods
    // ============================================

    // Coordinates round-trip through the J2735 1/10-microdegree integer encoding, so allow for
    // floating-point rounding rather than requiring bit-exact doubles.
    private static final double COORDINATE_DELTA = 1e-6;

    private void assertCoordinatesEqual(double[][] expected, double[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i][0], actual[i][0], COORDINATE_DELTA);
            assertEquals(expected[i][1], actual[i][1], COORDINATE_DELTA);
        }
    }

    private IntersectionGeometry buildIntersection(GenericLane... lanes) {
        IntersectionGeometry intersection = new IntersectionGeometry();

        Position3D refPoint = new Position3D();
        refPoint.setLat(new Latitude(390000000));
        refPoint.setLong_(new Longitude(-1050000000));
        refPoint.setElevation(new Elevation(0));
        intersection.setRefPoint(refPoint);

        LaneList laneSet = new LaneList();
        for (GenericLane lane : lanes) {
            laneSet.add(lane);
        }
        intersection.setLaneSet(laneSet);
        return intersection;
    }

    private ConnectingLanesFeature<LineString> getOnlyFeature(GenericLane... lanes) {
        ConnectingLanesFeatureCollection<LineString> collection =
                converter.createConnectingLanesFeatureCollection(null, buildIntersection(lanes));
        assertEquals(1, collection.getFeatures().length);
        return collection.getFeatures()[0];
    }

    private GenericLane lane(int laneId, boolean isIngress, boolean isEgress, double[] firstLatLon,
            double[] lastLatLon, Integer... connectsToLaneIds) {
        GenericLane lane = new GenericLane();
        lane.setLaneID(new LaneID(laneId));

        LaneDirection laneDirection = new LaneDirection();
        laneDirection.setIngressPath(isIngress);
        laneDirection.setEgressPath(isEgress);
        LaneAttributes laneAttributes = new LaneAttributes();
        laneAttributes.setDirectionalUse(laneDirection);
        lane.setLaneAttributes(laneAttributes);

        NodeSetXY nodes = new NodeSetXY();
        nodes.add(latLonNode(firstLatLon));
        nodes.add(latLonNode(lastLatLon));
        NodeListXY nodeListXY = new NodeListXY();
        nodeListXY.setNodes(nodes);
        lane.setNodeList(nodeListXY);

        if (connectsToLaneIds != null && connectsToLaneIds.length > 0) {
            ConnectsToList connectsTo = new ConnectsToList();
            for (Integer connectsToLaneId : connectsToLaneIds) {
                ConnectingLane connectingLane = new ConnectingLane();
                connectingLane.setLane(new LaneID(connectsToLaneId));
                Connection connection = new Connection();
                connection.setConnectingLane(connectingLane);
                connection.setSignalGroup(new SignalGroupID(1));
                connectsTo.add(connection);
            }
            lane.setConnectsTo(connectsTo);
        }

        return lane;
    }

    private NodeXY latLonNode(double[] latLon) {
        Node_LLmD_64b nodeLatLon = new Node_LLmD_64b();
        nodeLatLon.setLat(new Latitude((long) (latLon[0] * 1e7)));
        nodeLatLon.setLon(new Longitude((long) (latLon[1] * 1e7)));
        NodeOffsetPointXY delta = new NodeOffsetPointXY();
        delta.setNode_LatLon(nodeLatLon);
        NodeXY node = new NodeXY();
        node.setDelta(delta);
        return node;
    }
}
