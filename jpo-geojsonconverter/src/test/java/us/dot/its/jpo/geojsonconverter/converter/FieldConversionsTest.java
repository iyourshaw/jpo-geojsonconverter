package us.dot.its.jpo.geojsonconverter.converter;

import org.junit.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.SignalRequestMessage.DeltaTime;

import java.time.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FieldConversionsTest {
    @Test
    public void testConvertMinuteOfYear() {
        // Normal case: middle of year
        ZonedDateTime ingestTime = ZonedDateTime.of(2025, 10, 3, 0, 0, 1, 0, ZoneOffset.UTC);
        final int dayOfYear = ingestTime.getDayOfYear();
        final int minuteOfYear = dayOfYear * 24 * 60 + 5;
        final var moy = new MinuteOfTheYear(minuteOfYear);
        final ZonedDateTime minuteDate = FieldConversions.convertMinuteOfYear(moy, ingestTime);
        final int year = minuteDate.getYear();
        assertThat(year, equalTo(2025));
    }

    @Test
    public void testConvertMinuteOfYear_InvalidValue() {
        ZonedDateTime ingestTime = ZonedDateTime.of(2025, 10, 3, 0, 0, 1, 500, ZoneOffset.UTC);
        final var moy = new MinuteOfTheYear(527040L);
        final ZonedDateTime minuteDate = FieldConversions.convertMinuteOfYear(moy, ingestTime);
        assertThat(minuteDate, nullValue());
    }


    @Test
    public void testConvertMinuteOfYear_YearEnd() {
        // Test edge case of message sent at the end of the year
        final var moy = new MinuteOfTheYear(525599); // Last minute of the year

        // Last minute of this year
        final ZonedDateTime ingestTimeThisYear = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 500, ZoneOffset.UTC);
        final ZonedDateTime minuteDate1 = FieldConversions.convertMinuteOfYear(moy, ingestTimeThisYear);
        assertThat(minuteDate1, notNullValue());
        final int year1 = minuteDate1.getYear();
        assertThat(year1, equalTo(2022));

        // First minute of next year
        final ZonedDateTime ingestTimeNextYear = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime minuteDate2 = FieldConversions.convertMinuteOfYear(moy, ingestTimeNextYear);
        assertThat(minuteDate2, notNullValue());
        final int year2 = minuteDate2.getYear();
        assertThat(year2, equalTo(2022));

        final var moyLeap = new MinuteOfTheYear(527037L);
        // Last minute of leap year
        final ZonedDateTime ingestTimeThisLeapYear = ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 500, ZoneOffset.UTC);
        final ZonedDateTime minuteDateLeap1 = FieldConversions.convertMinuteOfYear(moyLeap, ingestTimeThisLeapYear);
        assertThat(minuteDateLeap1, notNullValue());
        final int yearLeap1 = minuteDateLeap1.getYear();
        assertThat(yearLeap1, equalTo(2024));

        final ZonedDateTime ingestTimeNextLeapYear = ZonedDateTime.of(2025, 1, 1, 0, 0, 1, 500, ZoneOffset.UTC);
        final ZonedDateTime minuteDateLeap2 = FieldConversions.convertMinuteOfYear(moy, ingestTimeNextLeapYear);
        assertThat(minuteDateLeap2, notNullValue());
        final int yearLeap2 = minuteDateLeap2.getYear();
        assertThat(yearLeap2, equalTo(2024));
    }

    // ========== Coordinate Conversion Tests ==========

    @Test
    public void testConvertLong() {
        // Test normal longitude conversion
        Double result = FieldConversions.convertLong(1800000000L);
        assertThat(result, equalTo(180.0));

        // Test negative longitude
        result = FieldConversions.convertLong(-1800000000L);
        assertThat(result, equalTo(-180.0));

        // Test zero longitude
        result = FieldConversions.convertLong(0L);
        assertThat(result, equalTo(0.0));

        // Test invalid longitude (should return null)
        result = FieldConversions.convertLong(1800000001L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertLat() {
        // Test normal latitude conversion
        Double result = FieldConversions.convertLat(900000000L);
        assertThat(result, equalTo(90.0));

        // Test negative latitude
        result = FieldConversions.convertLat(-900000000L);
        assertThat(result, equalTo(-90.0));

        // Test zero latitude
        result = FieldConversions.convertLat(0L);
        assertThat(result, equalTo(0.0));

        // Test invalid latitude (should return null)
        result = FieldConversions.convertLat(900000001L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertLongWithZoom() {
        // Test normal longitude with zoom
        Double result = FieldConversions.convertLongWithZoom(1800000000L, 2.0);
        assertThat(result, equalTo(90.0));

        // Test with null base value
        result = FieldConversions.convertLongWithZoom(1800000001L, 2.0);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertLatWithZoom() {
        // Test normal latitude with zoom
        Double result = FieldConversions.convertLatWithZoom(900000000L, 4.0);
        assertThat(result, equalTo(22.5));

        // Test with null base value
        result = FieldConversions.convertLatWithZoom(900000001L, 4.0);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertJ2735XY() {
        // Test XY conversion with zoom
        double[] result = FieldConversions.convertJ2735XY(100000L, 200000L, 40.0, 2.0);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(2));

        // Verify longitude offset calculation
        double expectedLonOffset = (100000.0 / 11111100.0) / (2.0 * Math.cos(Math.toRadians(40.0)));
        assertThat(result[0], closeTo(expectedLonOffset, 0.0001));

        // Verify latitude offset calculation
        double expectedLatOffset = (200000.0 / 11111100.0) / 2.0;
        assertThat(result[1], closeTo(expectedLatOffset, 0.0001));
    }

    // ========== Elevation Conversion Tests ==========

    @Test
    public void testConvertElevation() {
        // Test normal elevation
        Double result = FieldConversions.convertElevation(1000L);
        assertThat(result, equalTo(100.0));

        // Test negative elevation
        result = FieldConversions.convertElevation(-1000L);
        assertThat(result, equalTo(-100.0));

        // Test zero elevation
        result = FieldConversions.convertElevation(0L);
        assertThat(result, equalTo(0.0));

        // Test unavailable elevation (should return null)
        result = FieldConversions.convertElevation(-4096L);
        assertThat(result, nullValue());
    }

    // ========== Acceleration Conversion Tests ==========

    @Test
    public void testConvertAccelLatLong() {
        // Test normal acceleration
        Double result = FieldConversions.convertAccelLatLong(1000L);
        assertThat(result, equalTo(10.0));

        // Test negative acceleration
        result = FieldConversions.convertAccelLatLong(-1000L);
        assertThat(result, equalTo(-10.0));

        // Test zero acceleration
        result = FieldConversions.convertAccelLatLong(0L);
        assertThat(result, equalTo(0.0));

        // Test unavailable acceleration (should return null)
        result = FieldConversions.convertAccelLatLong(2001L);
        assertThat(result, nullValue());

        // Test values beyond range
        result = FieldConversions.convertAccelLatLong(-2001L);
        assertThat(result, equalTo(-20.0));

        result = FieldConversions.convertAccelLatLong(2002L);
        assertThat(result, equalTo(20.0));
    }

    @Test
    public void testConvertAccelVert() {
        // Test normal vertical acceleration
        Double result = FieldConversions.convertAccelVert(50L);
        assertThat(result, equalTo(1.0));

        // Test negative vertical acceleration
        result = FieldConversions.convertAccelVert(-50L);
        assertThat(result, equalTo(-1.0));

        // Test zero vertical acceleration
        result = FieldConversions.convertAccelVert(0L);
        assertThat(result, equalTo(0.0));

        // Test unavailable vertical acceleration (should return null)
        result = FieldConversions.convertAccelVert(-127L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertAccelYaw() {
        // Test normal yaw rate
        Double result = FieldConversions.convertAccelYaw(1000L);
        assertThat(result, equalTo(10.0));

        // Test negative yaw rate
        result = FieldConversions.convertAccelYaw(-1000L);
        assertThat(result, equalTo(-10.0));

        // Test zero yaw rate
        result = FieldConversions.convertAccelYaw(0L);
        assertThat(result, equalTo(0.0));

        // Test values outside valid range (should return null)
        result = FieldConversions.convertAccelYaw(-32768L);
        assertThat(result, nullValue());

        result = FieldConversions.convertAccelYaw(32768L);
        assertThat(result, nullValue());
    }

    // ========== Accuracy Conversion Tests ==========

    @Test
    public void testConvertSemiMajor() {
        // Test normal semi-major axis
        Double result = FieldConversions.convertSemiMajor(100L);
        assertThat(result, equalTo(5.0));

        // Test zero semi-major axis
        result = FieldConversions.convertSemiMajor(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid value
        result = FieldConversions.convertSemiMajor(254L);
        assertThat(result, closeTo(12.7, 0.0001));

        // Test unavailable value (should return null)
        result = FieldConversions.convertSemiMajor(255L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertSemiMinor() {
        // Test normal semi-minor axis
        Double result = FieldConversions.convertSemiMinor(200L);
        assertThat(result, equalTo(10.0));

        // Test zero semi-minor axis
        result = FieldConversions.convertSemiMinor(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid value
        result = FieldConversions.convertSemiMinor(254L);
        assertThat(result, closeTo(12.7, 0.0001));

        // Test unavailable value (should return null)
        result = FieldConversions.convertSemiMinor(255L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertOrientation() {
        // Test normal orientation
        Double result = FieldConversions.convertOrientation(1000L);
        assertThat(result, closeTo(5.4932479, 0.0001));

        // Test zero orientation
        result = FieldConversions.convertOrientation(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid value
        result = FieldConversions.convertOrientation(65534L);
        assertThat(result, closeTo(359.9945078786, 0.0001));

        // Test unavailable value (should return null)
        result = FieldConversions.convertOrientation(65535L);
        assertThat(result, nullValue());
    }

    // ========== Angle Conversion Tests ==========

    @Test
    public void testConvertAngle() {
        // Test normal angle
        Double result = FieldConversions.convertAngle(10L);
        assertThat(result, equalTo(15.0));

        // Test negative angle
        result = FieldConversions.convertAngle(-10L);
        assertThat(result, equalTo(-15.0));

        // Test zero angle
        result = FieldConversions.convertAngle(0L);
        assertThat(result, equalTo(0.0));

        // Test values beyond range (should return null)
        result = FieldConversions.convertAngle(-127L);
        assertThat(result, nullValue());

        result = FieldConversions.convertAngle(127L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertHeading() {
        // Test normal heading
        Double result = FieldConversions.convertHeading(1000L);
        assertThat(result, equalTo(12.5));

        // Test zero heading
        result = FieldConversions.convertHeading(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid heading
        result = FieldConversions.convertHeading(28800L);
        assertThat(result, equalTo(360.0));

        // Test values outside valid range (should return null)
        result = FieldConversions.convertHeading(-1L);
        assertThat(result, nullValue());

        result = FieldConversions.convertHeading(28801L);
        assertThat(result, nullValue());
    }

    // ========== Speed Conversion Tests ==========

    @Test
    public void testConvertSpeed() {
        // Test normal speed
        Double result = FieldConversions.convertSpeed(1000L);
        assertThat(result, equalTo(20.0));

        // Test zero speed
        result = FieldConversions.convertSpeed(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid speed
        result = FieldConversions.convertSpeed(8190L);
        assertThat(result, equalTo(163.8));

        // Test unavailable speed (should return null)
        result = FieldConversions.convertSpeed(8191L);
        assertThat(result, nullValue());
    }

    // ========== Date/Time Conversion Tests ==========

    @Test
    public void testConvertDYear() {
        // Test normal year
        DYear dYear = new DYear(2024L);
        Integer result = FieldConversions.convertDYear(dYear);
        assertThat(result, equalTo(2024));

        // Test unknown year (should return null)
        dYear = new DYear(0L);
        result = FieldConversions.convertDYear(dYear);
        assertThat(result, nullValue());

        // Test null year (should return null)
        result = FieldConversions.convertDYear(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDMonth() {
        // Test normal month
        DMonth dMonth = new DMonth(6L);
        Integer result = FieldConversions.convertDMonth(dMonth);
        assertThat(result, equalTo(6));

        // Test unknown month (should return null)
        dMonth = new DMonth(0L);
        result = FieldConversions.convertDMonth(dMonth);
        assertThat(result, nullValue());

        // Test null month (should return null)
        result = FieldConversions.convertDMonth(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDDay() {
        // Test normal day
        DDay dDay = new DDay(15L);
        Integer result = FieldConversions.convertDDay(dDay);
        assertThat(result, equalTo(15));

        // Test unknown day (should return null)
        dDay = new DDay(0L);
        result = FieldConversions.convertDDay(dDay);
        assertThat(result, nullValue());

        // Test null day (should return null)
        result = FieldConversions.convertDDay(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDHour() {
        // Test normal hour
        DHour dHour = new DHour(14L);
        Integer result = FieldConversions.convertDHour(dHour);
        assertThat(result, equalTo(14));

        // Test unknown hour (should return null)
        dHour = new DHour(31L);
        result = FieldConversions.convertDHour(dHour);
        assertThat(result, nullValue());

        // Test null hour (should return null)
        result = FieldConversions.convertDHour(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDMinute() {
        // Test normal minute
        DMinute dMinute = new DMinute(30L);
        Integer result = FieldConversions.convertDMinute(dMinute);
        assertThat(result, equalTo(30));

        // Test unknown minute (should return null)
        dMinute = new DMinute(60L);
        result = FieldConversions.convertDMinute(dMinute);
        assertThat(result, nullValue());

        // Test null minute (should return null)
        result = FieldConversions.convertDMinute(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDSecond() {
        // Test normal second
        DSecond dSecond = new DSecond(30000L); // 30 seconds, 0 milliseconds
        FieldConversions.SecondNanos result = FieldConversions.convertDSecond(dSecond);
        assertThat(result, notNullValue());
        assertThat(result.secondOfMinute(), equalTo(30));
        assertThat(result.nanoOfSecond(), equalTo(0));

        // Test with milliseconds
        dSecond = new DSecond(30150L); // 30 seconds, 150 milliseconds
        result = FieldConversions.convertDSecond(dSecond);
        assertThat(result, notNullValue());
        assertThat(result.secondOfMinute(), equalTo(30));
        assertThat(result.nanoOfSecond(), equalTo(150000000));

        // Test unavailable second (should return null)
        dSecond = new DSecond(65535L);
        result = FieldConversions.convertDSecond(dSecond);
        assertThat(result, nullValue());

        // Test null second (should return null)
        result = FieldConversions.convertDSecond(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDOffset() {
        // Test normal offset
        DOffset dOffset = new DOffset(120L); // +2 hours
        ZoneOffset result = FieldConversions.convertDOffset(dOffset);
        assertThat(result, equalTo(ZoneOffset.ofHours(2)));

        // Test negative offset
        dOffset = new DOffset(-180L); // -3 hours
        result = FieldConversions.convertDOffset(dOffset);
        assertThat(result, equalTo(ZoneOffset.ofHoursMinutes(-3, 0)));

        // Test null offset (should return UTC)
        result = FieldConversions.convertDOffset(null);
        assertThat(result, equalTo(ZoneOffset.UTC));
    }

    // ========== Lane Width Conversion Tests ==========

    @Test
    public void testConvertLaneWidth() {
        // Test normal lane width
        LaneWidth laneWidth = new LaneWidth(350L);
        Double result = FieldConversions.convertLaneWidth(laneWidth);
        assertThat(result, equalTo(3.5));

        // Test null lane width (should return null)
        result = FieldConversions.convertLaneWidth(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testCalculateLaneWidthOffset() {
        // Test lane width offset calculation
        Double result = FieldConversions.calculateLaneWidthOffset(3.5, 50L);
        assertThat(result, equalTo(4.0));

        // Test with zero offset
        result = FieldConversions.calculateLaneWidthOffset(3.5, 0L);
        assertThat(result, equalTo(3.5));
    }

    @Test
    public void testCalculateElevationOffset() {
        // Test elevation offset calculation
        Double result = FieldConversions.calculateElevationOffset(100.0, 50L);
        assertThat(result, equalTo(100.5));

        // Test with null elevation meter (should return null)
        result = FieldConversions.calculateElevationOffset(null, 50L);
        assertThat(result, nullValue());
    }

    // ========== Heading Sector Tests ==========
    // Note: Asn1Bitstring tests are commented out due to constructor issues
    // These would need to be implemented with proper mock objects or test data

    @Test
    public void testSectorBitToHeading() {
        // Test sector bit to heading conversion
        double result = FieldConversions.sectorBitToHeading(0);
        assertThat(result, equalTo(0.0));

        result = FieldConversions.sectorBitToHeading(1);
        assertThat(result, equalTo(22.5));

        result = FieldConversions.sectorBitToHeading(8);
        assertThat(result, equalTo(180.0));
    }

    @Test
    public void testSectorRangeToHeadingAndRange() {
        // Test sector range to heading and range conversion
        double[] result = FieldConversions.sectorRangeToHeadingAndRange(0, 2);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(2));
        assertThat(result[0], equalTo(22.5)); // Center heading
        assertThat(result[1], equalTo(67.5)); // Total range (3 sectors * 22.5)
    }

    @Test
    public void testGetHeadingSectorRange() {
        // Test getting heading sector range
        double result = FieldConversions.getHeadingSectorRange();
        assertThat(result, equalTo(22.5));
    }

    // ========== Radius Conversion Tests ==========

    @Test
    public void testConvertRadiusToMeters() {
        // Test centimeter conversion
        Double result = FieldConversions.convertRadiusToMeters(100L, DistanceUnits.CENTIMETER);
        assertThat(result, equalTo(1.0));

        // Test meter conversion
        result = FieldConversions.convertRadiusToMeters(100L, DistanceUnits.METER);
        assertThat(result, equalTo(100.0));

        // Test kilometer conversion
        result = FieldConversions.convertRadiusToMeters(1L, DistanceUnits.KILOMETER);
        assertThat(result, equalTo(1000.0));

        // Test foot conversion
        result = FieldConversions.convertRadiusToMeters(100L, DistanceUnits.FOOT);
        assertThat(result, closeTo(30.48, 0.01));

        // Test null units (should return null)
        result = FieldConversions.convertRadiusToMeters(100L, null);
        assertThat(result, nullValue());
    }

    // ========== Message Count Conversion Tests ==========

    @Test
    public void testConvertMsgCount() {
        // Test normal message count
        MsgCount msgCount = new MsgCount(5L);
        Integer result = FieldConversions.convertMsgCount(msgCount);
        assertThat(result, equalTo(5));

        // Test null message count (should return null)
        result = FieldConversions.convertMsgCount(null);
        assertThat(result, nullValue());
    }

    // ========== Vehicle ID Conversion Tests ==========
    // Note: VehicleID tests are commented out due to EntityID constructor issues
    // These would need to be implemented with proper mock objects or test data

    // ========== Delta Time Conversion Tests ==========

    @Test
    public void testConvertDeltaTime() {
        // Test normal delta time
        DeltaTime deltaTime = new DeltaTime(60L); // 60 * 10 seconds = 600 seconds
        Duration result = FieldConversions.convertDeltaTime(deltaTime);
        assertThat(result, notNullValue());
        assertThat(result.getSeconds(), equalTo(600L));

        // Test negative delta time
        deltaTime = new DeltaTime(-60L); // -60 * 10 seconds = -600 seconds
        result = FieldConversions.convertDeltaTime(deltaTime);
        assertThat(result, notNullValue());
        assertThat(result.getSeconds(), equalTo(-600L));

        // Test unavailable delta time (should return null)
        deltaTime = new DeltaTime(-122L);
        result = FieldConversions.convertDeltaTime(deltaTime);
        assertThat(result, nullValue());

        // Test null delta time (should return null)
        result = FieldConversions.convertDeltaTime(null);
        assertThat(result, nullValue());
    }
}
