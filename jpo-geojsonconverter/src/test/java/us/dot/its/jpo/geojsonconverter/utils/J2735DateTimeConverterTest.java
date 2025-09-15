package us.dot.its.jpo.geojsonconverter.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class J2735DateTimeConverterTest {

    // Test data - January 1, 2024 12:00:00 UTC
    private static final ZonedDateTime TEST_BASE_DATE = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));

    // Test data - June 15, 2024 15:30:45 UTC
    private static final ZonedDateTime TEST_ODE_DATE = ZonedDateTime.of(2024, 6, 15, 15, 30, 45, 0, ZoneId.of("UTC"));

    @Test
    public void testGenerateUTCTimestampWithMoyAndDSecond() {
        // Test basic MOY and DSecond conversion
        Integer moy = 1000; // 1000 minutes from start of year
        Integer dSecond = 5000; // 5 seconds (5000 milliseconds)
        Integer year = 2024;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, year);

        assertNotNull("Result should not be null", result);
        assertEquals("Year should be correct", 2024, result.getYear());
        assertEquals("Month should be January", 1, result.getMonthValue());
        assertEquals("Day should be 1", 1, result.getDayOfMonth());
        assertEquals("Hour should be 16 (1000 minutes = 16 hours 40 minutes)", 16, result.getHour());
        assertEquals("Minute should be 40", 40, result.getMinute());
        assertEquals("Second should be 5", 5, result.getSecond());
        assertEquals("Zone should be UTC", ZoneId.of("UTC"), result.getZone());
    }

    @Test
    public void testGenerateUTCTimestampWithMoyOnly() {
        // Test MOY only (no DSecond)
        Integer moy = 1440; // 1440 minutes = 24 hours = 1 day
        Integer year = 2024;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, null, TEST_ODE_DATE, year);

        assertNotNull("Result should not be null", result);
        assertEquals("Year should be correct", 2024, result.getYear());
        assertEquals("Month should be January", 1, result.getMonthValue());
        assertEquals("Day should be 2 (1440 minutes = 24 hours)", 2, result.getDayOfMonth());
        assertEquals("Hour should be 0", 0, result.getHour());
        assertEquals("Minute should be 0", 0, result.getMinute());
        assertEquals("Second should be 0", 0, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoy() {
        // Test with null MOY - should use ODE date
        Integer dSecond = 2000; // 2 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMonthValue(), result.getMonthValue());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getHour(), result.getHour());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 2 (from DSecond)", 2, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampWithNullYear() {
        // Test with null year - should use ODE date year
        Integer moy = 100;
        Integer dSecond = 1000;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, null);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date year", TEST_ODE_DATE.getYear(), result.getYear());
    }

    @Test
    public void testGenerateUTCTimestampThreeParameterOverload() {
        // Test the 3-parameter overload
        Integer moy = 2000;
        Integer dSecond = 3000;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date year", TEST_ODE_DATE.getYear(), result.getYear());
    }

    @Test
    public void testGenerateUTCTimestampTwoParameterOverload() {
        // Test the 2-parameter overload with MOY
        Integer moy = 500;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date year", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should calculate correct time from MOY", 8, result.getHour()); // 500 minutes = 8 hours 20 minutes
        assertEquals("Should calculate correct time from MOY", 20, result.getMinute());
    }

    @Test
    public void testGenerateUTCTimestampTwoParameterOverloadWithNullMoy() {
        // Test the 2-parameter overload with null MOY
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return ODE date unchanged", TEST_ODE_DATE, result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkBasic() {
        // Test basic time mark conversion
        Integer timeMark = 5000; // 50 seconds (5000 centiseconds = 500000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be same as base", TEST_BASE_DATE.getHour(), result.getHour());
        assertEquals("Minute should be 8 (500000ms = 8min 20s)", 8, result.getMinute());
        assertEquals("Second should be 20", 20, result.getSecond());
        assertEquals("Zone should be UTC", ZoneId.of("UTC"), result.getZone());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkUnknown36011() {
        // Test special time mark 36011 (unknown)
        Integer timeMark = 36011;

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return epoch time for unknown time mark", 0, result.toEpochSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkUnknown36001() {
        // Test special time mark 36001 (unknown)
        Integer timeMark = 36001;

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return epoch time for unknown time mark", 0, result.toEpochSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkRollover() {
        // Test rollover logic - within 10 minutes of next hour with small time mark
        ZonedDateTime nearHourEnd = ZonedDateTime.of(2024, 1, 1, 15, 55, 0, 0, ZoneId.of("UTC")); // 5 minutes before 4
                                                                                                  // PM
        Integer timeMark = 1000; // 10 seconds (1000 centiseconds = 100000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(nearHourEnd, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should add an hour for rollover", 16, result.getHour()); // Should be 4 PM, not 3 PM
        assertEquals("Minute should be 1 (100000ms = 1min 40s)", 1, result.getMinute());
        assertEquals("Second should be 40", 40, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkNoRollover() {
        // Test no rollover - not within 10 minutes of next hour
        ZonedDateTime normalTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 0, 0, ZoneId.of("UTC")); // 30 minutes past hour
        Integer timeMark = 1000; // 10 seconds (1000 centiseconds = 100000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(normalTime, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should not add an hour", 15, result.getHour()); // Should stay at 3 PM
        assertEquals("Minute should be 1 (100000ms = 1min 40s)", 1, result.getMinute());
        assertEquals("Second should be 40", 40, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkNull() {
        // Test with null time mark
        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, null);

        assertNull("Result should be null for null time mark", result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkBasic() {
        // Test basic sec mark conversion
        Integer secMark = 5000; // 5 seconds (5000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be same as base", TEST_BASE_DATE.getHour(), result.getHour());
        assertEquals("Minute should be same as base", TEST_BASE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 5", 5, result.getSecond());
        assertEquals("Millisecond should be 0", 0, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkWithMilliseconds() {
        // Test sec mark with milliseconds
        Integer secMark = 5234; // 5 seconds 234 milliseconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Second should be 5", 5, result.getSecond());
        assertEquals("Millisecond should be 234", 234, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkUnknown65535() {
        // Test special sec mark 65535 (unknown)
        Integer secMark = 65535;

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return epoch time for unknown sec mark", 0, result.toEpochSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkRollover() {
        // Test rollover logic - within 10 seconds of next minute with large sec mark
        ZonedDateTime nearMinuteEnd = ZonedDateTime.of(2024, 1, 1, 15, 30, 5, 0, ZoneId.of("UTC")); // 5 seconds past
                                                                                                    // minute
        Integer secMark = 55000; // 55 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(nearMinuteEnd, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should subtract a minute for rollover", 29, result.getMinute()); // Should be 29 minutes, not 30
        assertEquals("Second should be 55", 55, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkNoRollover() {
        // Test no rollover - not within 10 seconds of next minute
        ZonedDateTime normalTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 30, 0, ZoneId.of("UTC")); // 30 seconds past
                                                                                                  // minute
        Integer secMark = 55000; // 55 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(normalTime, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should not subtract a minute", 30, result.getMinute()); // Should stay at 30 minutes
        assertEquals("Second should be 55", 55, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkNull() {
        // Test with null sec mark
        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, null);

        assertNull("Result should be null for null sec mark", result);
    }

    @Test
    public void testGenerateUTCTimestampEdgeCaseZeroMoy() {
        // Test with zero MOY
        Integer moy = 0;
        Integer dSecond = 1000;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be start of year", 1, result.getMonthValue());
        assertEquals("Should be start of year", 1, result.getDayOfMonth());
        assertEquals("Should be start of year", 0, result.getHour());
        assertEquals("Should be start of year", 0, result.getMinute());
        assertEquals("Should be 1 second from DSecond", 1, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampEdgeCaseLargeMoy() {
        // Test with large MOY (near end of year)
        Integer moy = 525600; // 525600 minutes = 365 days = 1 year
        Integer dSecond = 0;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be end of year", 12, result.getMonthValue());
        assertEquals("Should be end of year", 31, result.getDayOfMonth());
        assertEquals("Should be end of year", 0, result.getHour());
        assertEquals("Should be end of year", 0, result.getMinute());
    }

    @Test
    public void testGenerateUTCTimestampEdgeCaseLargeDSecond() {
        // Test with large DSecond (near end of minute)
        Integer moy = 100;
        Integer dSecond = 59000; // 59 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be 59 seconds", 59, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkEdgeCaseLargeTimeMark() {
        // Test with large time mark (near end of hour)
        Integer timeMark = 359900; // 59 minutes 59 seconds (359900 centiseconds = 35990000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Minute should be 59", 59, result.getMinute());
        assertEquals("Second should be 50 (35990000ms = 35990s = 59min 50s)", 50, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkEdgeCaseLargeSecMark() {
        // Test with large sec mark (near end of minute)
        Integer secMark = 59999; // 59 seconds 999 milliseconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Second should be 59", 59, result.getSecond());
        assertEquals("Millisecond should be 999", 999, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateUTCTimestampWithNegativeValues() {
        // Test with negative values (should handle gracefully)
        Integer moy = -100;
        Integer dSecond = -1000;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        // The method should handle negative values gracefully
        assertNotNull("Result should not be null even with negative values", result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkWithNegativeTimeMark() {
        // Test with negative time mark
        Integer timeMark = -1000;

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        // The method should handle negative values gracefully
        assertNotNull("Result should not be null even with negative time mark", result);
    }

    @Test
    public void testGenerateUTCTimestampWithLeapYear() {
        // Test with leap year
        Integer moy = 1440; // 1 day
        Integer dSecond = 0;
        Integer leapYear = 2024; // 2024 is a leap year

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, leapYear);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be January 2nd", 2, result.getDayOfMonth());
    }

    @Test
    public void testGenerateUTCTimestampWithNonLeapYear() {
        // Test with non-leap year
        Integer moy = 1440; // 1 day
        Integer dSecond = 0;
        Integer nonLeapYear = 2023; // 2023 is not a leap year

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, nonLeapYear);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be January 2nd", 2, result.getDayOfMonth());
    }

    @Test
    public void testGenerateUTCTimestampConsistency() {
        // Test that different overloads produce consistent results
        Integer moy = 1000;
        Integer dSecond = 5000;
        Integer year = 2024;

        ZonedDateTime result1 = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, year);
        ZonedDateTime result2 = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE);

        assertNotNull("Result 1 should not be null", result1);
        assertNotNull("Result 2 should not be null", result2);
        assertEquals("Results should be consistent", result1, result2);
    }
}
