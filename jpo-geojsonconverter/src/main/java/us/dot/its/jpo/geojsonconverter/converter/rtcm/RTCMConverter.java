package us.dot.its.jpo.geojsonconverter.converter.rtcm;


import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.CoordinateXY;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.RTCMcorrections.RTCM_Revision;
import us.dot.its.jpo.asn.j2735.r2024.RTCMcorrections.RTCMcorrections;
import us.dot.its.jpo.asn.j2735.r2024.RTCMcorrections.RTCMcorrectionsMessageFrame;
import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.DecodedRTCMmessage;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.ProcessedRTCM;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.RTCMProperties;
import us.dot.its.jpo.geojsonconverter.standards.RtcmStandard;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;

import java.time.Instant;
import java.util.*;

import static us.dot.its.jpo.geojsonconverter.converter.FieldConversions.*;


/**
 * Encapsulate methods for converting, decoding, and validating RTCM MessageFrames.
 */
@Component
@Slf4j
public class RTCMConverter {

    private final RTCMDecoder decoder;
    private final RtcmStandard rtcmStandardVersion;
    private final String spec;
    private final String conformanceIssue;

    private final String TYPE = "type";

    @Autowired
    public RTCMConverter(RTCMDecoder decoder, GeoJsonConverterProperties properties) {
        this.decoder = decoder;
        this.rtcmStandardVersion = properties.getRtcmStandardVersion();
        if (rtcmStandardVersion != null) {
            this.spec = rtcmStandardVersion.getShortName();
        } else {
            log.error("properties.rtcmStandardVersion is null");
            this.spec = "unknown standard version";
        }
        this.conformanceIssue = spec + " conformance issue: ";
    }

    /**
     * Converts a J2735 RTCMcorrections Message Frame to a ProcessedRTCM.
     * Uses the current system time to determine what week it is for the GPS timestamp.
     * @param messageFrame The RTCM message frame
     * @return The processed RTCM
     */
    public ProcessedRTCM processRTCM(final RTCMcorrectionsMessageFrame messageFrame) {
        return processRTCM(Instant.now(), messageFrame);
    }

    /**
     * Converts a J2735 RTCMcorrections Message Frame to a ProcessedRTCM
     * @param referenceTimestamp Reference timestamp for the sole purpose of determining the
     *                           gps week to use with the time-of-week timestamp.
     * @param rtcmFrame The RTCM message frame
     * @return The processed RTCM
     */
    @SuppressWarnings("java:S3776") // Ignore Sonar 'cognitive complexity' warning
    public ProcessedRTCM processRTCM(Instant referenceTimestamp,
                                     final RTCMcorrectionsMessageFrame rtcmFrame) {
        var properties = new RTCMProperties();

        if (rtcmFrame == null) {
            log.error("RTCM frame is null");
            return new ProcessedRTCM(null, properties);
        }

        RTCMcorrections rtcm = rtcmFrame.getValue();

        if (rtcm == null) {
            log.error("RTCM corrections is null");
            return new ProcessedRTCM(null, properties);
        }

        properties.setMsgCnt((int)rtcm.getMsgCnt().getValue());

        final String rev = rtcm.getRev().getName();
        properties.setRev(rev);
        if (!RTCM_Revision.RTCMREV3.getName().equals(rev)) {
            // CTI 4501 v01.01, Sec. 4.3.3.5.1: Revision 3 is required
            log.debug("Invalid rev: {}", rev);
            properties.addValidationMessage(
                    conformanceIssue + "The RTCMcorrections 'rev' (DE_RTCM_Revision) is not 'rtcmRev3'.");
        }

        // CTI 4501 v01.01, Sec. 4.3.3.5.1: optional timestamp is forbidden
        MinuteOfTheYear timestamp = rtcm.getTimeStamp();
        if (timestamp != null) {

            properties.addValidationMessage(
              conformanceIssue + "The RTCMcorrections optional field 'timestamp' (DE_MinuteOfTheYear) is " +
                      "present.  It is forbidden by CTI-4501.");
        }

        // CTI 4501 v01.01, Sec. 4.3.3.5.1: optional RTCMheader is forbidden
        RTCMheader header = rtcm.getRtcmHeader();
        if (header != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The RTCMcorrections optional field 'rtcmHeader' (DF_RTCMheader) is" +
                            " present.  It is forbidden by CTI-4501.");
        }

        final RTCMmessageList messageList = rtcm.getMsgs();
        if (messageList != null) {
            decodeMessages(properties, messageList);
        } else {
            log.info("RTCM messageList is null");
        }



        // FullPositionVector is mandatory in CTI 4501
        // See CTI 4501 v01.01, Sec. 4.3.3.1.1.11, Table 11
        if (rtcmStandardVersion == RtcmStandard.CTI4501_V1) {
            FullPositionVector anchor = rtcm.getAnchorPoint();
            if (anchor != null) {
                processFullPosition(properties, anchor);
            } else {
                properties.addValidationMessage(
                        conformanceIssue + "The RTCMcorrections 'anchorPoint' (DF_FullPositionVector) is missing.");
            }
        } else if (rtcmStandardVersion == RtcmStandard.J3258_DRAFT){
            // FullPositionVector is not used in J3258
            FullPositionVector anchor = rtcm.getAnchorPoint();
            if (anchor != null) {
                properties.addValidationMessage(
                        conformanceIssue + "The RTCMcorrections 'anchorPoint' (DF_FullPositionVector) is " +
                                "present. It is disallowed by J3258.");
            }
            // In J3258, get coordinates and timestamp from the decodes messages instead
            // of from full position vector
            processCoordinatesAndTimestamp(referenceTimestamp, properties);
        }



        // CTI 4501 v01.01, Sec. 4.3.3.5.1: optional regional extension is forbidden
        RTCMcorrections.SequenceOfRegional regionalSeq = rtcm.getRegional();
        if (regionalSeq != null && !regionalSeq.isEmpty()) {
            properties.addValidationMessage(
                    conformanceIssue + "The RTCMcorrections has regional extensions present which are " +
                            "forbidden by CTI-4501.");
        }

        if (!properties.getValidationMessages().isEmpty()) {
            properties.setCti4501Conformant(false);
        }

        Point geometry = new Point(properties.getLongitude(), properties.getLatitude());
        return new ProcessedRTCM(geometry, properties);
    }


    /**
     * CTI 4501 v01.01, Sec. 4.3.3.5.1:
     * DF_FullPositionVector shall include utcTime, latitude, longitude, and elevation.
     * It shall not include any other fields.
     * @param properties The ProcessedRTCM
     * @param anchor The anchorPoint
     */
    private void processFullPosition(RTCMProperties properties, FullPositionVector anchor) {

        log.info("FullPositionVector: {}", anchor);

        DDateTime utcTime = anchor.getUtcTime();
        List<String> utcValidations = new ArrayList<>();
        Long timestamp = convertDDateTime(utcValidations, utcTime);
        properties.setUtcTime(timestamp);
        for (String utcValidation : utcValidations) {
            properties.addValidationMessage(conformanceIssue + "anchorPoint (DF_FullPositionVector) " +
                    "'utcTime' field: " + utcValidation);
        }

        Longitude lon = anchor.getLong_();
        if (lon != null) {
            properties.setLongitude(convertLong(lon.getValue()));
        }
        if (properties.getLongitude() == null){
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'long' field (DE_Longitude) is missing.");
        }

        Latitude lat = anchor.getLat();
        if (lat != null) {
            properties.setLatitude(convertLat(lat.getValue()));
        }
        if (properties.getLatitude() == null){
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'lat' field (DE_Latitude) is missing.");
        }

        Elevation elevation = anchor.getElevation();
        if (elevation != null) {
            properties.setElevation(convertElevation(elevation.getValue()));
        }
        if (properties.getElevation() == null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'elevation' field (DE_Elevation) is missing.");
        }

        // Check for extras that should not be present in full position vector
        if (anchor.getHeading() != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'heading' field is present " +
                            "but should not be included.");
        }

        if (anchor.getSpeed() != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'speed' field is present " +
                            "but should not be included.");
        }

        if (anchor.getPosAccuracy() != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'posAccuracy' field is " +
                            "present but should not be included.");
        }

        if (anchor.getTimeConfidence() != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'timeConfidence' field is " +
                            "present but should not be included.");
        }

        if (anchor.getPosConfidence() != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'posConfidence' field is " +
                            "present but should not be included.");
        }

        if (anchor.getSpeedConfidence() != null) {
            properties.addValidationMessage(
                    conformanceIssue + "The anchorPoint (DF_FullPositionVector) 'speedConfidence' field is " +
                            "present but should not be included.");
        }

    }

    /**
     * For J3258, get lat/long and timestamp from the x,y,z and tow fields of the
     * decoded messages.
     * @param properties Properties assumed to include decoded messages
     */
    private void processCoordinatesAndTimestamp(Instant referenceTimestamp,
                                                RTCMProperties properties) {
        processCoordinates(properties);
        processTimestamp(referenceTimestamp, properties);
    }

    private void processCoordinates(RTCMProperties properties) {
        // Find station ref message type 1005 or 1006
        Optional<DecodedRTCMmessage> stationRefOpt = properties.getMessages().stream().filter(message -> {
            JsonNode decodedMessage = message.getDecodedMessage();
            if (decodedMessage != null && decodedMessage.hasNonNull(TYPE)) {
                int messageType = decodedMessage.get(TYPE).asInt();
                return messageType == 1005 || messageType == 1006;
            }
            log.warn("type missing from decoded message {}", decodedMessage);
            return false;
        }).findFirst();

        if (stationRefOpt.isEmpty()) {
            log.error("No 1005 or 1006 type message is present, can't get rtcm coordinates");
            return;
        }

        DecodedRTCMmessage stationRef = stationRefOpt.get();
        JsonNode nodes = stationRef.getDecodedMessage();
        JsonNode xNode = nodes.get("x");
        JsonNode yNode = nodes.get("y");
        JsonNode zNode = nodes.get("z");
        if (xNode == null || yNode == null || zNode == null) {
            log.error("One or more coordinates are missing.");
            return;
        }
        if (!xNode.isNumber() || !yNode.isNumber() || !zNode.isNumber()) {
            log.error("Invalid coordinates format x,y,z: {}, {}, {}", xNode, yNode, zNode);
            return;
        }
        double x = xNode.doubleValue();
        double y = yNode.doubleValue();
        double z = zNode.doubleValue();
        Optional<CoordinateXY> lonLatOpt = gpsXyzToWgs84LonLat(x, y, z);
        if (lonLatOpt.isEmpty()) {
            log.error("Couldn't convert xyz coords to lon-lat");
            return;
        }
        CoordinateXY lonLat = lonLatOpt.get();
        properties.setLongitude(lonLat.getX());
        properties.setLatitude(lonLat.getY());
    }

    /**
     * Convert GPS Earth Centered (ECEF) coordinates to WGS-84 lon-lat coords
     */
    private Optional<CoordinateXY> gpsXyzToWgs84LonLat(double x, double y, double z) {
        try {
            MathTransform ecefToLonLatHeight =
                    CRS.findMathTransform(DefaultGeocentricCRS.CARTESIAN, DefaultGeographicCRS.WGS84_3D);
            double[] srcPts = {x, y, z};
            double[] dstPts = new double[3];
            ecefToLonLatHeight.transform(srcPts, 0, dstPts, 0, 1);
            // We don't care about elevation for now, discard it
            return Optional.of(new CoordinateXY(dstPts[0], dstPts[1]));
        } catch (FactoryException | TransformException e) {
            log.error("Unable to convert ECEF ({}, {}, {}) to WGS-84 lon/lat", x, y, z, e);
            return Optional.empty();
        }
    }

    /**
     * Process GPS timestamp to get utc timestamp
     * @param referenceTime - Reference timestamp to figure out what gps week it is
     * @param properties rtcm properties, including decoded payloads
     */
    private void processTimestamp(Instant referenceTime, RTCMProperties properties) {
        // Find the GPS MSM message (1071-1077)
        // Timestamp calc will only work with GPS which is supposed to be there, so don't try to
        // deal with any other constellations.
        Optional<DecodedRTCMmessage> msmOpt = properties.getMessages().stream().filter(message -> {
            JsonNode decodedMessage = message.getDecodedMessage();
            if (decodedMessage != null && decodedMessage.hasNonNull(TYPE)) {
                int messageType = decodedMessage.get(TYPE).asInt();
                return messageType >= 1071 && messageType <= 1077;
            }
            log.warn("type missing from decoded message {}", message.getDecodedMessage());
            return false;
        }).findFirst();

        if (msmOpt.isEmpty()) {
            log.error("No GPS MSM message is present, can't get rtcm timestamp");
            return;
        }

        DecodedRTCMmessage msm = msmOpt.get();
        JsonNode nodes = msm.getDecodedMessage();
        JsonNode towNode = nodes.get("tow");
        if (towNode == null) {
            log.error("tow node not found in MSM message, can't get rtcm timestamp");
            return;
        }
        if (!towNode.isLong()) {
            log.error("tow node {} in MSM is not long int, can't get rtcm timestamp", towNode);
        }
        long tow = towNode.asLong();
        long gpsWeek = getGpsWeek(referenceTime);
        long utcMillis = convertGpsTimeOfWeekToEpochMillis(gpsWeek, tow);
        properties.setUtcTime(utcMillis);
    }

    private static final Instant GPS_EPOCH = Instant.parse("1980-01-06T00:00:00Z");
    private static final long MILLIS_PER_GPS_WEEK = 604_800_000L; // 7 days in ms

    // Effective instant of the most recent GPS<->UTC leap second.
    // Ref https://en.wikipedia.org/wiki/Leap_second
    private static final long LATEST_LEAP_SECOND_EPOCH_MILLIS =
            Instant.parse("2017-01-01T00:00:00Z").toEpochMilli();
    // Number of leap seconds since start of GPS time in 1980
    private static final int CURRENT_GPS_UTC_LEAP_SECONDS = 18;

    /**
     * Number of GPS<->UTC leap seconds in effect at the given approximate UTC instant.
     */
    private static int gpsUtcLeapSecondOffset(long approxUtcEpochMilli) {
        if (approxUtcEpochMilli < LATEST_LEAP_SECOND_EPOCH_MILLIS) {
            // Log an error if the timestamp is too old.
            log.error(
                    "Timestamp {} is too old. GPS<->UTC leap second offset is not known for timestamps before {}," +
                            "using {} which is not correct.",
                    Instant.ofEpochMilli(approxUtcEpochMilli),
                    Instant.ofEpochMilli(LATEST_LEAP_SECOND_EPOCH_MILLIS),
                    CURRENT_GPS_UTC_LEAP_SECONDS);
        }
        return CURRENT_GPS_UTC_LEAP_SECONDS;
    }

    /**
     * Convert GPS time of week timestamp to epoch millisecond timestamp
     * @param gpsWeek GPS week number
     * @param tow time of week
     * @return timestamp in epoch milliseconds
     */
    private long convertGpsTimeOfWeekToEpochMillis(long gpsWeek, long tow) {
        long gpsEpochMillis = GPS_EPOCH.toEpochMilli() + gpsWeek * MILLIS_PER_GPS_WEEK + tow;
        // we need to care about leap seconds because the offset is 18 seconds from utc as of 2026
        return gpsEpochMillis - gpsUtcLeapSecondOffset(gpsEpochMillis) * 1000L;
    }

    /**
     * Get the GPS week of the instant
     * @param instant timestamp
     * @return GPS week number
     */
    private long getGpsWeek(Instant instant) {
        long utcEpochMillis = instant.toEpochMilli();
        long gpsEpochMillis = utcEpochMillis + gpsUtcLeapSecondOffset(utcEpochMillis) * 1000L;
        return (gpsEpochMillis - GPS_EPOCH.toEpochMilli()) / MILLIS_PER_GPS_WEEK;
    }

    @SuppressWarnings("java:S3776") // Ignore Sonar 'cognitive complexity' warning
    private void decodeMessages(RTCMProperties properties, RTCMmessageList messageList) {
        List<DecodedRTCMmessage> decodedMessages = new ArrayList<>();
        Set<Integer> types = new LinkedHashSet<>();
        final String STATION_ID = "station_id";

        if (rtcmStandardVersion == RtcmStandard.CTI4501_V1) {
            // CTI-4501 v1: Each item in the SEQUENCE-OF has a single message
            for (RTCMmessage message : messageList) {
                var decodedMessage = new DecodedRTCMmessage();
                decodedMessage.setHex(message.getValue());
                decodedMessages.add(decodedMessage);
                JsonNode node = decoder.decodeRtcm(message.getOctets());
                decodedMessage.setDecodedMessage(node);

                if (node.has("type")) {
                    types.add(node.get("type").asInt());
                }

                if (node.has(STATION_ID)) {
                    properties.setStationId(node.get(STATION_ID).asInt());
                }
            }
        } else if (rtcmStandardVersion == RtcmStandard.J3258_DRAFT) {
            // J3258 draft: The messages need to be combined together then split
            // based on the length determinants within them, because there isn't
            // a 1-1 with items in the SEQUENCE-OF
            try {
                List<byte[]> splitMessages = decoder.splitMessages(messageList);
                for (byte[] message : splitMessages) {
                    var decodedMessage = new DecodedRTCMmessage();
                    decodedMessage.setHex(HexFormat.of().formatHex(message));
                    decodedMessages.add(decodedMessage);
                    JsonNode node = decoder.decodeRtcm(message);
                    decodedMessage.setDecodedMessage(node);

                    if (node.has("type")) {
                        types.add(node.get("type").asInt());
                    }

                    if (node.has(STATION_ID)) {
                        properties.setStationId(node.get(STATION_ID).asInt());
                    }
                }
            } catch (RTCMDecodeException e) {
                properties.addValidationMessage("Error decoding RTCM messages: " + e.getMessage());
            }
        }

        properties.setMessageTypes(types);
        if (types.isEmpty()) {
            properties.addValidationMessage("No RTCM message types found.");
        }
        if (properties.getStationId() == null) {
            properties.addValidationMessage("RTCM station ID not found.");
        }

        properties.setMessages(decodedMessages);

        // CTI 4501 v01.01, Sec. 4.3.3.5.1. Rules for grouping message types.
        // See also https://www.use-snip.com/kb/knowledge-base/an-rtcm-message-cheat-sheet/
        //
        // Descriptors and system parameters 1005, 1006, 1013, 1033 should be grouped together.
        //
        // MSM messages should be grouped together.
        // MSM 4 support is mandatory, MSM5,6,7 are allowed.
        // Must support GPS and one other constellation (GLONASS, Galileo, or BeiDou)
        //
        // MSM4 - 1074 (GPS), 1084 (GLONASS), 1094 (Galileo), 1104 (SBAS), 1114 (QZSS), 1124 (BeiDou)
        // MSM5 - 1075, 1085, 1095, 1195, 1115, 1125
        // MSM6 - 1076, 1086, 1096, 1196, 1116, 1126
        // MSM7 - 1077, 1087, 1097, 1197, 1117, 1127
        //
        // Groups should not be mixed.
        //
        // These grouping rules are CTI-4501 specific and don't apply to J3258.
        if (rtcmStandardVersion == RtcmStandard.CTI4501_V1) {
            LinkedHashSet<String> categories = new LinkedHashSet<>();
            boolean isDescriptors = categorize(DESCRIPTOR_TYPES, types);
            if (isDescriptors) {
                categories.add("Descriptor types: 1005, 1006, 1013, 1033");
            }
            boolean isMsm4 = categorize(MSM4_TYPES, types);
            if (isMsm4) {
                categories.add("MSM4");
            }
            boolean isMsm5 = categorize(MSM5_TYPES, types);
            if (isMsm5) {
                categories.add("MSM5");
            }
            boolean isMsm6 = categorize(MSM6_TYPES, types);
            if (isMsm6) {
                categories.add("MSM6");
            }
            boolean isMsm7 = categorize(MSM7_TYPES, types);
            if (isMsm7) {
                categories.add("MSM7");
            }

            log.debug("Categories: {}", categories);

            if (categories.isEmpty()) {
                log.debug("No CTI 4501 categories found.");
                properties.addValidationMessage(
                        conformanceIssue + "None of the message types are in categories mentioned in CTI-4501");
            }
            if (categories.size() > 1) {
                log.debug("Multiple CTI 4501 categories found.");
                properties.addValidationMessage(
                        String.format(
                                conformanceIssue + "The message list contains message types from more than" +
                                        " one category: %s", categories));
            }
            if (categories.size() == 1) {
                String category = categories.iterator().next();
                checkMsmTypes(category, types, "MSM4", MSM4_GPS, properties);
                checkMsmTypes(category, types, "MSM5", MSM5_GPS, properties);
                checkMsmTypes(category, types, "MSM6", MSM6_GPS, properties);
                checkMsmTypes(category, types, "MSM7", MSM7_GPS, properties);
            }
        }
    }

    private void checkMsmTypes(String category, Set<Integer> types, String msmVersion, int gpsType, RTCMProperties properties) {
        if (msmVersion.equals(category)) {
            if (!types.contains(gpsType)) {
                properties.addValidationMessage(
                        String.format("CTI-4501 conformance issue: The message list contains %s messages but does " +
                                "not contain an %s GPS message: %s", msmVersion, msmVersion, gpsType));
            } else if (types.size() == 1) {
                properties.addValidationMessage(
                        String.format("CTI-4501 conformance issue: The message list contains an %s GPS message, " +
                                "but no %s messages for other constellations", msmVersion, msmVersion));
            }
        }
    }

    private final static Set<Integer> DESCRIPTOR_TYPES = Set.of(1005, 1006, 1013, 1033);
    private final static int MSM4_GPS = 1074;
    private final static int MSM5_GPS = 1075;
    private final static int MSM6_GPS = 1076;
    private final static int MSM7_GPS = 1077;
    private final static Set<Integer> MSM4_TYPES = Set.of(MSM4_GPS, 1084, 1094, 1104, 1114, 1124);
    private final static Set<Integer> MSM5_TYPES = Set.of(MSM5_GPS, 1085, 1095, 1195, 1115, 1125);
    private final static Set<Integer> MSM6_TYPES = Set.of(MSM6_GPS, 1086, 1096, 1196, 1116, 1126);
    private final static Set<Integer> MSM7_TYPES = Set.of(MSM7_GPS, 1087, 1097, 1197, 1117, 1127);

    private boolean categorize(Set<Integer> category, Set<Integer> types) {
        Set<Integer> categorized = new LinkedHashSet<>(types);
        categorized.retainAll(category);
        return !categorized.isEmpty();
    }


    /**
     * Add JSON schema validation results for J2735 and Metadata validation.
     * @param properties The ProcessedRTCM to add validation messages to
     * @param validatorResult the schema validator result
     */
    public void jsonValidation(RTCMProperties properties, JsonValidatorResult validatorResult) {
        var messages = new ArrayList<ProcessedValidationMessage>();
        for (Exception exception : validatorResult.getExceptions()) {
            var msg = new ProcessedValidationMessage();
            msg.setMessage(exception.getMessage());
            msg.setException(Arrays.toString(exception.getStackTrace()));
            messages.add(msg);
        }
        for (Error vm : validatorResult.getValidationMessages()) {
            var msg = new ProcessedValidationMessage();
            msg.setMessage(vm.getMessage());
            final var schemaLocation = vm.getSchemaLocation();
            if (schemaLocation != null) {
                msg.setSchemaPath(schemaLocation.toString());
            } else {
                log.warn("validationMessage.schemaLocation is null");
            }
            final var evaluationPath = vm.getEvaluationPath();
            if (evaluationPath != null) {
                msg.setJsonPath(vm.getEvaluationPath().toString());
            }
            messages.add(msg);
        }
        properties.addValidationMessages(messages);
    }
}
