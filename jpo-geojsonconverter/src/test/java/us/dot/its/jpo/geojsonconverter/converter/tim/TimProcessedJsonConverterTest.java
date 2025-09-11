package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.junit.Before;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.tim.DeserializedRawTim;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiPolygon;

public class TimProcessedJsonConverterTest {
    private TimProcessedJsonConverter timProcessedJsonConverter;

    @Before
    public void setup() {
        timProcessedJsonConverter = new TimProcessedJsonConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(timProcessedJsonConverter);
    }

    @Test
    public void testInit() {
        ProcessorContext mockContext = mock(ProcessorContext.class);
        timProcessedJsonConverter.init(mockContext);
        assertNotNull(timProcessedJsonConverter);
    }

    @Test
    public void testClose() {
        timProcessedJsonConverter.close();
        assertNotNull(timProcessedJsonConverter);
    }

    @Test
    public void testTransformWithValidationFailure() {
        // Create a DeserializedRawTim with validation failure
        DeserializedRawTim rawTim = new DeserializedRawTim();
        rawTim.setValidationFailure(true);

        List<ProcessedValidationMessage> validationMessages = new ArrayList<>();
        ProcessedValidationMessage message = new ProcessedValidationMessage();
        message.setMessage("Test validation error");
        validationMessages.add(message);
        rawTim.setValidatorResults(validationMessages);
        rawTim.setFailedMessage("Test failed message");

        KeyValue<RsuIntersectionKey, ProcessedTim> result = timProcessedJsonConverter.transform(null, rawTim);

        assertNotNull(result);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNotNull(result.value);
    }

    @Test
    public void testTransformWithNullInput() {
        KeyValue<RsuIntersectionKey, ProcessedTim> result = timProcessedJsonConverter.transform(null, null);

        assertNotNull(result);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNull(result.value);
    }

    @Test
    public void testCreateFailureProcessedTim() {
        List<ProcessedValidationMessage> validationMessages = new ArrayList<>();
        ProcessedValidationMessage message = new ProcessedValidationMessage();
        message.setMessage("Test validation error");
        validationMessages.add(message);

        ProcessedTim result = timProcessedJsonConverter.createFailureProcessedTim(validationMessages, "Test message");

        assertNotNull(result);
        assertNotNull(result.getCompliance());
        assertFalse(result.getCompliance().isEmpty());
        assertFalse(result.getCompliance().get(0).isCompliant());
        assertEquals(validationMessages, result.getCompliance().get(0).getValidationMessages());
    }

    @Test
    public void testGeometryClassesExist() {
        // Test that our new geometry classes can be instantiated
        double[][] lineStringCoords = {{0.0, 0.0}, {1.0, 1.0}};
        LineString lineString = new LineString(lineStringCoords);
        assertNotNull(lineString);
        assertEquals("LineString", lineString.getType());

        double[][][] polygonCoords = {{{0.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}, {0.0, 1.0}, {0.0, 0.0}}};
        Polygon polygon = new Polygon(polygonCoords);
        assertNotNull(polygon);
        assertEquals("Polygon", polygon.getType());

        double[][][] multiLineStringCoords = {{{0.0, 0.0}, {1.0, 1.0}}, {{2.0, 2.0}, {3.0, 3.0}}};
        MultiLineString multiLineString = new MultiLineString(multiLineStringCoords);
        assertNotNull(multiLineString);
        assertEquals("MultiLineString", multiLineString.getType());

        double[][][][] multiPolygonCoords = {{{{0.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}, {0.0, 1.0}, {0.0, 0.0}}}};
        MultiPolygon multiPolygon = new MultiPolygon(multiPolygonCoords);
        assertNotNull(multiPolygon);
        assertEquals("MultiPolygon", multiPolygon.getType());
    }
}
