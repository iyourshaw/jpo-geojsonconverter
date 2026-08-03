package us.dot.its.jpo.geojsonconverter.pojos.spat;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedSpeedConfidence;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;

import static org.junit.Assert.*;

/**
 * Verifies that ProcessedSpat.equals()/hashCode() compare every value in the object graph, not
 * object references.  Each test deserializes the same fixture twice, which produces two
 * structurally identical graphs that share no object instances at any depth.  A correct equals()
 * must treat them as equal despite that.
 */
public class ProcessedSpatEqualityComparisonTest {

    private static final String SAMPLE_SPAT_RESOURCE = "/json/sample.processed-spat.json";

    private ProcessedSpat loadSampleSpat() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE_SPAT_RESOURCE);
                JsonDeserializer<ProcessedSpat> deserializer = new JsonDeserializer<>(ProcessedSpat.class)) {
            byte[] bytes = IOUtils.toByteArray(in);
            return deserializer.deserialize("test-topic", bytes);
        }
    }

    private ProcessedAdvisorySpeed newAdvisorySpeed() {
        ProcessedAdvisorySpeed speed = new ProcessedAdvisorySpeed();
        speed.setType(ProcessedAdvisorySpeedType.GREENWAVE);
        speed.setSpeed(35);
        speed.setConfidence(ProcessedSpeedConfidence.PREC10MS);
        return speed;
    }

    @Test
    public void testEqualObjectGraphs_areEqual() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsValidationMessageDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.getValidationMessages().getFirst().setMessage("mutated message");

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameValidationMessageChange() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat1.getValidationMessages().getFirst().setMessage("mutated message");
        spat2.getValidationMessages().getFirst().setMessage("mutated message");

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsStatusBitstringDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.getStatus().set(0, true);

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameStatusBitstringChange() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat1.getStatus().set(0, true);
        spat2.getStatus().set(0, true);

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsMovementStateDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.getStates().getFirst().setSignalGroup(999);

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameMovementStateChange() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat1.getStates().getFirst().setSignalGroup(999);
        spat2.getStates().getFirst().setSignalGroup(999);

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsTimingDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.getStates().getFirst().getStateTimeSpeed().getFirst().getTiming()
                .setMinEndTime(ZonedDateTime.parse("2030-01-01T00:00:00Z"));

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameTimingChange() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat1.getStates().getFirst().getStateTimeSpeed().getFirst().getTiming()
                .setMinEndTime(ZonedDateTime.parse("2030-01-01T00:00:00Z"));
        spat2.getStates().getFirst().getStateTimeSpeed().getFirst().getTiming()
                .setMinEndTime(ZonedDateTime.parse("2030-01-01T00:00:00Z"));

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsEventStateDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.getStates().getFirst().getStateTimeSpeed().getFirst().setEventState(ProcessedMovementPhaseState.DARK);

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameEventStateChange() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat1.getStates().getFirst().getStateTimeSpeed().getFirst().setEventState(ProcessedMovementPhaseState.DARK);
        spat2.getStates().getFirst().getStateTimeSpeed().getFirst().setEventState(ProcessedMovementPhaseState.DARK);

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsAdvisorySpeedDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        ProcessedAdvisorySpeedList speeds = new ProcessedAdvisorySpeedList();
        speeds.add(newAdvisorySpeed());
        spat2.getStates().getFirst().getStateTimeSpeed().getFirst().setSpeeds(speeds);

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_isTrueWhenBothGraphsHaveSameAdvisorySpeedChange() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        ProcessedAdvisorySpeedList speeds1 = new ProcessedAdvisorySpeedList();
        speeds1.add(newAdvisorySpeed());
        spat1.getStates().getFirst().getStateTimeSpeed().getFirst().setSpeeds(speeds1);

        ProcessedAdvisorySpeedList speeds2 = new ProcessedAdvisorySpeedList();
        speeds2.add(newAdvisorySpeed());
        spat2.getStates().getFirst().getStateTimeSpeed().getFirst().setSpeeds(speeds2);

        assertEquals(spat1, spat2);
        assertEquals(spat1.hashCode(), spat2.hashCode());
    }

    @Test
    public void testEquals_detectsEnabledLanesDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.getEnabledLanes().add(99);

        assertNotEquals(spat1, spat2);
    }

    @Test
    public void testEquals_detectsTopLevelFieldDifference() throws IOException {
        ProcessedSpat spat1 = loadSampleSpat();
        ProcessedSpat spat2 = loadSampleSpat();

        spat2.setOriginIp("mutated-ip");

        assertNotEquals(spat1, spat2);
    }
}
