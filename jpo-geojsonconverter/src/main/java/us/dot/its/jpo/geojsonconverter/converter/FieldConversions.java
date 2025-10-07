package us.dot.its.jpo.geojsonconverter.converter;


import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.runtime.types.Asn1Bitstring;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;

public class FieldConversions {

    // Heading sector constants
    private static final double HEADING_SECTOR_DEGREES = 22.5;
    private static final double HEADING_SECTOR_RANGE = 22.5;
    private static final int MAX_HEADING_SECTORS = 16;
    private static final double CENTIMETERS_PER_DEGREE_LATITUDE = 11111100.0;
    private static final double J2735_DECIMAL_CONVERSION_FACTOR = 10000000.0;

    public static Double convertLong(long j2735Long) {
        // Longitude ::= INTEGER (-1799999999..1800000001)
        // -- LSB = 1/10 microdegree
        // -- Providing a range of plus-minus 180 degrees
        Double returnValue = null;
        if (j2735Long != 1800000001) {
            returnValue = j2735Long / J2735_DECIMAL_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    public static Double convertLat(long j2735Lat) {
        // Latitude ::= INTEGER (-900000000..900000001)
        // -- LSB = 1/10 microdegree
        // -- Providing a range of plus-minus 90 degrees
        Double returnValue = null;
        if (j2735Lat != 900000001) {
            returnValue = j2735Lat / J2735_DECIMAL_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Convert J2735 longitude value to decimal degrees with zoom scaling.
     * 
     * @param j2735Long J2735 longitude value
     * @param zoomFactor Zoom scaling factor (2^zoom)
     * @return Longitude in decimal degrees
     */
    public static Double convertLongWithZoom(long j2735Long, double zoomFactor) {
        Double baseValue = convertLong(j2735Long);
        if (baseValue != null) {
            return baseValue / zoomFactor;
        }
        return null;
    }

    /**
     * Convert J2735 latitude value to decimal degrees with zoom scaling.
     * 
     * @param j2735Lat J2735 latitude value
     * @param zoomFactor Zoom scaling factor (2^zoom)
     * @return Latitude in decimal degrees
     */
    public static Double convertLatWithZoom(long j2735Lat, double zoomFactor) {
        Double baseValue = convertLat(j2735Lat);
        if (baseValue != null) {
            return baseValue / zoomFactor;
        }
        return null;
    }

    /**
     * Convert J2735 XY coordinate to decimal degrees with zoom scaling.
     * 
     * @param j2735X J2735 X coordinate value (centimeters)
     * @param j2735Y J2735 Y coordinate value (centimeters)
     * @param currentLat Current latitude for longitude scaling
     * @param zoomFactor Zoom scaling factor (2^zoom)
     * @return Array with [longitude_offset, latitude_offset] in decimal degrees
     */
    public static double[] convertJ2735XY(long j2735X, long j2735Y, double currentLat, double zoomFactor) {
        double latOffset = (j2735Y / CENTIMETERS_PER_DEGREE_LATITUDE) / zoomFactor;
        double lonOffset =
                (j2735X / (CENTIMETERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(currentLat)))) / zoomFactor;
        return new double[] {lonOffset, latOffset};
    }

    /**
     * Converts a J2735 elevation value to meters. Providing a range of -409.5 to + 6143.9 meters. The value -4096 shall
     * be used when Unknown is to be sent.
     *
     * @param j2735Elev J2735 elevation value.
     * @return Elevation in meters, or null if unavailable.
     */
    public static Double convertElevation(long j2735Elev) {
        Double returnValue = null;
        if (j2735Elev != -4096) {
            returnValue = j2735Elev * 1e-1;
        }
        return returnValue;
    }

    public static Double convertAccelLatLong(long accelLatLong) {
        // Acceleration ::= INTEGER (-2000..2001)
        // -- LSB units are 0.01 m/s^2
        // -- the value 2000 shall be used for values greater than 2000
        // -- the value -2000 shall be used for values less than -2000
        // -- a value of 2001 means the value is unavailable
        Double returnValue = null;
        if (accelLatLong < -2000) {
            returnValue = -20.0;
        } else if (accelLatLong > 2001) {
            returnValue = 20.0;
        } else if (accelLatLong != 2001) {
            returnValue = accelLatLong * 0.01;
        }
        return returnValue;
    }

    public static Double convertAccelVert(long accelVert) {
        // VerticalAcceleration ::= INTEGER (-127..127)
        // -- LSB units of 0.02 G steps over -2.52 to +2.54 G
        // -- The value +127 shall be used for ranges >= 2.54 G
        // -- The value -126 shall be used for ranges <= -2.52 G
        // -- The value -127 shall be used for unavailable
        Double returnValue = null;
        if (accelVert != -127) {
            returnValue = accelVert * 0.02;
        }
        return returnValue;
    }

    public static Double convertAccelYaw(long accelYaw) {
        // YawRate ::= INTEGER (-32767..32767)
        // -- LSB units of 0.01 degrees per second (signed)
        Double returnValue = null;
        if (accelYaw >= -32767 && accelYaw <= 32767) {
            returnValue = accelYaw * 0.01;
        }
        return returnValue;
    }

    public static Double convertSemiMajor(long semiMajor) {
        // SemiMajorAxisAccuracy ::= INTEGER (0..255)
        // -- semi-major axis accuracy at one standard dev
        // -- range 0-12.7 meter, LSB = .05m
        // -- 254 = any value equal or greater than 12.70 meter
        // -- 255 = unavailable semi-major axis value
        Double returnValue = null;
        if (semiMajor >= 0 && semiMajor < 255) {
            returnValue = semiMajor * 0.05;
        }
        return returnValue;
    }

    public static Double convertSemiMinor(long semiMinor) {
        // SemiMinorAxisAccuracy ::= INTEGER (0..255)
        // -- semi-minor axis accuracy at one standard dev
        // -- range 0-12.7 meter, LSB = .05m
        // -- 254 = any value equal or greater than 12.70 meter
        // -- 255 = unavailable semi-minor axis val
        Double returnValue = null;
        if (semiMinor >= 0 && semiMinor < 255) {
            returnValue = semiMinor * 0.05;
        }
        return returnValue;
    }

    public static Double convertOrientation(long orientation) {
        // SemiMajorAxisOrientation ::= INTEGER (0..65535)
        // -- orientation of semi-major axis
        // -- relative to true north (0~359.9945078786 degrees)
        // -- LSB units of 360/65535 deg = 0.0054932479
        // -- a value of 0 shall be 0 degrees
        // -- a value of 1 shall be 0.0054932479 degrees
        // -- a value of 65534 shall be 359.9945078786 deg
        // -- a value of 65535 shall be used for orientation unavailable
        Double returnValue = null;
        if (orientation >= 0 && orientation < 65535) {
            returnValue = 0.0054932479 * orientation;
        }
        return returnValue;
    }

    public static Double convertAngle(long angle) {
        // SteeringWheelAngle ::= INTEGER (-126..127)
        // -- LSB units of 1.5 degrees, a range of -189 to +189 degrees
        // -- +001 = +1.5 deg
        // -- -126 = -189 deg and beyond
        // -- +126 = +189 deg and beyond
        // -- +127 to be used for unavai
        Double returnValue = null;
        if (angle >= -126 && angle < 127) {
            returnValue = angle * 1.5;
        }
        return returnValue;
    }

    public static Double convertHeading(long angle) {
        // Heading ::= INTEGER (0..28800)
        // -- LSB of 0.0125 degrees
        // -- A range of 0 to 359.9875 degrees
        Double returnValue = null;
        if (angle >= 0 && angle <= 28800) {
            returnValue = angle * 0.0125;
        }
        return returnValue;
    }

    public static Double convertSpeed(long speed) {
        // Speed ::= INTEGER (0..8191) -- Units of 0.02 m/s
        // -- The value 8191 indicates that
        // -- speed is unavailable
        Double returnValue = null;
        if (speed >= 0 && speed < 8191) {
            returnValue = speed * 0.02;
        }
        return returnValue;
    }

    public static Long convertDDateTime(List<String> validationMessages, DDateTime dDateTime) {
        if (dDateTime == null) {
            validationMessages.add("DDateTime is missing.");
            return null;
        }

        Integer year = convertDYear(dDateTime.getYear());
        if (year == null) {
            validationMessages.add("DDateTime 'year' field is missing.");
        }

        Integer month = convertDMonth(dDateTime.getMonth());
        if (month == null) {
            validationMessages.add("DDateTime 'month' field is missing.");
        }

        Integer dayOfMonth = convertDDay(dDateTime.getDay());
        if (dayOfMonth == null) {
            validationMessages.add("DDateTime 'day' field is missing.");
        }

        Integer hour = convertDHour(dDateTime.getHour());
        if (hour == null) {
            validationMessages.add("DDateTime 'hour' field is missing.");
        }

        Integer minute = convertDMinute(dDateTime.getMinute());
        if (minute == null) {
            validationMessages.add("DDateTime 'minute' field is missing.");
        }

        SecondNanos secondNanos = convertDSecond(dDateTime.getSecond());
        if (secondNanos == null) {
            validationMessages.add("DDateTime 'second' (millisecond of minute) field is missing.");
        }


        ZoneOffset offset = convertDOffset(dDateTime.getOffset());

        if (year != null && month != null && dayOfMonth != null && hour != null && minute != null
                && secondNanos != null) {
            OffsetDateTime odt = OffsetDateTime.of(year, month, dayOfMonth, hour, minute, secondNanos.secondOfMinute(),
                    secondNanos.nanoOfSecond(), offset);
            return odt.toInstant().toEpochMilli();
        }
        return null;
    }

    public static Integer convertDYear(DYear dYear) {
        if (dYear == null)
            return null;
        long value = dYear.getValue();
        // 0 represents unknown year
        if (value == 0)
            return null;
        return (int) value;
    }

    public static Integer convertDMonth(DMonth dMonth) {
        if (dMonth == null)
            return null;
        long value = dMonth.getValue();
        // 0 Represents unknown month
        if (value == 0)
            return null;
        return (int) value;
    }

    public static Integer convertDDay(DDay dDay) {
        if (dDay == null)
            return null;
        long value = dDay.getValue();
        // 0 represents unknown day
        if (value == 0)
            return null;
        return (int) value;
    }

    public static Integer convertDHour(DHour dHour) {
        if (dHour == null)
            return null;
        long value = dHour.getValue();
        // Per J2735 (2024) sec 7.34: 31 represents unknown hours and the values 24-30 are used by some applications
        // to represent schedule adherence.
        // But they are omitted here for use by the RTCM timestamp.
        if (value > 23)
            return null;
        return (int) value;
    }

    public static Integer convertDMinute(DMinute dMinute) {
        if (dMinute == null)
            return null;
        long value = dMinute.getValue();
        // Per J2735 (2024) sec 7.37: 60 represents unknown hours
        if (value == 60)
            return null;
        return (int) value;
    }

    /**
     * Millisecond of minute.
     *
     * @param dSecond DE_DSecond
     * @return milliseconds or null if absent
     */
    public static SecondNanos convertDSecond(DSecond dSecond) {
        if (dSecond == null)
            return null;
        long value = dSecond.getValue();
        // Per J2735 (2024) sec. 7.43: 65535 represents unavailable, and values 61000 and above are reserved.
        if (value >= 61000)
            return null;
        final int secondOfMinute = Math.floorDiv((int) value, 1000);
        final int milliOfSecond = (int) value - (secondOfMinute * 1000);
        final int nanoOfSecond = milliOfSecond * 1000000;
        return new SecondNanos(secondOfMinute, nanoOfSecond);
    }

    public record SecondNanos(Integer secondOfMinute, Integer nanoOfSecond) {

    }


    /**
     * Convert time zone offset
     *
     * @param dOffset Offset in minutes
     * @return Java ZoneOffset
     */
    public static ZoneOffset convertDOffset(DOffset dOffset) {
        if (dOffset == null)
            return ZoneOffset.UTC;
        final int value = (int) dOffset.getValue();
        int offsetHours = Math.floorDiv(value, 60);
        int offsetMinutes = value - (offsetHours * 60);
        return ZoneOffset.ofHoursMinutes(offsetHours, offsetMinutes);
    }

    /**
     * Converts a J2735 LaneWidth value to meters. Providing a range of 0 to + 327.67 m meters.
     *
     * @param j2735Elev J2735 lane width value.
     * @return Lane width in meters, or null if unavailable.
     */
    public static Double convertLaneWidth(LaneWidth laneWidth) {
        if (laneWidth == null) {
            return null;
        }
        return laneWidth.getValue() * 1e-2d;
    }

    /**
     * Calculates the lane width based on the lane width meter and lane width offset in centimeters.
     *
     * @param laneWidthMeter J2735 lane width meter value.
     * @param laneWidthOffsetCm J2735 lane width offset in centimeters.
     * @return Lane width in meters.
     */
    public static Double calculateLaneWidthOffset(Double laneWidthMeter, long laneWidthOffsetCm) {
        Double returnValue = null;
        returnValue = laneWidthMeter + (laneWidthOffsetCm * 1e-2);
        return returnValue;
    }

    /**
     * Calculates the elevation based on the elevation meter and elevation offset in centimeters.
     *
     * @param elevationMeter J2735 elevation meter value.
     * @param elevationOffsetCm J2735 elevation offset in centimeters.
     * @return Elevation in meters.
     */
    public static Double calculateElevationOffset(Double elevationMeter, long elevationOffsetCm) {
        Double returnValue = null;
        if (elevationMeter != null) {
            returnValue = elevationMeter + (elevationOffsetCm * 1e-2);
        }
        return returnValue;
    }

    /**
     * Parse heading sectors directly from Asn1Bitstring. Each bit represents a 22.5-degree sector starting from North
     * (0°) and moving clockwise.
     *
     * @param directionBitstring The Asn1Bitstring direction field
     * @return Array of active sector bit positions
     */
    public static int[] parseHeadingSectorsFromBitstring(Asn1Bitstring directionBitstring) {
        if (directionBitstring == null) {
            return new int[0];
        }

        List<Integer> activeSectors = new ArrayList<>();

        // Check each bit up to the maximum number of heading sectors or the bitstring size
        int bitstringSize = directionBitstring.size();
        int maxBits = Math.min(MAX_HEADING_SECTORS, bitstringSize);

        for (int bit = 0; bit < maxBits; bit++) {
            if (directionBitstring.get(bit)) {
                activeSectors.add(bit);
            }
        }

        return activeSectors.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Parse heading sectors as ranges from Asn1Bitstring, merging adjacent sectors into continuous ranges. Each bit
     * represents a 22.5-degree sector starting from North (0°) and moving clockwise.
     *
     * @param directionBitstring The Asn1Bitstring direction field
     * @return Array of heading ranges, where each range is represented as [startBit, endBit] (inclusive)
     */
    public static int[][] parseHeadingSectorsAsRanges(Asn1Bitstring directionBitstring) {
        if (directionBitstring == null) {
            return new int[0][];
        }

        List<Integer> activeSectors = new ArrayList<>();

        // Check each bit up to the maximum number of heading sectors or the bitstring size
        int bitstringSize = directionBitstring.size();
        int maxBits = Math.min(MAX_HEADING_SECTORS, bitstringSize);

        for (int bit = 0; bit < maxBits; bit++) {
            if (directionBitstring.get(bit)) {
                activeSectors.add(bit);
            }
        }

        if (activeSectors.isEmpty()) {
            return new int[0][];
        }

        // Group adjacent sectors into ranges
        List<int[]> ranges = new ArrayList<>();
        int rangeStart = activeSectors.get(0);
        int rangeEnd = rangeStart;

        for (int i = 1; i < activeSectors.size(); i++) {
            int currentBit = activeSectors.get(i);

            if (currentBit == rangeEnd + 1) {
                // Adjacent sector, extend the current range
                rangeEnd = currentBit;
            } else {
                // Non-adjacent sector, save the current range and start a new one
                ranges.add(new int[] {rangeStart, rangeEnd});
                rangeStart = currentBit;
                rangeEnd = currentBit;
            }
        }

        // Add the last range
        ranges.add(new int[] {rangeStart, rangeEnd});

        return ranges.toArray(new int[0][]);
    }


    /**
     * Convert sector bit position to heading degrees.
     *
     * @param sectorBit The sector bit position (0-15)
     * @return Heading in degrees
     */
    public static double sectorBitToHeading(int sectorBit) {
        return sectorBit * HEADING_SECTOR_DEGREES;
    }

    /**
     * Convert a range of sector bits to heading degrees and range.
     *
     * @param startBit The starting sector bit position (0-15)
     * @param endBit The ending sector bit position (0-15, inclusive)
     * @return Array containing [heading, range] in degrees
     */
    public static double[] sectorRangeToHeadingAndRange(int startBit, int endBit) {
        double startHeading = sectorBitToHeading(startBit);
        double endHeading = sectorBitToHeading(endBit);

        // Calculate the center heading of the range
        double centerHeading = (startHeading + endHeading) / 2.0;

        // Calculate the total range (number of sectors * 22.5 degrees)
        int numberOfSectors = endBit - startBit + 1;
        double totalRange = numberOfSectors * HEADING_SECTOR_DEGREES;

        return new double[] {centerHeading, totalRange};
    }

    /**
     * Get the range for a heading sector (22.5 degrees).
     *
     * @return Range in degrees (22.5)
     */
    public static double getHeadingSectorRange() {
        return HEADING_SECTOR_RANGE; // Each sector is exactly 22.5° wide
    }

    /**
     * Convert a radius value from the specified distance units to meters.
     * 
     * @param radius The radius value in the specified units
     * @param units The distance units enum value
     * @return Radius converted to meters, or null if units is null
     */
    public static Double convertRadiusToMeters(long radius, DistanceUnits units) {
        if (units == null) {
            return null;
        }

        switch (units) {
            case CENTIMETER:
                return radius * 0.01; // 1 cm = 0.01 m
            case CM2_5:
                return radius * 0.025; // 1 cm2-5 = 0.025 m (2.5 cm)
            case DECIMETER:
                return radius * 0.1; // 1 dm = 0.1 m
            case METER:
                return (double) radius; // 1 m = 1 m
            case KILOMETER:
                return radius * 1000.0; // 1 km = 1000 m
            case FOOT:
                return radius * 0.3048; // 1 ft = 0.3048 m
            case YARD:
                return radius * 0.9144; // 1 yd = 0.9144 m
            case MILE:
                return radius * 1609.344; // 1 mi = 1609.344 m
            default:
                // Default to meters if unknown unit
                return (double) radius;
        }
    }
}
