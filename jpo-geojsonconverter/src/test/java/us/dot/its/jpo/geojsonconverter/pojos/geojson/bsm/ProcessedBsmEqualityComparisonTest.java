package us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedTractionControlStatus;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedTransmissionState;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.ProcessedBsmDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedBsm.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth.  A correct equals()
 * must treat them as equal despite that.
 */
public class ProcessedBsmEqualityComparisonTest {

    private static final String SAMPLE_BSM_RESOURCE = "/json/sample.processed-bsm.json";

    private ProcessedBsm<Point> loadSampleBsm() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_BSM_RESOURCE);
                ProcessedBsmDeserializer<Point> deserializer = new ProcessedBsmDeserializer<>(Point.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsGeometryDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getGeometry().getCoordinates()[0] += 1.0;

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameGeometryChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getGeometry().getCoordinates()[0] += 1.0;
        bsm2.getGeometry().getCoordinates()[0] += 1.0;

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsAccelSetDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().getAccelSet().setAccelLat(99.0);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameAccelSetChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getProperties().getAccelSet().setAccelLat(99.0);
        bsm2.getProperties().getAccelSet().setAccelLat(99.0);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsAccuracyDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().getAccuracy().setSemiMajor(99.0);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameAccuracyChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getProperties().getAccuracy().setSemiMajor(99.0);
        bsm2.getProperties().getAccuracy().setSemiMajor(99.0);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsWheelBrakesBitstringDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().getBrakes().getWheelBrakes().set(1, true);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameWheelBrakesBitstringChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getProperties().getBrakes().getWheelBrakes().set(1, true);
        bsm2.getProperties().getBrakes().getWheelBrakes().set(1, true);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsTractionDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().getBrakes().setTraction(ProcessedTractionControlStatus.ENGAGED);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameTractionChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getProperties().getBrakes().setTraction(ProcessedTractionControlStatus.ENGAGED);
        bsm2.getProperties().getBrakes().setTraction(ProcessedTractionControlStatus.ENGAGED);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsSizeDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().getSize().setWidth(999);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameSizeChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getProperties().getSize().setWidth(999);
        bsm2.getProperties().getSize().setWidth(999);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsTransmissionDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().setTransmission(ProcessedTransmissionState.PARK);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameTransmissionChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm1.getProperties().setTransmission(ProcessedTransmissionState.PARK);
        bsm2.getProperties().setTransmission(ProcessedTransmissionState.PARK);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsValidationMessageDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        ProcessedValidationMessage message = new ProcessedValidationMessage();
        message.setMessage("mutated message");
        bsm2.getProperties().getValidationMessages().add(message);

        assertNotEquals(bsm1, bsm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameValidationMessageChange() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        ProcessedValidationMessage message1 = new ProcessedValidationMessage();
        message1.setMessage("mutated message");
        bsm1.getProperties().getValidationMessages().add(message1);

        ProcessedValidationMessage message2 = new ProcessedValidationMessage();
        message2.setMessage("mutated message");
        bsm2.getProperties().getValidationMessages().add(message2);

        assertEquals(bsm1, bsm2);
        assertEquals(bsm1.hashCode(), bsm2.hashCode());
    }

    @Test
    public void testEquals_detectsTopLevelPropertiesFieldDifference() throws IOException {
        ProcessedBsm<Point> bsm1 = loadSampleBsm();
        ProcessedBsm<Point> bsm2 = loadSampleBsm();

        bsm2.getProperties().setOriginIp("mutated-ip");

        assertNotEquals(bsm1, bsm2);
    }
}
