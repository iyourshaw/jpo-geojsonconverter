package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.standards.RtcmStandard;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

/**
 * Regression tests for gpsXyzToWgs84LonLat rejecting out-of-range/degenerate ECEF input.
 */
public class RTCMConverter_CoordinateLimitsTest {

    private final RTCMConverter converter = getConverter();

    @Test
    public void testCenterOfEarth_ReturnsEmpty() {
        assertThat(converter.gpsXyzToWgs84LonLat(0, 0, 0), equalTo(Optional.empty()));
    }

    @Test
    public void testNaNInput_ReturnsEmpty() {
        assertThat(converter.gpsXyzToWgs84LonLat(Double.NaN, Double.NaN, Double.NaN), equalTo(Optional.empty()));
    }

    @Test
    public void testInfiniteInput_ReturnsEmpty() {
        assertThat(converter.gpsXyzToWgs84LonLat(Double.POSITIVE_INFINITY, 0, 0), equalTo(Optional.empty()));
    }

    @Test
    public void testExtremeMagnitude_ReturnsEmpty() {
        assertThat(converter.gpsXyzToWgs84LonLat(1e20, 1e20, 1e20), equalTo(Optional.empty()));
    }

    @Test
    public void testValidEcefPoint_ReturnsLonLat() {
        Optional<CoordinateXY> result = converter.gpsXyzToWgs84LonLat(507057.73, -4697843.2433, 4270129.3858);
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get().getX(), closeTo(-83.84, 0.01));
        assertThat(result.get().getY(), closeTo(42.30, 0.01));
    }

    private RTCMConverter getConverter() {
        RTCMDecoder decoder = new RTCMDecoder(false);
        var properties = new GeoJsonConverterProperties();
        properties.setRtcmStandardVersion(RtcmStandard.CTI4501_V1);
        return new RTCMConverter(decoder, properties);
    }
}
