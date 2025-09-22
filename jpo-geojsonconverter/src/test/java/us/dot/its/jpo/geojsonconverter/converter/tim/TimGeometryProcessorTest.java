package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import org.locationtech.jts.geom.Point;

public class TimGeometryProcessorTest {
    private TimGeometryProcessor geometryProcessor;
    private OdeMessageFrameData timMF;

    @Before
    public void setup() throws IOException {
        geometryProcessor = new TimGeometryProcessor();

        // Load sample TIM JSON file
        String timJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/sample.ode-tim.json")));

        try (JsonDeserializer<OdeMessageFrameData> odeTimDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            timMF = odeTimDeserializer.deserialize("test-topic", timJsonString.getBytes());
        }
    }

    @Test
    public void testCreateGeometryFromDataFrame() {
        // Extract ASN.1 data
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        TravelerDataFrame dataFrame = messageFrame.getValue().getDataFrames().get(0);

        // Test geometry creation
        Geometry geometry = geometryProcessor.createGeometryFromDataFrame(dataFrame);

        // Verify geometry creation
        assertNotNull(geometry);
        assertTrue(geometry instanceof LineString);

        LineString lineString = (LineString) geometry;
        assertNotNull(lineString.getCoordinates());
        assertTrue(lineString.getCoordinates().length > 0);

        // Verify coordinates are valid
        for (double[] coord : lineString.getCoordinates()) {
            assertTrue(coord[0] >= -180.0 && coord[0] <= 180.0, "Invalid longitude: " + coord[0]);
            assertTrue(coord[1] >= -90.0 && coord[1] <= 90.0, "Invalid latitude: " + coord[1]);
        }
    }

    @Test
    public void testCreateGeometryFromRegion() {
        // Extract ASN.1 data
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        GeographicalPath region = messageFrame.getValue().getDataFrames().get(0).getRegions().get(0);

        // Test geometry creation from region
        Geometry geometry = geometryProcessor.createGeometryFromRegion(region);

        // Verify geometry creation
        assertNotNull(geometry);
        assertTrue(geometry instanceof LineString);

        LineString lineString = (LineString) geometry;
        assertNotNull(lineString.getCoordinates());
        assertTrue(lineString.getCoordinates().length > 0);
    }

    @Test
    public void testCalculateCenterLocationFromRegions() {
        // Extract ASN.1 data
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        TravelerInformation travelerInfo = messageFrame.getValue();

        // Test center location calculation
        Point centerPoint = geometryProcessor.calculateCenterLocationFromRegions(travelerInfo);

        // Verify center point calculation
        assertNotNull(centerPoint);
        assertNotNull(centerPoint.getCoordinate());

        // Verify coordinates are valid
        double x = centerPoint.getX();
        double y = centerPoint.getY();
        assertTrue(x >= -180.0 && x <= 180.0, "Invalid longitude: " + x);
        assertTrue(y >= -90.0 && y <= 90.0, "Invalid latitude: " + y);
    }

    @Test
    public void testRegionTypeDetermination() {
        // Extract ASN.1 data
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        GeographicalPath region = messageFrame.getValue().getDataFrames().get(0).getRegions().get(0);

        // Test region type determination by creating geometry
        Geometry geometry = geometryProcessor.createGeometryFromRegion(region);

        // Verify that PATH regions create LineString geometries
        assertNotNull(geometry);
        assertTrue(geometry instanceof LineString);
    }
}
