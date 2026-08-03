package us.dot.its.jpo.geojsonconverter.pojos.geojson.map;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.ConnectingLanesFeature;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.ProcessedMapDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedMap.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth (fresh arrays, fresh
 * nested POJOs everywhere).  A correct equals() must treat them as equal despite that.
 */
public class ProcessedMapEqualityComparisonTest {

    private static final String SAMPLE_MAP_RESOURCE = "/json/sample.processed-map.json";

    private ProcessedMap<LineString> loadSampleMap() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_MAP_RESOURCE);
                ProcessedMapDeserializer<LineString> deserializer =
                        new ProcessedMapDeserializer<>(LineString.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals_detectsMapFeatureGeometryDifference() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map2.getMapFeatureCollection().getFeatures()[0].getGeometry().getCoordinates()[0][0] += 1.0;

        assertNotEquals(map1, map2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameGeometryChange() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map1.getMapFeatureCollection().getFeatures()[0].getGeometry().getCoordinates()[0][0] += 1.0;
        map2.getMapFeatureCollection().getFeatures()[0].getGeometry().getCoordinates()[0][0] += 1.0;

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals_detectsMapFeaturePropertiesDifference() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map2.getMapFeatureCollection().getFeatures()[0].getProperties().setLaneId(999999);

        assertNotEquals(map1, map2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSamePropertiesChange() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map1.getMapFeatureCollection().getFeatures()[0].getProperties().setLaneId(999999);
        map2.getMapFeatureCollection().getFeatures()[0].getProperties().setLaneId(999999);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals_detectsMapNodeDeltaDifference() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map2.getMapFeatureCollection().getFeatures()[0].getProperties().getNodes().getFirst()
                .setDelta(new Integer[] {9999, 9999});

        assertNotEquals(map1, map2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameMapNodeDeltaChange() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map1.getMapFeatureCollection().getFeatures()[0].getProperties().getNodes().getFirst()
                .setDelta(new Integer[] {9999, 9999});
        map2.getMapFeatureCollection().getFeatures()[0].getProperties().getNodes().getFirst()
                .setDelta(new Integer[] {9999, 9999});

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals_detectsLaneTypeBitstringDifference() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map2.getMapFeatureCollection().getFeatures()[0].getProperties().getLaneType().getVehicle().set(0, true);

        assertNotEquals(map1, map2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameLaneTypeBitstringChange() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map1.getMapFeatureCollection().getFeatures()[0].getProperties().getLaneType().getVehicle().set(0, true);
        map2.getMapFeatureCollection().getFeatures()[0].getProperties().getLaneType().getVehicle().set(0, true);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals_detectsConnectingLanesFeatureDifference() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        ConnectingLanesFeature<LineString>[] features = map2.getConnectingLanesFeatureCollection().getFeatures();
        features[0] = new ConnectingLanesFeature<>("mutated-id", features[0].getGeometry(), features[0].getProperties());

        assertNotEquals(map1, map2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameConnectingLanesFeatureChange() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        ConnectingLanesFeature<LineString>[] features1 = map1.getConnectingLanesFeatureCollection().getFeatures();
        features1[0] = new ConnectingLanesFeature<>("mutated-id", features1[0].getGeometry(), features1[0].getProperties());
        ConnectingLanesFeature<LineString>[] features2 = map2.getConnectingLanesFeatureCollection().getFeatures();
        features2[0] = new ConnectingLanesFeature<>("mutated-id", features2[0].getGeometry(), features2[0].getProperties());

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals_detectsSharedPropertiesDifference() throws IOException {
        ProcessedMap<LineString> map1 = loadSampleMap();
        ProcessedMap<LineString> map2 = loadSampleMap();

        map2.getProperties().setOriginIp("mutated-ip");

        assertNotEquals(map1, map2);
    }
}
