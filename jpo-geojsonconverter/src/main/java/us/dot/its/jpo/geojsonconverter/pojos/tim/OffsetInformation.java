package us.dot.its.jpo.geojsonconverter.pojos.tim;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data structure to hold elevation and lane width offset information.
 */
@Data
@AllArgsConstructor
public class OffsetInformation {
    private final List<Long> elevationOffsets;
    private final List<Long> laneWidthOffsets;
}
