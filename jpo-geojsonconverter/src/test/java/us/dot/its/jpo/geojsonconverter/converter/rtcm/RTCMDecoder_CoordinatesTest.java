package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

/**
 * Tests for extracting geographic coordinates from Station Referenced (1005 or 1006)
 * RTCM messages.
 */
@Slf4j
public class RTCMDecoder_CoordinatesTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetXYZCoordsFromRefStation() throws JsonProcessingException {
        // Get expected values
        JsonNode decoded = mapper.readValue(MSG_1006_DECODED, JsonNode.class);
        JsonNode xNode = decoded.get("x");
        JsonNode yNode = decoded.get("y");
        JsonNode zNode = decoded.get("z");
        BigDecimal expectedX = xNode.decimalValue();
        BigDecimal expectedY = yNode.decimalValue();
        BigDecimal expectedZ = zNode.decimalValue();
        final var expectedCoords = new RTCMDecoder.XYZCoords(expectedX, expectedY, expectedZ);
        log.info("expected coords: {}", expectedCoords);

        // Test
        byte[] bytes = HexFormat.of().parseHex(MSG_1006_HEX);
        Optional<RTCMDecoder.XYZCoords> optCoords = RTCMDecoder.getXYZCoordsFromRefStation(bytes);
        assertTrue(optCoords.isPresent());
        RTCMDecoder.XYZCoords coords = optCoords.get();
        log.info("actual coords: {}", coords);
        // Use compareTo to test BigDecimal equality ignoring scale
        assertThat(coords.x(), comparesEqualTo(expectedCoords.x()));
        assertThat(coords.y(), comparesEqualTo(expectedCoords.y()));
        assertThat(coords.z(), comparesEqualTo(expectedCoords.z()));
    }

    public static String MSG_1006_HEX = "d300153ee00103012e3ade94350fde024f89f13209220000d81007";
    public static String MSG_1006_DECODED = """
            {
                "class": "RTCM3",
                "device": "stdin",
                "type": 1006,
                "length": 21,
                "station_id": 1,
                "system": [
                    "GPS",
                    "GLONASS"
                ],
                "refstation": false,
                "sro": false,
                "x": 507057.73,
                "y": -4697843.2433,
                "z": 4270129.3858,
                "h": 0.0
            }
            """;
}
