package us.dot.its.jpo.geojsonconverter.pojos.geojson.psm;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.ProcessedPsmDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedPsm.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth.
 */
public class ProcessedPsmEqualityComparisonTest {

    private static final String SAMPLE_PSM_RESOURCE = "/json/sample.processed-psm.json";

    private ProcessedPsm<Point> loadSamplePsm() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_PSM_RESOURCE);
                ProcessedPsmDeserializer<Point> deserializer = new ProcessedPsmDeserializer<>(Point.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        assertEquals(psm1, psm2);
        assertEquals(psm1.hashCode(), psm2.hashCode());
    }

    @Test
    public void testEquals_detectsGeometryDifference() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        psm2.getGeometry().getCoordinates()[0] += 1.0;

        assertNotEquals(psm1, psm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameGeometryChange() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        psm1.getGeometry().getCoordinates()[0] += 1.0;
        psm2.getGeometry().getCoordinates()[0] += 1.0;

        assertEquals(psm1, psm2);
        assertEquals(psm1.hashCode(), psm2.hashCode());
    }

    @Test
    public void testEquals_detectsBasicTypeDifference() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        psm2.getProperties().setBasicType(ProcessedPersonalDeviceUserType.ANANIMAL);

        assertNotEquals(psm1, psm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameBasicTypeChange() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        psm1.getProperties().setBasicType(ProcessedPersonalDeviceUserType.ANANIMAL);
        psm2.getProperties().setBasicType(ProcessedPersonalDeviceUserType.ANANIMAL);

        assertEquals(psm1, psm2);
        assertEquals(psm1.hashCode(), psm2.hashCode());
    }

    @Test
    public void testEquals_detectsValidationMessageDifference() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        ProcessedValidationMessage message = new ProcessedValidationMessage();
        message.setMessage("mutated message");
        psm2.getProperties().getValidationMessages().add(message);

        assertNotEquals(psm1, psm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameValidationMessageChange() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        ProcessedValidationMessage message1 = new ProcessedValidationMessage();
        message1.setMessage("mutated message");
        psm1.getProperties().getValidationMessages().add(message1);

        ProcessedValidationMessage message2 = new ProcessedValidationMessage();
        message2.setMessage("mutated message");
        psm2.getProperties().getValidationMessages().add(message2);

        assertEquals(psm1, psm2);
        assertEquals(psm1.hashCode(), psm2.hashCode());
    }

    @Test
    public void testEquals_detectsTopLevelPropertiesFieldDifference() throws IOException {
        ProcessedPsm<Point> psm1 = loadSamplePsm();
        ProcessedPsm<Point> psm2 = loadSamplePsm();

        psm2.getProperties().setOriginIp("mutated-ip");

        assertNotEquals(psm1, psm2);
    }
}
