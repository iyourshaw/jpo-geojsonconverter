package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

public class RTCMDecodeExceptionTest {

    @Test
    public void constructor_setsMessage() {
        RTCMDecodeException exception = new RTCMDecodeException("bad message");

        assertThat(exception.getMessage(), equalTo("bad message"));
        assertThat(exception, instanceOf(Exception.class));
    }

    @Test
    public void constructor_allowsNullMessage() {
        RTCMDecodeException exception = new RTCMDecodeException(null);

        assertThat(exception.getMessage(), nullValue());
    }

    @Test
    public void isCheckedException_mustBeDeclaredOrCaught() {
        try {
            throwIt();
        } catch (RTCMDecodeException e) {
            assertThat(e.getMessage(), equalTo("thrown"));
            return;
        }
        throw new AssertionError("Expected RTCMDecodeException to be thrown");
    }

    private void throwIt() throws RTCMDecodeException {
        throw new RTCMDecodeException("thrown");
    }
}
