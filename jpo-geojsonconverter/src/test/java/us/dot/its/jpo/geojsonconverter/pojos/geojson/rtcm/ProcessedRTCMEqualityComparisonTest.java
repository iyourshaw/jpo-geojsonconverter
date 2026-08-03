package us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedRTCM.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth.
 */
public class ProcessedRTCMEqualityComparisonTest {

    private static final String SAMPLE_RTCM_RESOURCE = "/json/sample.processed-rtcm.json";

    private ProcessedRTCM loadSampleRtcm() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_RTCM_RESOURCE);
                JsonDeserializer<ProcessedRTCM> deserializer = new JsonDeserializer<>(ProcessedRTCM.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        assertEquals(rtcm1, rtcm2);
        assertEquals(rtcm1.hashCode(), rtcm2.hashCode());
    }

    @Test
    public void testEquals_detectsGeometryDifference() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm2.getGeometry().getCoordinates()[0] += 1.0;

        assertNotEquals(rtcm1, rtcm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameGeometryChange() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm1.getGeometry().getCoordinates()[0] += 1.0;
        rtcm2.getGeometry().getCoordinates()[0] += 1.0;

        assertEquals(rtcm1, rtcm2);
        assertEquals(rtcm1.hashCode(), rtcm2.hashCode());
    }

    @Test
    public void testEquals_detectsMessageTypesDifference() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm2.getProperties().getMessageTypes().add(9999);

        assertNotEquals(rtcm1, rtcm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameMessageTypesChange() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm1.getProperties().getMessageTypes().add(9999);
        rtcm2.getProperties().getMessageTypes().add(9999);

        assertEquals(rtcm1, rtcm2);
        assertEquals(rtcm1.hashCode(), rtcm2.hashCode());
    }

    @Test
    public void testEquals_detectsDecodedMessageDifference() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm2.getProperties().getMessages().getFirst().setHex("mutated-hex");

        assertNotEquals(rtcm1, rtcm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameDecodedMessageChange() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm1.getProperties().getMessages().getFirst().setHex("mutated-hex");
        rtcm2.getProperties().getMessages().getFirst().setHex("mutated-hex");

        assertEquals(rtcm1, rtcm2);
        assertEquals(rtcm1.hashCode(), rtcm2.hashCode());
    }

    @Test
    public void testEquals_detectsJsonNodePayloadDifference() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        ((com.fasterxml.jackson.databind.node.ObjectNode) rtcm2.getProperties().getMessages().getFirst().getDecodedMessage())
                .put("station_id", 99999);

        assertNotEquals(rtcm1, rtcm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameJsonNodePayloadChange() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        ((com.fasterxml.jackson.databind.node.ObjectNode) rtcm1.getProperties().getMessages().getFirst().getDecodedMessage())
                .put("station_id", 99999);
        ((com.fasterxml.jackson.databind.node.ObjectNode) rtcm2.getProperties().getMessages().getFirst().getDecodedMessage())
                .put("station_id", 99999);

        assertEquals(rtcm1, rtcm2);
        assertEquals(rtcm1.hashCode(), rtcm2.hashCode());
    }

    @Test
    public void testEquals_detectsValidationMessageDifference() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm2.getProperties().getValidationMessages().getFirst().setMessage("mutated message");

        assertNotEquals(rtcm1, rtcm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameValidationMessageChange() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm1.getProperties().getValidationMessages().getFirst().setMessage("mutated message");
        rtcm2.getProperties().getValidationMessages().getFirst().setMessage("mutated message");

        assertEquals(rtcm1, rtcm2);
        assertEquals(rtcm1.hashCode(), rtcm2.hashCode());
    }

    @Test
    public void testEquals_detectsTopLevelPropertiesFieldDifference() throws IOException {
        ProcessedRTCM rtcm1 = loadSampleRtcm();
        ProcessedRTCM rtcm2 = loadSampleRtcm();

        rtcm2.getProperties().setOriginIp("mutated-ip");

        assertNotEquals(rtcm1, rtcm2);
    }
}
