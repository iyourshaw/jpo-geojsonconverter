package us.dot.its.jpo.geojsonconverter.converter;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.beans.factory.annotation.Autowired;

import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.converter.rtcm.RTCMConverter;
import us.dot.its.jpo.geojsonconverter.validator.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class JsonConverterServiceControllerTest {
    JsonConverterServiceController geoJsonConverterServiceController;
    GeoJsonConverterProperties props;

    @Autowired
    MapJsonValidator mapJsonValidator;

    @Autowired
    SpatJsonValidator spatJsonValidator;

    @Autowired
    BsmJsonValidator bsmJsonValidator;

    @Autowired
    PsmJsonValidator psmJsonValidator;

    @Autowired
    RTCMJsonValidator rtcmJsonValidator;

    @Autowired
    RTCMConverter rtcmConverter;

    @Autowired
    TimJsonValidator timJsonValidator;

    @Before
    public void setup() {
        props = new GeoJsonConverterProperties();
        props.initialize();
    }

    @Test
    public void testSpringBootLoaded() {
        geoJsonConverterServiceController =
                new JsonConverterServiceController(props, mapJsonValidator, spatJsonValidator, bsmJsonValidator,
                        psmJsonValidator, rtcmJsonValidator, rtcmConverter, timJsonValidator);
        assertNotNull(geoJsonConverterServiceController);
    }
}
