package us.dot.its.jpo.geojsonconverter.pojos.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedTimFeatureCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;

/**
 * Represents a processed TIM (Traveler Information Message) message.
 * <p>
 * schemaVersion - The jpo-geojsonconverter schema version for ProcessedTim
 * <p>
 * messageType - TIM
 * <p>
 * odeReceivedAt - The time the origin OdeTimJson message was received by the ODE, in UTC
 * <p>
 * originIp - The IP address the origin OdeTimJson message was received from
 * <p>
 * asn1 - The ASN.1 encoded string of the origin J2735 TIM message
 * <p>
 * msgCnt - The message count of the TIM
 * <p>
 * timeStamp - The timestamp of the TIM message in UTC
 * <p>
 * packetId - The packet ID of the TIM message
 * <p>
 * gnisRegionId - The GNIS region ID extracted from packet ID
 * <p>
 * location - GeoJSON Point representing the center location for MongoDB 2D sphere indexing
 * <p>
 * compliance - List of compliance validation results
 * <p>
 * dataFrameFeatureCollection - GeoJSON FeatureCollection containing the TIM data frames
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTim {
    // Default schemaVersion is -1 for older messages that lack a schemaVersion value
    private int schemaVersion = -1;
    private String messageType = "TIM";
    private String odeReceivedAt;
    private String originIp;
    private String asn1;
    private Integer msgCnt;
    private ZonedDateTime timeStamp;
    private String packetId;
    private Integer gnisRegionId;
    private Point location;
    private List<ProcessedCompliance> compliance;
    private ProcessedTimFeatureCollection dataFrameFeatureCollection;

    @Override
    public String toString() {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        String testReturn = "";
        try {
            testReturn = (mapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return testReturn;
    }
}
