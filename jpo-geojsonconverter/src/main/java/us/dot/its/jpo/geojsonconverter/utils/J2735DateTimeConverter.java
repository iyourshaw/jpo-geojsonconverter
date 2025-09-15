package us.dot.its.jpo.geojsonconverter.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class J2735DateTimeConverter {

    /**
     * Generate UTC timestamp from Minute of Year (MOY) and optional DSecond values.
     * 
     * @param moy Minute of Year (minutes from beginning of year)
     * @param dSecond Optional DSecond value (milliseconds in current minute)
     * @param odeTimestamp ODE received timestamp as fallback
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateUTCTimestamp(Integer moy, Integer dSecond, ZonedDateTime odeDate,
            Integer year) {
        ZonedDateTime date = null;
        try {
            if (year == null) {
                year = odeDate.getYear();
            }
            String dateString;
            long milliseconds;
            if (moy != null) {
                long minutes = moy;
                milliseconds = dSecond != null ? (long) dSecond : 0; // milliseconds in current minute
                dateString = String.format("%d-01-01T00:00:00.00Z", year);
                date = Instant.parse(dateString).atZone(ZoneId.of("UTC"));
                date = date.plusMinutes(minutes);
                date = date.plus(milliseconds, ChronoUnit.MILLIS);
            } else {
                date = odeDate;
                if (dSecond != null) {
                    milliseconds = dSecond; // milliseconds from beginning of minute
                    date = date.withSecond(0);
                    date = date.withNano(0);
                    date = date.plus(milliseconds, ChronoUnit.MILLIS);
                }
            }

        } catch (Exception e) {
            String errMsg = String.format("Failed to generate UTC Timestamp. Message: %s", e.getMessage());
            log.error(errMsg, e);
        }

        return date;
    }

    /**
     * Generate UTC timestamp from Minute of Year (MOY) and optional DSecond values.
     * 
     * @param moy Minute of Year (minutes from beginning of year)
     * @param dSecond Optional DSecond value (milliseconds in current minute)
     * @param odeTimestamp ODE received timestamp as fallback
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateUTCTimestamp(Integer moy, Integer dSecond, ZonedDateTime odeDate) {
        Integer year = odeDate.getYear();
        return generateUTCTimestamp(moy, dSecond, odeDate, year);
    }

    /**
     * Generate UTC timestamp from optional Minute of Year (MOY) and ODE received timestamp.
     * 
     * @param moy Minute of Year (minutes from beginning of year)
     * @param odeTimestamp ODE received timestamp as fallback
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateUTCTimestamp(Integer moy, ZonedDateTime odeDate) {
        if (moy == null) {
            return odeDate;
        }

        Integer year = odeDate.getYear();
        return generateUTCTimestamp(moy, null, odeDate, year);
    }

    /**
     * Generate offset UTC timestamp for SPAT converter. Handles special time mark values and rollover logic.
     *
     * @param originTimestamp Base timestamp to offset from
     * @param timeMark Time mark in centiseconds (1/100 second)
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateOffsetUTCTimestampForTimeMark(ZonedDateTime originTimestamp, Integer timeMark) {
        try {
            if (timeMark != null) {
                long millis = Long.valueOf(timeMark) * 100;
                ZonedDateTime date = originTimestamp;
                if (timeMark == 36011 || timeMark == 36001) {
                    // Return UTC time zero if the Zoned Date time is marked as unknown, UTC time zero chosen so that a
                    // null value can represent an empty field in the SPaT. But 36011, can represent an intentionally
                    // unidentified field.
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));

                } else {
                    // If we are within 10 minutes of the next hour, and the timeMark is a small number, it probably
                    // means that the time is rolling over.
                    // In this case, add an hour to the UTC timestamp so that it appears in the future instead of in the
                    // past.
                    if (originTimestamp.getMinute() > 50 && timeMark < 6000) {
                        date = date.plusHours(1);
                    }

                    date = date.withMinute(0);
                    date = date.withSecond(0);
                    date = date.withNano(0);
                    date = date.plus(millis, ChronoUnit.MILLIS);
                    return date;
                }

            } else {
                return null;
            }
        } catch (Exception e) {
            String errMsg = String.format("Failed to generateOffsetUTCTimestamp. Message: %s", e.getMessage());
            log.error(errMsg, e);
            return null;
        }
    }

    /**
     * Generate offset UTC timestamp for BSM and PSM converters. Handles special secMark values and rollover logic.
     *
     * @param odeReceivedAt ODE received timestamp
     * @param secMark Second mark (milliseconds from beginning of minute)
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateOffsetUTCTimestampForSecMark(ZonedDateTime odeReceivedAt, Integer secMark) {
        try {
            if (secMark != null) {
                int millis = (int) (secMark % 1000);
                int seconds = (int) (secMark / 1000);
                ZonedDateTime date = odeReceivedAt;
                if (secMark == 65535) {
                    // Return UTC time zero if the Zoned Date time is marked as unknown, UTC time zero chosen so that a
                    // null value can represent an empty field in the BSM/PSM. But 65535, can represent an intentionally
                    // unidentified field.
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));

                } else {
                    // If we are within 10 seconds of the next minute, and the timeMark is a large number, it probably
                    // means that the time rolled over before reception.
                    // In this case, subtract a minute from the odeReceivedAt so that the true time represents the
                    // minute in the past.
                    if (odeReceivedAt.getSecond() < 10 && secMark > 50000) {
                        date = date.minusMinutes(1);
                    }

                    date = date.withSecond(seconds);
                    date = date.withNano(0);
                    date = date.plus(millis, ChronoUnit.MILLIS);
                    return date;
                }

            } else {
                return null;
            }
        } catch (Exception e) {
            String errMsg = String.format("Failed to generateOffsetUTCTimestamp. Message: %s", e.getMessage());
            log.error(errMsg, e);
            return null;
        }
    }
}
