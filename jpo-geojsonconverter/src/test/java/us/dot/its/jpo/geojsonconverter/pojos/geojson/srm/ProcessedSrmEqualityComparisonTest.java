package us.dot.its.jpo.geojsonconverter.pojos.geojson.srm;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedBasicVehicleRole;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedSrm.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth.
 */
public class ProcessedSrmEqualityComparisonTest {

    private static final String SAMPLE_SRM_RESOURCE = "/json/sample.processed-srm.json";

    private ProcessedSrm loadSampleSrm() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_SRM_RESOURCE);
                JsonDeserializer<ProcessedSrm> deserializer = new JsonDeserializer<>(ProcessedSrm.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        assertEquals(srm1, srm2);
        assertEquals(srm1.hashCode(), srm2.hashCode());
    }

    @Test
    public void testEquals_detectsGeometryDifference() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm2.getGeometry().getCoordinates()[0] += 1.0;

        assertNotEquals(srm1, srm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameGeometryChange() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm1.getGeometry().getCoordinates()[0] += 1.0;
        srm2.getGeometry().getCoordinates()[0] += 1.0;

        assertEquals(srm1, srm2);
        assertEquals(srm1.hashCode(), srm2.hashCode());
    }

    @Test
    public void testEquals_detectsRoleDifference() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm2.getProperties().setRole(ProcessedBasicVehicleRole.DANGEROUSGOODS);

        assertNotEquals(srm1, srm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameRoleChange() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm1.getProperties().setRole(ProcessedBasicVehicleRole.DANGEROUSGOODS);
        srm2.getProperties().setRole(ProcessedBasicVehicleRole.DANGEROUSGOODS);

        assertEquals(srm1, srm2);
        assertEquals(srm1.hashCode(), srm2.hashCode());
    }

    @Test
    public void testEquals_detectsRequestFieldDifference() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm2.getProperties().getRequests().getFirst().setRequestID(999);

        assertNotEquals(srm1, srm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameRequestFieldChange() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm1.getProperties().getRequests().getFirst().setRequestID(999);
        srm2.getProperties().getRequests().getFirst().setRequestID(999);

        assertEquals(srm1, srm2);
        assertEquals(srm1.hashCode(), srm2.hashCode());
    }

    @Test
    public void testEquals_detectsTransitVehicleStatusBitstringDifference() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        ProcessedTransitVehicleStatus status2 = new ProcessedTransitVehicleStatus();
        status2.set(0, true);
        srm2.getProperties().setTransitStatus(status2);

        assertNotEquals(srm1, srm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameTransitVehicleStatusChange() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        ProcessedTransitVehicleStatus status1 = new ProcessedTransitVehicleStatus();
        status1.set(0, true);
        srm1.getProperties().setTransitStatus(status1);

        ProcessedTransitVehicleStatus status2 = new ProcessedTransitVehicleStatus();
        status2.set(0, true);
        srm2.getProperties().setTransitStatus(status2);

        assertEquals(srm1, srm2);
        assertEquals(srm1.hashCode(), srm2.hashCode());
    }

    @Test
    public void testEquals_detectsValidationMessageDifference() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        ProcessedValidationMessage message = new ProcessedValidationMessage();
        message.setMessage("mutated message");
        srm2.getProperties().getValidationMessages().add(message);

        assertNotEquals(srm1, srm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameValidationMessageChange() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        ProcessedValidationMessage message1 = new ProcessedValidationMessage();
        message1.setMessage("mutated message");
        srm1.getProperties().getValidationMessages().add(message1);

        ProcessedValidationMessage message2 = new ProcessedValidationMessage();
        message2.setMessage("mutated message");
        srm2.getProperties().getValidationMessages().add(message2);

        assertEquals(srm1, srm2);
        assertEquals(srm1.hashCode(), srm2.hashCode());
    }

    @Test
    public void testEquals_detectsTopLevelPropertiesFieldDifference() throws IOException {
        ProcessedSrm srm1 = loadSampleSrm();
        ProcessedSrm srm2 = loadSampleSrm();

        srm2.getProperties().setOriginIp("mutated-ip");

        assertNotEquals(srm1, srm2);
    }
}
