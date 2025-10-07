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
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
        assertTrue(geometry instanceof MultiLineString);

        MultiLineString multiLineString = (MultiLineString) geometry;
        assertNotNull(multiLineString.getCoordinates());
        assertTrue(multiLineString.getCoordinates().length > 0);
        assertTrue(multiLineString.getCoordinates().length == 2);

        // verify that both linestrings in the multilinestring are valid
        for (double[][] lineString : multiLineString.getCoordinates()) {
            for (double[] coord : lineString) {
                assertTrue(coord[0] >= -180.0 && coord[0] <= 180.0, "Invalid longitude: " + coord[0]);
                assertTrue(coord[1] >= -90.0 && coord[1] <= 90.0, "Invalid latitude: " + coord[1]);
            }
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

    @Test
    public void testClosedPathPolygonGeometry() {
        // Extract ASN.1 data - test the 4th dataframe which contains a closed path
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        TravelerDataFrame dataFrame = messageFrame.getValue().getDataFrames().get(3); // 4th dataframe (index 3)
        GeographicalPath region = dataFrame.getRegions().get(0);

        // Test geometry creation from closed path region
        Geometry geometry = geometryProcessor.createGeometryFromRegion(region);

        // Verify geometry creation
        assertNotNull(geometry);
        assertTrue(geometry instanceof Polygon);

        Polygon polygon = (Polygon) geometry;
        assertNotNull(polygon.getCoordinates());
        assertTrue(polygon.getCoordinates().length > 0);

        // Verify coordinates are valid - Polygon has double[][][] structure
        double[][][] coords = polygon.getCoordinates();
        assertTrue(coords.length > 0, "Polygon should have at least one ring");
        assertTrue(coords[0].length > 0, "Polygon ring should have coordinates");

        for (double[][] ring : coords) {
            for (double[] coord : ring) {
                assertTrue(coord[0] >= -180.0 && coord[0] <= 180.0, "Invalid longitude: " + coord[0]);
                assertTrue(coord[1] >= -90.0 && coord[1] <= 90.0, "Invalid latitude: " + coord[1]);
            }
        }

        // Verify polygon is closed (first and last coordinates should be the same)
        if (coords.length > 0 && coords[0].length > 1) {
            double[] first = coords[0][0];
            double[] last = coords[0][coords[0].length - 1];
            assertTrue(Math.abs(first[0] - last[0]) < 0.000001, "Polygon should be closed (longitude)");
            assertTrue(Math.abs(first[1] - last[1]) < 0.000001, "Polygon should be closed (latitude)");
        }
    }

    @Test
    public void testCircleGeometry() {
        // Extract ASN.1 data - test the 3rd dataframe which contains a circle
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        TravelerDataFrame dataFrame = messageFrame.getValue().getDataFrames().get(2); // 3rd dataframe (index 2)
        GeographicalPath region = dataFrame.getRegions().get(0);

        // Test geometry creation from circle region
        Geometry geometry = geometryProcessor.createGeometryFromRegion(region);

        // Verify geometry creation
        assertNotNull(geometry);
        assertTrue(geometry instanceof Polygon);

        Polygon polygon = (Polygon) geometry;
        assertNotNull(polygon.getCoordinates());
        assertTrue(polygon.getCoordinates().length > 0);

        // Verify coordinates are valid - Polygon has double[][][] structure
        double[][][] coords = polygon.getCoordinates();
        assertTrue(coords.length > 0, "Polygon should have at least one ring");
        assertTrue(coords[0].length > 0, "Polygon ring should have coordinates");

        // Verify we have enough points for a good circle approximation (adaptive based on diameter)
        // For a 250m radius circle (500m diameter), we expect around 26 points (optimal balance)
        assertTrue(coords[0].length >= 12, "Circle should have at least 12 approximation points for visual quality");
        assertTrue(coords[0].length <= 64, "Circle should not exceed 64 points to prevent excessive storage");

        for (double[][] ring : coords) {
            for (double[] coord : ring) {
                assertTrue(coord[0] >= -180.0 && coord[0] <= 180.0, "Invalid longitude: " + coord[0]);
                assertTrue(coord[1] >= -90.0 && coord[1] <= 90.0, "Invalid latitude: " + coord[1]);
            }
        }

        // Verify polygon is closed (first and last coordinates should be the same)
        if (coords.length > 0 && coords[0].length > 1) {
            double[] first = coords[0][0];
            double[] last = coords[0][coords[0].length - 1];
            assertTrue(Math.abs(first[0] - last[0]) < 0.000001, "Circle polygon should be closed (longitude)");
            assertTrue(Math.abs(first[1] - last[1]) < 0.000001, "Circle polygon should be closed (latitude)");
        }

        // Verify circle has reasonable radius by checking distance from center to edge points
        // The circle in the test data has radius 250 meters
        double centerLon = -104.6636836; // From anchor point in test data
        double centerLat = 41.1501408; // From anchor point in test data
        double expectedRadius = 250.0; // From circle radius in test data

        if (coords.length > 0 && coords[0].length > 1) {
            double[] firstPoint = coords[0][0];
            // Calculate distance using Geotools GeodeticCalculator
            GeodeticCalculator calculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
            calculator.setStartingGeographicPoint(centerLon, centerLat);
            calculator.setDestinationGeographicPoint(firstPoint[0], firstPoint[1]);
            double distance = calculator.getOrthodromicDistance();

            // Allow some tolerance for approximation (within 10% of expected radius)
            double tolerance = expectedRadius * 0.1;
            assertTrue(Math.abs(distance - expectedRadius) <= tolerance,
                    String.format(
                            "Circle radius should be approximately %f meters, but calculated distance is %f meters",
                            expectedRadius, distance));
        }
    }
}
