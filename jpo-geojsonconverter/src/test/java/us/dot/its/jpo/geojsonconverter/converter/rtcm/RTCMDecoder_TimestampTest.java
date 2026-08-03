package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.utils.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.HexFormat;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

/**
 * Tests for extracting the timestamp (tow) from MSM messages.
 */
@Slf4j
public class RTCMDecoder_TimestampTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetTimeOfWeekFromMSM() throws JsonProcessingException {
        // Get expected value
        JsonNode decoded = mapper.readValue(MSG_1074_DECODED, JsonNode.class);
        log.info("decoded: {}", decoded);
        JsonNode towNode = decoded.get("tow");
        final long expectedTow = towNode.asLong();
        log.info("expected tow: {}", expectedTow);

        // Test
        byte[] bytes = HexFormat.of().parseHex(MSG_1074_HEX);
        Optional<Long> optTow = RTCMDecoder.getTimeOfWeekFromMSM(bytes);
        assertTrue(optTow.isPresent());
        long tow = optTow.get();
        log.info("actual tow: {}", tow);
        assertThat(tow, equalTo(expectedTow));
    }

    public static String MSG_1074_HEX = "d300da432001751b6e62002084012d060000000020210100777ffff7a3a1a3a2a826232929038b7a0c00b86f9f50f75fee51dd1b2bce53fcab25d30b981741306c04d4098814103ea0c2419b034408c7e55bcbe798ef50a257049a8954cf6da1bb44b68f5fc3823f3485fcd21fcffc6f4b84bd2e110c1770318260c60983685a01acb80b39802ce640e67e0141e8041d501075806bd8fd7adbf616dfd85abfeeda04ccf81285a04a16fcf06df444dfd1130f480c7ffffffffffffffffffffffffffffc0000001965969a698e3a6f9e7bf54cf6e34d37e38e38c91477801323ea";
    public static String MSG_1074_DECODED = """
            {
                "class": "RTCM3",
                "device": "stdin",
                "type": 1074,
                "length": 218,
                "station_id": 1,
                "gnssid": 0,
                "subtype": "MSM4",
                "tow": 491183000,
                "sync": "1",
                "IODS": 0,
                "steering": 1,
                "extclk": 0,
                "smoothing": 0,
                "interval": 1,
                "MaskSat": 577122709842952192,
                "MaskSig": 1078067712,
                "MaskCell": 4009754607,
                "NSat": 8,
                "NSig": 4,
                "NCell": 32
            }
            """;
}
