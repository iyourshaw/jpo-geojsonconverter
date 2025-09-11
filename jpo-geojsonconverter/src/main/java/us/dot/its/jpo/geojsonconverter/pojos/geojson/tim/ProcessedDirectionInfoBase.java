package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for TIM direction information.
 * <p>
 * type - The type of direction information (directionality, heading)
 * <p>
 * directionality - The directionality (forward, reverse, both, unknown)
 */
@Data
@Generated
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public abstract class ProcessedDirectionInfoBase {
    private ProcessedDirectionType directionType;
}
