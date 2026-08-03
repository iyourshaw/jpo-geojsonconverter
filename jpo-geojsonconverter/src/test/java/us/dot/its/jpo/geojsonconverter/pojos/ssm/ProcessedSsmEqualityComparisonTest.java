package us.dot.its.jpo.geojsonconverter.pojos.ssm;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedPrioritizationResponseStatus;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedSsm.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth.
 */
public class ProcessedSsmEqualityComparisonTest {

    private static final String SAMPLE_SSM_RESOURCE = "/json/sample.processed-ssm.json";

    private ProcessedSsm loadSampleSsm() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_SSM_RESOURCE);
                JsonDeserializer<ProcessedSsm> deserializer = new JsonDeserializer<>(ProcessedSsm.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        assertEquals(ssm1, ssm2);
        assertEquals(ssm1.hashCode(), ssm2.hashCode());
    }

    @Test
    public void testEquals_detectsSignalStatusFieldDifference() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm2.getStatusList().getFirst().setRequestID(999);

        assertNotEquals(ssm1, ssm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameSignalStatusFieldChange() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm1.getStatusList().getFirst().setRequestID(999);
        ssm2.getStatusList().getFirst().setRequestID(999);

        assertEquals(ssm1, ssm2);
        assertEquals(ssm1.hashCode(), ssm2.hashCode());
    }

    @Test
    public void testEquals_detectsSignalStatusEnumDifference() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm2.getStatusList().getFirst().setStatus(ProcessedPrioritizationResponseStatus.REJECTED);

        assertNotEquals(ssm1, ssm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameSignalStatusEnumChange() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm1.getStatusList().getFirst().setStatus(ProcessedPrioritizationResponseStatus.REJECTED);
        ssm2.getStatusList().getFirst().setStatus(ProcessedPrioritizationResponseStatus.REJECTED);

        assertEquals(ssm1, ssm2);
        assertEquals(ssm1.hashCode(), ssm2.hashCode());
    }

    @Test
    public void testEquals_detectsSignalStatusDurationDifference() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm2.getStatusList().getFirst().setEstimatedTimeOfArrivalDurationSeconds(Duration.ofSeconds(999));

        assertNotEquals(ssm1, ssm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameSignalStatusDurationChange() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm1.getStatusList().getFirst().setEstimatedTimeOfArrivalDurationSeconds(Duration.ofSeconds(999));
        ssm2.getStatusList().getFirst().setEstimatedTimeOfArrivalDurationSeconds(Duration.ofSeconds(999));

        assertEquals(ssm1, ssm2);
        assertEquals(ssm1.hashCode(), ssm2.hashCode());
    }

    @Test
    public void testEquals_detectsValidationMessageDifference() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ProcessedValidationMessage message = new ProcessedValidationMessage();
        message.setMessage("mutated message");
        ssm2.getValidationMessages().add(message);

        assertNotEquals(ssm1, ssm2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameValidationMessageChange() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ProcessedValidationMessage message1 = new ProcessedValidationMessage();
        message1.setMessage("mutated message");
        ssm1.getValidationMessages().add(message1);

        ProcessedValidationMessage message2 = new ProcessedValidationMessage();
        message2.setMessage("mutated message");
        ssm2.getValidationMessages().add(message2);

        assertEquals(ssm1, ssm2);
        assertEquals(ssm1.hashCode(), ssm2.hashCode());
    }

    @Test
    public void testEquals_detectsTopLevelFieldDifference() throws IOException {
        ProcessedSsm ssm1 = loadSampleSsm();
        ProcessedSsm ssm2 = loadSampleSsm();

        ssm2.setOriginIp("mutated-ip");

        assertNotEquals(ssm1, ssm2);
    }
}
